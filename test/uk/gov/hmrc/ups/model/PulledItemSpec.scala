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
import uk.gov.hmrc.ups.utils.DateFormats.instantFormats

import java.time.Instant
import java.util.UUID

class PulledItemSpec extends PlaySpec {

  "PulledItem" should {
    import PulledItem.formats

    "serialize and deserialize" in {

      val originalPulledItem = PulledItem(
        entityId = EntityId("some-id"),
        paperless = true,
        updatedAt = instantFormats.reads(Json.parse("\"2024-06-10T12:34:56Z\"")).get,
        callbackUrl = s"https://callback.url/${UUID.randomUUID()}"
      )

      val json = Json.toJson(originalPulledItem)
      val deserializedPulledItem = json.as[PulledItem]

      deserializedPulledItem mustEqual originalPulledItem
    }

    "throw exception for invalid json" in {
      intercept[JsResultException] {
        Json.parse("""{}""").as[PulledItem]
      }
    }
  }
}
