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

package test_utils

import org.joda.time.{ DateTimeZone, DateTime }
import scala.concurrent.Future
import play.api.test.Helpers.OK
import java.util.Calendar
import java.text.SimpleDateFormat
import models.CitizenName
import test_utils.TestData.Ninos
import test_utils.TestData.Cids
import uk.gov.hmrc.http.BadGatewayException

object TestConstants {

  val TEST_CURRENT_DATE = new DateTime(2016, 4, 10, 0, 0, DateTimeZone.forID("Europe/London"))

  private val USER_HAPPY_PATH = s"""{"user_record":{"cid": ${Cids.cid1}, "timestamp": "2015", "has_allowance": false,
     "name":{"firstName":"Foo","lastName":"Bar"}}, "status": {"status_code":"OK"}}"""

  private val USER_HAS_RELATIONSHIP = """{"user_record":{"cid": 1000002, "timestamp": "2015", "has_allowance": true}, "status": {"status_code":"OK"}}"""

  private val USER_LOA_1_5 = """{"user_record":{"cid": 1000015, "timestamp": "2015", "has_allowance": false}, "status": {"status_code":"OK"}}"""

  private val TRANSFEROR_CID_NOT_FOUND = """{"status": {"status_code":"TAMC:ERROR:TRANSFEROR-NOT-FOUND"}}"""

  private val TRANSFEROR_DECEASED = """{"status": {"status_code":"TAMC:ERROR:TRANSFEROR-DECEASED"}}"""

  private val COC_NO_RELATIONSHIP = """{"relationship_record":{"relationships":[],
    "userRecord":{"cid":999700100,"timestamp":"2015","name":{"firstName":"Foo","lastName":"Bar"}}},
    "status":{"status_code":"OK"}}"""

  private val COC_ACTIVE_RELATIONSHIP = """{"relationship_record":{"relationships":
    [{"participant":"Recipient","creationTimestamp":"20150531235901","participant1StartDate":"20011230","otherParticipantInstanceIdentifier":"123456789012345",
    "otherParticipantUpdateTimestamp":"20150531235901"}],"userRecord":{"cid":999700100,"timestamp":"2015","name":{"firstName":"Foo","lastName":"Bar"}}},
    "status":{"status_code":"OK"}}"""

  private val COC_HISTORIC_RELATIONSHIP = """{"relationship_record":{"relationships":
    [{"participant":"Recipient","creationTimestamp":"20150531235901","participant1StartDate":"20011230",
    "relationshipEndReason":"DEATH","participant1EndDate":"20101230","otherParticipantInstanceIdentifier":"123456789012345",
    "otherParticipantUpdateTimestamp":"20150531235901"}],"userRecord":{"cid":999700100,"timestamp":"2015","name":{"firstName":"Foo","lastName":"Bar"}}},
    "status":{"status_code":"OK"}}"""

  private val COC_HISTORIC_REJECTABLE_RELATIONSHIP = """{"relationship_record":{"relationships":
    [
      {"participant":"Transferor","creationTimestamp":"20150531235901","participant1StartDate":"20131230","relationshipEndReason":"DIVORCE","participant1EndDate":"20141230","otherParticipantInstanceIdentifier":"123456789012345","otherParticipantUpdateTimestamp":"20150531235901"},
      {"participant":"Recipient","creationTimestamp":"20150531235901","participant1StartDate":"20021230","relationshipEndReason":"DIVORCE","participant1EndDate":"20121230","otherParticipantInstanceIdentifier":"123456789012345","otherParticipantUpdateTimestamp":"20150531235901"}
    ],"userRecord":{"cid":999700100,"timestamp":"2015","name":{"firstName":"Foo","lastName":"Bar"}}},
    "status":{"status_code":"OK"}}"""

  private val COC_GAP_IN_YEARS = """{"relationship_record":{"relationships":
    [
      {"participant":"Recipient","creationTimestamp":"20150531235901","participant1StartDate":"20161230","otherParticipantInstanceIdentifier":"123456789012345","otherParticipantUpdateTimestamp":"20150531235901"},
      {"participant":"Transferor","creationTimestamp":"20150531235902","participant1StartDate":"20131230","relationshipEndReason":"DIVORCE","participant1EndDate":"20141230","otherParticipantInstanceIdentifier":"123456789012345","otherParticipantUpdateTimestamp":"20150531235901"},
      {"participant":"Recipient","creationTimestamp":"20150531235903","participant1StartDate":"20021230","relationshipEndReason":"DIVORCE","participant1EndDate":"20121230","otherParticipantInstanceIdentifier":"123456789012345","otherParticipantUpdateTimestamp":"20150531235901"}
    ],"userRecord":{"cid":999700100,"timestamp":"2015","name":{"firstName":"Foo","lastName":"Bar"}}},
    "status":{"status_code":"OK"}}"""

  private val COC_HISTORICALLY_ACTIVE_RELATIONSHIP = """{"relationship_record":{"relationships":
    [{"participant":"Recipient","creationTimestamp":"20150531235901","participant1StartDate":"20011230",
    "relationshipEndReason":"DEATH","participant1EndDate":"20201230","otherParticipantInstanceIdentifier":"123456789012345",
    "otherParticipantUpdateTimestamp":"20150531235901"}, {"participant":"Recipient","creationTimestamp":"20150531235901","participant1StartDate":"20011230",
    "relationshipEndReason":"DEATH","participant1EndDate":"20101230","otherParticipantInstanceIdentifier":"123456789012345",
    "otherParticipantUpdateTimestamp":"20150531235901"}],"userRecord":{"cid":999700100,"timestamp":"2015","name":{"firstName":"Foo","lastName":"Bar"}}},
    "status":{"status_code":"OK"}}"""

  private val COC_ACTIVE_HISTORIC_RELATIONSHIP = """{"relationship_record":{"relationships":
    [{"participant":"Recipient","creationTimestamp":"20150531235901","participant1StartDate":"20011230","otherParticipantInstanceIdentifier":"123456789012345",
    "otherParticipantUpdateTimestamp":"20150531235901"}, {"participant":"Recipient","creationTimestamp":"20150531235901","participant1StartDate":"20011230",
    "relationshipEndReason":"DEATH","participant1EndDate":"20101230","otherParticipantInstanceIdentifier":"123456789012345",
    "otherParticipantUpdateTimestamp":"20150531235901"}],"userRecord":{"cid":999700100,"timestamp":"2015","name":{"firstName":"Foo","lastName":"Bar"}}},
    "status":{"status_code":"OK"}}"""

  private val COC_CITIZEN_NOT_FOUND = """{"relationship_record":{"relationships":[],
    "userRecord":{"cid":999700100,"timestamp":"2015","name":{"firstName":"Foo","lastName":"Bar"}}},
    "status":{"status_code":"TAMC:ERROR:CITIZEN-NOT-FOUND"}}"""

  private val COC_BAD_REQUEST = """{"relationship_record":{"relationships":[],
    "userRecord":{"cid":999700100,"timestamp":"2015","name":{"firstName":"Foo","lastName":"Bar"}}},
    "status":{"status_code":"TAMC:ERROR:BAD-REQUEST"}}"""

  val GENERIC_CITIZEN_NAME = Some(CitizenName(Some("Foo"), Some("Bar")))

  val CREATE_SUCCESSFUL_RELATIONSHIP_REQUEST = "CreateRelationshipRequestHolder(CreateRelationshipRequest(" + Cids.cid1 + ",2015," + Cids.cid2 + ",2015,List(2015)),CreateRelationshipNotificationRequest(UNKNOWN,example123@example.com,false))"

  val UPDATE_SUCCESSFUL_RELATIONSHIP_REQUEST = "UpdateRelationshipRequestHolder(UpdateRelationshipRequest(RecipientInformation(999700100,2015),TransferorInformation(),RelationshipInformation(,Cancelled by Transferor," + getDateInRequiredFormate + ")),UpdateRelationshipNotificationRequest(UNKNOWN,example@example.com,Recipient,false,false))"

  val REJECT_ACTIVE_REL_REQ = "UpdateRelationshipRequestHolder(UpdateRelationshipRequest(RecipientInformation(999700100,2015),TransferorInformation(),RelationshipInformation(123456,Rejected by Recipient,20110406)),UpdateRelationshipNotificationRequest(UNKNOWN,example@example.com,Recipient,false,false))"

  val REJECT_HISTORIC_REL_REQ = "UpdateRelationshipRequestHolder(UpdateRelationshipRequest(RecipientInformation(999700100,2015),TransferorInformation(),RelationshipInformation(98765,Rejected by Recipient,20120406)),UpdateRelationshipNotificationRequest(UNKNOWN,example@example.com,Recipient,false,true))"

  val UPDATE_RELATIONSHIP_CITIZEN_NOT_FOUND = "UpdateRelationshipRequestHolder(UpdateRelationshipRequest(RecipientInformation(999700101,2015),TransferorInformation(),RelationshipInformation(,Cancelled by Transferor," + getDateInRequiredFormate + ")),UpdateRelationshipNotificationRequest(UNKNOWN,example@example.com,Recipient,false,false))"

  val UPDATE_RELATIONSHIP_BAD_REQUEST = "UpdateRelationshipRequestHolder(UpdateRelationshipRequest(RecipientInformation(999700102,2015),TransferorInformation(),RelationshipInformation(,Cancelled by Transferor," + getDateInRequiredFormate + ")),UpdateRelationshipNotificationRequest(UNKNOWN,example@example.com,Recipient,false,false))"

  val CANNOT_CREATE_RELATIONSHIP_REQUEST = "CreateRelationshipRequestHolder(CreateRelationshipRequest(" + Cids.cid1 + ",2015,123456,2015,List(2015)),CreateRelationshipNotificationRequest(UNKNOWN,example123@example.com,false))"

  val CREATE_RELATIONSHIP_TESTING_PUT_ERROR = """CreateRelationshipRequestHolder(CreateRelationshipRequest(" + Cids.cid1 + ",2015,999999,2015,List(2015)),CreateRelationshipNotificationRequest(UNKNOWN,example123@example.com,false))"""

  val ERROR_HEADING = "There is a problem"

  val ERROR_GENERAL_TEXT = "Check your information is correct, in the right place and in the right format."

  val ERROR_MANDATORY_DATA_TEXT = "Check that you have given answers."

  val dummyHttpGetResponseJsonMap = Map(Ninos.ninoHappyPath -> Future.successful(new DummyHttpResponse(TestConstants.USER_HAPPY_PATH, OK)),
    Ninos.ninoWithRelationship -> Future.successful(new DummyHttpResponse(TestConstants.USER_HAS_RELATIONSHIP, OK)),
    Ninos.ninoWithLOA1_5 -> Future.successful(new DummyHttpResponse(TestConstants.USER_LOA_1_5, OK)),
    Ninos.ninoTransferorNotFound -> Future.successful(new DummyHttpResponse(TestConstants.TRANSFEROR_CID_NOT_FOUND, OK)),
    Ninos.ninoTransferorDeceased -> Future.successful(new DummyHttpResponse(TestConstants.TRANSFEROR_DECEASED, OK)),
    Ninos.ninoWithNoRelationship -> Future.successful(new DummyHttpResponse(TestConstants.COC_NO_RELATIONSHIP, OK)),
    Ninos.ninoWithActiveRelationship -> Future.successful(new DummyHttpResponse(TestConstants.COC_ACTIVE_RELATIONSHIP, OK)),
    Ninos.ninoWithHistoricRelationship -> Future.successful(new DummyHttpResponse(TestConstants.COC_HISTORIC_RELATIONSHIP, OK)),
    Ninos.ninoWithHistoricRejectableRelationship -> Future.successful(new DummyHttpResponse(TestConstants.COC_HISTORIC_REJECTABLE_RELATIONSHIP, OK)),
    Ninos.ninoWithHistoricallyActiveRelationship -> Future.successful(new DummyHttpResponse(TestConstants.COC_HISTORICALLY_ACTIVE_RELATIONSHIP, OK)),
    Ninos.ninoWithHistoricActiveRelationship -> Future.successful(new DummyHttpResponse(TestConstants.COC_ACTIVE_HISTORIC_RELATIONSHIP, OK)),
    Ninos.ninoCitizenNotFound -> Future.successful(new DummyHttpResponse(TestConstants.COC_CITIZEN_NOT_FOUND, OK)),
    Ninos.ninoForBadRequest -> Future.successful(new DummyHttpResponse(TestConstants.COC_BAD_REQUEST, OK)),
    Ninos.ninoWithGapInYears -> Future.successful(new DummyHttpResponse(TestConstants.COC_GAP_IN_YEARS, OK)),
    Ninos.ninoError -> Future.failed(new BadGatewayException("TESTING-GET-ERROR")))

  val dummyHttpPutResponseJsonMap = Map(
    CREATE_SUCCESSFUL_RELATIONSHIP_REQUEST -> Future.successful(new DummyHttpResponse("""{"status": {"status_code":"OK"}}""", 200)),
    UPDATE_SUCCESSFUL_RELATIONSHIP_REQUEST -> Future.successful(new DummyHttpResponse("""{"status": {"status_code":"OK"}}""", 200)),
    REJECT_ACTIVE_REL_REQ -> Future.successful(new DummyHttpResponse("""{"status": {"status_code":"OK"}}""", 200)),
    REJECT_HISTORIC_REL_REQ -> Future.successful(new DummyHttpResponse("""{"status": {"status_code":"OK"}}""", 200)),
    CANNOT_CREATE_RELATIONSHIP_REQUEST -> Future.successful(new DummyHttpResponse("""{"status": {"status_code":"TAMC:ERROR:CANNOT-CREATE-RELATIONSHIP"}}""", OK)),
    UPDATE_RELATIONSHIP_CITIZEN_NOT_FOUND -> Future.successful(new DummyHttpResponse("""{"status": {"status_code":"TAMC:ERROR:CITIZEN-NOT-FOUND"}}""", OK)),
    UPDATE_RELATIONSHIP_BAD_REQUEST -> Future.successful(new DummyHttpResponse("""{"status": {"status_code":"TAMC:ERROR:BAD-REQUEST"}}""", OK)),
    CREATE_RELATIONSHIP_TESTING_PUT_ERROR -> Future.failed(new BadGatewayException("TESTING-PUT-ERROR")))

  private def getDateInRequiredFormate(): String = {
    val DATE_FORMAT = "yyyyMMdd"
    val sdf = new SimpleDateFormat(DATE_FORMAT)
    val currentdate = Calendar.getInstance()
    sdf.format(currentdate.getTime())
  }

}
