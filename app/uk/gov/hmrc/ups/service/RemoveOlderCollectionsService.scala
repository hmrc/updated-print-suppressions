/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.ups.service

import org.apache.pekko.{ Done, NotUsed }
import org.apache.pekko.stream.{ KillSwitch, KillSwitches, Materializer }
import org.apache.pekko.stream.scaladsl.{ Keep, Sink, Source }
import play.api.inject.ApplicationLifecycle
import play.api.{ Configuration, Logger }
import uk.gov.hmrc.mongo.lock.{ LockRepository, LockService }
import uk.gov.hmrc.ups.UpsRemoveOlderCollectionsConfig
import uk.gov.hmrc.ups.repository.UpdatedPrintSuppressionsDatabase
import uk.gov.hmrc.ups.scheduled.{ Failed, RemoveOlderCollections, Succeeded }
import uk.gov.hmrc.ups.scheduling.Result

import java.util.concurrent.TimeUnit
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.util.{ Failure, Success, Try }

@Singleton
class RemoveOlderCollectionsService @Inject() (
  configuration: Configuration,
  updatedPrintSuppressionsDatabase: UpdatedPrintSuppressionsDatabase,
  lockRepo: LockRepository,
  lifecycle: ApplicationLifecycle, // Play's lifecycle hook
  config: UpsRemoveOlderCollectionsConfig,
  sink: Sink[Unit, _] = Sink.ignore
)(implicit ec: ExecutionContext, mat: Materializer)
    extends LockService with RemoveOlderCollections {
  val logger: Logger = Logger(this.getClass)

  override def repository: UpdatedPrintSuppressionsDatabase = updatedPrintSuppressionsDatabase

  val name: String = "removeOlderCollections"

  private var killSwitch: Option[KillSwitch] = None

  override val lockRepository: LockRepository = lockRepo
  override val lockId: String = s"${config.name}-scheduled-job-lock"
  override val ttl: Duration = config.releaseLockAfter

  // Only run the stream if enabled in config
  if (config.taskEnabled) {
    start()
  }

  // Entrypoint
  def start(): Unit = {
    logger.warn(s"Stream starting: initialDelay: ${config.initialDelay}, interval: ${config.interval}, lock-ttl: $ttl")

    val (killSwitch, streamDone) =
      // Tick source, generates a Unit element to start execution periodically
      Source
        .tick(config.initialDelay, config.interval, tick = ())
        .mapAsync(1)(_ =>
          logger.debug(s"-> Tick")
            startWorkloadStream ()
        )
        .viaMat(KillSwitches.single)(Keep.right)
        .toMat(sink)(Keep.both)
        .run() // Run forever

    this.killSwitch = Some(killSwitch)

    // Register cleanup on shutdown
    lifecycle.addStopHook { () =>
      logger.warn("Shutting down publish subscribers stream...")
      killSwitch.shutdown() // Terminate the stream gracefully
      Future.successful(())
    }
  }

  // Attempt to acquire lock and run the body
  private def startWorkloadStream(): Future[Unit] = {

    logger.warn("startWorkloadStream: before acquiring lock")
    // Acquire a lock
    withLock {
      logger.warn("startWorkloadStream: after acquiring lock")

      // Execute this body when lock successfully acquired
      execute
    }
      .map {
        case Some(result) =>
          logger.debug(s"Successfully processed work under lock: $result")
        case None =>
          logger.debug("Lock held by another instance; skipping")
      }
      .recover { case ex =>
        logger.error(s"Lock acquisition failed: $ex")
      }
  }

  def execute: Future[Result] = {
    logger.warn("ExecuteRemoveOlderCollectionsService job...")
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
      Result(text)
    }
  }

  private lazy val durationInDays = {
    val days = configuration
      .getOptional[Int](s"$name.durationInDays")
      .getOrElse(throw new IllegalStateException(s"Config key $name.durationInDays missing"))
    FiniteDuration(days, TimeUnit.DAYS)
  }

}
