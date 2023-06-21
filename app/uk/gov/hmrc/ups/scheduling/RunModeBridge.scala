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

package uk.gov.hmrc.ups.scheduling

import play.api.Configuration

import javax.inject.{ Inject, Singleton }
import scala.concurrent.duration._

// $COVERAGE-OFF$
@Singleton
class RunModeBridge @Inject()(configuration: Configuration) {

  def getStringForMode(suffix: String): String =
    configuration
      .get[String](suffix)

  def getMillisForScheduling(name: String, propertyKey: String): FiniteDuration =
    getLongMillis(s"scheduling.$name.$propertyKey").milliseconds

  def getEnabledFlag(name: String, propertyKey: String): Boolean =
    configuration.getOptional[Boolean](s"scheduling.$name.$propertyKey").getOrElse(false)

  def getBatchSize(name: String, propertyKey: String): Int =
    configuration.getOptional[Int](s"scheduling.$name.$propertyKey").getOrElse(0)

  def getOptionalMillisForScheduling(name: String, propertyKey: String): Option[FiniteDuration] =
    configuration
      .getOptional[Duration](s"scheduling.$name.$propertyKey")
      .flatMap(duration => Some(duration.toMillis.milliseconds))

  def getLongMillis(suffix: String): Long =
    configuration
      .get[Duration](suffix)
      .toMillis

}
// $COVERAGE-ON$
