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
import models.admin.SCAWrapperToggle
import models.auth.BaseUserRequest
import play.api.Logging
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.sca.models.BannerConfig
import uk.gov.hmrc.sca.models.auth.AuthenticatedRequest
import uk.gov.hmrc.sca.services.WrapperService

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[MainImpl])
trait Main {
  def apply(
             pageTitle: Option[String],
             serviceName: Option[String] = Some("tamc.apply"),
             backLinkHref: Option[String] = None,
             sessionExpiredPage: Boolean = false,
             scripts: Option[Html] = None,
             backLinkAttrs: Map[String, String] = Map.empty,
             afterContent: Option[Html] = None,
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
                           oldMain: views.html.oldMain,
                           additionalStyles: views.html.components.additionalStyles,
                           additionalScripts: views.html.components.additionalScripts
                         ) extends Main with Logging {

  //noinspection ScalaStyle
  override def apply(
                      pageTitle: Option[String],
                      serviceName: Option[String],
                      backLinkHref: Option[String],
                      sessionExpiredPage: Boolean,
                      scripts: Option[Html],
                      backLinkAttrs: Map[String, String],
                      afterContent: Option[Html],
                      disableBackLink: Boolean,
                    )(
                      contentBlock: Html
                    )(implicit BaseUserRequest: BaseUserRequest[_], messages: Messages): HtmlFormat.Appendable = {
    val scaWrapperToggle = Await.result(featureFlagService.get(SCAWrapperToggle), Duration(appConfig.scaWrapperFutureTimeout, SECONDS))
    val trustedHelper    = Try(BaseUserRequest.asInstanceOf[AuthenticatedRequest[_]]) match {
      case Failure(_: java.lang.ClassCastException) => None
      case Success(value)                           => value.trustedHelper
      case Failure(exception)                       => throw exception
    }

    if (scaWrapperToggle.isEnabled) {
      logger.debug(s"SCA Wrapper layout used for request `${BaseUserRequest.uri}``")

      wrapperService.layout(
        content = contentBlock,
        pageTitle = pageTitle,
        serviceNameKey = Some(messages("tamc.apply")),
        serviceNameUrl = None,
        signoutUrl = controllers.routes.AuthorisationController.logout.url,
        timeOutUrl = Some(controllers.routes.AuthorisationController.sessionTimeout.url),
        keepAliveUrl = "/keep-alive",
        backLinkUrl = backLinkHref,
        showSignOutInHeader = false,
        hideMenuBar = !BaseUserRequest.isAuthenticated,
        scripts = Seq(additionalScripts(scripts)),
        styleSheets = Seq(
          additionalStyles()
        ),
        bannerConfig = BannerConfig(
          showAlphaBanner = false,
          showBetaBanner = true,
          showHelpImproveBanner = true
        ),
        optTrustedHelper = trustedHelper,
        fullWidth = false
      )(messages, HeaderCarrierConverter.fromRequest(BaseUserRequest), BaseUserRequest)

    } else {
      logger.debug(s"Old layout used for request `${BaseUserRequest.uri}``")

      oldMain(
        pageTitle,
        serviceName,
        backLinkHref,
        sessionExpiredPage,
        scripts,
        backLinkAttrs,
        afterContent,
        disableBackLink
      )(
        contentBlock
      )
    }
  }
}
