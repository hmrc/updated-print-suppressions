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

package uk.gov.hmrc.ups.repository

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.mongo.test.MongoSupport

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UpdatedPrintSuppressionsDatabaseSpec extends PlaySpec with ScalaFutures with BeforeAndAfterAll with MongoSupport {
  private val updatedPrintSuppressionsDatabase = new UpdatedPrintSuppressionsDatabase(mongoComponent)

  private val today = LocalDate.now()
  private val upsCollectionName1 = UpdatedPrintSuppressions.repoNameTemplate(today)
  private val upsCollectionName2 = UpdatedPrintSuppressions.repoNameTemplate(today.minusDays(1))
  private val counters = "counters"

  override def beforeAll(): Unit = {
    super.beforeAll()
    await(mongoComponent.database.drop().toFuture())
  }

  "collections list repo" should {
    "return a list of UPS collections" in {
      await(
        Future.sequence(
          List(
            mongoComponent.database.createCollection(upsCollectionName1).toFuture(),
            mongoComponent.database.createCollection(upsCollectionName2).toFuture(),
            mongoComponent.database.createCollection(counters).toFuture()
          )
        )
      )
      updatedPrintSuppressionsDatabase.upsCollectionNames.futureValue must contain.only(
        upsCollectionName1,
        upsCollectionName2
      )
    }
  }

  "drop collection and return true if successful" in {
    await(mongoComponent.database.createCollection("db-1").toFuture())
    await(updatedPrintSuppressionsDatabase.dropCollection("db-1"))

    mongoComponent.database.listCollectionNames().toFuture().futureValue must not contain "db-1"
  }

  override def afterAll(): Unit = {
    super.afterAll()
    val _ = await(mongoComponent.database.drop().toFuture())
  }
}
