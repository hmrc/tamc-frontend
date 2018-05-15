/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import javax.inject.Inject
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.Future

class LanguageController @Inject()(val messagesApi: MessagesApi) extends BaseController with I18nSupport {

  def enGb(redirectUrl: String): Action[AnyContent] = changeLang(redirectUrl=redirectUrl, language="en")
  def cyGb(redirectUrl: String): Action[AnyContent] = changeLang(redirectUrl=redirectUrl, language="cy")

  def changeLang(redirectUrl: String, language: String) = Action.async { implicit request =>

    val path = request.path.split('/').filter(_ != "")

    if (path.length > 0 && (redirectUrl contains path.head))
      Future.successful(Redirect(redirectUrl).withLang(Lang(language)))
    else
      Future.successful(Redirect("/" + path + "/" + redirectUrl).withLang(Lang(language)))
  }
}