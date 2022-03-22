/*
 * Copyright 2022 HM Revenue & Customs
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

import org.joda.time.{ DateTime, LocalDate }
import org.mongodb.scala.model.Filters
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.time.DateTimeUtils
import uk.gov.hmrc.ups.model.PrintPreference

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.reflectiveCalls

class UpdatedPrintSuppressionsRepositorySpec
    extends PlaySpec with DefaultPlayMongoRepositorySupport[UpdatedPrintSuppressions] with BeforeAndAfterEach with ScalaFutures with IntegrationPatience
    with GuiceOneAppPerSuite {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val TODAY: LocalDate = new LocalDate()
  private val counterRepoStub = new MongoCounterRepository(mongoComponent) {
    var counter: Int = -1

    override def next(counterName: String)(implicit ec: ExecutionContext): Future[Int] = {
      counter = counter + 1
      Future(counter)(ec)
    }

    def reset(): Unit =
      counter = -1
  }

  override protected def repository = new UpdatedPrintSuppressionsRepository(mongoComponent, TODAY, counterRepoStub)

  override def beforeEach(): Unit = {
    counterRepoStub.reset()
    await(repository.collection.deleteMany(Filters.empty()).toFuture().map(_ => ()))
  }

  def toCounterAndPreference(ups: UpdatedPrintSuppressions): (Int, PrintPreference, DateTime) =
    (ups.counter, ups.printPreference, ups.updatedAt)

  "UpdatedPrintSuppressionsRepository" should {

    val now = DateTimeUtils.now

    "increment the counter and save the updated print suppression" in {

      val ppOne = PrintPreference("11111111", "someType", List.empty)
      val ppTwo = PrintPreference("22222222", "someType", List.empty)

      await(repository.insert(ppOne, now))
      await(repository.insert(ppTwo, now))

      val all: Future[Seq[UpdatedPrintSuppressions]] = repository.collection.find().toFuture()
      await(all).map { toCounterAndPreference } mustBe List((0, ppOne, now), (1, ppTwo, now))
    }

    "find and return all records within a range" in {

      0 to 9 foreach (n => await(repository.insert(PrintPreference(s"id_$n", "a type", List.empty), now)))

      repository.find(0, 2).futureValue mustBe List(
        PrintPreference("id_0", "a type", List.empty),
        PrintPreference("id_1", "a type", List.empty)
      )
    }

    "override previous update with same utr" in {
      val pp = PrintPreference("11111111", "someType", List.empty)
      val preferenceWithSameId: PrintPreference = pp.copy(formIds = List("SomeId"))

      await(repository.insert(pp, now))
      await(repository.insert(preferenceWithSameId, now.plusMillis(1)))

      val all = repository.collection.find().toFuture()
      await(all).map { toCounterAndPreference } mustBe List((0, preferenceWithSameId, now.plusMillis(1)))
    }

    "duplicate keys due to race conditions are recoverable" in {
      val utr: String = "11111111"

      await(
        Future.sequence(
          List(
            repository.insert(PrintPreference(utr, "someType", List.empty), now),
            repository.insert(PrintPreference(utr, "someType", List("something")), now.plusMillis(1))
          )
        )
      )

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

      await(
        Future.sequence(list.map(_ => repository.insert(PrintPreference(utr, "someType", List("1")), now)))
      )
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
