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

package controllers.auth

import com.google.inject.ImplementedBy
import config.ApplicationConfig
import connectors.PertaxAuthConnector
import models.pertaxAuth.PertaxAuthResponseModel
import play.api.Logging
import play.api.http.Status.UNAUTHORIZED
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{ActionFilter, ControllerComponents, Request, Result, Results}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
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
  main: Main,
  appConfig: ApplicationConfig
)(
  implicit val executionContext: ExecutionContext,
  controllerComponents: ControllerComponents
) extends ActionFilter[Request]
  with Results
  with PertaxAuthAction
  with I18nSupport
  with Logging {

  override def messagesApi: MessagesApi = controllerComponents.messagesApi

  override def filter[A](request: Request[A]): Future[Option[Result]] = {
    implicit val implicitRequest: Request[A] = request
    implicit val hc: HeaderCarrier           = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    pertaxAuthConnector.pertaxPostAuthorise.value.flatMap {
      case Left(UpstreamErrorResponse(_, status, _, _)) if status == UNAUTHORIZED          =>
        Future.successful(Some(Redirect(appConfig.ggSignInUrl)))
      case Left(_)                                                                         =>
        Future.successful(Some(InternalServerError(technicalIssue())))
      case Right(PertaxAuthResponseModel(ACCESS_GRANTED, _, _, _))                                =>
        Future.successful(None)
      case Right(PertaxAuthResponseModel(NO_HMRC_PT_ENROLMENT, _, Some(redirect), _))             =>
        Future.successful(Some(Redirect(s"$redirect?redirectUrl=${SafeRedirectUrl(request.uri).encodedUrl}")))
      case Right(PertaxAuthResponseModel("CONFIDENCE_LEVEL_UPLIFT_REQUIRED", _, _, _)) =>
        Future.successful(Some(Redirect(appConfig.ivUpliftUrl)))
      case Right(PertaxAuthResponseModel("CREDENTIAL_STRENGTH_UPLIFT_REQUIRED", _, Some(_), _))     =>
        val ex =
          new RuntimeException(
            s"Weak credentials should be dealt before the service"
          )
        logger.error(ex.getMessage, ex)
        Future.successful(Some(InternalServerError(technicalIssue())))

      case Right(PertaxAuthResponseModel(_, _, _, Some(errorView))) =>
        pertaxAuthConnector.loadPartial(errorView.url).map {
          case partial: HtmlPartial.Success =>
            Some(Status(errorView.statusCode)(main(partial.title.getOrElse(""))(partial.content)))
          case _: HtmlPartial.Failure       =>
            logger.error(s"The partial ${errorView.url} failed to be retrieved")
            Some(InternalServerError(technicalIssue()))
        }
      case Right(response)                                 =>
        val ex =
          new RuntimeException(
            s"Pertax response `${response.code}` with message ${response.message} is not handled"
          )
        logger.error(ex.getMessage, ex)
        Future.successful(Some(InternalServerError(technicalIssue())))
    }
  }
}

@ImplementedBy(classOf[PertaxAuthActionImpl])
trait PertaxAuthAction extends ActionFilter[Request]
