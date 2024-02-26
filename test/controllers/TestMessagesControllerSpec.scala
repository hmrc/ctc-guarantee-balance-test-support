/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.http.HeaderNames
import play.api.http.MimeTypes
import play.api.libs.json.Json
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

  val simulatedJsonResponse = SimulatedResponse(
    TaxIdentifier("GB12345678900"),
    GuaranteeReference("05DE3300BE0001067A001017"),
    Some(UniqueReference("7acb933dbe7039")),
    BalanceRequestSuccess(BigDecimal("12345678.90"), CurrencyCode("GBP"))
  )

  val simulatedXmlResponse =
    <CD037A>
      <SynIdeMES1>UNOC</SynIdeMES1>
      <SynVerNumMES2>3</SynVerNumMES2>
      <MesSenMES3>NTA.GB</MesSenMES3>
      <MesRecMES6>MDTP-GUA-22b9899e24ee48e6a18997d1</MesRecMES6>
      <DatOfPreMES9>20210806</DatOfPreMES9>
      <TimOfPreMES10>1505</TimOfPreMES10>
      <IntConRefMES11>deadbeefcafeba</IntConRefMES11>
      <MesIdeMES19>deadbeefcafeba</MesIdeMES19>
      <MesTypMES20>GB037A</MesTypMES20>
      <TRAPRIRC1>
        <TINRC159>GB12345678900</TINRC159>
      </TRAPRIRC1>
      <CUSTOFFGUARNT>
        <RefNumRNT1>GB000001</RefNumRNT1>
      </CUSTOFFGUARNT>
      <GUAREF2>
        <GuaRefNumGRNREF21>21GB3300BE0001067A001017</GuaRefNumGRNREF21>
        <AccDatREF24>20210114</AccDatREF24>
        <GuaTypREF22>4</GuaTypREF22>
        <GuaMonCodREF23>1</GuaMonCodREF23>
        <GUAQUE>
          <QueIdeQUE1>2</QueIdeQUE1>
        </GUAQUE>
        <EXPEXP>
          <ExpEXP1>2751.95</ExpEXP1>
          <ExpCouEXP2>2448</ExpCouEXP2>
          <BalEXP3>1212211848.45</BalEXP3>
          <CurEXP4>GBP</CurEXP4>
        </EXPEXP>
      </GUAREF2>
    </CD037A>

  "TestMessagesController" should "pass through responses from trader router for JSON requests" in forAll(
    responseCodes
  ) { responseCode =>
    val request = FakeRequest()
      .withHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      .withBody(Json.stringify(Json.toJson(simulatedJsonResponse)))

    val connectorResponse = HttpResponse(responseCode, "")
    val controller        = mkController(injectMessageResponse = IO.pure(connectorResponse))
    val result            = controller.injectEisResponse(messageRecipient)(request)

    status(result) shouldBe responseCode
    contentAsString(result) shouldBe connectorResponse.body
  }

  it should "return a JSON parsing error for JSON requests that can't be parsed as a simulated response" in {
    val request = FakeRequest()
      .withHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      .withBody("""{
      |  "taxIdentifier": "GB12345678900",
      |  "guaranteeReference": "21GB3300BE0001067A001017"
      |}""".trim.stripMargin)

    val controller = mkController()
    val result     = controller.injectEisResponse(messageRecipient)(request)

    status(result) shouldBe BAD_REQUEST
    contentAsJson(result) shouldBe Json.obj(
      "code"    -> "INVALID_REQUEST_JSON",
      "message" -> "Invalid request JSON",
      "errors"  -> Json.obj("$.response" -> Json.arr("error.path.missing"))
    )
  }

  it should "return a bad request error for requests that can't be parsed as valid JSON" in {
    val request = FakeRequest()
      .withHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      .withBody("""{
      |  "taxIdentifier": "GB12345678900,
      |  "guaranteeReference": "21GB3300BE0001067A001017"
      |}""".trim.stripMargin)

    val controller = mkController()
    val result     = controller.injectEisResponse(messageRecipient)(request)

    status(result) shouldBe BAD_REQUEST
    contentAsString(result) should startWith("Invalid Json:")
  }

  it should "return a bad request error for requests that can't be parsed as valid XML" in {
    val request = FakeRequest()
      .withHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.XML)
      .withBody("""<CD037A><CD037A>""".trim.stripMargin)

    val controller = mkController()
    val result     = controller.injectEisResponse(messageRecipient)(request)

    status(result) shouldBe BAD_REQUEST
    contentAsString(result) should startWith("Invalid XML:")
  }

  it should "pass through responses from trader router for XML requests" in forAll(
    responseCodes
  ) { responseCode =>
    val request = FakeRequest()
      .withHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.XML)
      .withBody(simulatedXmlResponse.toString)

    val connectorResponse = HttpResponse(responseCode, "")
    val controller        = mkController(injectMessageResponse = IO.pure(connectorResponse))
    val result            = controller.injectEisResponse(messageRecipient)(request)

    status(result) shouldBe responseCode
    contentAsString(result) shouldBe connectorResponse.body
  }

  it should "reject unsupported media types" in {
    val request = FakeRequest().withBody("")

    val controller = mkController()
    val result     = controller.injectEisResponse(messageRecipient)(request)

    status(result) shouldBe UNSUPPORTED_MEDIA_TYPE
  }
}
