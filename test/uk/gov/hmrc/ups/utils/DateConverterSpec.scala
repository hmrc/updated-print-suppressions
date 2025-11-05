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

package uk.gov.hmrc.ups.utils

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers.*

import java.time.format.DateTimeParseException
import java.time.{ Instant, LocalDate, ZoneId }

class DateConverterSpec extends AnyWordSpec with ScalaFutures {

  "parseToLocalDate" should {
    "parse a valid date string" in {
      val date = "2026-11-01"
      val result = DateConverter.parseToLocalDate(date)
      result mustBe LocalDate.of(2026, 11, 1)
    }

    "throw DateTimeParseException for invalid format" in {
      intercept[DateTimeParseException] {
        DateConverter.parseToLocalDate("15/11/2026")
      }
    }

  }

  "formatToString from LocalDate" should {
    "format a LocalDate to ISO date string" in {
      val date = LocalDate.of(2025, 11, 1)
      val result = DateConverter.formatToString(date)
      result mustBe "2025-11-01"
    }
  }

  "dateFormatter" should {
    "be ISO_DATE format (yyyy-MM-dd)" in {
      val date = LocalDate.of(2025, 11, 1)
      val formatted = DateConverter.formatToString(date)
      formatted mustBe "2025-11-01"
    }
  }
}
