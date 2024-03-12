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

package uk.gov.hmrc.ups.repository

import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.model._
import org.mongodb.scala.result.UpdateResult
import play.api.Logger
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{ Codecs, PlayMongoRepository }
import uk.gov.hmrc.ups.model.PrintPreference
import uk.gov.hmrc.ups.repository.UpdatedPrintSuppressions.updatedAtAsJson

import java.time.{ Instant, LocalDate }
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class UpdatedPrintSuppressionsRepository @Inject()(mongoComponent: MongoComponent, date: LocalDate, counterRepo: MongoCounterRepository)(
  implicit ec: ExecutionContext)
    extends PlayMongoRepository[UpdatedPrintSuppressions](
      mongoComponent,
      UpdatedPrintSuppressions.repoNameTemplate(date),
      UpdatedPrintSuppressions.formats,
      List(
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
            .sparse(false))
      )
    ) {
  private[this] val logger = Logger(getClass)
  private[this] val counterRepoDate = UpdatedPrintSuppressions.toString(date)

  def find(offset: Long, limit: Int): Future[List[PrintPreference]] = {
    val query = Filters.and(Filters.gte("counter", offset), Filters.lt("counter", offset + limit))
    val e: Future[Seq[UpdatedPrintSuppressions]] = collection.find(query).toFuture()
    e.map(_.map(ups => ups.printPreference).toList)
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
          .insertOne(UpdatedPrintSuppressions(new ObjectId(), counter, printPreference, updatedAt))
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

  def count(): Future[Long] = collection.countDocuments().toFuture()
}
