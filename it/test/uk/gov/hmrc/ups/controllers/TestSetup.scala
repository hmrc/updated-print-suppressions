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

package uk.gov.hmrc.ups.controllers

import org.mongodb.scala.model.Filters
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.OFormat
import play.api.test.Helpers._
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.ups.model.PrintPreference
import uk.gov.hmrc.ups.repository.{ MongoCounterRepository, UpdatedPrintSuppressionsRepository }

import java.time.{ LocalDate, ZoneOffset }
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext

trait TestSetup extends PlaySpec with ScalaFutures with BeforeAndAfterEach with MongoSupport {

  val mongoCounterRepository: MongoCounterRepository
  val today: LocalDate = LocalDate.now
  val yesterday: LocalDate = today.minusDays(1)

  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE

  val todayString: String = dateFormatter.format(today)
  val yesterdayAsString: String = dateFormatter.format(yesterday)

  implicit val ppFormats: OFormat[PrintPreference] = PrintPreference.formats
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  // Reset the counters
  await(mongoCounterRepository.collection.deleteMany(Filters.empty()).toFuture())

  val repoToday = new UpdatedPrintSuppressionsRepository(mongoComponent, today, mongoCounterRepository)
  await(repoToday.collection.deleteMany(Filters.empty()).toFuture())

  val repoYesterday = new UpdatedPrintSuppressionsRepository(mongoComponent, yesterday, mongoCounterRepository)
  await(repoYesterday.collection.deleteMany(Filters.empty()).toFuture())

  def todayAtStartOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC)
  def yesterdayAtStartOfDay = yesterday.atStartOfDay().toInstant(ZoneOffset.UTC)

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repoYesterday.collection.drop().toFuture())
  }

  override def afterEach(): Unit = {
    super.afterEach()
    await(repoYesterday.collection.drop().toFuture())
  }
}
