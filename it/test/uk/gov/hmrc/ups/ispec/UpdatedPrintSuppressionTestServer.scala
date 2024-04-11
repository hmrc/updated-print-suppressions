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

package uk.gov.hmrc.ups.ispec

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.{ Eventually, IntegrationPatience, ScalaFutures }
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.{ Application, Environment, Logger, Mode }
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.ups.repository.{ MongoCounterRepository, UpdatedPrintSuppressions }

import java.time.{ LocalDate, LocalDateTime }
import java.time.format.DateTimeFormatter

abstract class UpdatedPrintSuppressionTestServer(override val databaseName: String = "updated-print-suppression-ispec")
    extends PlaySpec with MongoSupport with Eventually with BeforeAndAfterEach with PreferencesStub
    with EntityResolverStub with ScalaFutures with BeforeAndAfterAll with IntegrationPatience
    with GuiceOneServerPerSuite {

  private val logger = Logger(getClass)

  override def fakeApplication(): Application = {
    logger.info(s"""Starting application with additional config:
                   |  ${configMap.mkString("\n  ")}
       """.stripMargin)
    // If applicationMode is not set, use Mode.Test (the default for GuiceApplicationBuilder)
    GuiceApplicationBuilder(environment = Environment.simple(mode = applicationMode.getOrElse(Mode.Test)))
      .configure(configMap)
      .build()
  }

  def testName: String =
    getClass.getSimpleName

  // If applicationMode is set, default to Mode.Dev, to preserve earlier behaviour
  def applicationMode: Option[Mode] =
    Some(Mode.Dev)

  protected val testId =
    TestId(testName)

  protected def serviceMongoUri =
    s"mongodb://localhost:27017/${testId.toString}"

  private lazy val mongoConfig =
    Map(s"mongodb.uri" -> serviceMongoUri)

  private lazy val configMap = mongoConfig ++ additionalConfig

  lazy val upsCollection: MongoCollection[UpdatedPrintSuppressions] = {
    val repoName = UpdatedPrintSuppressions.repoNameTemplate(LocalDate.now)
    await(
      mongoComponent.database
        .createCollection(
          repoName
        )
        .toFuture()
    )
    mongoComponent.database.getCollection[UpdatedPrintSuppressions](repoName)
  }
  lazy val stubPort = 11111
  lazy val stubHost = "localhost"
  val wireMockServer: WireMockServer = new WireMockServer(wireMockConfig().port(stubPort))
  private val mongoCounterRepository = app.injector.instanceOf[MongoCounterRepository]

  def additionalConfig: Map[String, _] =
    Map[String, Any](
      "auditing.consumer.baseUri.host"                   -> stubHost,
      "auditing.consumer.baseUri.port"                   -> stubPort,
      "microservice.services.preferences.host"           -> stubHost,
      "microservice.services.preferences.port"           -> stubPort,
      "microservice.services.entity-resolver.host"       -> stubHost,
      "microservice.services.entity-resolver.port"       -> stubPort,
      "scheduling.updatedPrintSuppressions.initialDelay" -> "10 hours",
      "mongodb.uri"                                      -> s"mongodb://localhost:27017/$databaseName"
    )

  override def beforeAll(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterAll(): Unit =
    wireMockServer.stop()

  override def beforeEach(): Unit = {
    WireMock.reset()
    await(upsCollection.deleteMany(Filters.empty()).toFuture())
    await(mongoCounterRepository.collection.deleteMany(Filters.empty()).toFuture())
  }
}

case class TestId(testName: String) {

  val runId: String =
    DateTimeFormatter.ofPattern("HHmmssSSS").format(LocalDateTime.now())

  override val toString =
    s"${testName.toLowerCase.take(30)}-$runId"
}
