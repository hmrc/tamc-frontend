package forms.coc

import forms.FormsBaseSpec
import play.api.data.FormError
import utils.Constants.forms.coc.MakeChangesDecisionFormConstants

import scala.collection.mutable

class MakeChangesDecisionFormTest extends FormsBaseSpec {

  "MakeChangesDecisionForm" should {
    val decisions = Seq(
      MakeChangesDecisionFormConstants.Divorce,
      MakeChangesDecisionFormConstants.IncomeChanges,
      MakeChangesDecisionFormConstants.NoLongerRequired,
      MakeChangesDecisionFormConstants.Bereavement
    )
    for (decision <- decisions) {
      s"bind a valid decision <- '$decision'" in {
        val formInput = Map[String, String](
          MakeChangesDecisionFormConstants.StopMAChoice -> decision
        )

        val form = MakeChangesDecisionForm.form.bind(formInput)
        val errors = form.errors
        val value = form.data

        errors shouldBe Seq()
        value shouldBe formInput
      }
    }

    //TODO add more
    val invalidDecisions = Seq(
      ""
    )
    for (decision <- invalidDecisions) {
      s"bind a invalid decision <- '$decision'" in {
        val formInput = Map[String, String](
          MakeChangesDecisionFormConstants.StopMAChoice -> decision
        )

        val form = MakeChangesDecisionForm.form.bind(formInput)
        val errors = form.errors
        val value = form.data

        errors shouldBe Seq(
          FormError(
            MakeChangesDecisionFormConstants.StopMAChoice,
            //TODO update after John will fix
            Seq("acacacacac"),
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

        val form = MakeChangesDecisionForm.form.bind(formInput)
        val errors = form.errors
        val value = form.data

        errors shouldBe Seq()
        value shouldBe formInput
        form.value shouldBe None
      }
    }

  }

}
