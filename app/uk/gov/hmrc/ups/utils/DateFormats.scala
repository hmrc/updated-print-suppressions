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

import play.api.libs.json.{ Format, Reads, Writes }

import java.time.{ Instant, ZoneOffset }
import java.time.format.DateTimeFormatter

object DateFormats {
  // Format Instant non-mongo
  implicit val instantFormats: Format[Instant] = {
    val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    val dateTimeWithMillis: DateTimeFormatter =
      DateTimeFormatter.ofPattern(dateFormat).withZone(ZoneOffset.UTC)

    Format(Reads.DefaultInstantReads, Writes.temporalWrites[Instant, DateTimeFormatter](dateTimeWithMillis))
  }
}
