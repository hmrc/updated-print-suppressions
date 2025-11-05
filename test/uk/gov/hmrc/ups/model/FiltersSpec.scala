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

class FiltersSpec extends PlaySpec {

  "Filters" should {
    "create an instance correctly" in {
      val filters = Filters(
        failedBefore = java.time.Instant.parse("2024-01-01T00:00:00Z"),
        availableBefore = java.time.Instant.parse("2024-01-02T00:00:00Z")
      )

      filters.failedBefore mustBe java.time.Instant.parse("2024-01-01T00:00:00Z")
      filters.availableBefore mustBe java.time.Instant.parse("2024-01-02T00:00:00Z")
    }

    "format" in {
      val filters = Filters(
        failedBefore = java.time.Instant.parse("2024-01-01T00:00:00Z"),
        availableBefore = java.time.Instant.parse("2024-01-02T00:00:00Z")
      )

      filters.failedBefore.toString mustBe "2024-01-01T00:00:00Z"
      filters.availableBefore.toString mustBe "2024-01-02T00:00:00Z"
    }
  }

}
