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

package uk.gov.hmrc.ups.utils

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{ Nino, SaUtr }
import uk.gov.hmrc.ups.model.EntityId

class GenerateSpec extends PlaySpec {

  "Generate" should {
    "nino" in {
      val nino = Generate.nino
      nino mustBe a[Nino]

      nino.value.nonEmpty mustBe true

      nino.value.matches("""^[A-Z]{2}[0-9]{6}[A-Z]""") mustBe true

    }
    "utr" in {
      val utr = Generate.utr
      utr mustBe a[SaUtr]
    }

    "entityId" in {
      val entityId = Generate.entityId
      entityId mustBe a[EntityId]
    }
  }

}
