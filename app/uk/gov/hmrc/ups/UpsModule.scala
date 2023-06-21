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

import com.google.inject.{AbstractModule, Provides, Singleton}
import net.codingwell.scalaguice.ScalaModule
import play.api.{Configuration, Logger}
import play.api.libs.concurrent.AkkaGuiceSupport
import uk.gov.hmrc.ups.scheduled.jobs.{RemoveOlderCollectionsJob, UpdatedPrintSuppressionJob}
import uk.gov.hmrc.ups.scheduling.ScheduledJob

class UpsModule extends AbstractModule with ScalaModule with AkkaGuiceSupport {

  private val logger: Logger = Logger(getClass)

  override def configure(): Unit = bind[UpsMain].asEagerSingleton()

  val taskEnabled: (Configuration, String) => Boolean = (config: Configuration, name: String) => {
    val enabled = config.getOptional[Boolean](s"scheduling.$name.taskEnabled").getOrElse(false)
    if (!enabled) logger.warn(s"'scheduling.$name.taskEnabled' is not true and the scheduled job will not be started")
    enabled
  }

  @Provides
  @Singleton
  def scheduledJobsProvider(
    removeOlderCollectionsJob: RemoveOlderCollectionsJob,
    updatedPrintSuppressionJob: UpdatedPrintSuppressionJob
  ): Seq[ScheduledJob] =
    Seq(
      removeOlderCollectionsJob,
      updatedPrintSuppressionJob
    )
}
