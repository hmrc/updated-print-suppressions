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

package uk.gov.hmrc.ups.utils

import org.scalatest.matchers.should.Matchers.shouldBe
import uk.gov.hmrc.ups.SpecBase
import uk.gov.hmrc.ups.TestData.{ TEST_DAY_1, TEST_MONTH_11, TEST_YEAR_2025, TEST_YEAR_2026 }

import java.time.{ Instant, LocalDate }

class DateTimeUtilsSpec extends SpecBase {

  "now" should {
    "return current time as Instant" in {
      DateTimeUtils.now shouldBe a[Instant]
    }
  }

  "daysBetween" should {
    "return the correct number of days for two dates" in {
      val startDate = LocalDate.of(TEST_YEAR_2025, TEST_MONTH_11, TEST_DAY_1)
      val endDate = LocalDate.of(TEST_YEAR_2026, TEST_MONTH_11, TEST_DAY_1)

      DateTimeUtils.daysBetween(startDate, endDate) mustBe 0
    }
  }

  "isEqualOrAfter" should {
    "return true when date is before the laterDate" in {
      val date = LocalDate.of(TEST_YEAR_2025, TEST_MONTH_11, TEST_DAY_1)
      val laterDate = LocalDate.of(TEST_YEAR_2026, TEST_MONTH_11, TEST_DAY_1)

      DateTimeUtils.isEqualOrAfter(date, laterDate) mustBe true
    }

    "return true when both dates are same" in {
      val date = LocalDate.of(TEST_YEAR_2025, TEST_MONTH_11, TEST_DAY_1)
      val laterDate = LocalDate.of(TEST_YEAR_2025, TEST_MONTH_11, TEST_DAY_1)

      DateTimeUtils.isEqualOrAfter(date, laterDate) mustBe true
    }

    "return false when date is after laterDate" in {
      val date = LocalDate.of(TEST_YEAR_2026, TEST_MONTH_11, TEST_DAY_1)
      val laterDate = LocalDate.of(TEST_YEAR_2025, TEST_MONTH_11, TEST_DAY_1)

      DateTimeUtils.isEqualOrAfter(date, laterDate) mustBe false
    }
  }
}
