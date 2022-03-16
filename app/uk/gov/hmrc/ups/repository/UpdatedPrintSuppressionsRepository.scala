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

package uk.gov.hmrc.ups.repository

import org.joda.time.{ DateTime, LocalDate }
import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.model._
import play.api.Logger
import play.api.libs.json.{ Format, Json, OFormat }
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.{ MongoFormats, MongoJodaFormats }
import uk.gov.hmrc.mongo.play.json.{ Codecs, PlayMongoRepository }
import uk.gov.hmrc.ups.model.PrintPreference
import uk.gov.hmrc.ups.repository.UpdatedPrintSuppressions.updatedAtAsJson

import javax.inject.Inject
import scala.collection.Seq
import scala.concurrent.{ ExecutionContext, Future }

case class UpdatedPrintSuppressions(_id: ObjectId, counter: Int, printPreference: PrintPreference, updatedAt: DateTime)

object UpdatedPrintSuppressions {
  implicit val objectIdFormat: Format[ObjectId] = MongoFormats.objectIdFormat
  implicit val pp: OFormat[PrintPreference] = PrintPreference.formats
  implicit val isoDateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat

  implicit val formats: OFormat[UpdatedPrintSuppressions] = Json.format[UpdatedPrintSuppressions]

  val datePattern = "yyyyMMdd"

  def toString(date: LocalDate): String = date.toString(datePattern)

  def repoNameTemplate(date: LocalDate): String = s"updated_print_suppressions_${toString(date)}"

  def updatedAtAsJson(updatedAt: DateTime) = Json.toJson(updatedAt)
}

class UpdatedPrintSuppressionsRepository @Inject()(mongoComponent: MongoComponent, date: LocalDate, counterRepo: MongoCounterRepository)(
  implicit ec: ExecutionContext)
    extends PlayMongoRepository[UpdatedPrintSuppressions](
      mongoComponent,
      UpdatedPrintSuppressions.repoNameTemplate(date),
      UpdatedPrintSuppressions.formats,
      Seq(
        IndexModel(
          Indexes.ascending("counter"),
          IndexOptions()
            .name("counterIdx")
            .unique(true)
            .sparse(false)),
        IndexModel(
          Indexes.ascending("printPreference.id", "printPreference.idType"),
          IndexOptions()
            .name("uniquePreferenceId")
            .unique(true)
            .sparse(false)),
      ),
      replaceIndexes = false
    ) {

  private val logger = Logger(getClass.getName)
  private val counterRepoDate = UpdatedPrintSuppressions.toString(date)

  def find(offset: Long, limit: Int): Future[List[PrintPreference]] = {
    val query = Filters.and(Filters.gte("counter", offset), Filters.lt("counter", offset + limit))
    val e: Future[Seq[UpdatedPrintSuppressions]] = collection.find(query).toFuture()
    e.map(_.map(ups => ups.printPreference).toList)
  }

  def insert(printPreference: PrintPreference, updatedAt: DateTime): Future[Unit] = {
    val selector = Filters.and(
      Filters.equal("printPreference.id", printPreference.id),
      Filters.equal("printPreference.idType", printPreference.idType)
    )
    val updatedAtSelector = Filters.lte("updatedAt", Codecs.toBson(updatedAtAsJson(updatedAt)))

    collection
      .find[UpdatedPrintSuppressions](selector)
      .first()
      .toFuture()
      .map(Option(_) match {
        case Some(ups) =>
          collection
            .updateOne(
              Filters.and(Filters.equal("_id", ups._id), updatedAtSelector),
              Seq(Updates.set("printPreference", Codecs.toBson(printPreference)), Updates.set("updatedAt", Codecs.toBson(updatedAtAsJson(updatedAt))))
            )
            .toFuture()
        case None =>
          counterRepo
            .next(counterRepoDate)
            .flatMap { counter =>
              collection
                .insertOne(UpdatedPrintSuppressions(new ObjectId(), counter, printPreference, updatedAt))
                .toFuture()
            }
            .recover {
              case e: MongoWriteException if e.getError.getCode == 11000 =>
                logger.warn(s"failed to insert print preference $printPreference updated at ${updatedAt.getMillis}", e)
                ()
            }
      })
      .map { _ =>
        ()
      }
  }

  def count(): Future[Long] = collection.countDocuments().toFuture()
}

case class Counter(_id: ObjectId, name: String, value: Int)

object Counter {
  val formats: OFormat[Counter] = {
    implicit val objectIdFormat: Format[ObjectId] = MongoFormats.objectIdFormat
    Json.format[Counter]
  }
}

trait CounterRepository {
  def next(counterName: String)(implicit ec: ExecutionContext): Future[Int]
}
