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

import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class MongoCounterRepository @Inject() (mongo: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[Counter](
      mongo,
      "counters",
      Counter.formats,
      Seq(
        IndexModel(
          Indexes.ascending("name"),
          IndexOptions()
            .name("nameIdx")
            .unique(true)
            .sparse(false)
        )
      ),
      replaceIndexes = false
    ) with CounterRepository {

  def next(counterName: String)(implicit ec: ExecutionContext): Future[Int] = {
    val query = Filters.equal("name", counterName)
    collection
      .findOneAndUpdate(
        query,
        Updates.inc("value", 1),
        FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      )
      .toFuture()
      .map(_.value)
  }
}

trait CounterRepository {
  def next(counterName: String)(implicit ec: ExecutionContext): Future[Int]
}
