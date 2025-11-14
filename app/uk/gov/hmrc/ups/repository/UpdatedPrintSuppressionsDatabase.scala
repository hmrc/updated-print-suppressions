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

import uk.gov.hmrc.mongo.MongoComponent
import org.mongodb.scala.ObservableFuture
import play.api.Logger

import scala.concurrent.duration._
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ Await, ExecutionContext, Future }

@Singleton
class UpdatedPrintSuppressionsDatabase @Inject() (mongoComponent: MongoComponent) {
  val logger: Logger = Logger(this.getClass)
  def dropCollection(collectionName: String)(implicit ec: ExecutionContext): Future[Unit] = {
    logger.warn("dropCollection invoked - this will drop a ups collection")
    mongoComponent.database.getCollection(collectionName).drop().toFuture().map(_ => ())
  }

  private def listCollectionNames(predicate: String => Boolean)(implicit ec: ExecutionContext): Future[List[String]] =
    mongoComponent.database.listCollectionNames().toFuture().map(_.filter(predicate).toList)

  def upsCollectionNames(implicit ec: ExecutionContext): Future[List[String]] = {
    val count = mongoComponent.database.listCollectionNames().toFuture().map(_.size)
    logger.warn(s"upsCollectionNames invoked - count is ${Await.result(count, 10.seconds)}")
    listCollectionNames(_.startsWith("updated"))
  }
}
