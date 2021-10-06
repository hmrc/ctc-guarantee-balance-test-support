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

package connectors

import cats.effect.IO
import com.google.inject.ImplementedBy
import config.AppConfig
import models.MessageType
import models.values.MessageRecipient
import play.api.http.HeaderNames
import play.api.http.MimeTypes
import runtime.IOFutures
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpResponse

import javax.inject.Inject
import javax.inject.Singleton
import scala.xml.NodeSeq

@ImplementedBy(classOf[TraderRouterConnectorImpl])
trait TraderRouterConnector {
  def injectEisResponse(recipient: MessageRecipient, messageType: MessageType, message: NodeSeq)(
    implicit hc: HeaderCarrier
  ): IO[HttpResponse]
}

@Singleton
class TraderRouterConnectorImpl @Inject() (
  appConfig: AppConfig,
  http: HttpClient
) extends TraderRouterConnector
    with IOFutures {

  def injectEisResponse(recipient: MessageRecipient, messageType: MessageType, message: NodeSeq)(
    implicit hc: HeaderCarrier
  ): IO[HttpResponse] = IO.runFuture { implicit ec =>
    val url = appConfig.traderRouterUrl

    val headers = Seq(
      HeaderNames.CONTENT_TYPE -> MimeTypes.XML,
      "X-Message-Recipient"    -> recipient.messageIdValue,
      "X-Message-Type"         -> messageType.code
    )

    http.POSTString[HttpResponse](url.toString, message.toString, headers)
  }
}
