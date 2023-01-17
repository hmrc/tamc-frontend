package views.errors

import org.jsoup.Jsoup
import utils.BaseTest
import views.html.errors.no_tax_year_transferor

class NoTaxYearTransferor extends BaseTest {

  lazy val noTaxYearTransferor = instanceOf[no_tax_year_transferor]

  "noTaxYearTransferor" should {
    "return the correct title" in {
      val doc = Jsoup.parse(noTaxYearTransferor.toString)
      val title = doc.title()
      val expected = messages("title.pattern", messages("title.no-tax-years"))

      title shouldBe expected
    }

    "return Your Marriage Allowance claims h1" in {

      val doc = Jsoup.parse(noTaxYearTransferor.toString)
      val h1Tag = doc.getElementsByTag("h1").toString
      val expected = messages("eligibility.check.header")

      h1Tag should include(expected)
    }


    "return We will cancel your Marriage Allowance content" in {

      val doc = Jsoup.parse(noTaxYearTransferor.toString)
      val paragraphTag = doc.getElementsByTag("p").toString
      val expected = messages("transferor.no-previous-years-available")

      paragraphTag should include(expected)

    }
  }


}
