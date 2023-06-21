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

import play.api.{Configuration, Logger}
import uk.gov.hmrc.ups.repository.UpdatedPrintSuppressionsDatabase
import uk.gov.hmrc.ups.scheduled.{Failed, RemoveOlderCollections, Succeeded}
import uk.gov.hmrc.ups.scheduling.Result

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

@Singleton
class RemoveOlderCollectionsService @Inject()(
  configuration: Configuration,
  updatedPrintSuppressionsDatabase: UpdatedPrintSuppressionsDatabase
)(implicit ec: ExecutionContext) extends RemoveOlderCollections {
  val logger: Logger = Logger(this.getClass)

  override def repository: UpdatedPrintSuppressionsDatabase = updatedPrintSuppressionsDatabase
  
  val name: String = "removeOlderCollections"
  
  def execute: Future[Result] = {
    removeOlderThan(durationInDays).map { totals =>
      (totals.failures ++ totals.successes).foreach {
        case Succeeded(collectionName) =>
          logger.info(s"successfully removed collection $collectionName older than $durationInDays in $name job")
  
        case Failed(collectionName, ex) =>
          val msg = s"attempted to removed collection $collectionName and failed in $name job"
          ex.fold(logger.error(msg)) {
            logger.error(msg, _)
          }
      }
      val text =
        s"""Completed $name with:
           |- failures on collections [${totals.failures.map(_.collectionName).mkString(",")}]
           |- collections [${totals.successes.map(_.collectionName).sorted.mkString(",")}] successfully removed
           |""".stripMargin
      Result(text)
    }
  }

  private lazy val durationInDays = {
    val days = configuration
      .getOptional[Int](s"$name.durationInDays")
      .getOrElse(throw new IllegalStateException(s"Config key $name.durationInDays missing"))
    FiniteDuration(days, TimeUnit.DAYS)
  }

}
