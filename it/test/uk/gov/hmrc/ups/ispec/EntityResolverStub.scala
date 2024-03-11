/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.ups.ispec

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.domain.TaxIds._
import uk.gov.hmrc.ups.model.EntityId

trait EntityResolverStub {
  def stubGetEntity(entityId: EntityId, taxId: TaxIdWithName): StubMapping =
    stubFor(
      get(urlMatching(s"/entity-resolver/${entityId.value}"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |"_id": "${entityId.value}",
                         |"${taxId.name}": "${taxId.value}"
                         |}
                         |""".stripMargin))
    )

  def stubGetEntityWithStatus(entityId: EntityId, status: Int): StubMapping =
    stubFor(
      get(urlMatching(s"/entity-resolver/${entityId.value}"))
        .willReturn(aResponse().withStatus(status))
    )

  def stubExceptionOnGetEntity(entityId: EntityId): StubMapping =
    stubFor(
      get(urlMatching(s"/entity-resolver/${entityId.value}")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
    )
}
