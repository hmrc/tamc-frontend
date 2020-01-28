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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import services._
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time
import utils.Constants.forms.coc._
import views.helpers.TextGenerators

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
      mockRegistrationService,
      mockCachingService,
      mockTimeService
    )(instanceOf[TemplateRenderer], instanceOf[FormPartialRetriever])

  //TODO remove??
  //  "list relationship page" should {
  //
  //    "display 'Cancel Marriage Allowance' button" in {
  //      when(mockListRelationshipService.listRelationship(any())(any(), any()))
  //        .thenReturn(
  //          Future.successful((RelationshipRecordData.activeRelationshipRecordList, false))
  //        )
  //      val result = controller.history()(request)
  //
  //      status(result) shouldBe OK
  //      val document = Jsoup.parse(contentAsString(result))
  //      document.getElementById("cancel-marriage-allowance").text() shouldBe "Cancel Marriage Allowance"
  //    }
  //
  //    "display only active relationship details" in {
  //      when(mockListRelationshipService.listRelationship(any())(any(), any()))
  //        .thenReturn(
  //          Future.successful((RelationshipRecordData.activeRelationshipRecordList, false))
  //        )
  //      val result = controller.history()(request)
  //
  //      status(result) shouldBe OK
  //      val document = Jsoup.parse(contentAsString(result))
  //      val activeRecord = document.getElementById("activeRecord")
  //      activeRecord shouldNot be(null)
  //
  //      val historicRecord = document.getElementById("historicRecord")
  //      historicRecord should be(null)
  //    }
  //
  //    "display only historic relationship details and link to how-it-works" in {
  //      when(mockListRelationshipService.listRelationship(any())(any(), any()))
  //        .thenReturn(
  //          Future.successful((RelationshipRecordData.historicRelationshipRecordList, false))
  //        )
  //      val result = controller.history()(request)
  //
  //      status(result) shouldBe OK
  //
  //      val document = Jsoup.parse(contentAsString(result))
  //      val activeRecord = document.getElementById("activeRecord")
  //      activeRecord should be(null)
  //
  //      val start = document.getElementById("start-now")
  //      start shouldNot be(null)
  //      start.attr("href") shouldBe controllers.routes.EligibilityController.howItWorks().url
  //
  //      val historicRecord = document.getElementById("historicRecords")
  //      historicRecord shouldNot be(null)
  //
  //      document.getElementById("line0-start").text shouldBe "2011 to 2013"
  //      document.getElementById("line0-reason").text shouldBe "Bereavement"
  //      document.getElementById("line0-remove") shouldBe null
  //    }
  //
  //    "display reject button when it should be displayed" in {
  //      when(mockListRelationshipService.listRelationship(any())(any(), any()))
  //        .thenReturn(
  //          Future.successful((RelationshipRecordData.multiHistoricRelRecordList, false))
  //        )
  //
  //      val result = controller.history()(request)
  //
  //      status(result) shouldBe OK
  //
  //      val document = Jsoup.parse(contentAsString(result))
  //      val activeRecord = document.getElementById("activeRecord")
  //      activeRecord should be(null)
  //
  //      val start = document.getElementById("start-now")
  //      start shouldNot be(null)
  //      start.attr("href") shouldBe controllers.routes.EligibilityController.howItWorks().url
  //
  //      val historicRecord = document.getElementById("historicRecords")
  //      historicRecord shouldNot be(null)
  //
  //      document.getElementById("line0-start").text shouldBe "2011 to 2013"
  //      document.getElementById("line0-reason").text shouldBe "Divorce or end of civil partnership"
  //      document.getElementById("line0-remove") shouldBe null
  //
  //      document.getElementById("line1-start").text shouldBe "2001 to 2013"
  //      document.getElementById("line1-reason").text shouldBe "Divorce or end of civil partnership"
  //      document.getElementById("line1-remove") shouldNot be(null)
  //    }
  //
  //    "display ’apply for previous years’ button if historic year is available" in {
  //      when(mockListRelationshipService.listRelationship(any())(any(), any()))
  //        .thenReturn(
  //          Future.successful((RelationshipRecordData.activeRelationshipRecordList, true))
  //        )
  //      val result = controller.history()(request)
  //
  //      status(result) shouldBe OK
  //
  //      val document = Jsoup.parse(contentAsString(result))
  //      val prevYearsButton = document.getElementById("previousYearsApply")
  //      prevYearsButton shouldNot be(null)
  //    }
  //
  //    "don't display apply for previous years button when previous years are available" in {
  //      when(mockListRelationshipService.listRelationship(any())(any(), any()))
  //        .thenReturn(
  //          Future.successful((RelationshipRecordData.activeRelationshipRecordList, false))
  //        )
  //      val result = controller.history()(request)
  //
  //      status(result) shouldBe OK
  //      val document = Jsoup.parse(contentAsString(result))
  //
  //      val prevYearsButton = document.getElementById("previousYearsApply")
  //      prevYearsButton should be(null)
  //    }
  //
  //    "display active and historic relationship details " in {
  //      when(mockListRelationshipService.listRelationship(any())(any(), any()))
  //        .thenReturn(
  //          Future.successful((RelationshipRecordData.bothRelationshipRecordList, false))
  //        )
  //      val result = controller.history()(request)
  //
  //      status(result) shouldBe OK
  //      val document = Jsoup.parse(contentAsString(result))
  //
  //      val activeRecord = document.getElementById("activeRecord")
  //      activeRecord shouldNot be(null)
  //
  //      val historicRecord = document.getElementById("historicRecords")
  //      historicRecord shouldNot be(null)
  //
  //      document.getElementById("active").text shouldBe "2012 to Present"
  //      historicRecord.toString should include("2011 to 2013")
  //    }
  //
  //    "display historical active relationship details" in {
  //      when(mockListRelationshipService.listRelationship(any())(any(), any()))
  //        .thenReturn(
  //          Future.successful((RelationshipRecordData.activeHistoricRelRecordList, false))
  //        )
  //      val result = controller.history()(request)
  //
  //      status(result) shouldBe OK
  //      val contentAsStringFromResult = contentAsString(result)
  //      val document = Jsoup.parse(contentAsString(result))
  //      val historicActiveMessage = document.getElementById("historicActiveMessage").text()
  //      val nextTaxYear = time.TaxYear.current.startYear + 1
  //      historicActiveMessage should be(s"You will stop receiving Marriage Allowance from your partner at end of the tax year (5 April $nextTaxYear).")
  //
  //      val historicRecord = document.getElementById("historicRecords")
  //      historicRecord shouldNot be(null)
  //    }
  //
  //    "display bereavement and change of income related details" in {
  //      when(mockListRelationshipService.listRelationship(any())(any(), any()))
  //        .thenReturn(
  //          Future.successful((RelationshipRecordData.bothRelationshipRecordList, false))
  //        )
  //      val result = controller.history()(request)
  //
  //      status(result) shouldBe OK
  //      val document = Jsoup.parse(contentAsString(result))
  //
  //      val activeRecord = document.getElementById("activeRecord")
  //      activeRecord shouldNot be(null)
  //
  //      val historicRecord = document.getElementById("historicRecords")
  //      historicRecord shouldNot be(null)
  //
  //      historicRecord.toString should include("2011 to 2013")
  //
  //      val incomeMessage = document.getElementById("incomeMessage")
  //      val bereavementMessage = document.getElementById("bereavementMessage")
  //      val incomeLink = document.getElementById("incomeLink")
  //      val bereavementLink = document.getElementById("bereavementLink")
  //      incomeMessage.text() shouldBe "To let us know about a change in income, contact HMRC"
  //      bereavementMessage.text() shouldBe "To let us know about a bereavement, contact HMRC"
  //      incomeLink.attr("href") shouldBe "/marriage-allowance-application/change-of-income"
  //      bereavementLink.attr("href") shouldBe "/marriage-allowance-application/bereavement"
  //    }
  //  }

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

  "After make changes redirect pages" should {
    "stopAllowance" in {
      val result: Future[Result] = controller().stopAllowance(request)

      val expected = Seq(
        messagesApi("pages.stopAllowance.paragraph1"),
        messagesApi("pages.stopAllowance.paragraph2")
      ).toArray
      val parsed = Jsoup.parse(contentAsString(result))
      val current = parsed.getElementsByTag("p").eachText().toArray()

      current shouldBe expected
    }

    "cancel" in {
      val result: Future[Result] = controller().cancel(request)

      val currentEndDate =
        TextGenerators.ukDateTransformer(Some(uk.gov.hmrc.time.TaxYear.current.finishes), isWelsh = false)
      val nextStartDate =
        TextGenerators.ukDateTransformer(Some(uk.gov.hmrc.time.TaxYear.current.next.starts), isWelsh = false)

      val expected = Seq(
        messagesApi("pages.cancel.paragraph1", currentEndDate),
        messagesApi("pages.cancel.paragraph2", nextStartDate)
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

    "bereavement(recipient)(text)" in {
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(Some(RelationshipRecord(Recipient.asString(), "", "", None, None, "", "")), None, None)
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

    "bereavement(transferor)(text)" in {
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(Some(RelationshipRecord(Transferor.asString(), "", "", None, None, "", "")), None, None)
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

    "bereavement(recipient)(bullet list)" in {
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(Some(RelationshipRecord(Recipient.asString(), "", "", None, None, "", "")), None, None)
          )
        )
      val result: Future[Result] = controller().bereavement(request)

      val expected = Array()
      val parsed = Jsoup.parse(contentAsString(result))
      val current = parsed.getElementsByTag("li").eachText().toArray()

      current shouldBe expected
    }

    "bereavement(transferor)(bullet list)" in {
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(Some(RelationshipRecord(Transferor.asString(), "", "", None, None, "", "")), None, None)
          )
        )
      val result: Future[Result] = controller().bereavement(request)
      val endOfYear =
        TextGenerators.ukDateTransformer(
          Some(uk.gov.hmrc.time.TaxYear.current.finishes),
          isWelsh = false,
          transformPattern = "d MMMM")

      val expected = Seq(
        messagesApi("pages.bereavement.transferor.point1"),
        messagesApi("pages.bereavement.transferor.point2", endOfYear)
      ).toArray
      val parsed = Jsoup.parse(contentAsString(result))
      val current = parsed.getElementsByTag("li").eachText().toArray()

      current shouldBe expected
    }

    "divorceEnterYear" in {
      when(mockUpdateRelationshipService.getDivorceDate(any(), any()))
        .thenReturn(Future.successful(Some(LocalDate.now().minusDays(1))))

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
