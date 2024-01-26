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

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.{ BeforeAndAfterEach, EitherValues }
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.ups.model.MessageDeliveryFormat.Digital
import uk.gov.hmrc.ups.model.{ NotifySubscriberRequest, PrintPreference }
import uk.gov.hmrc.ups.repository.{ MongoCounterRepository, UpdatedPrintSuppressionsRepository }

import java.time.Instant
import scala.concurrent.{ ExecutionContext, Future }

class UpdatedPrintSuppressionServiceSpec
    extends PlaySpec with ScalaFutures with BeforeAndAfterEach with IntegrationPatience with MockitoSugar with EitherValues {

  trait Setup {
    implicit val ec = ExecutionContext.Implicits.global
    implicit val hc = HeaderCarrier()
    private val config = Configuration(data = ("form-types.saAll", List("abc")))
    
    private val mongoComponent = mock[MongoComponent]
    private val counterRepository = mock[MongoCounterRepository]

    val mockRepo: UpdatedPrintSuppressionsRepository = mock[UpdatedPrintSuppressionsRepository]

    val service = new UpdatedPrintSuppressionService(mongoComponent, counterRepository, config) {
      override def repository(): UpdatedPrintSuppressionsRepository = mockRepo
    }
  }

  "updated print suppressions service" should {

    "process preference success" in new Setup {
      when(mockRepo.insert(any[PrintPreference], any[DateTime]))
        .thenReturn(Future.successful(()))

      private val nsr = NotifySubscriberRequest(Digital, Instant.now(), Map("sautr" -> "sautr1"))
      private val eitherResult = service.process(nsr)

      eitherResult.value.futureValue must be(Right(()))
    }

    "process preference throws" in new Setup {
      when(mockRepo.insert(any[PrintPreference], any[DateTime]))
        .thenThrow(new RuntimeException("whatever"))

      private val nsr = NotifySubscriberRequest(Digital, Instant.now(), Map("sautr" -> "sautr1"))
      private val eitherResult = service.process(nsr).value.futureValue

      eitherResult.left.value mustBe a[RuntimeException]
      eitherResult.left.value.getMessage must be("whatever")
    }

    "execute" in new Setup {}
  }
}
