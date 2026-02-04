/*
 * Copyright 2025 HM Revenue & Customs
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

import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.model.*
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.ToSingleObservablePublisher
import play.api.Logger
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{ Codecs, PlayMongoRepository }
import uk.gov.hmrc.ups.model.PrintPreference
import uk.gov.hmrc.ups.repository.UpdatedPrintSuppressions.{ localDateFormat, updatedAtAsJson }
import java.time.{ Instant, LocalDate }
import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class UpsRepository @Inject() (
  mongoComponent: MongoComponent,
  date: LocalDate,
  counterRepo: MongoCounterRepository
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[UpdatedPrintSuppressions](
      mongoComponent,
      "updated_print_suppressions",
      UpdatedPrintSuppressions.formats,
      List(
        IndexModel(
          Indexes.compoundIndex(Indexes.ascending("date"), Indexes.ascending("counter")),
          IndexOptions()
            .name("dateCounterIdx")
            .unique(true)
            .sparse(false)
        ),
        IndexModel(
          Indexes.compoundIndex(
            Indexes.ascending("date"),
            Indexes.ascending("printPreference.id"),
            Indexes.ascending("printPreference.idType")
          ),
          IndexOptions()
            .name("uniquePreferenceIdPerDay")
            .unique(true)
            .sparse(false)
        ),
        IndexModel(
          Indexes.ascending("updatedAt"),
          IndexOptions()
            .name("updatedAtTtlIdx")
            .expireAfter(30, java.util.concurrent.TimeUnit.DAYS)
        )
      )
    ) {
  private[this] val logger = Logger(getClass)
  private[this] val counterRepoDate = UpdatedPrintSuppressions.toString(date)

  def find(offset: Long, limit: Int): Future[List[PrintPreference]] = {
    val query = Filters.and(
      Filters.equal("date", Codecs.toBson(date)),
      Filters.gte("counter", offset),
      Filters.lt("counter", offset + limit)
    )
    collection.find(query).toFuture().map(_.map(_.printPreference).toList)
  }

  private def updateOnInsert(
    printPreference: PrintPreference,
    ups: UpdatedPrintSuppressions,
    updatedAt: Instant
  ): Future[UpdateResult] = {
    val updatedAtSelector = Filters.lte("updatedAt", Codecs.toBson(updatedAtAsJson(updatedAt)))

    collection
      .updateOne(
        Filters.and(Filters.equal("_id", ups._id), updatedAtSelector),
        Updates.combine(
          Updates.set("printPreference", Codecs.toBson(printPreference)),
          Updates.set("updatedAt", Codecs.toBson(updatedAtAsJson(updatedAt)))
        )
      )
      .toFuture()
  }

  private def insertNew(printPreference: PrintPreference, updatedAt: Instant): Future[Unit] =
    counterRepo
      .next(counterRepoDate)
      .flatMap { counter =>
        collection
          .insertOne(UpdatedPrintSuppressions(new ObjectId(), counter, printPreference, updatedAt, date))
          .toSingle()
          .toFuture()
      }
      .map(_ => ())
      .recover {
        case e: MongoWriteException if e.getError.getCode == 11000 =>
          logger.warn(s"failed to insert print preference $printPreference updated at ${updatedAt.toEpochMilli}", e)
      }

  def insert(printPreference: PrintPreference, updatedAt: Instant): Future[Unit] = {
    val selector = Filters.and(
      Filters.equal("date", Codecs.toBson(date)),
      Filters.equal("printPreference.id", printPreference.id),
      Filters.equal("printPreference.idType", printPreference.idType)
    )

    collection
      .find[UpdatedPrintSuppressions](selector)
      .first()
      .toFuture()
      .flatMap(Option(_) match {
        case Some(ups) => updateOnInsert(printPreference, ups, updatedAt)
        case None      => insertNew(printPreference, updatedAt)
      })
      .map[Unit](_ => ())
  }

  def count(): Future[Long] =
    collection.countDocuments(Filters.equal("date", Codecs.toBson(date))).toFuture()
}
