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

package uk.gov.hmrc.ups.service

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import uk.gov.hmrc.ups.repository.UpdatedPrintSuppressionsDatabase
import uk.gov.hmrc.ups.scheduling.Result

import scala.concurrent.{ExecutionContext, Future}

class RemoveOlderCollectionsServiceSpec extends PlaySpec with ScalaFutures {

  implicit val ec = ExecutionContext.Implicits.global
  
  "remove older collections service" should {
    "pass" in new SetUp {
      when(mockDB.upsCollectionNames).thenReturn(Future.successful(List("updated_20230620")))
      when(mockDB.dropCollection(any[String])(any[ExecutionContext])).thenReturn(Future.successful(()))
      
      val result: Future[Result] = service.execute

      val value = result.futureValue.message
      
      value.contains("failures on collections []") must be(true)
      value.contains("collections [updated_20230620] successfully removed") must be(true)
    }
  }

  trait SetUp {
    val config = Configuration(
      ("removeOlderCollections.durationInDays", 1)
    )
    val mockDB = mock[UpdatedPrintSuppressionsDatabase]
    val service = new RemoveOlderCollectionsService(config, mockDB)
  }
}
