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

package uk.gov.hmrc.ups.controllers

import cats.data.EitherT
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.util.Timeout
import cats.syntax.either.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{ reset, when }
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.ContentTypes
import play.api.http.Status.{ BAD_REQUEST, INTERNAL_SERVER_ERROR, OK }
import play.api.libs.json.Json
import play.api.test.Helpers.{ CONTENT_TYPE, contentAsString, status }
import play.api.test.{ FakeHeaders, FakeRequest, Helpers }
import uk.gov.hmrc.ups.model.NotifySubscriberRequest
import uk.gov.hmrc.ups.service.{ SaUtrNotFoundException, UpdatedPrintSuppressionService }
import uk.gov.hmrc.ups.utils.DateTimeUtils

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt

class UpdatedPrintSuppressionsControllerSpec extends PlaySpec with ScalaFutures with BeforeAndAfterEach {
  spec =>

  implicit lazy val system: ActorSystem = ActorSystem()
  implicit def ec: ExecutionContext = global

  val updatedOn: UpdatedOn = mock[UpdatedOn]
  val upsService: UpdatedPrintSuppressionService = mock[UpdatedPrintSuppressionService]

  val controller = new UpdatedPrintSuppressionsController(
    updatedOn,
    Helpers.stubControllerComponents(),
    upsService
  )

  override def beforeEach(): Unit = {
    reset(updatedOn)
    reset(upsService)
    super.beforeEach()
  }

  "UpdatedPrintSuppressionsController" should {
    "list print preferences" in {
      val result = controller.list(None, None)
      result mustNot be(null)
    }

    "notify subscriber" in {
      val reqBody =
        s"""{
           |  "changedValue" : "paper",
           |  "updatedAt"    : "${DateTimeUtils.now}",
           |  "taxIds"       : { "nino" : "AB112233C", "sautr" : "abcde" }
           |}""".stripMargin

      when(upsService.process(any[NotifySubscriberRequest]))
        .thenReturn(EitherT.rightT[Future, Unit](()))

      val request = createRequest(reqBody)
      val result = controller.notifySubscriber()(request).futureValue
      result.header.status must be(OK)
    }

    "notify subscriber, missing sautr" in {
      val reqBody =
        s"""{
           |  "changedValue" : "paper",
           |  "updatedAt"    : "${DateTimeUtils.now}",
           |  "taxIds"       : { "nino" : "AB112233C" }
           |}""".stripMargin

      when(upsService.process(any[NotifySubscriberRequest]))
        .thenReturn(EitherT.leftT[Future, Throwable](new SaUtrNotFoundException))

      val request = createRequest(reqBody)
      val result = controller.notifySubscriber()(request).futureValue
      result.header.status must be(BAD_REQUEST)
    }

    "notify subscriber, exception" in {
      implicit val timeout: Timeout = Timeout(5.seconds)
      val reqBody =
        s"""{
           |  "changedValue" : "paper",
           |  "updatedAt"    : "${DateTimeUtils.now}",
           |  "taxIds"       : { "nino" : "AB112233C" }
           |}""".stripMargin

      when(upsService.process(any[NotifySubscriberRequest]))
        .thenReturn(EitherT.leftT[Future, Throwable](new RuntimeException("whatever")))

      val request = createRequest(reqBody)
      val result = controller.notifySubscriber()(request)
      status(result) must be(INTERNAL_SERVER_ERROR)
      contentAsString(result) must include("whatever")
    }

    "notify subscriber, invalid message delivery request" in {
      implicit val timeout: Timeout = Timeout(5.seconds)
      val reqBody =
        s"""{
           |  "changedValue" : "paperz",
           |  "updatedAt"    : "${DateTimeUtils.now}",
           |  "taxIds"       : { "nino" : "AB112233C" }
           |}""".stripMargin

      val request = createRequest(reqBody)
      val result = controller.notifySubscriber()(request)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include("Invalid message delivery format")
    }
  }

  private def createRequest(reqBody: String) =
    FakeRequest(
      "POST",
      routes.UpdatedPrintSuppressionsController.notifySubscriber().url,
      FakeHeaders(Seq(CONTENT_TYPE -> ContentTypes.JSON)),
      Json.parse(reqBody)
    )
}
