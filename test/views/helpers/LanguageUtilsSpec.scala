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

package views.helpers

import forms.EmailForm
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import play.api.i18n.{Lang, Messages}
import uk.gov.hmrc.emailaddress.EmailAddress
import utils.BaseTest

import java.time.LocalDate
import scala.collection.immutable

class LanguageUtilsSpec extends BaseTest with Matchers {

  val languageUtilsImpl: LanguageUtilsImpl = instanceOf[LanguageUtilsImpl]


  private val currentYear = LocalDate.now().getYear
  private val years: immutable.Seq[Int] = (currentYear - 2 to currentYear + 3).toList

  trait EnglishSetup {
    val languageUtils: LanguageUtils = EnglishLangaugeUtils
    implicit val englishMessage: Messages = mock[Messages]
    when(englishMessage.lang).thenReturn(Lang("en"))
  }

  trait WelshSetup {
    val languageUtils: LanguageUtils = WelshLanguageUtils
    implicit val welshMessage: Messages = mock[Messages]
    when(welshMessage.lang).thenReturn(Lang("cy"))
  }

  "isWelsh" must {

    "return true if the language is set to Welsh" in new WelshSetup {
      languageUtilsImpl.isWelsh(welshMessage) shouldBe true
    }

    "return false if the language is not set to Welsh" in new EnglishSetup {
      languageUtilsImpl.isWelsh(englishMessage) shouldBe false
    }

    "seperator" must {
      "return English separator" in new EnglishSetup {
        languageUtils.separator shouldBe " to "
      }

      "return Welsh separator" in new WelshSetup {
        languageUtils.separator shouldBe " i "
      }
    }

    "ukDateTransformer" must {

      val englishList = List(
        (LocalDate.of(2016, 5, 6), "May"),
        (LocalDate.of(1993, 11, 11), "November"),
        (LocalDate.of(2020, 1, 1), "January")
      )

      englishList.foreach { dateMonthTuple =>
        s"return month as English ${dateMonthTuple._2}" in new EnglishSetup {
          languageUtils.ukDateTransformer(dateMonthTuple._1) should include(dateMonthTuple._2)
        }
      }

      val welshMonthsInOrder = Seq("Ionawr", "Chwefror", "Mawrth", "Ebrill", "Mai", "Mehefin", "Gorffennaf", "Awst",
        "Medi", "Hydref", "Tachwedd", "Rhagfyr")


      for ((month, monthAsInt) <- welshMonthsInOrder.zip(Stream from 1)) {
        s"return month as Welsh $month" in new WelshSetup {
          languageUtils.ukDateTransformer(LocalDate.of(2020, monthAsInt, 1)) should include(month)
        }
      }
    }

    "formPossessive" must {
      "append an 's in English" in new EnglishSetup {
        languageUtils.formPossessive("Richard") shouldBe "Richard’s"
      }

      "remain the same in Welsh" in new WelshSetup {
        languageUtils.formPossessive("Gareth") shouldBe "Gareth"
      }
    }

    "taxDateInterval" must {
      for (year <- years) {
        val start = year
        val finish = year + 1
        s"return the beginning and end dates of $year tax year(UK)" in new EnglishSetup {
          languageUtils.taxDateInterval(year) shouldBe s"6 April $start to 5 April $finish"
        }
        s"return the beginning and end dates of $year tax year(CY)" in new WelshSetup {
          languageUtils.taxDateInterval(year) shouldBe s"6 Ebrill $start i 5 Ebrill $finish"
        }
      }
    }

    "taxDateIntervalMultiYear" must {
      for (year <- years) {
        val start = year
        val finish = year + 1
        val expected = finish + 1
        s"return the years that two tax years span from $start to $finish (UK)" in  new EnglishSetup {
          languageUtils.taxDateIntervalMultiYear(start, finish) shouldBe s"$start to $expected"
        }
        s"return the years that two tax years span from $start to $finish (CY)" in new WelshSetup {
          languageUtils.taxDateIntervalMultiYear(start, finish) shouldBe s"$start i $expected"
        }
      }
    }

    "taxDateIntervalShort" must {
      for (year <- years) {
        val start = year
        val finish = year + 1
        s"return the years that a single($start) tax year spans across(UK)" in new EnglishSetup {
          languageUtils.taxDateIntervalShort(start) shouldBe s"$start to $finish"
        }
        s"return the years that a single($start) tax year spans across(CY)" in new WelshSetup {
          languageUtils.taxDateIntervalShort(start) shouldBe s"$start i $finish"
        }
      }
    }

    "taxDateIntervalString" must {
      "return dates for one tax year to present(UK)" in new EnglishSetup {
        languageUtils.taxDateIntervalString("20160505", Some("20170505")) shouldBe "2016 to 2018"
      }

      "return dates for one tax year to another(UK)" in new EnglishSetup {
        languageUtils.taxDateIntervalString("20160505", None) shouldBe "2016 to Present"
      }

      "return dates for one tax year to present(CY)" in new WelshSetup  {
        languageUtils.taxDateIntervalString("20160505", Some("20170505")) shouldBe "2016 i 2018"
      }

      "return dates for one tax year to another(CY)" in new WelshSetup {
        languageUtils.taxDateIntervalString("20160505", None) shouldBe "2016 i’r Presennol"
      }
    }

    "formPageDateJourney" must {
      "return list of keys string" in new EnglishSetup {
        val formWithErrors = EmailForm.emailForm.bind(Map("transferor-email" -> "exampleemail.com"))

        languageUtils.formPageDataJourney("prefix", formWithErrors) shouldBe
          "prefix-erroneous(transferor-email)"
      }

      "return prefix" in new EnglishSetup {
        val form = EmailForm.emailForm.fill(EmailAddress("example@email.com"))

        languageUtils.formPageDataJourney("prefix", form) shouldBe
          "prefix"
      }
    }

    "dateTransformer" must {
      "return String from LocalDate" in new EnglishSetup {
        languageUtils.dateTransformer(LocalDate.of(2020, 2, 2)) shouldBe
          "02/02/2020"
      }

      "return LocalDate from String" in new EnglishSetup {
        languageUtils.dateTransformer("20200202") shouldBe
          LocalDate.of(2020, 2, 2)
      }
    }

    "nonBreakingSpace" must {
      "replace spaces in String" in new EnglishSetup {
        languageUtils.nonBreakingSpace("Break space") shouldBe
          "Break\u00A0space"
      }
    }

  }
}