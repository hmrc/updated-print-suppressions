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

package uk.gov.hmrc.ups.scheduled

import uk.gov.hmrc.ups.repository.{ UpdatedPrintSuppressions, UpdatedPrintSuppressionsDatabase }

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

trait RemoveOlderCollections extends DeleteCollectionFilter with SelectAndRemove {

  def repository: UpdatedPrintSuppressionsDatabase

  def removeOlderThan(days: Duration)(implicit ec: ExecutionContext): Future[Totals] =
    compose(() => repository.upsCollectionNames, repository.dropCollection, filterUpsCollectionsOnly(_, days))

}

trait SelectAndRemove {

  def compose(
    listCollections: () => Future[List[String]],
    expireCollection: String => Future[Unit],
    filter: String => Boolean
  )(implicit ec: ExecutionContext): Future[Totals] =
    listCollections().flatMap { names =>
      Future.foldLeft(
        names.filter(filter).map { name =>
          expireCollection(name).map(_ => Succeeded(name)).recover { case ex => Failed(name, Some(ex)) }
        }
      )(Totals(List.empty, List.empty))(resultHandler)
    }

  private def resultHandler(totals: Totals, result: ProcessingResult) = result match {
    case x: Succeeded => totals.copy(successes = x :: totals.successes)
    case x: Failed    => totals.copy(failures = x :: totals.failures)
  }

}

trait DeleteCollectionFilter {
  private def today: LocalDate = LocalDate.now()

  def filterUpsCollectionsOnly(collectionName: String, days: Duration): Boolean = {

    val formatter = DateTimeFormatter.ofPattern(UpdatedPrintSuppressions.datePattern)
    def extractDateFromCollection(value: String) = LocalDate.parse(value.dropWhile(!_.isDigit), formatter)

    val collectionDaysOld = today.toEpochDay - extractDateFromCollection(collectionName).toEpochDay
    collectionDaysOld >= days.toDays
  }
}
final case class Totals(failures: List[Failed], successes: List[Succeeded])
