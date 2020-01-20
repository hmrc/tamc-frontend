/*
 * Copyright 2020 HM Revenue & Customs
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

package viewModels

import config.ApplicationConfig
import models.{RelationshipRecordGroup, Role}
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.time.TaxYear
import utils.LanguageUtils
import views.helpers.TextGenerators

case class BereavementViewModel(bereavementParagraphWithLink: Html, role: Role){
  def endOfYear(implicit messages: Messages): String = {
    TextGenerators.ukDateTransformer(Some(TaxYear.current.finishes),
      LanguageUtils.isWelsh(messages), transformPattern = "d MMMM")
  }
}

object BereavementViewModel {

  def apply(relationshipRecordGroup: RelationshipRecordGroup)(implicit messages: Messages): BereavementViewModel = {

    BereavementViewModel(bereavementParagraphWithLink, relationshipRecordGroup.role)
  }

  private def bereavementParagraphWithLink(implicit messages: Messages): Html = {
    Html(
    s"""<p>${messages("notify.you.can")} <a href="${ApplicationConfig.generalEnquiriesLink}">
       |${messages("pages.bereavement.notify-hmrc.link")}</a> ${messages("pages.bereavement.notify-hmrc.part2")}</p>""".stripMargin)
  }

}