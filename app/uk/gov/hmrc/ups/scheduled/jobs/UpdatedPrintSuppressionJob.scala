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

package uk.gov.hmrc.ups.scheduled.jobs

import com.google.inject.{ Inject, Singleton }
import play.api.Logger
import uk.gov.hmrc.mongo.lock.LockRepository
import uk.gov.hmrc.ups.scheduling.{ LockedScheduledJob, Result, RunModeBridge }
import uk.gov.hmrc.ups.service.UpdatedPrintSuppressionService

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.Duration

// $COVERAGE-OFF$Disabling
@Singleton
class UpdatedPrintSuppressionJob @Inject()(
  lockRepository: LockRepository,
  updatedPrintSuppressionService: UpdatedPrintSuppressionService,
  override val runModeBridge: RunModeBridge)
    extends LockedScheduledJob {

  val logger: Logger = Logger(this.getClass)
  val name: String = "updatedPrintSuppressions"

  def executeInLock(implicit ec: ExecutionContext): Future[Result] = {
    val result = updatedPrintSuppressionService.execute
    result.foreach(r => logger.warn(r.message))
    result
  }

  override val releaseLockAfter: Duration = lockDuration.getOrElse(Duration("1 hour"))
  override val lockRepo: LockRepository = lockRepository
}

// $COVERAGE-ON$
