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

package uk.gov.hmrc.ups.model

import play.api.libs.json.{ JsResultException, JsString, Json }
import uk.gov.hmrc.ups.SpecBase
import uk.gov.hmrc.ups.TestData.TEST_TIME_INSTANT
import uk.gov.hmrc.ups.model.MessageDeliveryFormat.{ Digital, Paper }

import java.time.Instant

class NotifySubscriberRequestSpec extends SpecBase {

  "NotifySubscriberRequest.reads" should {
    import NotifySubscriberRequest.reads

    "read the json correctly" in new Setup {
      Json.parse(notifySubscriberRequestJsonString).as[NotifySubscriberRequest] mustBe notifySubscriberRequest
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(notifySubscriberRequestInvalidJsonString1).as[NotifySubscriberRequest]
      }

      intercept[JsResultException] {
        Json.parse(notifySubscriberRequestInvalidJsonString2).as[NotifySubscriberRequest]
      }
    }
  }

  "NotifySubscriberRequest.toString" should {
    "return correct string representation" in new Setup {
      notifySubscriberRequest.toString must be(
        "changedValue: [Digital], updatedAt: [1970-01-07T13:34:05.389Z], taxIds: sautr"
      )
    }
  }

  "MessageDeliveryFormat.reads" should {
    import MessageDeliveryFormat.reads

    "read the json correctly" in new Setup {
      JsString(paperMessageDeliveryFormatString).as[MessageDeliveryFormat] mustBe Paper
      JsString(digitalMessageDeliveryFormatString).as[MessageDeliveryFormat] mustBe Digital
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        JsString("unknown").as[MessageDeliveryFormat]
      }
    }
  }

  trait Setup {
    val paperMessageDeliveryFormatString = "paper"
    val digitalMessageDeliveryFormatString = "digital"

    val notifySubscriberRequest: NotifySubscriberRequest =
      NotifySubscriberRequest(
        changedValue = Digital,
        updatedAt = TEST_TIME_INSTANT,
        taxIds = Map("sautr" -> "12345678781")
      )

    val notifySubscriberRequestJsonString: String =
      """{
        |"changedValue":"digital",
        |"updatedAt":"1970-01-07T13:34:05.389Z",
        |"taxIds":{"sautr":"12345678781"}
        |}""".stripMargin

    val notifySubscriberRequestInvalidJsonString1: String =
      """{
        |"updatedAt":"1970-01-01T00:13:09.245+0000",
        |"taxIds":{"sautr":"12345678781"}
        |}""".stripMargin

    val notifySubscriberRequestInvalidJsonString2: String =
      """{
        |"changedValue":"digital",
        |"updatedAt":1234576,
        |"taxIds":{"sautr":"12345678781"}
        |}""".stripMargin
  }
}
