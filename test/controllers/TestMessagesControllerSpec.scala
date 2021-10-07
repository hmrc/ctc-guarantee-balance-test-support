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

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import connectors.FakeTraderRouterConnector
import models.BalanceRequestSuccess
import models.SimulatedResponse
import models.values.CurrencyCode
import models.values.GuaranteeReference
import models.values.MessageIdRecipient
import models.values.TaxIdentifier
import models.values.UniqueReference
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.test.FakeRequest
import play.api.test.Helpers
import play.api.test.Helpers._
import services.XmlFormattingService
import uk.gov.hmrc.http.HttpResponse

import java.security.SecureRandom
import java.time.Clock

class TestMessagesControllerSpec
    extends AnyFlatSpec
    with Matchers
    with ScalaFutures
    with ScalaCheckPropertyChecks {

  def mkController(
    injectMessageResponse: IO[HttpResponse] = IO.stub
  ) =
    new TestMessagesController(
      FakeTraderRouterConnector(injectMessageResponse),
      new XmlFormattingService(Clock.systemUTC(), new SecureRandom),
      Helpers.stubControllerComponents(),
      IORuntime.global
    )

  val responseCodes = Gen.oneOf(
    Seq(
      OK,
      BAD_REQUEST,
      FORBIDDEN,
      BAD_GATEWAY,
      SERVICE_UNAVAILABLE,
      INTERNAL_SERVER_ERROR
    )
  )

  val messageRecipient = MessageIdRecipient("MDTP-GUA-22b9899e24ee48e6a18997d1")

  val simulatedResponse = SimulatedResponse(
    TaxIdentifier("GB12345678900"),
    GuaranteeReference("05DE3300BE0001067A001017"),
    Some(UniqueReference("7acb933dbe7039")),
    BalanceRequestSuccess(BigDecimal("12345678.90"), CurrencyCode("GBP"))
  )

  "TestMessagesController" should "pass through responses from trader router" in forAll(
    responseCodes
  ) { responseCode =>
    val request           = FakeRequest().withBody(simulatedResponse)
    val connectorResponse = HttpResponse(responseCode, "")
    val controller        = mkController(injectMessageResponse = IO.pure(connectorResponse))
    val result            = controller.injectEisResponse(messageRecipient)(request)
    status(result) shouldBe responseCode
    contentAsString(result) shouldBe connectorResponse.body
  }
}
