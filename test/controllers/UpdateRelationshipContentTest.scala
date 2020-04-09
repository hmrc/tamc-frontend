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
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time.TaxYear
import utils.ControllerBaseTest
import views.helpers.TextGenerator

import scala.concurrent.Future

class UpdateRelationshipContentTest extends ControllerBaseTest {

  val mockRegistrationService: TransferService = mock[TransferService]
  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]
  val mockCachingService: CachingService = mock[CachingService]
  val mockTimeService: TimeService = mock[TimeService]
  val loggedInUser = LoggedInUserInfo(1, "20130101",None, Some(CitizenName(Some("Test"), Some("User"))))

  def controller(updateRelationshipService: UpdateRelationshipService = mockUpdateRelationshipService): UpdateRelationshipController =
    new UpdateRelationshipController(
      messagesApi,
      instanceOf[AuthenticatedActionRefiner],
      updateRelationshipService,
      mockTimeService
    )(instanceOf[TemplateRenderer], instanceOf[FormPartialRetriever])

  def getContactHMRCText(testCase: String): String = {
    testCase match  {
      case "changeOfIncome" =>
        (messagesApi("general.helpline.enquiries.link.pretext") + " "
          + messagesApi("general.helpline.enquiries.link") + " "
          + messagesApi("pages.changeOfIncome.enquiries.link.paragraph"))
      case "bereavement" =>
        (messagesApi("general.helpline.enquiries.link.pretext") + " "
          + messagesApi("general.helpline.enquiries.link") + " "
          + messagesApi("pages.bereavement.enquiries.link.paragraph"))
      case _ => throw new RuntimeException("asd")
    }
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

    when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(any())(any(), any()))
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
            RelationshipRecords(RelationshipRecord(Recipient.value, "", "", None, None, "", ""), Seq(), loggedInUser)
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
            RelationshipRecords(RelationshipRecord(Transferor.value, "", "", None, None, "", ""), Seq(), loggedInUser)
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
            RelationshipRecords(RelationshipRecord(Recipient.value, "", "", None, None, "", ""), Seq(), loggedInUser)
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
            RelationshipRecords(RelationshipRecord(Transferor.value, "", "", None, None, "", ""), Seq(), loggedInUser)
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

     val expectedHeading = messagesApi("pages.divorce.title")
     val expectedParas = Seq(
       messagesApi("pages.divorce.paragraph1"),
       messagesApi("pages.divorce.date.hint")
     ).toArray

     val expectedLabel = Seq(
       "Day",
       "Month",
       "Year"
     ).toArray

     val parsed = Jsoup.parse(contentAsString(result))

     val heading = parsed.getElementsByTag("h1").text
     val paras = parsed.getElementsByTag("p").eachText().toArray()
     val formLabel = parsed.getElementsByTag("label").eachText.toArray
     val formInput = parsed.getElementsByTag("input").eachAttr("type")

     heading shouldBe expectedHeading
     paras shouldBe expectedParas
     formLabel shouldBe expectedLabel
     formInput.size shouldBe 3
     formInput contains "text"
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

      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(any())(any(), any()))
        .thenReturn(endingDates)

      val result = controller().divorceEndExplanation(request)
      val expectedHeading = messagesApi("pages.divorce.explanation.title")
      val expectedParas = Seq(
        messagesApi("pages.divorce.explanation.paragraph1", s"6 April ${date.getYear}"),
        messagesApi("pages.divorce.explanation.paragraph2", messagesApi("pages.divorce.explanation.current.taxYear"))
      ).toArray

      val expectedBullets = Seq(
        messagesApi("pages.divorce.explanation.previous.bullet", s"5 April ${TaxYear.current.previous.finishYear}"),
        messagesApi("pages.divorce.explanation.adjust.code.bullet")
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

      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(any())(any(), any()))
        .thenReturn(endingDates)

      val result =  controller().divorceEndExplanation(request)

      val view = Jsoup.parse(contentAsString(result))
      val expectedHeading = messagesApi("pages.divorce.explanation.title")
      val expectedParas = Seq(
        messagesApi("pages.divorce.explanation.paragraph1", s"5 April ${divorceDate.getYear}"),
        messagesApi("pages.divorce.explanation.paragraph2", messagesApi("pages.divorce.explanation.previous.taxYear"))
      ).toArray

      val expectedBullets = Seq(
        messagesApi("pages.divorce.explanation.previous.bullet", s"5 April ${new LocalDate(2017, 4, 5).getYear}"),
        messagesApi("pages.divorce.explanation.adjust.code.bullet")
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

      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(any())(any(), any()))
        .thenReturn(endingDates)

      val result =  controller().divorceEndExplanation(request)

      val view = Jsoup.parse(contentAsString(result))
      val expectedHeading = messagesApi("pages.divorce.explanation.title")
      val expectedParas = Seq(
        messagesApi("pages.divorce.explanation.paragraph1", s"6 April ${divorceDate.getYear}"),
        messagesApi("pages.divorce.explanation.paragraph2", messagesApi("pages.divorce.explanation.current.taxYear"))
      ).toArray

      val expectedBullets = Seq(
        messagesApi("pages.divorce.explanation.current.ma.bullet", s"5 April ${TaxYear.current.finishYear}"),
        messagesApi("pages.divorce.explanation.current.pa.bullet", s"6 April ${TaxYear.current.next.startYear}")
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

      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(any())(any(), any()))
        .thenReturn(endingDates)

      val result =  controller().divorceEndExplanation(request)

      val view = Jsoup.parse(contentAsString(result))
      val expectedHeading = messagesApi("pages.divorce.explanation.title")
      val expectedParas = Seq(
        messagesApi("pages.divorce.explanation.paragraph1", s"5 April ${divorceDate.getYear}"),
        messagesApi("pages.divorce.explanation.paragraph2", messagesApi("pages.divorce.explanation.previous.taxYear"))
      ).toArray

      val expectedBullets = Seq(
        messagesApi("pages.divorce.explanation.previous.bullet", s"5 April ${TaxYear.current.previous.finishYear}"),
        messagesApi("pages.divorce.explanation.adjust.code.bullet")
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

    val expectedHeading = messagesApi("pages.form.field.your-confirmation")
    val expectedParas = Seq(
      messagesApi("change.status.confirm.info"),
      messagesApi("change.status.confirm.more.info")
    ).toArray
    val expectedLabel = messagesApi("pages.form.field.transferor-email")

    val result = controller( ).confirmEmail(FakeRequest().withHeaders(("Referer", "referer")))

    val view = Jsoup.parse(contentAsString(result))

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

      val expectedHeader = messagesApi("pages.confirm.cancel.heading")
      val expectedPara = messagesApi("pages.confirm.cancel.message")
      val expectedList = Seq(
        messagesApi("pages.confirm.cancel.message1", s"5 April ${TaxYear.current.finishYear}"),
        messagesApi("pages.confirm.cancel.message2", s"6 April ${TaxYear.current.next.startYear}")
      ).toArray
      val expectedTableHeadings = Seq(
        messagesApi("pages.confirm.cancel.your-name"),
        messagesApi("pages.divorce.title"),
        messagesApi("pages.confirm.cancel.email")
      ).toArray

      val result = controller().confirmUpdate(request)

      val view = Jsoup.parse(contentAsString(result))

      val header = view.getElementsByTag("h1").text
      val para = view.getElementsByTag("p").text
      val list = view.getElementsByTag("li").eachText.toArray
      val tableHeadings = view.getElementsByTag("th").eachText.toArray

      header shouldBe expectedHeader
      para shouldBe expectedPara
      list shouldBe expectedList
      tableHeadings.size shouldBe 3
      tableHeadings shouldBe expectedTableHeadings
    }

    "display two rows when no DivorceDate is present" in {
      when(mockUpdateRelationshipService.getConfirmationUpdateAnswers(any(), any()))
        .thenReturn(Future.successful(
          ConfirmationUpdateAnswers(loggedInUser, None, "email@email.com", MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts))))

      val expectedHeader = messagesApi("pages.confirm.cancel.heading")
      val expectedPara = messagesApi("pages.confirm.cancel.message")
      val expectedList = Seq(
        messagesApi("pages.confirm.cancel.message1", s"5 April ${TaxYear.current.finishYear}"),
        messagesApi("pages.confirm.cancel.message2", s"6 April ${TaxYear.current.next.startYear}")
      ).toArray
      val expectedTableHeadings = Seq(
        messagesApi("pages.confirm.cancel.your-name"),
        messagesApi("pages.confirm.cancel.email")
      ).toArray

      val result = controller().confirmUpdate(request)

      val view = Jsoup.parse(contentAsString(result))

      val header = view.getElementsByTag("h1").text
      val para = view.getElementsByTag("p").text
      val list = view.getElementsByTag("li").eachText.toArray
      val tableHeadings = view.getElementsByTag("th").eachText.toArray

      header shouldBe expectedHeader
      para shouldBe expectedPara
      list shouldBe expectedList
      tableHeadings.size shouldBe 2
      tableHeadings shouldBe expectedTableHeadings
    }
  }

  "Finish Update page" should {

    "display the corrrect content" in {

      when(mockUpdateRelationshipService.getEmailAddressForConfirmation(any(), any()))
        .thenReturn(Future.successful("email@email.com"))

      when(mockUpdateRelationshipService.removeCache(any(), any()))
        .thenReturn(mock[HttpResponse])

      val expectedHeading = messagesApi("pages.coc.finish.header")

      val expectedParas = Seq(
        messagesApi("pages.coc.finish.acknowledgement", "email@email.com"),
        messagesApi("pages.coc.finish.junk"),
        messagesApi("pages.coc.finish.para1")
      ).toArray

      val result = controller().finishUpdate(request)

      val view = Jsoup.parse(contentAsString(result))

      val heading = view.getElementsByTag("h1").text
      val paras = view.getElementsByTag("p").eachText.toArray

      heading shouldBe expectedHeading
      paras shouldBe expectedParas

    }
  }
}
