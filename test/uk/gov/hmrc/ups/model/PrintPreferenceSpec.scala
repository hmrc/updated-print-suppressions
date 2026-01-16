/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsResultException, Json, OFormat }
import uk.gov.hmrc.ups.TestData.{ TEST_FORM_ID, TEST_ID, TEST_ID_TYPE_SAUTR }

class PrintPreferenceSpec extends PlaySpec {

  "UpdatedPrintPreferences.formats" should {
    implicit val uppFormat: OFormat[UpdatedPrintPreferences] = UpdatedPrintPreferences.formats

    "serialize and deserialize correctly" in {
      val originalUpp = UpdatedPrintPreferences(
        pages = 2,
        next = Some("next-page-token"),
        updates = List(
          PrintPreference("some-id", "sautr", List("some-form"))
        )
      )
      val json = Json.toJson(originalUpp)
      val deserializedUpp = json.as[UpdatedPrintPreferences]
      deserializedUpp mustEqual originalUpp
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(updatedPrintPreferencesInvalidJson).as[UpdatedPrintPreferences]
      }
    }
  }

  "PrintPreference.formats" should {
    import PrintPreference.formats

    "read the json correctly" in new Setup {
      Json.parse(printPreferenceJsonString).as[PrintPreference] mustBe printPreference
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(printPreferenceInvalidJsonString).as[PrintPreference]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(printPreference) mustBe Json.parse(printPreferenceJsonString)
    }
  }

  trait Setup {
    val printPreference: PrintPreference =
      PrintPreference(id = TEST_ID, idType = TEST_ID_TYPE_SAUTR, formIds = List(TEST_FORM_ID))

    val printPreferenceJsonString: String =
      """{
        |"id":"test_id",
        |"idType":"sautr",
        |"formIds":["form_id"]
        |}""".stripMargin

    val printPreferenceInvalidJsonString: String =
      """{
        |"idType":"sautr",
        |"formIds":["form_id"]
        |}""".stripMargin

    val updatedPrintPreferencesInvalidJson: String =
      """{
        |"next":"next-page-token",
        |"updates":[{"id":"some-id","idType":"sautr","formIds":["some-form"]}]}""".stripMargin
  }
}
