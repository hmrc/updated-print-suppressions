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

package uk.gov.hmrc.ups

import org.apache.pekko.actor.ActorSystem

import javax.inject.{ Inject, Singleton }
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import play.api.Configuration
import uk.gov.hmrc.ups.scheduling.ScheduledJob

import scala.concurrent.{ ExecutionContext, Future }

// $COVERAGE-OFF$
@Singleton
class UpsMain @Inject()(actorSystem: ActorSystem, configuration: Configuration, lifecycle: ApplicationLifecycle, scheduledJobs: Seq[ScheduledJob])(
  implicit val ec: ExecutionContext) {

  val logger: Logger = Logger(this.getClass)
  lifecycle.addStopHook(() =>
    Future {
      actorSystem.terminate()
  })

  scheduledJobs.foreach(startScheduleJob)

  private def startScheduleJob(job: ScheduledJob)(implicit ec: ExecutionContext): Unit =
    if (job.taskEnabled) {
      logger.warn(s"Starting scheduled job $job")
      actorSystem.scheduler.scheduleWithFixedDelay(job.initialDelay, job.interval) { () =>
        job.execute
      }
    } else {
      logger.warn(s"${job.name} will not run, taskEnabled is false")
    }

  val refreshInterval: Int = configuration
    .getMillis(s"microservice.metrics.gauges.interval")
    .toInt

}
// $COVERAGE-ON$
