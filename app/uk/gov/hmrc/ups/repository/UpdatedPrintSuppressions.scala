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

package uk.gov.hmrc.ups.repository

import org.joda.time.{ DateTime, LocalDate }
import org.mongodb.scala.bson.ObjectId
import play.api.libs.json.{ Format, JsValue, Json, OFormat }
import uk.gov.hmrc.mongo.play.json.formats.{ MongoFormats, MongoJodaFormats }
import uk.gov.hmrc.ups.model.PrintPreference

case class UpdatedPrintSuppressions(_id: ObjectId, counter: Int, printPreference: PrintPreference, updatedAt: DateTime)

object UpdatedPrintSuppressions {
  implicit val objectIdFormat: Format[ObjectId] = MongoFormats.objectIdFormat
  implicit val pp: OFormat[PrintPreference] = PrintPreference.formats
  implicit val isoDateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat

  implicit val formats: OFormat[UpdatedPrintSuppressions] = Json.format[UpdatedPrintSuppressions]

  val datePattern = "yyyyMMdd"

  def toString(date: LocalDate): String = date.toString(datePattern)

  def repoNameTemplate(date: LocalDate): String = s"updated_print_suppressions_${toString(date)}"

  def updatedAtAsJson(updatedAt: DateTime): JsValue = Json.toJson(updatedAt)
}
