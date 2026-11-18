/*
 * Copyright 2026 HM Revenue & Customs
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

package views.UpdateRelationship

import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time.TaxYear
import utils.{BaseTest, NinoGenerator}
import views.html.coc.cancellationSubmitted

import java.time.format.DateTimeFormatter
import java.util.Locale

class FinishedChangeContentTest extends BaseTest with Injecting with NinoGenerator {

  lazy val view: cancellationSubmitted = inject[cancellationSubmitted]
  lazy val nino: String                = generateNino().nino

  implicit val request: AuthenticatedUserRequest[?] =
    AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  override implicit lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])
  private val dateFormatter                         = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)
  private val futureEndDate                         = TaxYear.current.finishes.format(dateFormatter)
  private val pastEndDate                           = TaxYear.current.previous.finishes.format(dateFormatter)
  private lazy val doc: Document                    = Jsoup.parse(view(futureEndDate, showActiveUntilText = true).toString())

  "Finished change" should {
    "Display correct page heading" in {
      doc.select("h1#pageHeading").text() shouldBe "Marriage Allowance cancellation submitted"
    }

    "display the email acknowledgement" in {
      doc.select("p").eachText() should contain(
        "We will send you a confirmation email within 24 hours. If you do not receive it, check your spam or junk folder."
      )
    }

    "display what happens next with Active status message for a future end date" in {
      doc.select("h2").eachText()                should contain("What happens next")
      doc.select("h2 + p.govuk-body").eachText() should contain(
        s"We will cancel your Marriage Allowance from $futureEndDate. Until then your Marriage Allowance will show as ‘Active’."
      )
    }

    "display what happens next with only the cancellation date sentence for a past end date" in {
      val pastDateDoc = Jsoup.parse(view(pastEndDate, showActiveUntilText = false).toString())

      pastDateDoc.select("h2").eachText()                should contain("What happens next")
      pastDateDoc.select("h2 + p.govuk-body").eachText() should contain(
        s"We will cancel your Marriage Allowance from $pastEndDate."
      )

      pastDateDoc.text() should not include "Until then your Marriage Allowance will show as ‘Active’."
    }

    "display the feedback section and survey link" in {
      doc.select("h2").eachText()                      should contain("Before you go")
      doc.select("p").eachText()                       should contain("Your feedback helps us make our service better.")
      doc.select("a#take-survey-link[href]").text()  shouldBe "Take a short survey"
      doc.select("p:has(a#take-survey-link)").text() shouldBe
        "Take a short survey to share your feedback on this service."
    }
  }
}
