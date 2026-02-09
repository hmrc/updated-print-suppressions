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

package uk.gov.hmrc.ups.repository

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.mongo.test.MongoSupport
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{ ObservableFuture, SingleObservableFuture }
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.ups.model.PrintPreference
import uk.gov.hmrc.ups.utils.DateTimeUtils
import scala.concurrent.ExecutionContext.Implicits.global
import java.time.temporal.{ ChronoUnit, Temporal, TemporalAmount }
import java.time.{ Instant, LocalDate }
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.reflectiveCalls

class UpdatedPrintSuppressionsRepositorySpec
    extends PlaySpec with MongoSupport with BeforeAndAfterEach with ScalaFutures with IntegrationPatience {

  val counterRepoStub = new CounterRepoStub(mongoComponent)
  private val TODAY: LocalDate = LocalDate.now()
  private val testConfiguration: Configuration = Configuration(
    "updatedPrintSuppressions.expiryDurationInDays" -> 30
  )

  protected val repository = new UpdatedPrintSuppressionsRepository(mongoComponent, TODAY, counterRepoStub)
  val now = DateTimeUtils.now

  def toCounterAndPreference(ups: UpdatedPrintSuppressions): (Int, PrintPreference, Instant) =
    (ups.counter, ups.printPreference, ups.updatedAt)

  override def beforeEach(): Unit = {
    counterRepoStub.reset()
    repository.collection.deleteMany(Filters.empty()).toFuture().futureValue
  }

  "insert" should {
    "increment counter and store printPreference" in {
      val ppOne = PrintPreference("11111111", "someType", List.empty)
      val ppTwo = PrintPreference("22222222", "someType", List.empty)

      repository.insert(ppOne, now).futureValue
      repository.insert(ppTwo, now).futureValue

      val all: Future[Seq[UpdatedPrintSuppressions]] = repository.collection.find().toFuture()
      all.futureValue.map(toCounterAndPreference) mustBe List((0, ppOne, now), (1, ppTwo, now))
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

    "create separate records for same id but different idType" in {
      val ppTypeA = PrintPreference("11111111", "someTypeA", List("somethingA"))
      val ppTypeB = PrintPreference("11111111", "someTypeB", List("somethingB"))

      repository.insert(ppTypeA, now).futureValue
      repository.insert(ppTypeB, now).futureValue

      val all = repository.collection.find().toFuture()
      all.futureValue.map(_.printPreference).toSet mustBe Set(ppTypeA, ppTypeB)
    }
  }

  "find" should {
    "return all records within a range" in {
      0 to 9 foreach (n => repository.insert(PrintPreference(s"id_$n", "a type", List.empty), now).futureValue)

      repository.find(0, 2).futureValue mustBe List(
        PrintPreference("id_0", "a type", List.empty),
        PrintPreference("id_1", "a type", List.empty)
      )
    }

    "return empty list when collection is empty" in {
      repository.find(0, 10).futureValue mustBe List.empty
    }

    "return empty list when offset is beyond available records" in {
      0 to 2 foreach (n => repository.insert(PrintPreference(s"id_$n", "a type", List.empty), now).futureValue)

      repository.find(11, 10).futureValue mustBe List.empty
    }

    "return records from middle of range" in {
      0 to 9 foreach (n => repository.insert(PrintPreference(s"id_$n", "a type", List.empty), now).futureValue)

      repository.find(5, 3).futureValue mustBe List(
        PrintPreference("id_5", "a type", List.empty),
        PrintPreference("id_6", "a type", List.empty),
        PrintPreference("id_7", "a type", List.empty)
      )
    }

    "return empty list when limit is 0" in {
      0 to 2 foreach (n => repository.insert(PrintPreference(s"id_$n", "a type", List.empty), now).futureValue)

      repository.find(0, 0).futureValue mustBe List.empty
    }
  }

  "count" should {
    "return 0 when collection is empty" in {
      repository.count().futureValue mustBe 0
    }

    "return correct count after inserts" in {
      repository.insert(PrintPreference("11111111", "type1", List.empty), now).futureValue
      repository.insert(PrintPreference("22222222", "type2", List.empty), now).futureValue
      repository.insert(PrintPreference("33333333", "type3", List.empty), now).futureValue

      repository.count().futureValue mustBe 3
    }

    "not increment count when updating existing record" in {
      val pp = PrintPreference("11111111", "someType", List.empty)
      val updatedPp = pp.copy(formIds = List("newForm"))

      repository.insert(pp, now).futureValue
      repository.insert(updatedPp, now.plusMillis(1)).futureValue

      repository.count().futureValue mustBe 1
    }
  }

  "updatedAt field" should {
    "be stored correctly on new insert" in {
      val pp = PrintPreference("11111111", "someType", List.empty)
      val timestamp = now.minus(5, ChronoUnit.DAYS)

      repository.insert(pp, timestamp).futureValue

      val result = repository.collection.find().toFuture().futureValue
      result.head.updatedAt mustBe timestamp
    }
  }

  class CounterRepoStub(mongoComponent: MongoComponent) extends MongoCounterRepository(mongoComponent) {
    var counter: AtomicInteger = new AtomicInteger(-1)

    override def next(counterName: String)(implicit ec: ExecutionContext): Future[Int] =
      Future(counter.incrementAndGet())

    def reset(): Unit =
      counter = new AtomicInteger(-1)
  }

}
