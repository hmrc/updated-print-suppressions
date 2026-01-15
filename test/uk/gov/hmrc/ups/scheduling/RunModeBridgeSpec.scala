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
import uk.gov.hmrc.ups.SpecBase

import scala.concurrent.duration.*

class RunModeBridgeSpec extends SpecBase {

  "getStringForMode" should {
    "return the correct value for the given suffix" in new Setup {
      runModeBridge.getStringForMode("mongodb.uri") must be("mongodb://localhost:27017/updated-print-suppressions")
    }
  }

  "getMillisForScheduling" should {
    "return correct value for the provided key" in new Setup {
      runModeBridge.getMillisForScheduling("removeOlderCollections", "initialDelay") mustBe 60000.milliseconds
    }
  }

  "getEnabledFlag" should {

    "return false" when {
      "name and propertyKey are present and has false value" in new Setup {
        runModeBridge.getEnabledFlag("updatedPrintSuppressions", "taskEnabled") must be(false)
      }

      "name and propertyKey combination is not present" in new Setup {
        runModeBridge.getEnabledFlag("updatedPrintSuppressions", "unknown") must be(false)
      }
    }

    "return true" when {
      "name and propertyKey are present and has true value" in new Setup {
        runModeBridge.getEnabledFlag("removeOlderCollections", "taskEnabled") must be(true)
      }
    }
  }

  "getBatchSize" should {
    "return batch size when the property value is present" in {
      val batchSize = 1000
      val config = Configuration(
        "scheduling.updatedPrintSuppressions.batch" -> 1000
      )

      val runModeBridge = RunModeBridge(config)
      runModeBridge.getBatchSize("updatedPrintSuppressions", "batch") must be(batchSize)
    }

    "return batch size as 0 when the property is not present" in new Setup {
      runModeBridge.getBatchSize("updatedPrintSuppressions", "unknown") must be(0)
    }
  }

  trait Setup {
    val runModeBridge: RunModeBridge = app.injector.instanceOf[RunModeBridge]
  }
}
