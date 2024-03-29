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

package models.errors

import play.api.i18n.Messages
import play.api.libs.json.JsPath
import play.api.libs.json.Json
import play.api.libs.json.JsonValidationError
import play.api.libs.json.OWrites
import scala.collection.Seq

case class JsonParsingError(
  message: String = "Invalid request JSON",
  errors: Seq[(JsPath, Seq[JsonValidationError])]
)

object JsonParsingError {
  private def toJsonPath(path: JsPath) =
    path.path.foldLeft("$")((root, next) => root + next.toJsonString)

  implicit def jsonParsingErrorWrites(implicit messages: Messages): OWrites[JsonParsingError] =
    OWrites { error =>
      Json.obj(
        "code"    -> "INVALID_REQUEST_JSON",
        "message" -> error.message,
        "errors" -> error.errors.foldLeft(Json.obj()) { case (obj, (path, errors)) =>
          obj ++ Json.obj(toJsonPath(path) -> errors.map { error =>
            messages(error.message, error.args: _*)
          })
        }
      )
    }
}
