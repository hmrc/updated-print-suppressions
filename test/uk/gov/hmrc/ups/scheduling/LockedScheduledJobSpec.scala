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

package uk.gov.hmrc.ups.scheduling

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Millis, Span }
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.ups.scheduled.jobs.UpdatedPrintSuppressionJob
import uk.gov.hmrc.ups.service.UpdatedPrintSuppressionService

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.util.Try

class LockedScheduledJobSpec
    extends AnyWordSpec with Matchers with ScalaFutures with GuiceOneAppPerTest with BeforeAndAfterAll with BeforeAndAfterEach with MockitoSugar {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure("mongodb.uri" -> "mongodb://localhost:27017/test-play-schedule")
      .build()

  val mongoComponent: MongoComponent = MongoComponent("mongodb://localhost:27017/test-play-schedule")

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(500, Millis), interval = Span(500, Millis))

//  class SimpleJob(val name: String) extends LockedScheduledJob {
//
//    val mockRunModeBridge = mock[RunModeBridge]
//
//    override val releaseLockAfter: Duration = Duration(5, TimeUnit.SECONDS)
//
//    val start = new CountDownLatch(1)
//
//    override val lockRepo: LockRepository = app.injector.instanceOf[MongoLockRepository] //  new MongoLockRepository()(ec)
//
//    def continueExecution(): Unit = start.countDown()
//
//    val executionCount = new AtomicInteger(0)
//
//    def executions: Int = executionCount.get()
//
//    override def executeInLock(implicit ec: ExecutionContext): Future[Result] =
//      Future.successful {
//        start.await(10, TimeUnit.SECONDS)
//        Result(executionCount.incrementAndGet().toString)
//      }
//
//    override lazy val initialDelay: FiniteDuration = FiniteDuration(1, TimeUnit.SECONDS)
//
//    override lazy val interval: FiniteDuration = FiniteDuration(1, TimeUnit.SECONDS)
//
//    override def runModeBridge: RunModeBridge = mockRunModeBridge
//  }

  trait Setup {
    val mockService = mock[UpdatedPrintSuppressionService]
    val mockLockRepository = mock[MongoLockRepository]
    val mockRunModeBridge = mock[RunModeBridge]
//    when(mockRunModeBridge.scheduledJobConfig(*)).thenReturn(ScheduledJobConfig(10.seconds, 10.seconds, true))
    when(mockRunModeBridge.getOptionalMillisForScheduling(any[String], matches("lockDuration")))
      .thenReturn(Option(Duration(1, TimeUnit.SECONDS)))

    when(mockRunModeBridge.getMillisForScheduling(any[String], matches("initialDelay")))
      .thenReturn(Duration(0, TimeUnit.SECONDS))

    when(mockRunModeBridge.getMillisForScheduling(any[String], matches("interval")))
      .thenReturn(Duration(0, TimeUnit.SECONDS))

    val job = new UpdatedPrintSuppressionJob(mockLockRepository, mockService, mockRunModeBridge)
  }

  override def afterAll(): Unit =
    // shutdown mongo system
    mongoComponent.client.close()

  "ExclusiveScheduledJob" should {

    "let job run in sequence" in new Setup {
      when(mockLockRepository.takeLock(any[String], any[String], any[Duration])).thenReturn(Future.successful(true))
      when(mockLockRepository.releaseLock(any[String], any[String])).thenReturn(Future.successful(()))
      when(mockService.execute).thenReturn(Future.successful(Result("")))

      val result1 = Await.result(job.execute, 1.minute)
      result1.message should include("updatedPrintSuppressions run and completed with result")

      val result2 = Await.result(job.execute, 1.minute)
      result2.message should include("updatedPrintSuppressions run and completed with result")
    }

    "not allow job to run in parallel" in new Setup {
      when(mockLockRepository.takeLock(any[String], any[String], any[Duration])).thenReturn(Future.successful(false))
      val result1 = Await.result(job.execute, 1.minute)
      result1.message should include("Job with updatedPrintSuppressions cannot aquire mongo lock, not running")
    }

    "should tolerate exceptions in execution" in new Setup {
      when(mockLockRepository.takeLock(any[String], any[String], any[Duration])).thenReturn(Future.successful(true))
      when(mockLockRepository.releaseLock(any[String], any[String])).thenReturn(Future.successful(()))
      when(mockService.execute).thenThrow(new RuntimeException("whatever"))

      Try {
        job.execute.futureValue
      }.isFailure shouldBe true
    }
  }

}
