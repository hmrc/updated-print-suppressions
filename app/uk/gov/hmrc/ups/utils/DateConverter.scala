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

import org.joda.time.{ DateTime, DateTimeFieldType, DateTimeZone, LocalDate }
import org.joda.time.format.DateTimeFormatterBuilder

trait DateConverter {
  private val Year = 4

  private val MonthOfYear = 2

  private val DayOfMonth = 2

  //yyyy-MM-dd
  lazy val dateFormatter = new DateTimeFormatterBuilder()
    .appendFixedDecimal(DateTimeFieldType.year, Year)
    .appendLiteral("-")
    .appendFixedDecimal(DateTimeFieldType.monthOfYear, MonthOfYear)
    .appendLiteral("-")
    .appendFixedDecimal(DateTimeFieldType.dayOfMonth, DayOfMonth)
    .toFormatter

  final def parseToLong(date: String): Long = dateFormatter.withZoneUTC().parseMillis(date)

  final def parseToDateTime(date: String): DateTime = dateFormatter.withZoneUTC().parseDateTime(date).toDateTime(DateTimeZone.UTC)

  final def parseToLocalDate(date: String): LocalDate = dateFormatter.parseLocalDate(date)

  final def formatToString(date: Long): String = new DateTime(date, DateTimeZone.UTC).toString(dateFormatter)

  final def formatToString(date: DateTime): String = date.toString(dateFormatter)

  final def formatToString(date: LocalDate): String = date.toString(dateFormatter)

  final def safeParse[A](f: => A)(t: => Throwable): A =
    try f
    catch {
      case _: IllegalArgumentException => throw t
    }
}

object DateConverter extends DateConverter