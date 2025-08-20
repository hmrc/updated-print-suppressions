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

package uk.gov.hmrc.ups

import com.google.inject.{ AbstractModule, Provides }
import net.codingwell.scalaguice.ScalaModule
import org.apache.pekko.Done
import org.apache.pekko.stream.scaladsl.Sink
import play.api.libs.concurrent.PekkoGuiceSupport
import uk.gov.hmrc.ups.service.RemoveOlderCollectionsService

import java.time.LocalDate
import scala.concurrent.Future

// $COVERAGE-OFF$Disabling
class UpsModule extends AbstractModule with ScalaModule with PekkoGuiceSupport {

  override def configure(): Unit =
    bind[UpsMain].asEagerSingleton()
  bind[RemoveOlderCollectionsService].asEagerSingleton()

  @Provides
  def sink(): Sink[Unit, _] = Sink.ignore

  @Provides
  def localDate(): LocalDate = LocalDate.now()

}
// $COVERAGE-ON$
