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
import io.lemonlabs.uri.UrlPath
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {
  private lazy val traderRouterBaseUrl: AbsoluteUrl =
    AbsoluteUrl.parse(servicesConfig.baseUrl("trader-router"))
  private lazy val traderRouterPath: UrlPath =
    UrlPath.parse(
      config.get[String]("microservice.services.trader-router.path")
    )
  lazy val traderRouterUrl: AbsoluteUrl =
    traderRouterBaseUrl.withPath(traderRouterPath)
}
