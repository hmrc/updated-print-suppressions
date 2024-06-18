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

import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{ ObservableFuture, SingleObservableFuture }
import org.scalatest.DoNotDiscover
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.ups.model.PrintPreference
import uk.gov.hmrc.ups.utils.DateTimeUtils

import java.time.LocalDate
import scala.concurrent.ExecutionContext

@DoNotDiscover
class RandomDataGenerator
    extends PlaySpec with GuiceOneAppPerSuite with DefaultPlayMongoRepositorySupport[UpdatedPrintSuppressions] {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val mongoCounterRepository = app.injector.instanceOf[MongoCounterRepository]
  val repository: UpdatedPrintSuppressionsRepository =
    new UpdatedPrintSuppressionsRepository(mongoComponent, LocalDate.now().minusDays(1), mongoCounterRepository)

  val BATCH_SIZE: Int = 100000

  "RandomDataGenerator" should {

    "create 3M random records in one day" in {
      await(repository.collection.deleteMany(Filters.empty()).toFuture().map(_ => ()))
      0 to 29 foreach { i =>
        println(s"Generating records from ${i * BATCH_SIZE} to ${(i * BATCH_SIZE) + BATCH_SIZE}")
        await(repository.collection.insertMany(generateBatchSizeEntries(i * BATCH_SIZE)).toFuture())
      }
    }

    def generateBatchSizeEntries(offset: Int): List[UpdatedPrintSuppressions] =
      for (n <- List.range(offset, offset + BATCH_SIZE))
        yield UpdatedPrintSuppressions(
          new ObjectId(),
          n,
          PrintPreference(s"anId_$n", "anId", List("f1", "f2")),
          DateTimeUtils.now
        )

  }
}
