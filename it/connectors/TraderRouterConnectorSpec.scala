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

package connectors

import cats.effect.unsafe.implicits.global
import com.github.tomakehurst.wiremock.client.WireMock._
import models.MessageType
import models.values.BalanceIdRecipient
import models.values.MessageIdRecipient
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID

class TraderRouterConnectorSpec
    extends AnyFlatSpec
    with Matchers
    with GuiceOneServerPerSuite
    with WireMockSpec
    with ScalaFutures
    with IntegrationPatience {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override protected def portConfigKeys: Seq[String] =
    Seq("microservice.services.trader-router.port")

  "TraderRouterConnector" should "forward success message to the trader router for message ID requests" in {
    val connector = app.injector.instanceOf[TraderRouterConnector]
    val recipient = MessageIdRecipient("MDTP-GUA-22b9899e24ee48e6a18997d1")

    wireMockServer.stubFor(
      post(urlEqualTo("/transit-movements-trader-router/messages"))
        .withHeader("Content-Type", equalTo("application/xml"))
        .withHeader("X-Message-Recipient", equalTo("MDTP-GUA-22b9899e24ee48e6a18997d1"))
        .withHeader("X-Message-Type", equalTo("IE037"))
        .willReturn(aResponse().withStatus(OK))
        .withRequestBody(equalToXml("<foo></foo>"))
    )

    val response = connector
      .injectEisResponse(recipient, MessageType.ResponseQueryOnGuarantees, <foo></foo>)
      .unsafeToFuture()
      .futureValue

    response.status shouldBe OK
  }

  it should "forward success message to the trader router for balance ID requests" in {
    val connector = app.injector.instanceOf[TraderRouterConnector]
    val recipient = BalanceIdRecipient(UUID.fromString("22b9899e-24ee-48e6-a189-97d1f45391c4"))

    wireMockServer.stubFor(
      post(urlEqualTo("/transit-movements-trader-router/messages"))
        .withHeader("Content-Type", equalTo("application/xml"))
        .withHeader("X-Message-Recipient", equalTo("MDTP-GUA-22b9899e24ee48e6a18997d1"))
        .withHeader("X-Message-Type", equalTo("IE037"))
        .willReturn(aResponse().withStatus(OK))
        .withRequestBody(equalToXml("<foo></foo>"))
    )

    val response = connector
      .injectEisResponse(recipient, MessageType.ResponseQueryOnGuarantees, <foo></foo>)
      .unsafeToFuture()
      .futureValue

    response.status shouldBe OK
  }

  it should "forward functional error message to the trader router" in {
    val connector = app.injector.instanceOf[TraderRouterConnector]
    val recipient = MessageIdRecipient("MDTP-GUA-22b9899e24ee48e6a18997d1")

    wireMockServer.stubFor(
      post(urlEqualTo("/transit-movements-trader-router/messages"))
        .withHeader("Content-Type", equalTo("application/xml"))
        .withHeader("X-Message-Recipient", equalTo("MDTP-GUA-22b9899e24ee48e6a18997d1"))
        .withHeader("X-Message-Type", equalTo("IE906"))
        .willReturn(aResponse().withStatus(OK))
        .withRequestBody(equalToXml("<foo></foo>"))
    )

    val response = connector
      .injectEisResponse(recipient, MessageType.FunctionalNack, <foo></foo>)
      .unsafeToFuture()
      .futureValue

    response.status shouldBe OK
  }

  it should "forward XML error message to the trader router" in {
    val connector = app.injector.instanceOf[TraderRouterConnector]
    val recipient = MessageIdRecipient("MDTP-GUA-22b9899e24ee48e6a18997d1")

    wireMockServer.stubFor(
      post(urlEqualTo("/transit-movements-trader-router/messages"))
        .withHeader("Content-Type", equalTo("application/xml"))
        .withHeader("X-Message-Recipient", equalTo("MDTP-GUA-22b9899e24ee48e6a18997d1"))
        .withHeader("X-Message-Type", equalTo("IE917"))
        .willReturn(aResponse().withStatus(OK))
        .withRequestBody(equalToXml("<foo></foo>"))
    )

    val response = connector
      .injectEisResponse(recipient, MessageType.XmlNack, <foo></foo>)
      .unsafeToFuture()
      .futureValue

    response.status shouldBe OK
  }

  it should "pass back client error" in {
    val connector = app.injector.instanceOf[TraderRouterConnector]
    val recipient = MessageIdRecipient("MDTP-GUA-22b9899e24ee48e6a18997d1")

    wireMockServer.stubFor(
      post(urlEqualTo("/transit-movements-trader-router/messages"))
        .willReturn(aResponse().withStatus(CONFLICT))
        .withRequestBody(equalToXml("<foo></foo>"))
    )

    val response = connector
      .injectEisResponse(recipient, MessageType.ResponseQueryOnGuarantees, <foo></foo>)
      .unsafeToFuture()
      .futureValue

    response.status shouldBe CONFLICT
  }

  it should "pass back server error" in {
    val connector = app.injector.instanceOf[TraderRouterConnector]
    val recipient = MessageIdRecipient("MDTP-GUA-22b9899e24ee48e6a18997d1")

    wireMockServer.stubFor(
      post(urlEqualTo("/transit-movements-trader-router/messages"))
        .willReturn(aResponse().withStatus(NOT_IMPLEMENTED))
        .withRequestBody(equalToXml("<foo></foo>"))
    )

    val response = connector
      .injectEisResponse(recipient, MessageType.ResponseQueryOnGuarantees, <foo></foo>)
      .unsafeToFuture()
      .futureValue

    response.status shouldBe NOT_IMPLEMENTED
  }
}
