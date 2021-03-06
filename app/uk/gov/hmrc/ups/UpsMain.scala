/*
 * Copyright 2021 HM Revenue & Customs
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

import akka.actor.ActorSystem
import javax.inject.{ Inject, Singleton }
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import play.api.{ Configuration, Environment }
import uk.gov.hmrc.play.scheduling.ScheduledJob

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class UpsMain @Inject()(
  env: Environment,
  scheduledJobs: Seq[ScheduledJob],
  actorSystem: ActorSystem,
  configuration: Configuration,
  lifecycle: ApplicationLifecycle)(implicit val ec: ExecutionContext) {

  lifecycle.addStopHook(() =>
    Future {
      actorSystem.terminate()
  })

  scheduledJobs.foreach(scheduleJob)

  val refreshInterval = configuration
    .getMillis(s"${env.mode}.microservice.metrics.gauges.interval")
    .toInt

  def scheduleJob(job: ScheduledJob)(implicit ec: ExecutionContext): Unit =
    actorSystem.scheduler.schedule(job.initialDelay, job.interval)(job.execute.map { result =>
      Logger.debug(s"Job ${job.name} result: ${result.message}")
    })
}
