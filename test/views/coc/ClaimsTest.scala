package views.coc

import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import utils.BaseTest
import views.html.coc.claims

import java.time.LocalDate

class ClaimsTest extends BaseTest {

  lazy val claims = instanceOf[claims]

  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino("AA000000A"))



  "claims" should {
    "return the correct title" in {

      val document = Jsoup.parse(claims()).toString)
      val title = document.title()
      val expected = messages("title.application.pattern", messages("title.date-of-marriage"))

      title shouldBe expected
    }

}
