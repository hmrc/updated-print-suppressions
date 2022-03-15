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

import uk.gov.hmrc.mongo.MongoComponent

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class UpdatedPrintSuppressionsDatabase @Inject()(mongoComponent: MongoComponent) {

  def dropCollection(collectionName: String)(implicit ec: ExecutionContext): Future[Unit] =
    mongoComponent.database.getCollection(collectionName).drop().toFuture().map(_ => ())

  private def listCollectionNames(predicate: String => Boolean)(implicit ec: ExecutionContext): Future[List[String]] =
    mongoComponent.database.listCollectionNames().toFuture().map(_.filter(predicate).toList)

  def upsCollectionNames(implicit ec: ExecutionContext): Future[List[String]] =
    listCollectionNames(_.startsWith("updated"))
}
