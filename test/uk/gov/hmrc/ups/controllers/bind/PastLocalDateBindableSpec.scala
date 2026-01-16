/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.ups.controllers.bind

import uk.gov.hmrc.ups.SpecBase
import uk.gov.hmrc.ups.TestData.{ TEST_DAY_1, TEST_MONTH_11, TEST_YEAR_2025, TEST_YEAR_2026 }
import uk.gov.hmrc.ups.model.PastLocalDate

import java.time.LocalDate

class PastLocalDateBindableSpec extends SpecBase {

  "unbind" should {
    "return correct string value" in {
      val pastLocalDateBindable: PastLocalDateBindable = PastLocalDateBindable(true)

      val pastLocalDate: PastLocalDate = PastLocalDate(LocalDate.of(TEST_YEAR_2026, TEST_MONTH_11, TEST_DAY_1))

      pastLocalDateBindable.unbind("startDate", pastLocalDate) mustBe "startDate=2026-11-01"
    }
  }

  "bind" should {
    "return the correct PastLocalDate" in {
      val pastLocalDateBindable: PastLocalDateBindable = PastLocalDateBindable(true)
      val pastLocalDate: PastLocalDate = PastLocalDate(LocalDate.of(TEST_YEAR_2025, TEST_MONTH_11, TEST_DAY_1))

      val result: Option[Either[String, PastLocalDate]] =
        pastLocalDateBindable.bind("startDate", Map("startDate" -> Seq("2025-11-01")))

      result.map { output =>
        output mustBe Right(pastLocalDate)
      }
    }

    "throw exception when date string is in invalid format" in {
      val pastLocalDateBindable: PastLocalDateBindable = PastLocalDateBindable(true)

      val result: Option[Either[String, PastLocalDate]] =
        pastLocalDateBindable.bind("startDate", Map("startDate" -> Seq("startDate=2026-11-01")))

      result.map { output =>
        output mustBe Left("updated-on parameter is in the wrong format. Should be (yyyy-MM-dd)")
      }
    }
  }
}
