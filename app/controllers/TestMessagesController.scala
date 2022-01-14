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

package controllers

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.all._
import com.fasterxml.jackson.core.JsonParseException
import connectors.TraderRouterConnector
import controllers.actions.IOActions
import models.MessageType
import models.SimulatedResponse
import models.errors.JsonParsingError
import models.values.MessageRecipient
import org.xml.sax.SAXParseException
import play.api.http.MimeTypes
import play.api.i18n.I18nSupport
import play.api.libs.json._
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import play.api.mvc.Result
import services.XmlFormattingService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.xml.NodeSeq

@Singleton
class TestMessagesController @Inject() (
  connector: TraderRouterConnector,
  formatter: XmlFormattingService,
  cc: ControllerComponents,
  val runtime: IORuntime
) extends BackendController(cc)
    with IOActions
    with I18nSupport {

  def validateJsonRequest(
    continue: SimulatedResponse => IO[Result]
  )(implicit request: Request[String]): IO[Result] = {
    val validateJson = for {
      json <- IO(Json.parse(request.body))
      result <- IO(json.validate[SimulatedResponse]).flatMap {
        case JsSuccess(simulatedResponse, _) =>
          continue(simulatedResponse)
        case JsError(errors) =>
          val parsingError = JsonParsingError(errors = errors)
          IO.pure(BadRequest(Json.toJson(parsingError)))
      }
    } yield result

    validateJson.recover { case exc: JsonParseException =>
      BadRequest("Invalid Json: " + exc.getMessage())
    }
  }

  def validateXmlRequest(
    continue: NodeSeq => IO[Result]
  )(implicit request: Request[String]): IO[Result] = {
    val validateXml = for {
      xml    <- IO(scala.xml.XML.loadString(request.body))
      result <- continue(xml)
    } yield result

    validateXml.recover { case exc: SAXParseException =>
      BadRequest("Invalid XML: " + exc.getMessage())
    }
  }

  def responseToResult(response: HttpResponse): Result = response match {
    case HttpResponse(status, body, responseHeaders) =>
      val headers = responseHeaders.toSeq.flatMap { case (headerName, headerValues) =>
        headerValues.map { headerValue =>
          headerName -> headerValue
        }
      }

      Status(status)(body).withHeaders(headers: _*)
  }

  def injectEisResponse(recipient: MessageRecipient) =
    Action.io(parse.tolerantText) { implicit request =>
      request.contentType match {
        case Some(MimeTypes.JSON) =>
          validateJsonRequest { simulatedResponse =>
            for {
              message <- formatter.formatMessage(recipient, simulatedResponse)
              messageType = MessageType.forBalanceResponse(simulatedResponse.response)
              response <- connector.injectEisResponse(recipient, messageType, message)
            } yield responseToResult(response)
          }

        case Some(MimeTypes.XML) =>
          validateXmlRequest { message =>
            connector
              .injectEisResponse(recipient, MessageType.ResponseQueryOnGuarantees, message)
              .map(responseToResult)
          }

        case _ =>
          IO.pure(UnsupportedMediaType)
      }
    }
}
