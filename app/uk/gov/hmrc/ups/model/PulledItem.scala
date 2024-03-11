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

import play.api.libs.json.{ Format, Json, OFormat }
import uk.gov.hmrc.ups.utils.DateFormats

import java.time.Instant

// $COVERAGE-OFF$Soon to be deleted
case class PulledItem(entityId: EntityId, paperless: Boolean, updatedAt: Instant, callbackUrl: String)

object PulledItem {

  implicit val formats: OFormat[PulledItem] = {
    implicit val entityIdFormats: Format[EntityId] = EntityId.formats
    implicit val dateFormats: Format[Instant] = DateFormats.instantFormats
    Json.format[PulledItem]
  }

}
// $COVERAGE-ON$
