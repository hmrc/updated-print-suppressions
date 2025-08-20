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

package uk.gov.hmrc.ups.service

import org.apache.pekko.Done
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.testkit.TestSubscriber
import org.apache.pekko.stream.testkit.scaladsl.TestSink
import org.apache.pekko.testkit.TestKit
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{ never, times, verify, verifyNoMoreInteractions, when }
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.mongo.lock.{ Lock, LockRepository }
import uk.gov.hmrc.ups.UpsRemoveOlderCollectionsConfig
import uk.gov.hmrc.ups.repository.UpdatedPrintSuppressionsDatabase
import uk.gov.hmrc.ups.scheduling.Result

import java.time.Instant
import scala.concurrent.{ ExecutionContext, Future }

class RemoveOlderCollectionsServiceSpec extends PlaySpec with ScalaFutures {

  val testKit = ActorTestKit()
  implicit val system: ActorSystem = testKit.system.classicSystem
  implicit lazy val materializer: Materializer = Materializer(system)
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  "remove older collections service" should {
    "pass" in new SetUp {
      when(mockDB.upsCollectionNames).thenReturn(Future.successful(List("updated_20230620")))
      when(mockDB.dropCollection(any[String])(any[ExecutionContext])).thenReturn(Future.successful(()))
      when(lockRepo.releaseLock(any, any)).thenReturn(Future.successful(()))

      val result: Future[Result] = service.execute

      val value = result.futureValue.message

      value.contains("failures on collections []") must be(true)
      value.contains("collections [updated_20230620] successfully removed") must be(true)
    }

    "emits elements correctly" in new SetUp {
      service.start()
      probeSubscriber
        .request(2)
        .expectNext(())
        .expectNext(())

    }

    "should respect configured delays and intervals" in new SetUp {
      val startTime = System.currentTimeMillis()
      service.start()

      probeSubscriber
        .request(2)
        .expectNext(()) // Should arrive after ~100ms

      val firstElementTime = System.currentTimeMillis()
      (firstElementTime - startTime) must be >= 100L

      probeSubscriber
        .expectNext(()) // Should arrive after another ~200ms

      val secondElementTime = System.currentTimeMillis()
      (secondElementTime - firstElementTime) must be >= 200L
    }

    "should successfully call external services during workload processing" in new SetUp {
      // Any date more than 1 day old will do
      when(mockDB.upsCollectionNames(any))
        .thenReturn(
          Future.successful(List("updated_print_suppressions_20250801", "updated_print_suppressions_20250802"))
        )
      when(mockDB.dropCollection(any)(any)).thenReturn(Future.successful(()))
      when(lockRepo.releaseLock(any, any)).thenReturn(Future.successful(()))

      service.start()

      probeSubscriber
        .request(1)
        .expectNext(())

      // Verify external service was called
      verify(mockDB).upsCollectionNames(any)
      verify(mockDB).dropCollection(ArgumentMatchers.eq("updated_print_suppressions_20250801"))(any[ExecutionContext])
      verify(mockDB).dropCollection(ArgumentMatchers.eq("updated_print_suppressions_20250802"))(any[ExecutionContext])
      verifyNoMoreInteractions(mockDB)
    }

    "should successfully call external services during workload processing, 2 cycles" in new SetUp {
      // Any date more than 1 day old will do
      when(mockDB.upsCollectionNames(any))
        .thenReturn(Future.successful(List("updated_print_suppressions_20250801")))
        .thenReturn(Future.successful(List("updated_print_suppressions_20250802")))

      when(mockDB.dropCollection(any)(any)).thenReturn(Future.successful(()))
      when(lockRepo.releaseLock(any, any)).thenReturn(Future.successful(()))

      service.start()

      probeSubscriber
        .request(2)
        .expectNext(())
        .expectNext(())

      // Verify external service was called
      verify(mockDB, times(2)).upsCollectionNames(any)
      verify(mockDB).dropCollection(ArgumentMatchers.eq("updated_print_suppressions_20250801"))(any[ExecutionContext])
      verify(mockDB).dropCollection(ArgumentMatchers.eq("updated_print_suppressions_20250802"))(any[ExecutionContext])
      verifyNoMoreInteractions(mockDB)
    }

    "should recover after a date format failure" in new SetUp {
      // Any date more than 1 day old will do
      when(mockDB.upsCollectionNames(any))
        .thenReturn(Future.successful(List("updated_print_suppressions_202508001"))) // This will fail - date formatting
        .thenReturn(Future.successful(List("updated_print_suppressions_20250802")))

      when(mockDB.dropCollection(any)(any)).thenReturn(Future.successful(()))
      when(lockRepo.releaseLock(any, any)).thenReturn(Future.successful(()))

      service.start()

      probeSubscriber
        .request(2)
        .expectNext(())
        .expectNext(())

      // Verify external service was called
      verify(mockDB, times(2)).upsCollectionNames(any)
      verify(mockDB, never()).dropCollection(ArgumentMatchers.eq("updated_print_suppressions_20250801"))(
        any[ExecutionContext]
      )
      verify(mockDB).dropCollection(ArgumentMatchers.eq("updated_print_suppressions_20250802"))(any[ExecutionContext])
      verifyNoMoreInteractions(mockDB)
    }

    "should recover after a drop collection failure" in new SetUp {
      // Any date more than 1 day old will do
      when(mockDB.upsCollectionNames(any))
        .thenReturn(Future.successful(List("updated_print_suppressions_20250801")))
        .thenReturn(Future.successful(List("updated_print_suppressions_20250802")))

      private val runtimeException = new RuntimeException("oops")
      when(mockDB.dropCollection(ArgumentMatchers.eq("updated_print_suppressions_20250801"))(any))
        .thenThrow(runtimeException)
      when(lockRepo.releaseLock(any, any)).thenReturn(Future.failed(runtimeException))

      when(mockDB.dropCollection(ArgumentMatchers.eq("updated_print_suppressions_20250802"))(any))
        .thenReturn(Future.successful(()))
      when(lockRepo.releaseLock(any, any)).thenReturn(Future.successful(()))

      service.start()

      probeSubscriber
        .request(2)
        .expectNext(())
        .expectNext(())

      // Verify external service was called
      verify(mockDB, times(2)).upsCollectionNames(any)
      verify(mockDB).dropCollection(ArgumentMatchers.eq("updated_print_suppressions_20250801"))(any[ExecutionContext])
      verify(mockDB).dropCollection(ArgumentMatchers.eq("updated_print_suppressions_20250802"))(any[ExecutionContext])
      verifyNoMoreInteractions(mockDB)
    }
  }

  trait SetUp {
    val configuration = Configuration(
      "removeOlderCollections.durationInDays"          -> 1,
      "scheduling.removeOlderCollections.initialDelay" -> "100 millis",
      "scheduling.removeOlderCollections.interval"     -> "200 millis",
      "scheduling.removeOlderCollections.taskEnabled"  -> true
    )
    val mockDB = mock[UpdatedPrintSuppressionsDatabase]
    val lockRepo = mock[LockRepository]
    val lifecycle = mock[ApplicationLifecycle]
    val config = UpsRemoveOlderCollectionsConfig(configuration)

    when(lockRepo.takeLock(any, any, any))
      .thenReturn(Future.successful(Some(Lock("1", "owner", Instant.now(), Instant.now().plusSeconds(2)))))

    val (probeSubscriber, probeSink) = TestSink.probe[Unit].preMaterialize()
    val service =
      new RemoveOlderCollectionsService(configuration, mockDB, lockRepo, lifecycle, config, sink = probeSink)
  }
}
