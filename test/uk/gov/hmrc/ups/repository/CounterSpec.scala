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

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.play.PlaySpec
import org.mongodb.scala.bson.ObjectId
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats

class CounterSpec extends PlaySpec {

  "Counter" should {
    "serialize and deserialize correctly" in {

      implicit val objectIdFormat = MongoFormats.objectIdFormat
      implicit val counterFormat = Counter.formats

      val originalCounter = Counter(new ObjectId(), "test-counter", 42)
      val json = Json.toJson(originalCounter)
      val deserializedCounter = json.as[Counter]

      deserializedCounter mustEqual originalCounter
    }
  }

}
