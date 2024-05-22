/*
 * Copyright 2023 HM Revenue & Customs
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

package views

import com.google.inject.{ImplementedBy, Inject}
import config.ApplicationConfig
import models.auth.BaseUserRequest
import play.api.Logging
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.hmrcstandardpage.ServiceURLs
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.sca.models.BannerConfig
import uk.gov.hmrc.sca.models.auth.AuthenticatedRequest
import uk.gov.hmrc.sca.services.WrapperService

import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[MainImpl])
trait Main {
  def apply(
             pageTitle: String,
             backLinkHref: Option[String] = None,
             sessionExpiredPage: Boolean = false,
             scripts: Option[Html] = None,
             backLinkAttrs: Map[String, String] = Map.empty,
             afterContent: Option[Html] = None,
             serviceTitle: String = "title.pattern",
             disableBackLink: Boolean = false
           )(
             contentBlock: Html
           )(implicit
             BaseUserRequest: BaseUserRequest[_],
             messages: Messages
           ): HtmlFormat.Appendable
}

class MainImpl @Inject() (
                           appConfig: ApplicationConfig,
                           featureFlagService: FeatureFlagService,
                           wrapperService: WrapperService,
                           additionalStyles: views.html.components.additionalStyles,
                           additionalScripts: views.html.components.additionalScripts
                         ) extends Main with Logging {

  override def apply(
                      pageTitle: String,
                      backLinkHref: Option[String],
                      sessionExpiredPage: Boolean,
                      scripts: Option[Html],
                      backLinkAttrs: Map[String, String],
                      afterContent: Option[Html],
                      serviceTitle: String = "title.pattern",
                      disableBackLink: Boolean,
                    )(
                      contentBlock: Html
                    )(implicit BaseUserRequest: BaseUserRequest[_], messages: Messages): HtmlFormat.Appendable = {

    val trustedHelper = Try(BaseUserRequest.asInstanceOf[AuthenticatedRequest[_]]) match {
      case Failure(_: java.lang.ClassCastException) => None
      case Success(value) => value.trustedHelper
      case Failure(exception) => throw exception
    }

    val fullPageTitle =
    {
      s"""$pageTitle - ${messages(serviceTitle)}"""
    }
    wrapperService.standardScaLayout(
      content = contentBlock,
      pageTitle = Some(fullPageTitle),
      serviceNameKey = Some("tamc.apply"),
      serviceURLs = ServiceURLs(
        signOutUrl = Some(controllers.routes.AuthorisationController.logout().url)
      ),
      timeOutUrl = Some(controllers.routes.AuthorisationController.sessionTimeout().url),
      keepAliveUrl = "/keep-alive",
      backLinkUrl = backLinkHref,
      hideMenuBar = !BaseUserRequest.isAuthenticated,
      scripts = Seq(additionalScripts(scripts)),
      styleSheets = Seq(
        additionalStyles()
      ),
      bannerConfig = BannerConfig(
        showAlphaBanner = false,
        showBetaBanner = true,
        showHelpImproveBanner = false
      ),
      optTrustedHelper = trustedHelper,
      fullWidth = false
    )(messages, BaseUserRequest)
  }
}
