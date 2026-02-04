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

package uk.gov.hmrc.ups.model

import play.api.libs.json.{ JsResultException, JsString, Json }
import uk.gov.hmrc.ups.SpecBase
import uk.gov.hmrc.ups.TestData.TEST_ID

class EntityIdSpec extends SpecBase {

  "formats" should {
    "read the json correctly" in new Setup {
      JsString(TEST_ID).as[EntityId] mustBe entityId
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(entityIdInvalidJsonString).as[EntityId]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(entityId) mustBe JsString(TEST_ID)
    }
  }

  "toString" should {
    "return correct string representation" in {
      EntityId(TEST_ID).toString mustBe TEST_ID
    }
  }

  trait Setup {
    val entityId: EntityId = EntityId(TEST_ID)

    val entityIdInvalidJsonString: String = """{"value":5}""".stripMargin
  }
}
