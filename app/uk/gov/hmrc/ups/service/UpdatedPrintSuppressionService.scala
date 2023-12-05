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

import cats.data.EitherT
import org.joda.time.DateTime
import play.api.Configuration
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.ups.model.MessageDeliveryFormat.Digital
import uk.gov.hmrc.ups.model.{ NotifySubscriberRequest, PrintPreference }
import uk.gov.hmrc.ups.repository.UpdatedPrintSuppressionsRepository
import uk.gov.hmrc.ups.scheduled.PreferencesProcessor
import uk.gov.hmrc.ups.scheduling.Result

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

@Singleton
class UpdatedPrintSuppressionService @Inject()(
  preferencesProcessor: PreferencesProcessor,
  repository: UpdatedPrintSuppressionsRepository,
  configuration: Configuration
)(implicit val ec: ExecutionContext) {

  lazy val formIds: List[String] =
    configuration
      .getOptional[Seq[String]]("form-types.saAll")
      .getOrElse(throw new RuntimeException(s"configuration property form-types is not set"))
      .toList

  def insert(pp: PrintPreference, time: DateTime): EitherT[Future, Throwable, Unit] =
    EitherT {
      repository
        .insert(pp, time)
        .map(a => Right(a))
        .recover(ex => Left(ex))
    }

  def process(request: NotifySubscriberRequest): EitherT[Future, Throwable, Unit] =
    for {
      pp <- createPrintPreference(request)
      _  <- insert(pp, new DateTime(request.updatedAt.toEpochMilli))
    } yield { () }

  def execute: Future[Result] =
    preferencesProcessor.run(HeaderCarrier()).map { totals =>
      Result(
        s"UpdatedPrintSuppressions: ${totals.processed} items processed with ${totals.failed} failures"
      )
    }

  private def createPrintPreference(request: NotifySubscriberRequest): EitherT[Future, Throwable, PrintPreference] =
    EitherT {
      Try {
        PrintPreference(
          getUtrValue(request),
          "utr",
          if (request.changedValue == Digital) formIds else List.empty
        )
      } match {
        case Success(printPreference) => Future.successful(Right(printPreference))
        case Failure(exception)       => Future.successful(Left(exception))
      }
    }

  private def getUtrValue(request: NotifySubscriberRequest) =
    request.taxIds.get("sautr") match {
      case Some(utr) => SaUtr(utr).value
      case None      => throw new SaUtrNotFoundException
    }

}
