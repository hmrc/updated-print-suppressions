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
import play.api.libs.json.Json
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.domain.TaxIds.TaxIdWithName

import java.util.UUID

class EntitySpec extends PlaySpec {

  "Entity" should {
    "create an instance correctly" in {
      val taxId: TaxIdWithName = SaUtr("1234567890")
      taxId.name mustBe "sautr"
      taxId.value mustBe "1234567890"

    }
  }

  "toStrings" should {
    "convert tax identifiers to strings correctly" in {
      val taxId1: TaxIdWithName = SaUtr("1234567890")
      val taxId2: TaxIdWithName = SaUtr("0987654321")
      val taxIds: Set[TaxIdWithName] = Set(taxId1, taxId2)

      val result: Set[String] = Entity.toStrings(taxIds)

      result must contain("sautr: 1234567890")
      result must contain("sautr: 0987654321")
    }
  }

  "reads" should {
    "deserialize JSON to Entity correctly" in {
      val entityId = UUID.randomUUID().toString
      val jsonString =
        s"""
           |{
           |  "_id": "$entityId",
           |  "sautr": "1234567890"
           |}
           |""".stripMargin

      val json = Json.parse(jsonString)
      val entity = json.as[Entity]

      entity.id.value mustBe entityId
      entity.taxIdentifiers.head.name mustBe "sautr"
      entity.taxIdentifiers.head.value mustBe "1234567890"
    }
  }

}
