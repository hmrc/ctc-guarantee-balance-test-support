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

package services

import cats.data.NonEmptyList
import cats.effect.unsafe.implicits.global
import models.BalanceRequestFunctionalError
import models.BalanceRequestSuccess
import models.BalanceRequestXmlError
import models.SimulatedResponse
import models.errors.FunctionalError
import models.errors.XmlError
import models.values.CurrencyCode
import models.values.ErrorType
import models.values.GuaranteeReference
import models.values.MessageIdRecipient
import models.values.TaxIdentifier
import models.values.UniqueReference
import org.scalatest.StreamlinedXmlEquality
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Random
import scala.xml.Utility

class XmlFormattingServiceSpec extends AsyncFlatSpec with Matchers with StreamlinedXmlEquality {
  val dateTime = LocalDateTime.of(2021, 9, 7, 15, 53, 16).toInstant(ZoneOffset.UTC)
  def service  = new XmlFormattingService(Clock.fixed(dateTime, ZoneOffset.UTC), new Random(0))

  val recipient = MessageIdRecipient("MDTP-GUA-22b9899e24ee48e6a18997d1")

  "XmlFormattingService" should "format successful response with correct guarantee type for guarantee reference with voucher code" in {
    val simulatedResponse = SimulatedResponse(
      TaxIdentifier("GB12345678900"),
      GuaranteeReference("05DE3300BE0001067A001017"),
      UniqueReference("7acb933dbe7039"),
      BalanceRequestSuccess(BigDecimal("12345678.90"), CurrencyCode("GBP"))
    )

    val successXml = {
      <CD037A>
        <SynIdeMES1>UNOC</SynIdeMES1>
        <SynVerNumMES2>3</SynVerNumMES2>
        <MesSenMES3>NTA.GB</MesSenMES3>
        <MesRecMES6>MDTP-GUA-22b9899e24ee48e6a18997d1</MesRecMES6>
        <DatOfPreMES9>20210907</DatOfPreMES9>
        <TimOfPreMES10>1553</TimOfPreMES10>
        <IntConRefMES11>60b420bb3851d9</IntConRefMES11>
        <MesIdeMES19>60b420bb3851d9</MesIdeMES19>
        <MesTypMES20>GB037A</MesTypMES20>
        <TRAPRIRC1>
          <TINRC159>GB12345678900</TINRC159>
        </TRAPRIRC1>
        <CUSTOFFGUARNT>
          <RefNumRNT1>GB000001</RefNumRNT1>
        </CUSTOFFGUARNT>
        <GUAREF2>
          <GuaRefNumGRNREF21>05DE3300BE0001067A001017</GuaRefNumGRNREF21>
          <AccDatREF24>20210114</AccDatREF24>
          <GuaTypREF22>4</GuaTypREF22>
          <GuaMonCodREF23>1</GuaMonCodREF23>
          <GUAQUE>
            <QueIdeQUE1>2</QueIdeQUE1>
          </GUAQUE>
          <EXPEXP>
            <ExpEXP1>2405364156714.86</ExpEXP1>
            <ExpCouEXP2>68843528</ExpCouEXP2>
            <BalEXP3>12345678.90</BalEXP3>
            <CurEXP4>GBP</CurEXP4>
          </EXPEXP>
        </GUAREF2>
      </CD037A>
    }

    service
      .formatMessage(recipient, simulatedResponse)
      .map { xml => Utility.trim(xml).toString shouldBe Utility.trim(successXml).toString }
      .unsafeToFuture()
  }

  it should "format successful response with correct guarantee type for standard guarantee reference" in {
    val simulatedResponse = SimulatedResponse(
      TaxIdentifier("GB12345678900"),
      GuaranteeReference("20GB0000010000GX1"),
      UniqueReference("7acb933dbe7039"),
      BalanceRequestSuccess(BigDecimal("12345678.90"), CurrencyCode("GBP"))
    )

    val successXml = {
      <CD037A>
        <SynIdeMES1>UNOC</SynIdeMES1>
        <SynVerNumMES2>3</SynVerNumMES2>
        <MesSenMES3>NTA.GB</MesSenMES3>
        <MesRecMES6>MDTP-GUA-22b9899e24ee48e6a18997d1</MesRecMES6>
        <DatOfPreMES9>20210907</DatOfPreMES9>
        <TimOfPreMES10>1553</TimOfPreMES10>
        <IntConRefMES11>60b420bb3851d9</IntConRefMES11>
        <MesIdeMES19>60b420bb3851d9</MesIdeMES19>
        <MesTypMES20>GB037A</MesTypMES20>
        <TRAPRIRC1>
          <TINRC159>GB12345678900</TINRC159>
        </TRAPRIRC1>
        <CUSTOFFGUARNT>
          <RefNumRNT1>GB000001</RefNumRNT1>
        </CUSTOFFGUARNT>
        <GUAREF2>
          <GuaRefNumGRNREF21>20GB0000010000GX1</GuaRefNumGRNREF21>
          <AccDatREF24>20210114</AccDatREF24>
          <GuaTypREF22>1</GuaTypREF22>
          <GuaMonCodREF23>1</GuaMonCodREF23>
          <GUAQUE>
            <QueIdeQUE1>2</QueIdeQUE1>
          </GUAQUE>
          <EXPEXP>
            <ExpEXP1>2405364156714.86</ExpEXP1>
            <ExpCouEXP2>68843528</ExpCouEXP2>
            <BalEXP3>12345678.90</BalEXP3>
            <CurEXP4>GBP</CurEXP4>
          </EXPEXP>
        </GUAREF2>
      </CD037A>
    }

    service
      .formatMessage(recipient, simulatedResponse)
      .map { xml => Utility.trim(xml).toString shouldBe Utility.trim(successXml).toString }
      .unsafeToFuture()
  }

  it should "format functional error response" in {
    val simulatedResponse = SimulatedResponse(
      TaxIdentifier("GB12345678900"),
      GuaranteeReference("20GB0000010000GX1"),
      UniqueReference("7acb933dbe7039"),
      BalanceRequestFunctionalError(
        NonEmptyList.one(
          FunctionalError(ErrorType(12), "Foo.Bar(1).Baz", Some("Invalid something or other"))
        )
      )
    )

    val errorXml = {
      <CD906A>
        <SynIdeMES1>UNOC</SynIdeMES1>
        <SynVerNumMES2>3</SynVerNumMES2>
        <MesSenMES3>NTA.GB</MesSenMES3>
        <MesRecMES6>MDTP-GUA-22b9899e24ee48e6a18997d1</MesRecMES6>
        <DatOfPreMES9>20210907</DatOfPreMES9>
        <TimOfPreMES10>1553</TimOfPreMES10>
        <IntConRefMES11>60b420bb3851d9</IntConRefMES11>
        <MesIdeMES19>60b420bb3851d9</MesIdeMES19>
        <MesTypMES20>GB906A</MesTypMES20>
        <OriMesIdeMES22>{simulatedResponse.originalMessageReference.value}</OriMesIdeMES22>
        <FUNERRER1>
          <ErrTypER11>12</ErrTypER11>
          <ErrPoiER12>Foo.Bar(1).Baz</ErrPoiER12>
          <ErrReaER13>Invalid something or other</ErrReaER13>
        </FUNERRER1>
      </CD906A>
    }

    service
      .formatMessage(recipient, simulatedResponse)
      .map { xml => Utility.trim(xml).toString shouldBe Utility.trim(errorXml).toString }
      .unsafeToFuture()
  }

  it should "format XML error response" in {
    val simulatedResponse = SimulatedResponse(
      TaxIdentifier("GB12345678900"),
      GuaranteeReference("20GB0000010000GX1"),
      UniqueReference("7acb933dbe7039"),
      BalanceRequestXmlError(
        NonEmptyList.one(XmlError(ErrorType(12), "Foo.Bar(1).Baz", None))
      )
    )

    val errorXml = {
      <CC917A>
        <SynIdeMES1>UNOC</SynIdeMES1>
        <SynVerNumMES2>3</SynVerNumMES2>
        <MesSenMES3>NTA.GB</MesSenMES3>
        <MesRecMES6>MDTP-GUA-22b9899e24ee48e6a18997d1</MesRecMES6>
        <DatOfPreMES9>20210907</DatOfPreMES9>
        <TimOfPreMES10>1553</TimOfPreMES10>
        <IntConRefMES11>60b420bb3851d9</IntConRefMES11>
        <MesIdeMES19>60b420bb3851d9</MesIdeMES19>
        <MesTypMES20>GB917A</MesTypMES20>
        <HEAHEA>
          <OriMesIdeMES22>{simulatedResponse.originalMessageReference.value}</OriMesIdeMES22>
        </HEAHEA>
        <FUNERRER1>
          <ErrTypER11>12</ErrTypER11>
          <ErrPoiER12>Foo.Bar(1).Baz</ErrPoiER12>
        </FUNERRER1>
      </CC917A>
    }

    service
      .formatMessage(recipient, simulatedResponse)
      .map { xml => Utility.trim(xml).toString shouldBe Utility.trim(errorXml).toString }
      .unsafeToFuture()
  }
}
