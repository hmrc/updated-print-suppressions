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
import play.api.Configuration
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.ups.model.MessageDeliveryFormat.Digital
import uk.gov.hmrc.ups.model.{ NotifySubscriberRequest, PrintPreference }
import uk.gov.hmrc.ups.repository.{ MongoCounterRepository, UpdatedPrintSuppressionsRepository, UpsRepository }

import java.time.{ Instant, LocalDate }
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

@Singleton
class UpdatedPrintSuppressionService @Inject() (
  mongoComponent: MongoComponent,
  mongoCounterRepository: MongoCounterRepository,
  configuration: Configuration
)(implicit val ec: ExecutionContext) {

  private lazy val formIds: List[String] =
    configuration
      .getOptional[Seq[String]]("form-types.saAll")
      .getOrElse(throw new RuntimeException(s"configuration property form-types is not set"))
      .toList

  def repository(): UpsRepository =
    new UpsRepository(
      mongoComponent,
      LocalDate.now(),
      mongoCounterRepository,
      configuration
    )

  def oldRepository(): UpdatedPrintSuppressionsRepository =
    new UpdatedPrintSuppressionsRepository(
      mongoComponent,
      LocalDate.now(),
      mongoCounterRepository
    )

  def process(request: NotifySubscriberRequest): EitherT[Future, Throwable, Unit] =
    for {
      pp  <- createPrintPreference(request)
      res <- insert(pp, request.updatedAt)
    } yield res

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

  private def insert(pp: PrintPreference, time: Instant): EitherT[Future, Throwable, Unit] =
    EitherT {
      Try {
        val upsRepoInsert = repository().insert(pp, time)
        val oldRepoInsert = oldRepository().insert(pp, time)
        Future
          .sequence(Seq(upsRepoInsert, oldRepoInsert))
          .map(_ => Right(()))
          .recover(ex => Left(ex))
      } match {
        case Success(v)  => v
        case Failure(ex) => Future.successful(Left(ex))
      }
    }

  private def getUtrValue(request: NotifySubscriberRequest) =
    request.taxIds.get("sautr") match {
      case Some(utr) => SaUtr(utr).value
      case None      => throw new SaUtrNotFoundException
    }

}
