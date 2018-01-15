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

import models.Cid
import models.Timestamp
import java.net.URLDecoder
import uk.gov.hmrc.domain.Generator

object TestData {

  object Ninos {
    private lazy val ninos = {
      val randomizer = new Generator()
      var ninos: Set[String] = Set()
      while (ninos.size <= 30) {
        ninos += randomizer.nextNino.nino
      }
      ninos.toList
    }

    val nino1: String = ninos(0)
    val nino2: String = ninos(1)
    val nino3: String = ninos(2)
    val nino4: String = ninos(3)
    val nino5: String = ninos(4)
    val nino6: String = ninos(5)
    val ninoWithRelationship: String = ninos(6)
    val ninoWithLOA1_5: String = ninos(7)
    val ninoWithLOA1: String = ninos(8)
    val ninoWithLOA1Spaces: String = ninoWithLOA1.substring(0, 2)+ " " + ninoWithLOA1.substring(2, 4) +" "+ninoWithLOA1.substring(4, 6) + " " + ninoWithLOA1.substring(6, 8) + " " + ninoWithLOA1.substring(8, 9)
    val ninoHappyPath: String = ninos(9)
    val ninoHappyPathWithSpaces: String = ninoHappyPath.substring(0, 2)+ " " + ninoHappyPath.substring(2, 4) +" "+ninoHappyPath.substring(4, 6) + " " + ninoHappyPath.substring(6, 8) + " " + ninoHappyPath.substring(8, 9)
    val ninoTransferorNotFound: String = ninos(10)
    val ninoTransferorDeceased: String = ninos(11)
    val ninoError: String = ninos(12)
    val ninoWithNoRelationship: String = ninos(13)
    val ninoWithActiveRelationship: String = ninos(14)
    val ninoWithGapInYears: String = ninos(15)
    val ninoWithHistoricRelationship: String = ninos(16)
    val ninoWithHistoricRejectableRelationship: String = ninos(17)
    val ninoWithHistoricallyActiveRelationship: String = ninos(18)
    val ninoWithHistoricActiveRelationship: String = ninos(19)
    val ninoCitizenNotFound: String = ninos(20)
    val ninoForBadRequest: String = ninos(21)
    val ninoWithCL100: String = ninos(22)
    val ninoWithConflict: String = ninos(23)
    val ninoWithLTM000503: String = ninos(24)
  }


  object Cids {
    private lazy val cids = {
      val randomizer = new java.util.Random
      var cids: Set[Cid] = Set()
      while (cids.size <= 6) {
        cids += randomizer.nextLong().abs
      }
      cids.toList
    }

    val cid1: Cid = cids(0)
    val cid2: Cid = cids(1)
    val cid3: Cid = cids(2)
    val cid4: Cid = cids(3)
    val cid5: Cid = cids(4)
    val cid6: Cid = cids(5)
  }
}
