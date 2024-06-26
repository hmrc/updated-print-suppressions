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

package uk.gov.hmrc.ups.controllers.admin

import play.api.libs.json.{ JsValue, OFormat }
import play.api.mvc.{ Action, AnyContent, ControllerComponents, QueryStringBindable }
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.controller.WithJsonBody
import uk.gov.hmrc.ups.controllers.UpdatedOn
import uk.gov.hmrc.ups.controllers.bind.PastLocalDateBindable
import uk.gov.hmrc.ups.model.{ Limit, PastLocalDate, PrintPreference }

import javax.inject.{ Inject, Singleton }

@Singleton
class AdminController @Inject() (updatedOn: UpdatedOn, cc: ControllerComponents)
    extends BackendController(cc) with WithJsonBody {

  val localDateBinder: QueryStringBindable[PastLocalDate] = PastLocalDateBindable(false)

  implicit val ppf: OFormat[PrintPreference] = PrintPreference.formats

  def list(optOffset: Option[Int], optLimit: Option[Limit]): Action[AnyContent] =
    Action.async { implicit request =>
      updatedOn.processUpdatedOn(
        optOffset,
        optLimit,
        localDateBinder.bind("updated-on", request.queryString),
        localDateBinder
      )
    }

  def insert(date: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[PrintPreference] { body =>
      updatedOn.insert(date, body)
    }
  }
}
