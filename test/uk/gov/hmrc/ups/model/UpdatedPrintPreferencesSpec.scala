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
import play.api.libs.json.{ Json, OFormat }

class UpdatedPrintPreferencesSpec extends PlaySpec {

  "UpdatedPrintPreferences" should {

    "serialize and deserialize correctly" in {
      implicit val uppFormat: OFormat[UpdatedPrintPreferences] = UpdatedPrintPreferences.formats
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
  }

}
