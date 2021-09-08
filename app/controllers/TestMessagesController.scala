/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import cats.effect.unsafe.IORuntime
import connectors.TraderRouterConnector
import controllers.actions.IOActions
import models.MessageType
import models.SimulatedResponse
import models.values.MessageRecipient
import play.api.mvc.ControllerComponents
import services.XmlFormattingService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestMessagesController @Inject() (
  connector: TraderRouterConnector,
  formatter: XmlFormattingService,
  cc: ControllerComponents,
  val runtime: IORuntime
) extends BackendController(cc)
    with IOActions {

  def injectEisResponse(recipient: MessageRecipient) =
    Action.io(parse.json[SimulatedResponse]) { implicit request =>
      for {
        message <- formatter.formatMessage(recipient, request.body)
        messageType = MessageType.forBalanceResponse(request.body.response)
        response <- connector.injectEisResponse(recipient, messageType, message)
      } yield response match {
        case HttpResponse(status, body, responseHeaders) =>
          val headers = responseHeaders.flatMap { case (headerName, headerValues) =>
            headerValues.headOption.map { headerValue =>
              headerName -> headerValue
            }
          }

          Status(status)(body).withHeaders(headers.toSeq: _*)
      }
    }
}
