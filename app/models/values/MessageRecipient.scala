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

package models.values

import play.api.Logging
import play.api.mvc.PathBindable

import java.util.UUID

sealed abstract class MessageRecipient extends Product with Serializable {
  def messageIdValue: String = this match {
    case MessageIdRecipient(value) =>
      value
    case BalanceIdRecipient(uuid) =>
      "MDTP-GUA-" + uuid.toString.replaceAll("-", "").take(24)
  }
}

case class MessageIdRecipient(value: String) extends MessageRecipient

case class BalanceIdRecipient(value: UUID) extends MessageRecipient

object MessageRecipient extends Logging {
  val MessageIdRegex = """MDTP-GUA-[0-9a-fA-F]{24}""".r

  implicit val messageIdentifierRecipientPathBindable: PathBindable[MessageIdRecipient] =
    new PathBindable.Parsing[MessageIdRecipient](
      MessageIdRecipient.apply,
      _.value,
      (key, exc) => {
        logger.warn("Unable to parse message identifier value", exc)
        s"Cannot parse parameter $key as a message identifier value"
      }
    )

  implicit val balanceIdRecipientPathBindable: PathBindable[BalanceIdRecipient] =
    PathBindable.bindableUUID.transform(BalanceIdRecipient.apply, _.value)

  implicit val messageRecipientPathBindable: PathBindable[MessageRecipient] =
    new PathBindable[MessageRecipient] {
      override def bind(key: String, value: String): Either[String, MessageRecipient] =
        value match {
          case MessageIdRegex() =>
            messageIdentifierRecipientPathBindable.bind(key, value)
          case _ =>
            balanceIdRecipientPathBindable.bind(key, value)
        }

      override def unbind(key: String, value: MessageRecipient): String =
        value match {
          case balanceId @ BalanceIdRecipient(_) =>
            balanceIdRecipientPathBindable.unbind(key, balanceId)
          case messageId @ MessageIdRecipient(_) =>
            messageIdentifierRecipientPathBindable.unbind(key, messageId)
        }
    }
}
