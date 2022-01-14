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

package models.values

import org.scalacheck.Gen
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.mvc.PathBindable

import java.util.UUID

class MessageRecipientSpec
    extends AnyFlatSpec
    with Matchers
    with EitherValues
    with ScalaCheckPropertyChecks {

  val binder = implicitly[PathBindable[MessageRecipient]]

  val validMessageIdRecipientGen = Gen.stringOfN(24, Gen.hexChar).map("MDTP-GUA-" + _)
  val validBalanceIdRecipientGen = Gen.uuid.map(_.toString)

  "MessageRecipient" should "be usable as a path parameter when given valid message ID input" in forAll(
    validMessageIdRecipientGen
  ) { id =>
    binder.bind("recipient", id) shouldBe Right(MessageIdRecipient(id))
  }

  it should "be usable as a path parameter when given valid balance ID input" in forAll(
    validBalanceIdRecipientGen
  ) { id =>
    binder.bind("recipient", id) shouldBe Right(BalanceIdRecipient(UUID.fromString(id)))
  }

  it should "return an error when given input missing the message sender prefix" in {
    binder.bind("recipient", "22b9899e24ee48e6a18997d1") shouldBe Left(
      "Cannot parse parameter recipient as UUID: Invalid UUID string: 22b9899e24ee48e6a18997d1"
    )
  }

  it should "return an error when given input containing invalid characters" in {
    binder.bind("recipient", "MDTP-GUA-X2b9899e24ee48e6a18997d1") shouldBe Left(
      "Cannot parse parameter recipient as UUID: Invalid UUID string: MDTP-GUA-X2b9899e24ee48e6a18997d1"
    )
  }

  it should "return an error when given input that looks like an arrivals identifier" in {
    binder.bind("recipient", "MDTP-ARR-00000000000000000000001-01") shouldBe Left(
      "Cannot parse parameter recipient as UUID: Invalid UUID string: MDTP-ARR-00000000000000000000001-01"
    )
  }

  it should "return an error when given input that looks like a departures identifier" in {
    binder.bind("recipient", "MDTP-DEP-00000000000000000000001-01") shouldBe Left(
      "Cannot parse parameter recipient as UUID: Invalid UUID string: MDTP-DEP-00000000000000000000001-01"
    )
  }
}
