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

import akka.actor.{ Actor, Timers }
import com.google.inject.{ Inject, Singleton }
import play.api.{ Configuration, Logger }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{ LockService, MongoLockRepository }
import uk.gov.hmrc.ups.scheduled.PreferencesProcessor
import uk.gov.hmrc.ups.scheduled.jobs.UpdatedPrintSuppressionJob.{ PeriodicTick, PeriodicTimerKey }

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

case class Result(message: String)

@Singleton
class UpdatedPrintSuppressionJob @Inject()(configuration: Configuration, mongoLockRepository: MongoLockRepository, preferencesProcessor: PreferencesProcessor)
    extends Actor with Timers {

  val logger: Logger = Logger(this.getClass)
  val name: String = "updatedPrintSuppressions"

  val ls: LockService =
    LockService(mongoLockRepository, s"$name-scheduled-job-lock", Duration(configuration.getMillis(s"$name.releaseLockAfter"), TimeUnit.MILLISECONDS))

  override def preStart(): Unit = {

    val initialDelay = Duration(configuration.getMillis(s"scheduling.$name.initialDelay"), TimeUnit.MILLISECONDS)
    val interval = Duration(configuration.getMillis(s"scheduling.$name.interval"), TimeUnit.MILLISECONDS)

    timers.startTimerWithFixedDelay(PeriodicTimerKey, PeriodicTick, initialDelay, interval)

    logger.warn(s"$name job starting, initialDelay: $initialDelay, interval: $interval")
    super.preStart()
  }

  override def receive: Receive = {
    case PeriodicTick => processPreferences()
  }

  def processPreferences(): Future[Result] = {
    logger.debug(s"Executing UpdatedPrintSuppressionJob")

    ls.withLock {
      preferencesProcessor.run(HeaderCarrier()).map { totals =>
        logger.warn(s"Completed UpdatedPrintSuppressionJob. ${totals.processed} items processed with ${totals.failed} failures")
        Result(s"UpdatedPrintSuppressions: ${totals.processed} items processed with ${totals.failed} failures")
      }
    } map {
      case Some(Result(msg)) => Result(s"Job with $name run and completed with result $msg")
      case None              => Result(s"Job with $name cannot acquire lock, not running")
    }
  }

  override def postStop(): Unit = {
    logger.warn(s"Job $name stopped")
    super.postStop()
  }
}

object UpdatedPrintSuppressionJob {
  case object PeriodicTimerKey
  case object PeriodicTick
}
