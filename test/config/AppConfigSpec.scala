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

package config

import io.lemonlabs.uri.AbsoluteUrl
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class AppConfigSpec extends AnyFlatSpec with Matchers {

  def mkAppConfig(config: Configuration) = {
    val servicesConfig = new ServicesConfig(config)
    new AppConfig(config, servicesConfig)
  }

  "AppConfig" should "deserialize trader-router config" in {
    val appConfig = mkAppConfig(
      Configuration(
        "microservice.services.trader-router.protocol" -> "https",
        "microservice.services.trader-router.host"     -> "foo",
        "microservice.services.trader-router.port"     -> "101010",
        "microservice.services.trader-router.path"     -> "/bar/baz/quu"
      )
    )

    appConfig.traderRouterUrl shouldBe AbsoluteUrl.parse("https://foo:101010/bar/baz/quu")
  }
}
