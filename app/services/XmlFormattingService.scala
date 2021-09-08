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

import cats.effect.IO
import models.BalanceRequestFunctionalError
import models.BalanceRequestSuccess
import models.BalanceRequestXmlError
import models.SimulatedResponse
import models.values.GuaranteeReference
import models.values.MessageRecipient
import models.values.TaxIdentifier

import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton
import scala.math.BigDecimal.RoundingMode
import scala.xml.Elem

@Singleton
class XmlFormattingService @Inject() (clock: Clock, random: Random) {

  private def newUniqueReference(): String = {
    val bytes = new Array[Byte](7)
    random.nextBytes(bytes)

    val sb = new StringBuilder

    for (byte <- bytes)
      sb.append(f"${byte}%02x")

    sb.toString
  }

  private val dateFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)
  private val timeFormatter =
    DateTimeFormatter.ofPattern("HHmm").withZone(ZoneOffset.UTC)

  private def formatSuccessMessage(
    recipient: MessageRecipient,
    taxIdentifier: TaxIdentifier,
    guaranteeReference: GuaranteeReference,
    successResponse: BalanceRequestSuccess
  ): Elem = {
    val BalanceRequestSuccess(balance, currency) = successResponse

    val uniqueRef = newUniqueReference()
    val dateTime  = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)

    val randomExposure = {
      val randomValue = random.nextDouble().abs * 9999999999999.99
      BigDecimal(randomValue).setScale(2, RoundingMode.HALF_EVEN)
    }

    <CD037A>
      <SynIdeMES1>UNOC</SynIdeMES1>
      <SynVerNumMES2>3</SynVerNumMES2>
      <MesSenMES3>NTA.GB</MesSenMES3>
      <MesRecMES6>{recipient.value}</MesRecMES6>
      <DatOfPreMES9>{dateFormatter.format(dateTime)}</DatOfPreMES9>
      <TimOfPreMES10>{timeFormatter.format(dateTime)}</TimOfPreMES10>
      <IntConRefMES11>{uniqueRef}</IntConRefMES11>
      <MesIdeMES19>{uniqueRef}</MesIdeMES19>
      <MesTypMES20>GB037A</MesTypMES20>
      <TRAPRIRC1>
        <TINRC159>{taxIdentifier.value}</TINRC159>
      </TRAPRIRC1>
      <CUSTOFFGUARNT>
        <RefNumRNT1>GB000001</RefNumRNT1>
      </CUSTOFFGUARNT>
      <GUAREF2>
        <GuaRefNumGRNREF21>{guaranteeReference.value}</GuaRefNumGRNREF21>
        <AccDatREF24>20210114</AccDatREF24>
        <GuaTypREF22>{if (guaranteeReference.value.length > 17) 4 else 1}</GuaTypREF22>
        <GuaMonCodREF23>1</GuaMonCodREF23>
        <GUAQUE>
          <QueIdeQUE1>2</QueIdeQUE1>
        </GUAQUE>
        <EXPEXP>
          <ExpEXP1>{randomExposure.toString}</ExpEXP1>
          <ExpCouEXP2>{random.nextInt(99999999)}</ExpCouEXP2>
          <BalEXP3>{balance.toString}</BalEXP3>
          <CurEXP4>{currency.value}</CurEXP4>
        </EXPEXP>
      </GUAREF2>
    </CD037A>
  }

  private def formatFunctionalErrorMessage(
    recipient: MessageRecipient,
    errorResponse: BalanceRequestFunctionalError
  ): Elem = {
    val uniqueRef = newUniqueReference()
    val dateTime  = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)

    <CD906A>
      <SynIdeMES1>UNOC</SynIdeMES1>
      <SynVerNumMES2>3</SynVerNumMES2>
      <MesSenMES3>NTA.GB</MesSenMES3>
      <MesRecMES6>{recipient.value}</MesRecMES6>
      <DatOfPreMES9>{dateFormatter.format(dateTime)}</DatOfPreMES9>
      <TimOfPreMES10>{timeFormatter.format(dateTime)}</TimOfPreMES10>
      <IntConRefMES11>{uniqueRef}</IntConRefMES11>
      <MesIdeMES19>{uniqueRef}</MesIdeMES19>
      <MesTypMES20>GB906A</MesTypMES20>
      <OriMesIdeMES22>{newUniqueReference()}</OriMesIdeMES22>
      {
      errorResponse.errors.toList.map { err =>
        <FUNERRER1>
          <ErrTypER11>{err.errorType.value}</ErrTypER11>
          <ErrPoiER12>{err.errorPointer}</ErrPoiER12>
          {err.errorReason.map(reason => <ErrReaER13>{reason}</ErrReaER13>).orNull}
        </FUNERRER1>
      }
    }
    </CD906A>
  }

  private def formatXmlErrorMessage(
    recipient: MessageRecipient,
    errorResponse: BalanceRequestXmlError
  ): Elem = {
    val uniqueRef = newUniqueReference()
    val dateTime  = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)

    <CC917A>
      <SynIdeMES1>UNOC</SynIdeMES1>
      <SynVerNumMES2>3</SynVerNumMES2>
      <MesSenMES3>NTA.GB</MesSenMES3>
      <MesRecMES6>{recipient.value}</MesRecMES6>
      <DatOfPreMES9>{dateFormatter.format(dateTime)}</DatOfPreMES9>
      <TimOfPreMES10>{timeFormatter.format(dateTime)}</TimOfPreMES10>
      <IntConRefMES11>{uniqueRef}</IntConRefMES11>
      <MesIdeMES19>{uniqueRef}</MesIdeMES19>
      <MesTypMES20>GB917A</MesTypMES20>
      <HEAHEA>
        <OriMesIdeMES22>{newUniqueReference()}</OriMesIdeMES22>
      </HEAHEA>
      {
      errorResponse.errors.toList.map { err =>
        <FUNERRER1>
          <ErrTypER11>{err.errorType.value}</ErrTypER11>
          <ErrPoiER12>{err.errorPointer}</ErrPoiER12>
          {err.errorReason.map(reason => <ErrReaER13>{reason}</ErrReaER13>).orNull}
        </FUNERRER1>
      }
    }
    </CC917A>
  }

  def formatMessage(
    recipient: MessageRecipient,
    simulatedResponse: SimulatedResponse
  ): IO[Elem] = IO.blocking {
    simulatedResponse.response match {
      case error @ BalanceRequestXmlError(_) =>
        formatXmlErrorMessage(recipient, error)
      case error @ BalanceRequestFunctionalError(_) =>
        formatFunctionalErrorMessage(recipient, error)
      case success @ BalanceRequestSuccess(_, _) =>
        formatSuccessMessage(
          recipient,
          simulatedResponse.taxIdentifier,
          simulatedResponse.guaranteeReference,
          success
        )
    }
  }
}
