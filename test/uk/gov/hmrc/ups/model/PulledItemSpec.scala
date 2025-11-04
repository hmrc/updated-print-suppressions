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

import java.util.UUID

class PulledItemSpec extends PlaySpec {

  "PulledItem" should {
    "format" in {
      val entityId = EntityId(UUID.randomUUID().toString)
      val pulledItem = PulledItem(
        entityId = entityId,
        paperless = true,
        updatedAt = java.time.Instant.parse("2024-01-01T12:00:00Z"),
        callbackUrl = "https://example.com/callback"
      )

      pulledItem.entityId mustBe entityId
      pulledItem.paperless mustBe true
      pulledItem.updatedAt mustBe java.time.Instant.parse("2024-01-01T12:00:00Z")
      pulledItem.callbackUrl mustBe "https://example.com/callback"
    }

  }

}
