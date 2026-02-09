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
import org.mongodb.scala.SingleObservableFuture
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.json.OFormat
import play.api.test.Helpers.*
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.ups.model.PrintPreference
import uk.gov.hmrc.ups.repository.{ MongoCounterRepository, UpdatedPrintSuppressions, UpdatedPrintSuppressionsRepository, UpsRepository }

import java.time.{ LocalDate, ZoneOffset }
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext

trait TestSetup extends PlaySpec with ScalaFutures with BeforeAndAfterEach with MongoSupport with TestServer {

  override lazy val mongoComponent = app.injector.instanceOf[MongoComponent]
  lazy val configuration: Configuration = app.injector.instanceOf[Configuration]

  val mongoCounterRepository: MongoCounterRepository
  val today: LocalDate = LocalDate.now
  val yesterday: LocalDate = today.minusDays(1)

  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE

  val todayString: String = dateFormatter.format(today)
  val yesterdayAsString: String = dateFormatter.format(yesterday)

  implicit val ppFormats: OFormat[PrintPreference] = PrintPreference.formats
  implicit val upsFormats: OFormat[UpdatedPrintSuppressions] = UpdatedPrintSuppressions.formats
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  lazy val repoToday: UpdatedPrintSuppressionsRepository =
    new UpdatedPrintSuppressionsRepository(mongoComponent, today, mongoCounterRepository)

  lazy val repoYesterday: UpdatedPrintSuppressionsRepository =
    new UpdatedPrintSuppressionsRepository(mongoComponent, yesterday, mongoCounterRepository)

  def todayAtStartOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC)
  def yesterdayAtStartOfDay = yesterday.atStartOfDay().toInstant(ZoneOffset.UTC)

  protected def initializeRepos(): Unit = {
    val _ = repoToday
    val _ = repoYesterday
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    initializeRepos()
    await(repoYesterday.collection.deleteMany(Filters.empty()).toFuture())
  }

  override def afterEach(): Unit = {
    super.afterEach()
    await(repoYesterday.collection.deleteMany(Filters.empty()).toFuture())
  }
}
