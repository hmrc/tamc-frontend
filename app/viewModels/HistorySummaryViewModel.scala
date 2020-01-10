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

import java.text.NumberFormat

import models.RelationshipRecord
import org.joda.time.LocalDate
import play.api.i18n.Messages
import play.twirl.api.Html
import utils.LanguageUtils
import views.helpers.TextGenerators

case class HistorySummaryButton(id: String, content: String, href: String)

case class HistorySummaryViewModel(paragraphContent: Html, button: HistorySummaryButton)

object HistorySummaryViewModel {

  def apply(isActiveRecord: Boolean, isHistoricActiveRecord: Boolean, activeRelationship: Option[RelationshipRecord],
            historicRelationship: Option[Seq[RelationshipRecord]], maxPATransfer: Int, maxBenefit: Int, endOfYear: LocalDate)
           (implicit messages: Messages): HistorySummaryViewModel = {

    if(isActiveRecord){
      createActiveRecordBasedViewModel(activeRelationship, maxPATransfer, maxBenefit)
    } else {
      createHistoricBasedViewModel(historicRelationship, endOfYear)
    }
  }

  private def createActiveRecordBasedViewModel(activeRelationShip: Option[RelationshipRecord], maxPATransfer: Int, maxBenefit: Int)
                                              (implicit messages: Messages): HistorySummaryViewModel = {
    //TODO no active records
    activeRelationShip.fold(handleError("active")){ activeRelationShip =>

      val paragraphContent = if(activeRelationShip.participant == "Transferor"){

        Html(s"<p>${messages("pages.history.active.transferor")}</p>")

      } else {
        val formattedMaxPATransfer = NumberFormat.getIntegerInstance().format(maxPATransfer)
        val formattedMaxBenefit = NumberFormat.getIntegerInstance().format(maxBenefit)

        Html(s"<p>${messages("pages.history.active.recipient.paragraph1", formattedMaxPATransfer)}</p>" +
          s"<p>${messages("pages.history.active.recipient.paragraph2", formattedMaxBenefit)}</P>")
      }

      val button = HistorySummaryButton("checkOrUpdateMarriageAllowance", messages("pages.history.active.button"),
        controllers.routes.UpdateRelationshipController.decision().url)
      HistorySummaryViewModel(paragraphContent, button)
    }
  }


  private def createHistoricBasedViewModel(historicRelationships: Option[Seq[RelationshipRecord]], endOfYear: LocalDate)
                                          (implicit messages: Messages): HistorySummaryViewModel = {

    historicRelationships match {
      case(Some(Seq(relationshipRecord, _*))) => {

        val formattedEndOfYear = TextGenerators.ukDateTransformer(Some(endOfYear), LanguageUtils.isWelsh(messages))

        val paragraphContent = if(relationshipRecord.participant == "Transferor"){

          //TODO welsh
          Html(s"<p>${messages("pages.history.historic.ended")}</p>" +
            s"<p>${messages("pages.history.historic.transferor", formattedEndOfYear)}</P>")

        }else{
          //TODO welsh
          Html(s"<p>${messages("pages.history.historic.ended")}</p>" +
            s"<p>${messages("pages.history.historic.recipient", formattedEndOfYear)}</P>")
        }

        val button = HistorySummaryButton("checkMarriageAllowance", messages("pages.history.historic.button"),
          controllers.routes.UpdateRelationshipController.claims().url)
        HistorySummaryViewModel(paragraphContent, button)

      }
      case _ => handleError("historic")
    }

  }

  private def handleError(accountStatus: String) = {

    val errorMessage = accountStatus match {
      case("active") => "No active relationship record found"
      case("historic") => "No historic relationship record found"
    }

    //TODO logging level

    throw new RuntimeException(errorMessage)
  }

}
