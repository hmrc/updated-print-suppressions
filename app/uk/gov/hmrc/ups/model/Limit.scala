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

package uk.gov.hmrc.ups.model

import play.api.mvc.QueryStringBindable

case class Limit(value: Int)

object Limit {
  private val LimitValue = 20000

  val max = Limit(LimitValue)

  implicit def queryStringBinder(implicit intBinder: QueryStringBindable[Int]): QueryStringBindable[Limit] =
    new QueryStringBindable[Limit] {

      import scala.language.postfixOps

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Limit]] =
        for {
          limit <- intBinder.bind(key + ".value", params)
        } yield limit match {
          case Right(limit) if limit < 0 => Left("limit parameter is less than zero")
          case Right(limit) if limit > Limit.max.value =>
            Left(s"limit parameter cannot be bigger than ${Limit.max.value}")
          case Right(limit) => Right(Limit(limit))
          case _            => Left("Cannot parse parameter limit as Int")
        }

      override def unbind(key: String, limit: Limit): String = QueryStringBindable.bindableInt.unbind(key, limit.value)
    }
}
