/*
 * Copyright 2019 HM Revenue & Customs
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

///*
// * Copyright 2019 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package controllers
//
//import controllers.actions.AuthenticatedActionRefiner
//import models._
//import org.jsoup.Jsoup
//import org.mockito.ArgumentMatchers
//import org.mockito.ArgumentMatchers.any
//import org.mockito.Mockito.when
//import play.api.test.FakeRequest
//import play.api.test.Helpers.{OK, contentAsString, defaultAwaitTimeout}
//import services._
//import test_utils.data.RelationshipRecordData
//import uk.gov.hmrc.play.partials.FormPartialRetriever
//import uk.gov.hmrc.renderer.TemplateRenderer
//import uk.gov.hmrc.time
//
//import scala.concurrent.Future
//
//class UpdateRelationshipContentTest extends ControllerBaseSpec {
//
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
//      incomeMessage.text() shouldBe "To let us know about a change in income, contact HMRC."
//      bereavementMessage.text() shouldBe "To let us know about a bereavement, contact HMRC."
//      incomeLink.attr("href") shouldBe "/marriage-allowance-application/change-of-income"
//      bereavementLink.attr("href") shouldBe "/marriage-allowance-application/bereavement"
//    }
//  }
//
//  "Update relationship make changes page" should {
//
//    "show transferor data when user is transferor" in {
//      val request = FakeRequest().withFormUrlEncodedBody("role" -> Role.TRANSFEROR, "historicActiveRecord" -> "false")
//      val result = controller.makeChange()(request)
//      status(result) shouldBe OK
//
//      val document = Jsoup.parse(contentAsString(result))
//
//      val endReason = document.getElementById("endReason")
//      endReason shouldNot be(null)
//
//      val endReasonCancel = endReason.getElementById("endReason-cancel")
//      val endReasonDivorce = endReason.getElementById("endReason-divorce")
//
//
//      endReasonCancel shouldNot be(null)
//      endReasonDivorce shouldNot be(null)
//
//
//      endReasonCancel.toString.contains(EndReasonCode.CANCEL) should be(true)
//      endReasonDivorce.toString.contains(EndReasonCode.DIVORCE) should be(true)
//
//    }
//
//    "show transferor data when user is recipient" in {
//      val request = FakeRequest().withFormUrlEncodedBody("role" -> Role.RECIPIENT, "historicActiveRecord" -> "false")
//      val result = controller.makeChange()(request)
//      status(result) shouldBe OK
//
//      val document = Jsoup.parse(contentAsString(result))
//
//      val endReason = document.getElementById("endReason")
//      endReason shouldNot be(null)
//
//      val endReasonReject = endReason.getElementById("endReason-reject")
//      val endReasonDivorce = endReason.getElementById("endReason-divorce")
//
//      endReasonReject shouldNot be(null)
//      endReasonDivorce shouldNot be(null)
//
//      endReasonReject.toString.contains(EndReasonCode.REJECT) should be(true)
//      endReasonDivorce.toString.contains(EndReasonCode.DIVORCE) should be(true)
//    }
//  }
//
//  "Update relationship confirmation page" should {
//
//    "confirm cancellation " in {
//      when(mockUpdateRelationshipService.saveEndRelationshipReason(ArgumentMatchers.eq(EndRelationshipReason(EndReasonCode.CANCEL)))(any(), any()))
//        .thenReturn(EndRelationshipReason(EndReasonCode.CANCEL))
//      when(mockTimeService.getEffectiveUntilDate(EndRelationshipReason(EndReasonCode.CANCEL)))
//        .thenReturn(Some(time.TaxYear.current.finishes))
//      when(mockTimeService.getEffectiveDate(EndRelationshipReason(EndReasonCode.CANCEL)))
//        .thenReturn(time.TaxYear.current.next.starts)
//      val result = controller.confirmCancel()(request)
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
//
//    }
//  }
//
//  "Update relationship make changes" should {
//
//    "return successful on historical active record for transferror" in {
//      val request = FakeRequest().withFormUrlEncodedBody("role" -> Role.TRANSFEROR, "historicActiveRecord" -> "true")
//      val result = controller.makeChange()(request)
//      status(result) shouldBe OK
//
//      val document = Jsoup.parse(contentAsString(result))
//      val endReason = document.getElementById("endReason")
//      endReason shouldNot be(null)
//
//      val endReasonCancel = endReason.getElementById("endReason-cancel")
//      val endReasonDivorce = endReason.getElementById("endReason-divorce")
//
//      endReasonCancel should be(null)
//      endReasonDivorce shouldNot be(null)
//
//      endReasonDivorce.toString should include(EndReasonCode.DIVORCE)
//    }
//
//    "return successful on non historically active record for transferror" in {
//      val request = FakeRequest().withFormUrlEncodedBody("role" -> Role.TRANSFEROR, "historicActiveRecord" -> "false")
//      val result = controller.makeChange()(request)
//      status(result) shouldBe OK
//
//      val document = Jsoup.parse(contentAsString(result))
//      val endReason = document.getElementById("endReason")
//      endReason shouldNot be(null)
//
//      val endReasonCancel = endReason.getElementById("endReason-cancel")
//      val endReasonDivorce = endReason.getElementById("endReason-divorce")
//
//      endReasonCancel shouldNot be(null)
//      endReasonDivorce shouldNot be(null)
//
//      endReasonDivorce.toString should include(EndReasonCode.DIVORCE)
//      endReasonCancel.toString should include(EndReasonCode.CANCEL)
//    }
//
//    "return successful on historical active record for recipient" in {
//      val request = FakeRequest().withFormUrlEncodedBody("role" -> Role.RECIPIENT, "historicActiveRecord" -> "true")
//      val result = controller.makeChange()(request)
//      status(result) shouldBe OK
//
//      val document = Jsoup.parse(contentAsString(result))
//
//      val endReason = document.getElementById("endReason")
//      endReason shouldNot be(null)
//
//      val endReasonReject = endReason.getElementById("endReason-reject")
//      val endReasonDivorce = endReason.getElementById("endReason-divorce")
//
//      endReasonDivorce should be(null)
//      endReasonReject shouldNot be(null)
//
//      endReasonReject.toString should include(EndReasonCode.REJECT)
//    }
//
//    "return successful on non historical active record for recipient" in {
//      val request = FakeRequest().withFormUrlEncodedBody("role" -> Role.RECIPIENT, "historicActiveRecord" -> "false")
//      val result = controller.makeChange()(request)
//      status(result) shouldBe OK
//
//      val document = Jsoup.parse(contentAsString(result))
//
//      val endReason = document.getElementById("endReason")
//      endReason shouldNot be(null)
//
//      val endReasonReject = endReason.getElementById("endReason-reject")
//      val endReasonDivorce = endReason.getElementById("endReason-divorce")
//
//      endReasonDivorce shouldNot be(null)
//      endReasonReject shouldNot be(null)
//
//      endReasonDivorce.toString.contains(EndReasonCode.DIVORCE) should be(true)
//      endReasonReject.toString.contains(EndReasonCode.REJECT) should be(true)
//    }
//  }
//
//  val mockRegistrationService: TransferService = mock[TransferService]
//  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]
//  val mockCachingService: CachingService = mock[CachingService]
//  val mockTimeService: TimeService = mock[TimeService]
//  val mockListRelationshipService: ListRelationshipService = mock[ListRelationshipService]
//
//
//  def controller: UpdateRelationshipController =
//    new UpdateRelationshipController(
//      messagesApi,
//      instanceOf[AuthenticatedActionRefiner],
//      mockUpdateRelationshipService,
//      mockListRelationshipService,
//      mockRegistrationService,
//      mockCachingService,
//      mockTimeService
//    )(instanceOf[TemplateRenderer], instanceOf[FormPartialRetriever])
//}
