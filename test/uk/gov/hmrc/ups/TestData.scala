/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.ups

/*
 * Copyright 2026 HM Revenue & Customs
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

import java.time.Instant

object TestData {
  val TEST_DAY_1 = 1
  val TEST_MONTH_11 = 11
  val TEST_YEAR_2025 = 2025
  val TEST_YEAR_2026 = 2026

  val EPOCH_MILI_SECONDS: Long = 567245389L
  val TEST_TIME_INSTANT: Instant = Instant.ofEpochMilli(EPOCH_MILI_SECONDS)

  val TEST_ID = "test_id"
  val TEST_ID_TYPE_SAUTR = "sautr"
  val TEST_FORM_ID = "form_id"
}
