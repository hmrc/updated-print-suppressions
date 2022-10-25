/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.ups.scheduled.jobs

import akka.actor.{Actor, Timers}
import com.google.inject.{Inject, Singleton}

import java.util.concurrent.TimeUnit
import play.api.{Configuration, Logger}
import uk.gov.hmrc.ups.repository.UpdatedPrintSuppressionsDatabase
import uk.gov.hmrc.ups.scheduled.jobs.RemoveOlderCollectionsJob.{PeriodicTick, PeriodicTimerKey}
import uk.gov.hmrc.ups.scheduled.{Failed, RemoveOlderCollections, Succeeded}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationLong, FiniteDuration}

object RemoveOlderCollectionsJob {
  case object PeriodicTimerKey
  case object PeriodicTick
}
@Singleton
class RemoveOlderCollectionsJob @Inject()(configuration: Configuration,
                                            updatedPrintSuppressionsDatabase: UpdatedPrintSuppressionsDatabase)
  extends Actor with Timers with RemoveOlderCollections {

  val logger: Logger = Logger(this.getClass)

  def name: String = "removeOlderCollections"

  def repository: UpdatedPrintSuppressionsDatabase = updatedPrintSuppressionsDatabase

  private lazy val durationInDays = {
    val days = configuration
      .getOptional[Int](s"$name.durationInDays")
      .getOrElse(throw new IllegalStateException(s"Config key $name.durationInDays missing"))
    FiniteDuration(days, TimeUnit.DAYS)
  }

  override def preStart(): Unit = {

    val initialDelay = Duration(configuration.getMillis(s"scheduling.$name.initialDelay"), TimeUnit.MILLISECONDS)
    val interval     = Duration(configuration.getMillis(s"scheduling.$name.interval"), TimeUnit.MILLISECONDS)

    timers.startTimerWithFixedDelay(PeriodicTimerKey, PeriodicTick, initialDelay, interval)

    logger.warn(s"$name job starting, initialDelay: $initialDelay, interval: $interval")
    super.preStart()
  }

  override def receive: Receive = {
    case PeriodicTick => {
      removeOlderThan(durationInDays).map { totals =>
        (totals.failures ++ totals.successes).foreach {
          case Succeeded(collectionName) =>
            logger.info(s"successfully removed collection $collectionName older than $durationInDays in $name job")

          case Failed(collectionName, ex) =>
            val msg = s"attempted to removed collection $collectionName and failed in $name job"
            ex.fold(logger.error(msg)) {
              logger.error(msg, _)
            }
        }
        val text =
          s"""Completed $name with:
             |- failures on collections [${totals.failures.map(_.collectionName).mkString(",")}]
             |- collections [${totals.successes.map(_.collectionName).sorted.mkString(",")}] successfully removed
             |""".stripMargin
        logger.warn(text)
        Result(text)
      }
    }
  }

  override def postStop(): Unit = {
    logger.warn(s"Job $name stopped")
    super.postStop()
  }
}
