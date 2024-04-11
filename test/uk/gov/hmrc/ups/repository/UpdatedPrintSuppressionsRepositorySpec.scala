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
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.ups.model.PrintPreference
import uk.gov.hmrc.ups.utils.DateTimeUtils

import java.time.{ Instant, LocalDate }
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.reflectiveCalls

class UpdatedPrintSuppressionsRepositorySpec
    extends PlaySpec with DefaultPlayMongoRepositorySupport[UpdatedPrintSuppressions] with BeforeAndAfterEach
    with ScalaFutures with IntegrationPatience {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val TODAY: LocalDate = LocalDate.now()
  private val counterRepoStub = new MongoCounterRepository(mongoComponent) {
    var counter: AtomicInteger = new AtomicInteger(-1)

    override def next(counterName: String)(implicit ec: ExecutionContext): Future[Int] =
      Future(counter.incrementAndGet())

    def reset(): Unit =
      counter = new AtomicInteger(-1)
  }

  override def checkTtlIndex: Boolean = false

  override protected val repository = new UpdatedPrintSuppressionsRepository(mongoComponent, TODAY, counterRepoStub)

  override def beforeEach(): Unit = {
    counterRepoStub.reset()
    repository.collection.deleteMany(Filters.empty()).toFuture().futureValue
  }

  def toCounterAndPreference(ups: UpdatedPrintSuppressions): (Int, PrintPreference, Instant) =
    (ups.counter, ups.printPreference, ups.updatedAt)

  "UpdatedPrintSuppressionsRepository" should {
    repository.collection.deleteMany(Filters.empty()).toFuture().futureValue

    val now = DateTimeUtils.now

    "increment the counter and save the updated print suppression" in {

      val ppOne = PrintPreference("11111111", "someType", List.empty)
      val ppTwo = PrintPreference("22222222", "someType", List.empty)

      repository.insert(ppOne, now).futureValue
      repository.insert(ppTwo, now).futureValue

      val all: Future[Seq[UpdatedPrintSuppressions]] = repository.collection.find().toFuture()
      all.futureValue.map(toCounterAndPreference) mustBe List((0, ppOne, now), (1, ppTwo, now))
    }

    "find and return all records within a range" in {

      0 to 9 foreach (n => repository.insert(PrintPreference(s"id_$n", "a type", List.empty), now).futureValue)

      repository.find(0, 2).futureValue mustBe List(
        PrintPreference("id_0", "a type", List.empty),
        PrintPreference("id_1", "a type", List.empty)
      )
    }

    "override previous update with same utr" in {
      repository.collection.deleteMany(Filters.empty()).toFuture().futureValue

      val pp = PrintPreference("11111111", "someType", List.empty)
      val preferenceWithSameId: PrintPreference = pp.copy(formIds = List("SomeId"))

      repository.insert(pp, now).futureValue
      repository.insert(preferenceWithSameId, now.plusMillis(1)).futureValue

      val all = repository.collection.find().toFuture()
      all.futureValue.map(toCounterAndPreference) mustBe List((0, preferenceWithSameId, now.plusMillis(1)))
    }

    "duplicate keys due to race conditions are recoverable" in {
      val utr: String = "11111111"

      repository.insert(PrintPreference(utr, "someType", List.empty), now).futureValue
      repository.insert(PrintPreference(utr, "someType", List("something")), now.plusMillis(1)).futureValue

      repository.collection
        .find()
        .toFuture()
        .map(
          _.find(_.printPreference.id == utr).map(_.printPreference.formIds)
        )
        .futureValue mustBe Some(List("something"))
    }

    "not throw an duplicate key error with near simultaneous confirms" in {
      val utr: String = "11111111"
      val list = 1 until 10

      list.map(_ => repository.insert(PrintPreference(utr, "someType", List("1")), now).futureValue)

      repository.collection
        .find()
        .toFuture()
        .map(
          _.find(_.printPreference.id == utr).map(_.printPreference.formIds)
        )
        .futureValue mustBe Some(List("1"))
    }
  }
}
