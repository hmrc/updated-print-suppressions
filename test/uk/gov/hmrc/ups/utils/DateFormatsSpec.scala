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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{ JsError, JsString, JsSuccess }

import java.time.Instant

class DateFormatsSpec extends AnyWordSpec with ScalaFutures {

  "format" should {
    "read an instant" in {
      val result = DateFormats.instantFormats.reads(JsString("2024-03-12T15:00:00.000Z"))
      result match {
        case JsSuccess(value, _) => value mustBe (Instant.parse("2024-03-12T15:00:00.000Z"))
        case JsError(_)          => fail("Failed to read JsString")
      }
    }
    "fail to read an instant" in {
      val result = DateFormats.instantFormats.reads(JsString("2024-03-12W15:00:00.000Z"))
      result match {
        case JsSuccess(value, _) => fail("Should fail to read")
        case JsError(err)        => err(0)._2(0).message mustBe ("error.expected.date.isoformat")
      }
    }
    "write" in {
      val inst = Instant.parse("2024-03-12T16:01:02.003Z")
      val str = DateFormats.instantFormats.writes(inst)
      str.as[String] mustBe ("2024-03-12T16:01:02.003Z")
    }
  }

}
