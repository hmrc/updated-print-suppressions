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

package uk.gov.hmrc.ups.scheduling

import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import scala.concurrent.duration._

class SchedulingConfigSpec extends PlaySpec with MockitoSugar {

  "SchedulingConfig" should {
    "load initialDelay from configuration" in {
      val mockRunModeBridge = mock[RunModeBridge]
      when(mockRunModeBridge.getMillisForScheduling("some-job", "initialDelay"))
        .thenReturn(500.milliseconds)

      val config = new TestSchedulingConfig(mockRunModeBridge)
      config.initialDelay mustBe 500.milliseconds
    }

    "load interval from configuration" in {
      val mockRunModeBridge = mock[RunModeBridge]
      when(mockRunModeBridge.getMillisForScheduling("some-job", "interval"))
        .thenReturn(1000.milliseconds)

      val config = new TestSchedulingConfig(mockRunModeBridge)
      config.interval mustBe 1000.milliseconds
    }

    "load optional lockDuration from configuration" in {
      val mockRunModeBridge = mock[RunModeBridge]
      when(mockRunModeBridge.getOptionalMillisForScheduling("some-job", "lockDuration"))
        .thenReturn(Some(2.hours))

      val config = new TestSchedulingConfig(mockRunModeBridge)
      config.lockDuration mustBe Some(2.hours)
    }

    "return None for lockDuration when not configured" in {
      val mockRunModeBridge = mock[RunModeBridge]
      when(mockRunModeBridge.getOptionalMillisForScheduling("some-job", "lockDuration"))
        .thenReturn(None)

      val config = new TestSchedulingConfig(mockRunModeBridge)
      config.lockDuration mustBe None
    }

    "load taskEnabled flag from configuration" in {
      val mockRunModeBridge = mock[RunModeBridge]
      when(mockRunModeBridge.getEnabledFlag("some-job", "taskEnabled")).thenReturn(true)

      val config = new TestSchedulingConfig(mockRunModeBridge)
      config.taskEnabled mustBe true
    }

    "load batchSize from configuration" in {
      val mockRunModeBridge = mock[RunModeBridge]
      when(mockRunModeBridge.getBatchSize("some-job", "batchSize")).thenReturn(100)

      val config = new TestSchedulingConfig(mockRunModeBridge)
      config.batchSize mustBe 100
    }

  }

  private class TestSchedulingConfig(val runModeBridge: RunModeBridge) extends SchedulingConfig {
    val name: String = "some-job"
  }

}
