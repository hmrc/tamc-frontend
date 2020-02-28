package controllers

import play.api.mvc.Result
import play.twirl.api.Html
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

trait ControllerViewTestHelper {
  this: UnitSpec =>

  implicit class ViewMatcherHelper(result: Future[Result]) {
    def rendersTheSameViewAs(expected: Html): Unit =
      contentAsString(result) should equal(expected.toString)
  }
}
