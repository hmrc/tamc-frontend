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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import errors.ErrorResponseStatus.{BAD_REQUEST, CITIZEN_NOT_FOUND, TRANSFEROR_NOT_FOUND}
import errors.{BadFetchRequest, CitizenNotFound, TransferorNotFound}
import models._
import org.joda.time.LocalDate
import play.api.libs.json.Json
import test_utils.TestData.Ninos
import test_utils._
import uk.gov.hmrc.domain.Nino
import utils.ConnectorBaseTest

import scala.concurrent.Future

class MarriageAllowanceConnectorTest extends ConnectorBaseTest {

  val nino = Nino(Ninos.nino1)

  "listRelationship" should {
    def serverStub(data: RelationshipRecordStatusWrapper): StubMapping = {
      server.stubFor(get(urlPathEqualTo(s"/paye/$nino/list-relationship"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(data).toString())
        )
      )
    }

    "return data" when {
      "success response returned from HOD" in {
        val response = RelationshipRecordStatusWrapper(RelationshipRecordList(Nil, None), ResponseStatus("OK"))
        serverStub(response)
        val result: Future[RelationshipRecordList] = MarriageAllowanceConnector.listRelationship(nino)
        await(result) shouldBe RelationshipRecordList(Nil, None)
      }
    }

    "throw an exception" when {

      "TRANSFEROR_NOT_FOUND is returned" in {
        serverStub(RelationshipRecordStatusWrapper(RelationshipRecordList(Nil, None), ResponseStatus(TRANSFEROR_NOT_FOUND)))
        intercept[TransferorNotFound](await(MarriageAllowanceConnector.listRelationship(nino)))
      }

      "CITIZEN_NOT_FOUND is returned" in {
        serverStub(RelationshipRecordStatusWrapper(RelationshipRecordList(Nil, None), ResponseStatus(CITIZEN_NOT_FOUND)))
        intercept[CitizenNotFound](await(MarriageAllowanceConnector.listRelationship(nino)))
      }

      "BAD_REQUEST is returned" in {
        serverStub(RelationshipRecordStatusWrapper(RelationshipRecordList(Nil, None), ResponseStatus(BAD_REQUEST)))
        intercept[BadFetchRequest](await(MarriageAllowanceConnector.listRelationship(nino)))
      }
    }
  }

  "getRecipientRelationship" should {
    "return a response" in {
      server.stubFor(post(urlPathEqualTo(s"/paye/$nino/get-recipient-relationship"))
        .willReturn(
          aResponse()
            .withStatus(200)
        ))
      val data = RegistrationFormInput("", "", Gender("M"), nino, LocalDate.now())

      await(MarriageAllowanceConnector.getRecipientRelationship(nino, data))
      verify(1,
        postRequestedFor(urlEqualTo(s"/paye/$nino/get-recipient-relationship"))
          .withRequestBody(equalToJson(Json.toJson(data).toString()))
      )
    }
  }

  "createRelationship" should {
    "return a response" in {
      server.stubFor(put(urlPathEqualTo(s"/paye/$nino/create-multi-year-relationship/pta"))
        .willReturn(
          aResponse()
            .withStatus(200)
        ))
      val data = MarriageAllowanceConnectorTestData.relationshipRequestHolder

      await(MarriageAllowanceConnector.createRelationship(nino, data, "pta"))
      verify(1,
        putRequestedFor(urlEqualTo(s"/paye/$nino/create-multi-year-relationship/pta"))
          .withRequestBody(equalToJson(Json.toJson(data).toString()))
      )
    }
  }

  "updateRelationship" should {
    "return a responce" in {
      server.stubFor(put(urlPathEqualTo(s"/paye/$nino/update-relationship"))
        .willReturn(
          aResponse()
            .withStatus(200)
        ))

      val data = MarriageAllowanceConnectorTestData.updateRelationshipRequestHolder

      await(MarriageAllowanceConnector.updateRelationship(nino, data))
      verify(1,
        putRequestedFor(urlEqualTo(s"/paye/$nino/update-relationship"))
          .withRequestBody(equalToJson(Json.toJson(data).toString()))
      )
    }
  }

}
