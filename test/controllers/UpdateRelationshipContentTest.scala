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

package controllers

import controllers.actions.AuthenticatedActionRefiner
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.time.TaxYear
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import viewModels.EmailViewModel
import views.helpers.TextGenerator

import scala.concurrent.Future

//TODO remove this class
class UpdateRelationshipContentTest extends ControllerBaseSpec {

  val mockRegistrationService: TransferService = mock[TransferService]
  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]
  val mockCachingService: CachingService = mock[CachingService]
  val mockTimeService: TimeService = mock[TimeService]
  val mockListRelationshipService: ListRelationshipService = mock[ListRelationshipService]

  def controller(updateRelationshipService: UpdateRelationshipService = mockUpdateRelationshipService): UpdateRelationshipController =
    new UpdateRelationshipController(
      messagesApi,
      instanceOf[AuthenticatedActionRefiner],
      updateRelationshipService,
      mockTimeService
    )(instanceOf[TemplateRenderer], instanceOf[FormPartialRetriever])

  private def transformDate(date: LocalDate, isWelsh: Boolean = false): String = {
    TextGenerator().ukDateTransformer(date)
  }

  "Update relationship cause - get view" should {
    "show all appropriate radio buttons" in {
      val expectedRadioButtons = Seq(
        messagesApi("pages.makeChanges.radio.divorce"),
        messagesApi("pages.makeChanges.radio.incomeChanges"),
        messagesApi("pages.makeChanges.radio.noLongerRequired"),
        messagesApi("pages.makeChanges.radio.bereavement")
      ).toArray

      val request = FakeRequest()
      when(mockUpdateRelationshipService.getMakeChangesDecision(any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller().makeChange()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val radioButtons = document.getElementsByClass("multiple-choice").eachText().toArray
      radioButtons.length shouldBe expectedRadioButtons.length
      radioButtons shouldBe expectedRadioButtons
    }
  }

  "Stop Allowance Page" in {
    val result: Future[Result] = controller().stopAllowance(request)

    val expected = Seq(
      messagesApi("pages.stopAllowance.paragraph1"),
      messagesApi("pages.stopAllowance.paragraph2")
    ).toArray
    val parsed = Jsoup.parse(contentAsString(result))
    val current = parsed.getElementsByTag("p").eachText().toArray()

    current shouldBe expected
  }

  "Cancel Page" in {
    val maEndingDates = MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts)

    when(mockUpdateRelationshipService.getMAEndingDatesForCancelation)
      .thenReturn(maEndingDates)

    when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(any())(any()))
      .thenReturn(Future.successful(maEndingDates))

    val result: Future[Result] = controller().cancel(request)

    val currentEndDate =
      TextGenerator().ukDateTransformer(TaxYear.current.finishes)
    val nextStartDate =
      TextGenerator().ukDateTransformer(TaxYear.current.next.starts)

    val expected = Seq(
      s"We will cancel your Marriage Allowance, but it will remain in place until 5 April ${TaxYear.current.finishYear}, the end of the current tax year.",
      s"Your Personal Allowance will not include any Marriage Allowance from 6 April ${TaxYear.current.next.startYear}" +
        s", the start of the new tax year. Your partner will not have to pay back any tax."
    ).toArray
    val parsed = Jsoup.parse(contentAsString(result))
    val current = parsed.getElementsByTag("p").eachText().toArray()

    current shouldBe expected
  }

  "changeOfIncome(text)" in {
    val result: Future[Result] = controller().changeOfIncome(request)

    val expected = Seq(
      getContactHMRCText("changeOfIncome"),
      messagesApi("pages.changeOfIncome.paragraph2")
    ).toArray
    val parsed = Jsoup.parse(contentAsString(result))
    val current = parsed.getElementsByTag("p").eachText().toArray()

    current shouldBe expected
  }

  "changeOfIncome(bullet list)" in {
    val result: Future[Result] = controller().changeOfIncome(request)

    val expected = Seq(
      messagesApi("pages.changeOfIncome.bullet1"),
      messagesApi("pages.changeOfIncome.bullet2")
    ).toArray
    val parsed = Jsoup.parse(contentAsString(result))
    val current = parsed.getElementsByTag("li").eachText().toArray()

    current shouldBe expected
  }

  "Bereavement page" when {
    "recipient text" in {
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(RelationshipRecord(Recipient.asString(), "", "", None, None, "", ""), Seq(), loggedInUser)
          )
        )
      val result: Future[Result] = controller().bereavement(request)

      val expected = Seq(
        getContactHMRCText("bereavement"),
        messagesApi("pages.bereavement.recipient.paragraph")
      ).toArray
      val parsed = Jsoup.parse(contentAsString(result))
      val current = parsed.getElementsByTag("p").eachText().toArray()

      current shouldBe expected
    }

    "transferor text" in {
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(RelationshipRecord(Transferor.asString(), "", "", None, None, "", ""), Seq(), loggedInUser)
          )
        )
      val result: Future[Result] = controller().bereavement(request)

      val expected = Seq(
        getContactHMRCText("bereavement"),
        messagesApi("pages.bereavement.transferor.paragraph")
      ).toArray
      val parsed = Jsoup.parse(contentAsString(result))
      val current = parsed.getElementsByTag("p").eachText().toArray()

      current shouldBe expected
    }

    "recipient bullet list" in {
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(RelationshipRecord(Recipient.asString(), "", "", None, None, "", ""), Seq(), loggedInUser)
          )
        )
      val result: Future[Result] = controller().bereavement(request)

      val expected = Array()
      val parsed = Jsoup.parse(contentAsString(result))
      val current = parsed.getElementsByTag("li").eachText().toArray()

      current shouldBe expected
    }

    "transferor bullet list" in {
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(RelationshipRecord(Transferor.asString(), "", "", None, None, "", ""), Seq(), loggedInUser)
          )
        )
      val result: Future[Result] = controller().bereavement(request)
      val endOfYear = TextGenerator().ukDateTransformer(uk.gov.hmrc.time.TaxYear.current.finishes)

      val expected = Seq(
        messagesApi("pages.bereavement.transferor.point1"),
        messagesApi("pages.bereavement.transferor.point2", endOfYear)
      ).toArray
      val parsed = Jsoup.parse(contentAsString(result))
      val current = parsed.getElementsByTag("li").eachText().toArray()

      current shouldBe expected
    }
  }

 "Divorce Enter Year Page" when {
   "divorceEnterYear" in {
     when(mockUpdateRelationshipService.getDivorceDate(any(), any()))
       .thenReturn(Future.successful(None))

     val result: Future[Result] = controller().divorceEnterYear(request)

     val expected = Seq(
       messagesApi("pages.divorce.paragraph1"),
       messagesApi("pages.divorce.date.hint")
     ).toArray
     val parsed = Jsoup.parse(contentAsString(result))
     val current = parsed.getElementsByTag("p").eachText().toArray()

     current shouldBe expected
   }
 }

  "Divorce End Explanation Page" when {

    "Transferor and DivorceDate is in Current Year" in {

      val endingDates = MarriageAllowanceEndingDates(TaxYear.current.previous.finishes, TaxYear.current.starts)
      val date = TaxYear.current.starts

      when(mockUpdateRelationshipService.getDataForDivorceExplanation(any(), any()))
        .thenReturn(Future.successful(Transferor, date))

      when(mockUpdateRelationshipService.getMAEndingDatesForDivorce(any(), any()))
        .thenReturn(endingDates)

      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(any())(any()))
        .thenReturn(endingDates)

      val result = controller().divorceEndExplanation(request)
      val expectedHeading = messagesApi("pages.divorce.explanation.title")
      val expectedParas = Seq(
        messagesApi("pages.divorce.explanation.paragraph1", s"6 April ${date.getYear}"),
        messagesApi("pages.divorce.explanation.paragraph2", messagesApi("pages.divorce.explanation.current.taxYear"))
      ).toArray

      val expectedBullets = Seq(
        messagesApi("pages.divorce.explanation.current.bullet1", s"5 April ${TaxYear.current.previous.finishYear}"),
        messagesApi("pages.divorce.explanation.current.bullet2", s"6 April ${TaxYear.current.startYear}")
      )

      val view = Jsoup.parse(contentAsString(result))

      val heading = view.getElementsByTag("h1").text()
      val paras = view.getElementsByTag("p").eachText().toArray
      val bullets = view.getElementsByTag("li").eachText().toArray

      heading shouldBe expectedHeading
      paras shouldBe expectedParas
      bullets shouldBe expectedBullets
    }

    "Transferor and DivorceDate is in PreviousYear" in {
      val endingDates = MarriageAllowanceEndingDates(new LocalDate(2017, 4, 5), new LocalDate(2017, 4, 6))
      val divorceDate  = new LocalDate(2017, 4, 5)

      when(mockUpdateRelationshipService.getDataForDivorceExplanation(any(), any()))
        .thenReturn(Future.successful(Transferor, divorceDate))

      when(mockUpdateRelationshipService.getMAEndingDatesForDivorce(any(), any()))
        .thenReturn(endingDates)

      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(any())(any()))
        .thenReturn(endingDates)

      val result =  controller().divorceEndExplanation(request)

      val view = Jsoup.parse(contentAsString(result))
      val expectedHeading = messagesApi("pages.divorce.explanation.title")
      val expectedParas = Seq(
        messagesApi("pages.divorce.explanation.paragraph1", s"5 April ${divorceDate.getYear}"),
        messagesApi("pages.divorce.explanation.paragraph2", messagesApi("pages.divorce.explanation.previous.taxYear"))
      ).toArray

      val expectedBullets = Seq(
        messagesApi("pages.divorce.explanation.previous.bullet1", s"5 April ${new LocalDate(2017, 4, 5).getYear}"),
        messagesApi("pages.divorce.explanation.previous.bullet2")
      ).toArray

      val heading = view.getElementsByTag("h1").text
      val paras = view.getElementsByTag("p").eachText().toArray
      val bullets = view.getElementsByTag("li").eachText().toArray

      heading shouldBe expectedHeading
      paras shouldBe expectedParas
      bullets shouldBe expectedBullets
    }

    "Recipient and DivorceDate is in current year" in {
      val endingDates = MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts)
      val divorceDate  = TaxYear.current.starts

      when(mockUpdateRelationshipService.getDataForDivorceExplanation(any(), any()))
        .thenReturn(Future.successful(Recipient, divorceDate))

      when(mockUpdateRelationshipService.getMAEndingDatesForDivorce(any(), any()))
        .thenReturn(endingDates)

      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(any())(any()))
        .thenReturn(endingDates)

      val result =  controller().divorceEndExplanation(request)

      val view = Jsoup.parse(contentAsString(result))
      val expectedHeading = messagesApi("pages.divorce.explanation.title")
      val expectedParas = Seq(
        messagesApi("pages.divorce.explanation.paragraph1", s"6 April ${divorceDate.getYear}"),
        messagesApi("pages.divorce.explanation.paragraph2", messagesApi("pages.divorce.explanation.current.taxYear"))
      ).toArray

      val expectedBullets = Seq(
        messagesApi("pages.divorce.explanation.current.bullet1", s"5 April ${TaxYear.current.finishYear}"),
        messagesApi("pages.divorce.explanation.current.bullet2", s"6 April ${TaxYear.current.next.startYear}")
      ).toArray

      val heading = view.getElementsByTag("h1").text
      val paras = view.getElementsByTag("p").eachText().toArray
      val bullets = view.getElementsByTag("li").eachText().toArray

      heading shouldBe expectedHeading
      paras shouldBe expectedParas
      bullets shouldBe expectedBullets
    }

    "Recipient and DivorceDate is in previous year" in{
      val endingDates = MarriageAllowanceEndingDates(TaxYear.current.previous.finishes, TaxYear.current.starts)
      val divorceDate = new LocalDate(2017, 4, 5)

      when(mockUpdateRelationshipService.getDataForDivorceExplanation(any(), any()))
        .thenReturn(Future.successful(Recipient, divorceDate))

      when(mockUpdateRelationshipService.getMAEndingDatesForDivorce(any(), any()))
        .thenReturn(endingDates)

      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(any())(any()))
        .thenReturn(endingDates)

      val result =  controller().divorceEndExplanation(request)

      val view = Jsoup.parse(contentAsString(result))
      val expectedHeading = messagesApi("pages.divorce.explanation.title")
      val expectedParas = Seq(
        messagesApi("pages.divorce.explanation.paragraph1", s"5 April ${divorceDate.getYear}"),
        messagesApi("pages.divorce.explanation.paragraph2", messagesApi("pages.divorce.explanation.previous.taxYear"))
      ).toArray

      val expectedBullets = Seq(
        messagesApi("pages.divorce.explanation.previous.bullet1", s"5 April ${TaxYear.current.previous.finishYear}"),
        messagesApi("pages.divorce.explanation.previous.bullet2")
      ).toArray

      val heading = view.getElementsByTag("h1").text
      val paras = view.getElementsByTag("p").eachText().toArray
      val bullets = view.getElementsByTag("li").eachText().toArray

      heading shouldBe expectedHeading
      paras shouldBe expectedParas
      bullets shouldBe expectedBullets
    }

    //TODO Put into Ticket to break these out in to their own view specs
  }

  "Confirm Email" in {

    when(mockUpdateRelationshipService.getEmailAddress(any(), any()))
      .thenReturn(Future.successful(None))

    val expectedHeading = messagesApi("pages.form.field.your-confirmation")
    val expectedParas = Seq(
      messagesApi("change.status.confirm.info"),
      messagesApi("change.status.confirm.more.info")
    ).toArray

    val result = controller( ).confirmEmail(FakeRequest().withHeaders(("Referer", "referer")))


    val view = Jsoup.parse(contentAsString(result))

    val heading = view.getElementsByTag("h1").text
    val paras = view.getElementsByTag("p").eachText().toArray

    heading shouldBe expectedHeading
    paras shouldBe expectedParas
  }

  //TODO remove with updateRelationshipAction()???
  //  "Update relationship confirmation page" should {
  //
  //    "confirm cancellation " in {
  //      when(mockUpdateRelationshipService.saveEndRelationshipReason(ArgumentMatchers.eq(EndRelationshipReason(EndReasonCode.CANCEL)))(any(), any()))
  //        .thenReturn(EndRelationshipReason(EndReasonCode.CANCEL))
  //      when(mockTimeService.getEffectiveUntilDate(EndRelationshipReason(EndReasonCode.CANCEL)))
  //        .thenReturn(Some(time.TaxYear.current.finishes))
  //      when(mockTimeService.getEffectiveDate(EndRelationshipReason(EndReasonCode.CANCEL)))
  //        .thenReturn(time.TaxYear.current.next.starts)
  //      val result = controller().confirmCancel()(request)
  //      status(result) shouldBe OK
  //
  //      val document = Jsoup.parse(contentAsString(result))
  //      val cancelHeading = document.getElementById("cancel-heading")
  //      val cancelContent = document.getElementById("cancel-content")
  //
  //      cancelHeading shouldNot be(null)
  //      cancelContent shouldNot be(null)
  //      val taxYear = time.TaxYear.current.startYear + 1
  //      cancelHeading.toString should include("Cancelling Marriage Allowance")
  //      cancelContent.text() shouldBe s"We will cancel your Marriage Allowance, but it will remain in place until 5 April $taxYear, the end of the current tax year."
  //    }
  //  }
}
