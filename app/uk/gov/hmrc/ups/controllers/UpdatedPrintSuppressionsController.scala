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

package uk.gov.hmrc.ups.controllers

import play.api.libs.json.JsValue
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.ups.controllers.bind.PastLocalDateBindable
import uk.gov.hmrc.ups.model.{Limit, NotifySubscriberRequest, PastLocalDate}
import uk.gov.hmrc.ups.service.{SaUtrNotFoundException, UpdatedPrintSuppressionService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class UpdatedPrintSuppressionsController @Inject()
(
  updatedOn: UpdatedOn,
  cc: ControllerComponents,
  updatedPrintSuppressionService: UpdatedPrintSuppressionService
)(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  val localDateBinder: QueryStringBindable[PastLocalDate] = PastLocalDateBindable(true)

  def list(optOffset: Option[Int], optLimit: Option[Limit]): Action[AnyContent] =
    Action.async { implicit request =>
      updatedOn.processUpdatedOn(
        optOffset,
        optLimit,
        localDateBinder.bind("updated-on", request.queryString),
        localDateBinder
      )
    }

  def notifySubscriber(): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      withJsonBody[NotifySubscriberRequest] { requestReceived =>
        updatedPrintSuppressionService.process(requestReceived)
          .fold(
            {
              case _: SaUtrNotFoundException => BadRequest("Missing SaUtr")
              case ex                        => InternalServerError(ex.getMessage)
            },
            _ => {
              Ok
            }
          )
      }
    }
}
