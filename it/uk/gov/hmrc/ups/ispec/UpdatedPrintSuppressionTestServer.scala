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
import org.joda.time.LocalDate
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.integration.ServiceSpec
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.ups.repository.{MongoCounterRepository, UpdatedPrintSuppressions}

import scala.annotation.nowarn

abstract class UpdatedPrintSuppressionTestServer(override val databaseName: String = "updated-print-suppression-ispec")
    extends PlaySpec with ServiceSpec with MongoSupport with Eventually with BeforeAndAfterEach with PreferencesStub with EntityResolverStub with ScalaFutures
    with BeforeAndAfterAll {

  lazy val upsCollection: MongoCollection[UpdatedPrintSuppressions] = {
    val repoName = UpdatedPrintSuppressions.repoNameTemplate(LocalDate.now)
    await(
      mongoComponent.database
        .createCollection(
          repoName
        )
        .toFuture())
    mongoComponent.database.getCollection[UpdatedPrintSuppressions](repoName)
  }
  lazy val stubPort = 11111
  lazy val stubHost = "localhost"
  val wireMockServer: WireMockServer = new WireMockServer(wireMockConfig().port(stubPort))
  private val mongoCounterRepository = app.injector.instanceOf[MongoCounterRepository]

  override def additionalConfig: Map[String, _] =
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

  @nowarn("msg=discarded non-Unit value")
  override def beforeEach(): Unit = {
    WireMock.reset()
    await(upsCollection.deleteMany(Filters.empty()).toFuture())
    await(mongoCounterRepository.collection.deleteMany(Filters.empty()).toFuture())
  }
}
