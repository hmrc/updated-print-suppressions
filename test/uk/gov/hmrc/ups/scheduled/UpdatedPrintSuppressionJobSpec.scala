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

package uk.gov.hmrc.ups.scheduled

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{atLeastOnce, verify, when}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.ups.scheduled.jobs.UpdatedPrintSuppressionJob

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class UpdatedPrintSuppressionJobSpec
  extends TestKit(ActorSystem("spec"))
    with ImplicitSender
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with Matchers  {

  val config = ConfigFactory.parseString(
    """
    akka.loglevel = DEBUG
    akka.log-config-on-start = on
    updatedPrintSuppressions.retryFailedUpdatesAfter = 1 second
    updatedPrintSuppressions.releaseLockAfter = 2 seconds
    scheduling {
      updatedPrintSuppressions {
          initialDelay = 0 second
          interval = 3 second
      }
    }
    """)

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An updated print suppression job " must {
    "execute the preferences processor in a timer" in {

      val mlr  = mock[MongoLockRepository]
      val pp = mock[PreferencesProcessor]

      when(mlr.takeLock(any[String], any[String], any[Duration])).thenReturn(Future.successful(true))
      when(pp.run(any[HeaderCarrier])).thenReturn(Future.successful(TotalCounts(2,2)))

      system.actorOf(Props(new UpdatedPrintSuppressionJob(Configuration(config), mlr, pp)))

      // TODO: Is there a better way to do this?
      Thread.sleep(3000)

      verify(mlr, atLeastOnce()).takeLock(any[String], any[String], any[Duration])
      verify(pp, atLeastOnce()).run(any[HeaderCarrier])
    }
  }
}
