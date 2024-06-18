/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.ups.scheduler

import org.mongodb.scala.SingleObservableFuture
import org.scalatest.{ BeforeAndAfterEach, Ignore }
import org.scalatest.concurrent.{ Eventually, IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.mongo.test.MongoSupport

import java.time.LocalDate
// DO NOT DELETE reactivemongo.play.json.ImplicitBSONHandlers._ even if your IDE tells you it is unnecessary
import play.api.test.Helpers._
import uk.gov.hmrc.ups.model.PrintPreference
import uk.gov.hmrc.ups.repository.UpdatedPrintSuppressions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Ignore
class CollectionRemovalISpec
    extends PlaySpec with GuiceOneServerPerSuite with ScalaFutures with MongoSupport with IntegrationPatience
    with Eventually with BeforeAndAfterEach {

  private val expirationPeriod = 3

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        s"mongodb.uri"                                         -> s"mongodb://localhost:27017/$databaseName",
        s"Test.removeOlderCollections.durationInDays"          -> expirationPeriod,
        s"Test.scheduling.removeOlderCollections.initialDelay" -> "10 days",
        s"Test.scheduling.removeOlderCollections.interval"     -> "24 hours"
      )
      .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    dropDatabase()
  }

  "removeOlderThan" should {
    "delete only those collections in the database that are older than the provided duration" in new SetUp {
      await {
        Future.sequence(
          (0 to 4).toList.map { days =>
            mongoComponent.database.createCollection(repoName(days)).toFuture()
          }
        )
      }
    }
  }

  trait SetUp {
    val pref = PrintPreference("11111111", "someType", List.empty)

    def daysFromToday(count: Int): LocalDate = LocalDate.now().minusDays(count)

    def repoName(daysToDecrement: Int): String =
      UpdatedPrintSuppressions.repoNameTemplate(daysFromToday(daysToDecrement))
  }
}
