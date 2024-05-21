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

import config.ApplicationConfig
import controllers.UpdateRelationshipController
import controllers.actions.AuthenticatedActionRefiner
import controllers.auth.PertaxAuthAction
import _root_.helpers.FakePertaxAuthAction
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.time.TaxYear
import utils.{BaseTest, MockAuthenticatedAction}

import java.time.LocalDate
import scala.concurrent.Future

class UpdateRelationshipContentTest extends BaseTest with Injecting {

  lazy val config: ApplicationConfig = inject[ApplicationConfig]
  val mockTransferService: TransferService = mock[TransferService]
  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]
  val mockCachingService: CachingService = mock[CachingService]
  val loggedInUser: LoggedInUserInfo = LoggedInUserInfo(1, "20130101",None, Some(CitizenName(Some("Test"), Some("User"))))
  val contactHMRCBereavementText: Timestamp = (messages("general.helpline.enquiries.link.pretext") + " "
    + messages("general.helpline.enquiries.link") + " "
    + messages("pages.bereavement.enquiries.link.paragraph"))
  implicit val request: Request[AnyContent] = FakeRequest()


  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[UpdateRelationshipService].toInstance(mockUpdateRelationshipService),
      bind[CachingService].toInstance(mockCachingService),
      bind[AuthenticatedActionRefiner].to[MockAuthenticatedAction],
      bind[MessagesApi].toInstance(stubMessagesApi()),
      bind[PertaxAuthAction].to[FakePertaxAuthAction],
    ).build()

  val controller: UpdateRelationshipController = inject[UpdateRelationshipController]

  "Update relationship cause - get view" should {
    "show all appropriate radio buttons" in {
      val expectedRadioButtons = Seq(
        messages("pages.makeChanges.radio.noLongerRequired"),
        messages("pages.makeChanges.radio.divorce"),
        messages("pages.makeChanges.radio.bereavement")
      ).toArray

      val request = FakeRequest()
      when(mockUpdateRelationshipService.getMakeChangesDecision(any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.makeChange()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val radioButtons = document.getElementsByClass("govuk-label").eachText().toArray
      radioButtons.length shouldBe expectedRadioButtons.length
      radioButtons shouldBe expectedRadioButtons

    }
  }

  "Stop Allowance Page" in {
    val result: Future[Result] = controller.stopAllowance(request)

    val expected = Seq(
      messages("pages.stopAllowance.paragraph1"),
      messages("pages.stopAllowance.paragraph2"),
      messages("claim.summary.link"),
      "Beta phase.banner.before phase.banner.link phase.banner.after"
    ).toArray
    val parsed = Jsoup.parse(contentAsString(result)).getElementById("main-content")
    val current = parsed.getElementsByTag("p").eachText().toArray()

    current shouldBe expected
  }

  "Cancel Page" in {
    val maEndingDates = MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts)

    when(mockUpdateRelationshipService.getMAEndingDatesForCancelation)
      .thenReturn(maEndingDates)

    when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(any())(any(), any()))
      .thenReturn(Future.successful(maEndingDates))

    val result: Future[Result] = controller.cancel(request)

    val expected = Seq(messages("pages.cancel.paragraph1"), messages("pages.cancel.paragraph2"),  "Beta phase.banner.before phase.banner.link phase.banner.after").toArray
    val parsed = Jsoup.parse(contentAsString(result)).getElementById("main-content")
    val current = parsed.getElementsByTag("p").eachText().toArray()

    current shouldBe expected
  }

  "Bereavement page" when {
    "recipient text" in {
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(RelationshipRecord(Recipient.value, "", "", None, None, "", ""), Seq(), loggedInUser)
          )
        )
      val result: Future[Result] = controller.bereavement(request)

      val expected = Seq(
        contactHMRCBereavementText,
        messages("pages.bereavement.recipient.paragraph"),
        "Beta phase.banner.before phase.banner.link phase.banner.after"
      ).toArray
      val parsed = Jsoup.parse(contentAsString(result)).getElementById("main-content")
      val current = parsed.getElementsByTag("p").eachText().toArray()

      current shouldBe expected
    }

    "transferor text" in {
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(RelationshipRecord(Transferor.value, "", "", None, None, "", ""), Seq(), loggedInUser)
          )
        )
      val result: Future[Result] = controller.bereavement(request)

      val expected = Seq(
        contactHMRCBereavementText,
        messages("pages.bereavement.transferor.paragraph"),
        "Beta phase.banner.before phase.banner.link phase.banner.after"
      ).toArray
      val parsed = Jsoup.parse(contentAsString(result)).getElementById("main-content")
      val current = parsed.getElementsByTag("p").eachText().toArray()

      current shouldBe expected
    }

    "recipient bullet list" in {
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(RelationshipRecord(Recipient.value, "", "", None, None, "", ""), Seq(), loggedInUser)
          )
        )
      val result: Future[Result] = controller.bereavement(request)

      val expected = Array()
      val parsed = Jsoup.parse(contentAsString(result)).getElementById("main-content")
      val current = parsed.getElementsByTag("li").eachText().toArray()

      current shouldBe expected
    }

    "transferor bullet list" in {
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(RelationshipRecord(Transferor.value, "", "", None, None, "", ""), Seq(), loggedInUser)
          )
        )
      val result: Future[Result] = controller.bereavement(request)

      val expected = Seq(
        messages("pages.bereavement.transferor.point1"),
        messages("pages.bereavement.transferor.point2")
      ).toArray

      val parsed = Jsoup.parse(contentAsString(result)).getElementById("main-content")
      val current = parsed.getElementsByTag("li").eachText().toArray()

      current shouldBe expected
    }
  }

 "Divorce Enter Year Page" when {
   "divorceEnterYear" in {
     when(mockUpdateRelationshipService.getDivorceDate(any(), any()))
       .thenReturn(Future.successful(None))

     val result: Future[Result] = controller.divorceEnterYear(request)

     val expectedHeading = messages("pages.divorce.title")
     val expectedParas = Seq(
       messages("pages.divorce.paragraph1"),
       "Beta phase.banner.before phase.banner.link phase.banner.after"
     ).toArray

     val expectedLabel = Seq(
       messages("date.input.day"),
       messages("date.input.month"),
       messages("date.input.year")
     ).toArray

     val expectedHint = messages("pages.divorce.date.hint")

     val parsed = Jsoup.parse(contentAsString(result)).getElementById("main-content")

     val heading = parsed.getElementsByTag("h1").text
     val paras = parsed.getElementsByTag("p").eachText().toArray()
     val formLabel = parsed.getElementsByTag("label").eachText.toArray
     val formInput = parsed.getElementsByTag("input").eachAttr("type")
     val hint = parsed.getElementsByClass("govuk-hint").text

     heading shouldBe expectedHeading
     paras shouldBe expectedParas
     formLabel shouldBe expectedLabel
     formInput.size shouldBe 3
     formInput contains "text"
     hint shouldBe expectedHint
   }
 }

  "Divorce End Explanation Page" when {

    "Transferor and DivorceDate is in Current Year" in {

      val endingDates = MarriageAllowanceEndingDates(TaxYear.current.previous.finishes, TaxYear.current.starts)
      val date = TaxYear.current.starts

      when(mockUpdateRelationshipService.getDataForDivorceExplanation(any(), any()))
        .thenReturn(Future.successful((Transferor, date)))

      when(mockUpdateRelationshipService.getMAEndingDatesForDivorce(any(), any()))
        .thenReturn(endingDates)

      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(any())(any(), any()))
        .thenReturn(endingDates)

      val result = controller.divorceEndExplanation(request)
      val expectedHeading = messages("pages.divorce.explanation.title")
      val expectedParas = Seq(
        messages("pages.divorce.explanation.paragraph1", s"6 April ${date.getYear}"),
        messages("pages.divorce.explanation.paragraph2", messages("pages.divorce.explanation.current.taxYear")), "Beta phase.banner.before phase.banner.link phase.banner.after"
      ).toArray

      val expectedBullets = Seq(
        messages("pages.divorce.explanation.previous.bullet", s"5 April ${TaxYear.current.previous.finishYear}"),
        messages("pages.divorce.explanation.adjust.code.bullet")
      )

      val view = Jsoup.parse(contentAsString(result)).getElementById("main-content")

      val heading = view.getElementsByTag("h1").text()
      val paras = view.getElementsByTag("p").eachText().toArray
      val bullets = view.getElementsByTag("li").eachText().toArray

      heading shouldBe expectedHeading
      paras shouldBe expectedParas
      bullets shouldBe expectedBullets
    }

    "Transferor and DivorceDate is in PreviousYear" in {
      val endingDates = MarriageAllowanceEndingDates(LocalDate.of(2017, 4, 5), LocalDate.of(2017, 4, 6))
      val divorceDate  = LocalDate.of(2017, 4, 5)

      when(mockUpdateRelationshipService.getDataForDivorceExplanation(any(), any()))
        .thenReturn(Future.successful((Transferor, divorceDate)))

      when(mockUpdateRelationshipService.getMAEndingDatesForDivorce(any(), any()))
        .thenReturn(endingDates)

      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(any())(any(), any()))
        .thenReturn(endingDates)

      val result =  controller.divorceEndExplanation(request)

      val view = Jsoup.parse(contentAsString(result)).getElementById("main-content")
      val expectedHeading = messages("pages.divorce.explanation.title")
      val expectedParas = Seq(
        messages("pages.divorce.explanation.paragraph1", s"5 April ${divorceDate.getYear}"),
        messages("pages.divorce.explanation.paragraph2", messages("pages.divorce.explanation.previous.taxYear")), "Beta phase.banner.before phase.banner.link phase.banner.after"
      ).toArray

      val expectedBullets = Seq(
        messages("pages.divorce.explanation.previous.bullet", s"5 April ${LocalDate.of(2017, 4, 5).getYear}"),
        messages("pages.divorce.explanation.adjust.code.bullet")
      ).toArray

      val heading = view.getElementsByTag("h1").text
      val paras = view.getElementsByTag("p").eachText().toArray
      val bullets = view.getElementsByTag("li").eachText().toArray

      heading shouldBe expectedHeading
      paras shouldBe expectedParas
      bullets shouldBe expectedBullets
    }

    "Recipient and DivorceDate is in current year" in {
      val taxYear: TaxYear = config.currentTaxYear()

      val endingDates = MarriageAllowanceEndingDates(taxYear.finishes, taxYear.next.starts)
      val divorceDate  = taxYear.starts

      when(mockUpdateRelationshipService.getDataForDivorceExplanation(any(), any()))
        .thenReturn(Future.successful((Recipient, divorceDate)))

      when(mockUpdateRelationshipService.getMAEndingDatesForDivorce(any(), any()))
        .thenReturn(endingDates)

      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(any())(any(), any()))
        .thenReturn(endingDates)

      val result =  controller.divorceEndExplanation(request)

      val view = Jsoup.parse(contentAsString(result)).getElementById("main-content")
      val expectedHeading = messages("pages.divorce.explanation.title")
      val expectedParas = Seq(
        messages("pages.divorce.explanation.paragraph1", s"6 April ${divorceDate.getYear}"),
        messages("pages.divorce.explanation.paragraph2", messages("pages.divorce.explanation.current.taxYear")), "Beta phase.banner.before phase.banner.link phase.banner.after"
      ).toArray

      val expectedBullets = Seq(
        messages("pages.divorce.explanation.current.ma.bullet", s"5 April ${taxYear.finishYear}"),
        messages("pages.divorce.explanation.current.pa.bullet", s"6 April ${taxYear.next.startYear}")
      ).toArray

      val heading = view.getElementsByTag("h1").text
      val paras = view.getElementsByTag("p").eachText().toArray
      val bullets = view.getElementsByTag("li").eachText().toArray

      heading shouldBe expectedHeading
      paras shouldBe expectedParas
      bullets shouldBe expectedBullets
    }

    "Recipient and DivorceDate is in previous year" in {
      val (year, month, day) = (2017, 4, 5)
      val endingDates = MarriageAllowanceEndingDates(TaxYear.current.previous.finishes, TaxYear.current.starts)
      val divorceDate = LocalDate.of(year, month, day)

      when(mockUpdateRelationshipService.getDataForDivorceExplanation(any(), any()))
        .thenReturn(Future.successful((Recipient, divorceDate)))

      when(mockUpdateRelationshipService.getMAEndingDatesForDivorce(any(), any()))
        .thenReturn(endingDates)

      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(any())(any(), any()))
        .thenReturn(endingDates)

      val result =  controller.divorceEndExplanation(request)

      val view = Jsoup.parse(contentAsString(result)).getElementById("main-content")
      val expectedHeading = messages("pages.divorce.explanation.title")
      val expectedParas = Seq(
        messages("pages.divorce.explanation.paragraph1", s"5 April ${divorceDate.getYear}"),
        messages("pages.divorce.explanation.paragraph2", messages("pages.divorce.explanation.previous.taxYear")), "Beta phase.banner.before phase.banner.link phase.banner.after"
      ).toArray

      val expectedBullets = Seq(
        messages("pages.divorce.explanation.previous.bullet", s"5 April ${TaxYear.current.previous.finishYear}"),
        messages("pages.divorce.explanation.adjust.code.bullet")
      ).toArray

      val heading = view.getElementsByTag("h1").text
      val paras = view.getElementsByTag("p").eachText().toArray
      val bullets = view.getElementsByTag("li").eachText().toArray

      heading shouldBe expectedHeading
      paras shouldBe expectedParas
      bullets shouldBe expectedBullets
    }
  }

  "Confirm Email" in {

    when(mockUpdateRelationshipService.getEmailAddress(any(), any()))
      .thenReturn(Future.successful(None))

    val expectedHeading = messages("pages.form.field.your-confirmation")
    val expectedParas = Seq(
      messages("change.status.confirm.info"),
      messages("change.status.confirm.more.info"),
      "Beta phase.banner.before phase.banner.link phase.banner.after"
    ).toArray
    val expectedLabel = messages("pages.form.field.transferor-email")

    val result = controller.confirmEmail(FakeRequest().withHeaders(("Referer", "referer")))

    val view = Jsoup.parse(contentAsString(result)).getElementById("main-content")

    val heading = view.getElementsByTag("h1").text
    val paras = view.getElementsByTag("p").eachText().toArray
    val formLabel = view.getElementsByTag("label").text()
    val formInput = view.getElementsByTag("input").eachAttr("type")

    heading shouldBe expectedHeading
    paras shouldBe expectedParas
    formLabel shouldBe expectedLabel
    formInput.size shouldBe 1
    formInput contains "input"
  }

  "Confirmation Update Page" when {
    "End reason divorce display divorce date row" in {

      when(mockUpdateRelationshipService.getConfirmationUpdateAnswers(any(), any()))
        .thenReturn(Future.successful(
          ConfirmationUpdateAnswers(loggedInUser, Some(LocalDate.now()), "email@email.com", MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts))))

      val expectedHeader = messages("pages.confirm.cancel.heading")
      val expectedParas = Seq(messages("pages.confirm.cancel.message"), "Beta phase.banner.before phase.banner.link phase.banner.after")
      val expectedList = Seq(
        messages("pages.confirm.cancel.message1", s"5 April ${TaxYear.current.finishYear}"),
        messages("pages.confirm.cancel.message2", s"6 April ${TaxYear.current.next.startYear}")
      ).toArray
      val expectedTableHeadings = Seq(
        messages("pages.confirm.cancel.your-name"),
        messages("pages.divorce.title"),
        messages("pages.confirm.cancel.email")
      ).toArray

      val result = controller.confirmUpdate(request)

      val view = Jsoup.parse(contentAsString(result)).getElementById("main-content")

      val header = view.getElementsByTag("h1").text
      val para = view.getElementsByTag("p").eachText.toArray
      val list = view.getElementsByTag("li").eachText.toArray
      val tableHeadings = view.getElementsByTag("dt").eachText.toArray

      header shouldBe expectedHeader
      para shouldBe expectedParas
      list shouldBe expectedList
      tableHeadings.size shouldBe 3
      tableHeadings shouldBe expectedTableHeadings
    }

    "display two rows when no DivorceDate is present" in {
      when(mockUpdateRelationshipService.getConfirmationUpdateAnswers(any(), any()))
        .thenReturn(Future.successful(
          ConfirmationUpdateAnswers(loggedInUser, None, "email@email.com", MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts))))

      val expectedHeader = messages("pages.confirm.cancel.heading")
      val expectedParas = Seq(messages("pages.confirm.cancel.message"), "Beta phase.banner.before phase.banner.link phase.banner.after")
      val expectedList = Seq(
        messages("pages.confirm.cancel.message1", s"5 April ${TaxYear.current.finishYear}"),
        messages("pages.confirm.cancel.message2", s"6 April ${TaxYear.current.next.startYear}")
      ).toArray
      val expectedTableHeadings = Seq(
        messages("pages.confirm.cancel.your-name"),
        messages("pages.confirm.cancel.email")
      ).toArray

      val result = controller.confirmUpdate(request)

      val view = Jsoup.parse(contentAsString(result)).getElementById("main-content")

      val header = view.getElementsByTag("h1").text
      val para = view.getElementsByTag("p").eachText.toArray
      val list = view.getElementsByTag("li").eachText.toArray
      val tableHeadings = view.getElementsByTag("dt").eachText.toArray

      header shouldBe expectedHeader
      para shouldBe expectedParas
      list shouldBe expectedList
      tableHeadings.size shouldBe 2
      tableHeadings shouldBe expectedTableHeadings
    }
  }

  "Finish Update page" should {

    "display the correct content" in {

      when(mockUpdateRelationshipService.getEmailAddressForConfirmation(any(), any()))
        .thenReturn(Future.successful(EmailAddress("email@email.com")))

      when(mockUpdateRelationshipService.removeCache(any(), any()))
        .thenReturn(mock[HttpResponse])

      val expectedHeading = messages("pages.coc.finish.header")

      val expectedParas = Seq(
        messages("pages.coc.finish.acknowledgement", "email@email.com"),
        messages("pages.coc.finish.junk"),
        messages("pages.coc.finish.para1"), "Beta phase.banner.before phase.banner.link phase.banner.after"
      ).toArray

      val result = controller.finishUpdate(request)

      val view = Jsoup.parse(contentAsString(result)).getElementById("main-content")

      val heading = view.getElementsByTag("h1").text
      val paras = view.getElementsByTag("p").eachText.toArray

      heading shouldBe expectedHeading
      paras shouldBe expectedParas

    }
  }
}
