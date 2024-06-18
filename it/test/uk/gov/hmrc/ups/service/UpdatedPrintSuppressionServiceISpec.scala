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

import org.mongodb.scala.SingleObservableFuture
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{ Suite, TestSuite }
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Configuration
import play.api.test.Injecting
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.ups.model.MessageDeliveryFormat.Digital
import uk.gov.hmrc.ups.model.NotifySubscriberRequest
import uk.gov.hmrc.ups.repository.{ MongoCounterRepository, UpdatedPrintSuppressionsRepository }
import uk.gov.hmrc.ups.utils.DateTimeUtils

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class UpdatedPrintSuppressionServiceISpec
    extends AnyFreeSpec with Matchers with TestSuite with GuiceOneServerPerSuite with ScalaFutures
    with IntegrationPatience with MongoSupport with Injecting {
  this: Suite =>

  trait Setup {
    implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
    val config = Configuration(data = ("form-types.saAll", List("abc")))

    private val counterRepository = inject[MongoCounterRepository]

    val repository: UpdatedPrintSuppressionsRepository =
      new UpdatedPrintSuppressionsRepository(
        mongoComponent,
        LocalDate.now(),
        counterRepository
      )

    val service = new UpdatedPrintSuppressionService(mongoComponent, counterRepository, config)
  }

  "updated print suppressions service" - {

    "process preference success" in new Setup {
      private val nsr = NotifySubscriberRequest(Digital, DateTimeUtils.now, Map("sautr" -> "sautr1"))
      private val eitherResult = service.process(nsr)

      eitherResult.value.futureValue must be(Right(()))
      repository.collection.countDocuments().toFuture().futureValue must be(1)
    }
  }
}
