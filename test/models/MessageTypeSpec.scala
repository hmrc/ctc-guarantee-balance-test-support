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

package models

import cats.data.NonEmptyList
import models.errors.FunctionalError
import models.errors.XmlError
import models.values.CurrencyCode
import models.values.ErrorType
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MessageTypeSpec extends AnyFlatSpec with Matchers {
  "MessageType.forBalanceResponse" should "return IE037 for a successful response" in {
    val response = BalanceRequestSuccess(BigDecimal("12345678.90"), CurrencyCode("GBP"))
    MessageType.forBalanceResponse(response) shouldBe MessageType.ResponseQueryOnGuarantees
  }

  it should "return IE906 for a functional error response" in {
    val response = BalanceRequestFunctionalError(
      NonEmptyList.one(FunctionalError(ErrorType(12), "Foo.Bar(1).Baz", None))
    )
    MessageType.forBalanceResponse(response) shouldBe MessageType.FunctionalNack
  }

  it should "return IE917 for an XML error response" in {
    val response = BalanceRequestXmlError(
      NonEmptyList.one(XmlError(ErrorType(12), "Foo.Bar(1).Baz", None))
    )
    MessageType.forBalanceResponse(response) shouldBe MessageType.XmlNack
  }
}
