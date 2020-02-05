package forms.coc

import config.ApplicationConfig
import forms.FormsBaseSpec
import org.joda.time.LocalDate
import play.api.data.FormError
import play.api.test.FakeRequest
import utils.Constants.forms.coc.{DivorceSelectYearFormConstants, MakeChangesDecisionFormConstants}

import scala.collection.mutable

class DivorceSelectYearFormTest extends FormsBaseSpec {

  "DivorceSelectYearForm" should {
    val decisions = Seq(
      MakeChangesDecisionFormConstants.Divorce,
      MakeChangesDecisionFormConstants.IncomeChanges,
      MakeChangesDecisionFormConstants.NoLongerRequired,
      MakeChangesDecisionFormConstants.Bereavement
    )

    "bind date in future" in {
      val input = Some(LocalDate.now().plusYears(10))

      val request = FakeRequest().withFormUrlEncodedBody(
        "dateOfDivorce.day" -> input.get.getDayOfMonth.toString,
        "dateOfDivorce.month" -> input.get.getMonthOfYear.toString,
        "dateOfDivorce.year" -> input.get.getYear.toString
      )

      val form = DivorceSelectYearForm.form.bindFromRequest()(request)
      val errors = form.errors
      val value = form.value

      errors shouldBe Seq(
        FormError(
          DivorceSelectYearFormConstants.DateOfDivorce,
          Seq("pages.form.field.dom.error.max-date"),
          Seq(input.get.toString("dd/MM/yyyy")).toArray
        )
      )
      value shouldBe None
    }

    "bind valid date" in {
      val input = Some(LocalDate.now())

      val request = FakeRequest().withFormUrlEncodedBody(
        "dateOfDivorce.day" -> input.get.getDayOfMonth.toString,
        "dateOfDivorce.month" -> input.get.getMonthOfYear.toString,
        "dateOfDivorce.year" -> input.get.getYear.toString
      )

      val form = DivorceSelectYearForm.form.bindFromRequest()(request)
      val errors = form.errors
      val value = form.value

      errors shouldBe Seq()
      value shouldBe Some(input)
    }

    "bind date in past" in {
      val input = Some(ApplicationConfig.TAMC_MIN_DATE.minusYears(1))

      val request = FakeRequest().withFormUrlEncodedBody(
        "dateOfDivorce.day" -> input.get.getDayOfMonth.toString,
        "dateOfDivorce.month" -> input.get.getMonthOfYear.toString,
        "dateOfDivorce.year" -> input.get.getYear.toString
      )

      val form = DivorceSelectYearForm.form.bindFromRequest()(request)
      val errors = form.errors
      val value = form.value

      errors shouldBe Seq(
        FormError(
          DivorceSelectYearFormConstants.DateOfDivorce,
          Seq("pages.form.field.dom.error.min-date"),
          mutable.WrappedArray.empty
        )
      )
      value shouldBe None
    }

    //TODO add more
    val invalidDecisions = Seq(
      ""
    )
    for (decision <- invalidDecisions) {
      s"bind a invalid decision <- '$decision'" in {
        val formInput = Map[String, String](
          DivorceSelectYearFormConstants.DateOfDivorce -> decision
        )

        val form = DivorceSelectYearForm.form.bind(formInput)
        val errors = form.errors
        val value = form.data

        errors shouldBe Seq(
          FormError(
            DivorceSelectYearFormConstants.DateOfDivorce,
            //TODO update after John will fix
            Seq("pages.form.field.dod.error.required"),
            mutable.WrappedArray.empty
          )
        )
        value shouldBe formInput
      }
    }

    //TODO add more
    for (decision <- decisions) {
      s"bind a invalid key decision <- '$decision'" in {
        val formInput = Map[String, String](
          "tralala" -> decision
        )

        val form = DivorceSelectYearForm.form.bind(formInput)
        val errors = form.errors
        val value = form.data

        errors shouldBe List(FormError("dateOfDivorce", List("pages.form.field.dod.error.required"), mutable.WrappedArray.empty))
        value shouldBe formInput
        form.value shouldBe None
      }
    }

  }
}
