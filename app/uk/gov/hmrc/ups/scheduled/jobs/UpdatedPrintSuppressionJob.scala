/*
 * Copyright 2019 HM Revenue & Customs
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

import java.util.concurrent.TimeUnit

import javax.inject.{Inject, Singleton}
import org.joda.time.Duration
import play.api.{Configuration, Logger}
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lock.{LockMongoRepository, LockRepository}
import uk.gov.hmrc.play.bootstrap.config.RunMode
import uk.gov.hmrc.play.scheduling.LockedScheduledJob
import uk.gov.hmrc.ups.scheduled.PreferencesProcessor

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdatedPrintSuppressionJob @Inject()(runMode: RunMode, configuration: Configuration, reactiveMongoComponent: ReactiveMongoComponent,
                                           preferencesProcessor: PreferencesProcessor) extends LockedScheduledJob {

  override lazy val releaseLockAfter: Duration = Duration.millis(
    configuration.getOptional[Long](s"${runMode.env}.updatedPrintSuppressions.releaseLockAfter").
      getOrElse(throw new IllegalStateException(s"${runMode.env}.updatedPrintSuppressions.releaseLockAfter config value not set"))
  )

  def executeInLock(implicit ec: ExecutionContext): Future[Result] = {
    Logger.info(s"Start UpdatedPrintSuppressionJob")
    preferencesProcessor.run(HeaderCarrier()).
      map { totals =>
        Result(
          s"UpdatedPrintSuppressions: ${totals.processed} items processed with ${totals.failed} failures"
        )
      }
  }

  override val name: String = "updatedPrintSuppressions"

  override val lockRepository: LockRepository = LockMongoRepository(
    reactiveMongoComponent.mongoConnector.db
  )

  private def durationFromConfig(propertyKey: String): FiniteDuration = {
    val millis = configuration.getOptional[Long](s"${runMode.env}.scheduling.$name.$propertyKey")
      .getOrElse(throw new IllegalStateException(s"Config key ${runMode.env}.scheduling.$name.$propertyKey missing"))
    FiniteDuration(millis, TimeUnit.MILLISECONDS)
  }
  override def initialDelay: FiniteDuration = durationFromConfig("initialDelay")

  override def interval: FiniteDuration = durationFromConfig("interval")
}