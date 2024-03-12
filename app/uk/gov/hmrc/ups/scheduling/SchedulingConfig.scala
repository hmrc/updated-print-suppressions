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

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.language.implicitConversions

//$COVERAGE-OFF$
trait SchedulingConfig {
  def runModeBridge: RunModeBridge

  val name: String

  protected[this] def durationFromConfig(propertyKey: String): FiniteDuration =
    runModeBridge.getMillisForScheduling(name, propertyKey)

  lazy val initialDelay: FiniteDuration = durationFromConfig("initialDelay")
  lazy val interval: FiniteDuration = durationFromConfig("interval")
  lazy val lockDuration: Option[FiniteDuration] = runModeBridge.getOptionalMillisForScheduling(name, "lockDuration")
  lazy val taskEnabled: Boolean = runModeBridge.getEnabledFlag(name, "taskEnabled")
  lazy val batchSize: Int = runModeBridge.getBatchSize(name, "batchSize")

  class WriteDuration(val d: Duration) {
    def toHM: String = {

      def print(value: Long, unit: TimeUnit): Option[String] = (value, unit) match {
        case (0, _) => None
        case (v, u) => Some(s"$v ${u.toString().toLowerCase()}")
      }

      val time = List[Option[String]](
        print(d.toHours, TimeUnit.HOURS),
        print(d.toMinutes % 60, TimeUnit.MINUTES),
        print(d.toSeconds % 60, TimeUnit.SECONDS)
      )
      val m = time.filter(_.isDefined).collect[String](a => a.get)
      s"${m.mkString(" ")}"
    }
  }

  implicit def durationToString(d: Duration): WriteDuration = new WriteDuration(d)

  override def toString = s"'$name' initialDelay: ${initialDelay.toHM} interval: ${interval.toHM}"
}
//$COVERAGE-ON$
