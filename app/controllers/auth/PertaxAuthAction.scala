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

package controllers.auth

import com.google.inject.ImplementedBy
import connectors.PertaxAuthConnector
import models.admin.PertaxBackendToggle
import models.auth.AuthenticatedUserRequest
import models.pertaxAuth.PertaxAuthResponseModel
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.{InternalServerError, Redirect, Status}
import play.api.mvc.{ActionRefiner, ControllerComponents, Result}
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.partials.HtmlPartial
import utils.Constants.{ACCESS_GRANTED, NO_HMRC_PT_ENROLMENT}
import views.Main
import views.html.errors.try_later

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PertaxAuthActionImpl @Inject()(
  pertaxAuthConnector: PertaxAuthConnector,
  technicalIssue: try_later,
  featureFlagService: FeatureFlagService,
  main: Main
)(
  implicit val executionContext: ExecutionContext,
  controllerComponents: ControllerComponents
) extends PertaxAuthAction
  with I18nSupport
  with Logging {

  override def messagesApi: MessagesApi = controllerComponents.messagesApi

  override protected def refine[A](request: AuthenticatedUserRequest[A]): Future[Either[Result, AuthenticatedUserRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    implicit val implicitRequest: AuthenticatedUserRequest[A] = request

    featureFlagService.get(PertaxBackendToggle).flatMap { flag =>
      if (flag.isEnabled) {
        pertaxAuthConnector.authorise(request.nino.nino).flatMap {
          case Right(PertaxAuthResponseModel(ACCESS_GRANTED, _, _, _)) =>
            Future.successful(Right(request))
          case Right(PertaxAuthResponseModel(NO_HMRC_PT_ENROLMENT, _, Some(redirect), _)) =>
            Future.successful(Left(Redirect(s"$redirect/?redirectUrl=${SafeRedirectUrl(request.uri).encodedUrl}")))
          case Right(PertaxAuthResponseModel(_, _, _, Some(errorPartial))) =>
            pertaxAuthConnector.loadPartial(errorPartial.url).map {
              case partial: HtmlPartial.Success =>
                Left(Status(errorPartial.statusCode)(main(partial.title)(partial.content)))
              case _: HtmlPartial.Failure =>
                logger.error("[PertaxAuthAction][refine] Failed to retrieve a partial from pertax auth service.")
                Left(InternalServerError(technicalIssue()))
            }
          case _@error =>
            logger.error(s"[PertaxAuthAction][refine] Error thrown during authentication.")
            error.left.foreach(error => logger.error("[PertaxAuthAction][refine] Error details:" +
              s"\nStatus: ${error.statusCode}\nMessages: ${error.message}"))
            Future.successful(Left(InternalServerError(technicalIssue())))
        }
      } else {
        Future.successful(Right(request))
      }
    }
  }
}

@ImplementedBy(classOf[PertaxAuthActionImpl])
trait PertaxAuthAction extends ActionRefiner[AuthenticatedUserRequest, AuthenticatedUserRequest]
