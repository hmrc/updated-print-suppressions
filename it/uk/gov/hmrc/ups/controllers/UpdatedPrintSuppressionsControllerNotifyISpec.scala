
package uk.gov.hmrc.ups.controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import org.mongodb.scala.model.Filters
import org.scalatest.{BeforeAndAfterEach, Suite, TestSuite}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.ContentTypes
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.CONTENT_TYPE
import play.api.test.{FakeHeaders, FakeRequest, Injecting}
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.ups.repository.UpdatedPrintSuppressionsRepository

import scala.concurrent.ExecutionContext

class UpdatedPrintSuppressionsControllerNotifyISpec
  extends AnyFreeSpec
    with Matchers
    with TestSuite
    with GuiceOneServerPerSuite
    with ScalaFutures
    with IntegrationPatience
    with MongoSupport
    with BeforeAndAfterEach
    with Injecting {
  this: Suite =>

  implicit lazy val system: ActorSystem = ActorSystem()
  implicit lazy val materializer: Materializer = Materializer(system)
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure("metrics.enabled" -> false, "logger.uk.gov" -> "DEBUG")
    .build()

  private val controller = inject[UpdatedPrintSuppressionsController]
  private val repo = inject[UpdatedPrintSuppressionsRepository]

  override protected def beforeEach(): Unit = {
    repo.collection.deleteMany(Filters.empty()).toFuture().futureValue
    dropDatabase()
    super.beforeEach()
  }
  
  "POST /preferences-changed" - {

    "return 200 success" in {
      val reqBody =
        s"""{
           |  "changedValue" : "paper",
           |  "updatedAt"    : "2023-12-22T01:30:00.000Z",
           |  "taxIds"       : { "nino" : "AB112233C", "sautr" : "abcde" }
           |}""".stripMargin

      val fakePostRequest = createFakeRequest(reqBody)
      val result = controller.notifySubscriber()(fakePostRequest).futureValue
      result.header.status must be(OK)

      val repoCount = repo.collection.countDocuments().toFuture().futureValue
      repoCount must be(1)
    }

    "return 400 for missing utr" in {
      val reqBody =
        s"""{
           |  "changedValue" : "paper",
           |  "updatedAt"    : "2023-12-22T01:30:00.000Z",
           |  "taxIds"       : { "nino" : "AB112233C" }
           |}""".stripMargin

      val fakePostRequest = createFakeRequest(reqBody)
      val result = controller.notifySubscriber()(fakePostRequest).futureValue
      result.header.status must be(BAD_REQUEST)
      
      
      val body = result.body.dataStream.runWith(
        Sink.fold(ByteString.empty)(_ ++ _)).map(_.utf8String).futureValue
      
      body must include("Missing SaUtr")
      
      val repoCount = repo.collection.countDocuments().toFuture().futureValue
      repoCount must be(0)
    }

  }

  private def createFakeRequest(reqBody: String) = {
    FakeRequest(
      "POST",
      routes.UpdatedPrintSuppressionsController.notifySubscriber().url,
      FakeHeaders(Seq(CONTENT_TYPE -> ContentTypes.JSON)),
      Json.parse(reqBody)
    )
  }
}
