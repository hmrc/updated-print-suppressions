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

class UpdatedPrintPreferencesSpec extends PlaySpec {

  "UpdatedPrintPreferences" should {

    "format" in {
      val updatedPrintPreferences = UpdatedPrintPreferences(
        pages = 5,
        next = Some("nextPageToken"),
        updates = List(
          PrintPreference(id = "1234567890", idType = "sautr", formIds = List("form1", "form2"))
        )
      )

      updatedPrintPreferences.pages mustBe 5
      updatedPrintPreferences.next mustBe Some("nextPageToken")
      updatedPrintPreferences.updates.length mustBe 1
      updatedPrintPreferences.updates.head.id mustBe "1234567890"
      updatedPrintPreferences.updates.head.idType mustBe "sautr"
      updatedPrintPreferences.updates.head.formIds must contain allOf ("form1", "form2")
    }
  }

}
