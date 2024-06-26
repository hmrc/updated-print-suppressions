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

import org.mongodb.scala.model.Filters
import org.mongodb.scala.{ ObservableFuture, SingleObservableFuture }
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.*
import uk.gov.hmrc.mongo.test.MongoSupport

import scala.concurrent.ExecutionContext

class MongoCounterRepositorySpec
    extends PlaySpec with MongoSupport with BeforeAndAfterEach with ScalaFutures with IntegrationPatience {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  protected val repository = new MongoCounterRepository(mongoComponent)

  override def beforeEach(): Unit =
    await(repository.collection.deleteMany(Filters.empty()).toFuture().map(_ => ()))

  "The counter repository" should {
    "initialise to zero" in {
      repository.next("test-counter").futureValue mustBe 1
      val counter: Counter = await(repository.collection.find().toFuture()).head
      counter.value mustBe 1
      counter.name mustBe "test-counter"
    }

    "initialise to zero only if the value doesn't exist already" in {
      await(repository.next("test-counter"))
      val repository1 = new MongoCounterRepository(mongoComponent)
      repository1.collection
        .find()
        .toFuture()
        .map {
          _.headOption.map(head => (head.value, head.name))
        }
        .futureValue mustBe Some((1, "test-counter"))
    }

    "increment and return the next value" in {
      await(repository.next("test-counter"))
      await(repository.next("test-counter"))

      repository.collection.countDocuments().toFuture().futureValue mustBe 1
      repository.collection
        .find()
        .toFuture()
        .map {
          _.headOption.map(head => (head.value, head.name))
        }
        .futureValue mustBe Some((2, "test-counter"))
    }
  }
}
