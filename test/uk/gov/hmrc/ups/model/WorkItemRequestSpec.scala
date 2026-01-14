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

import play.api.libs.json.{ JsResultException, Json }
import uk.gov.hmrc.ups.TestData.TEST_TIME_INSTANT
import uk.gov.hmrc.ups.SpecBase

class WorkItemRequestSpec extends SpecBase {

  "Filters.format" must {
    import Filters.format

    "read the json correctly" in new Setup {
      Json.parse(filtersJsonString).as[Filters] mustBe filters
    }

    "throw the exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(filtersInvalidJsonString).as[Filters]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(filters) mustBe Json.parse(filtersJsonString)
    }
  }

  "WorkItemRequest.format" must {
    import WorkItemRequest.format

    "read the json correctly" in new Setup {
      Json.parse(workItemRequestJsonString).as[WorkItemRequest] mustBe workItemRequest
    }

    "throw the exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(workItemRequestInvalidJsonString).as[WorkItemRequest]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(workItemRequest) mustBe Json.parse(workItemRequestJsonString)
    }
  }

  trait Setup {
    val filters: Filters = Filters(TEST_TIME_INSTANT, TEST_TIME_INSTANT)
    val workItemRequest: WorkItemRequest = WorkItemRequest(filters)

    val filtersJsonString: String =
      """{"failedBefore":"1970-01-07T13:34:05.389Z","availableBefore":"1970-01-07T13:34:05.389Z"}""".stripMargin

    val filtersInvalidJsonString: String =
      """{"availableBefore":"1970-01-07T13:34:05.389Z"}""".stripMargin

    val workItemRequestJsonString: String =
      """{"filters":{"failedBefore":"1970-01-07T13:34:05.389Z","availableBefore":"1970-01-07T13:34:05.389Z"}}""".stripMargin

    val workItemRequestInvalidJsonString: String =
      """{"filters":{"availableBefore":"1970-01-07T13:34:05.389Z"}}""".stripMargin

  }
}
