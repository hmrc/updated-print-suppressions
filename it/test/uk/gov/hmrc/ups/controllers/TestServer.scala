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

import com.codahale.metrics.SharedMetricRegistries
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.WsScalaTestClient
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.{ Application, Environment, Mode }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{ WSClient, WSRequest }
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.ups.repository.MongoCounterRepository

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

trait TestServer
    extends AnyWordSpec with ScalaFutures with IntegrationPatience with GuiceOneServerPerSuite with WsScalaTestClient
    with BeforeAndAfterEach with MongoSupport {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder(environment = Environment.simple(mode = applicationMode.getOrElse(Mode.Test)))
      .configure(
        s"mongodb.uri"     -> serviceMongoUri,
        "play.http.router" -> "testOnlyDoNotUseInAppConf.Routes",
        "metrics.jvm"      -> false
      )
      .build()

  def serviceMongoUri =
    s"mongodb://localhost:27017/${testId.toString}"

  def testName: String =
    getClass.getSimpleName

  // If applicationMode is set, default to Mode.Dev, to preserve earlier behaviour
  def applicationMode: Option[Mode] =
    Some(Mode.Dev)

  protected val testId =
    TestId(testName)

  implicit val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val testCounterRepository: MongoCounterRepository = app.injector.instanceOf[MongoCounterRepository]

  override def beforeEach(): Unit =
    SharedMetricRegistries.clear()

  def preferencesSaIndividualPrintSuppression(
    updatedOn: Option[String],
    offset: Option[String],
    limit: Option[String],
    isAdmin: Boolean = false
  ): WSRequest = {

    val queryString = Seq(
      updatedOn.map(value => "updated-on" -> value),
      offset.map(value => "offset" -> value),
      limit.map(value => "limit" -> value)
    ).flatten

    if (isAdmin) {
      wsUrl("/test-only/preferences/sa/individual/print-suppression").withQueryStringParameters(queryString: _*)
    } else {
      wsUrl("/preferences/sa/individual/print-suppression").withQueryStringParameters(queryString: _*)
    }
  }

  def get(url: WSRequest) = url.get().futureValue

}

case class TestId(testName: String) {

  val runId =
    DateTimeFormatter.ofPattern("HHmmssSSS").format(LocalDateTime.now())

  override val toString =
    s"${testName.toLowerCase.take(30)}-$runId"
}
