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

package forms.coc

import forms.FormsBaseSpec
import play.api.data.FormError

import scala.collection.mutable

class CheckClaimOrCancelDecisionFormTest extends FormsBaseSpec {

  "CheckClaimOrCancelDecisionForm" should {
    val decisions = Seq(
      CheckClaimOrCancelDecisionForm.CheckMarriageAllowanceClaim,
      CheckClaimOrCancelDecisionForm.StopMarriageAllowance
    )
    for (decision <- decisions) {
      s"bind a valid decision <- '$decision'" in {
        val formInput = Map[String, String](
          CheckClaimOrCancelDecisionForm.DecisionChoice -> decision
        )

        val form = CheckClaimOrCancelDecisionForm.form.bind(formInput)
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
          CheckClaimOrCancelDecisionForm.DecisionChoice -> decision
        )

        val form = CheckClaimOrCancelDecisionForm.form.bind(formInput)
        val errors = form.errors
        val value = form.data

        errors shouldBe Seq(
          FormError(
            CheckClaimOrCancelDecisionForm.DecisionChoice,
            //TODO update after John will fix
            Seq("cxcxcxcx"),
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

        val form = CheckClaimOrCancelDecisionForm.form.bind(formInput)
        val errors = form.errors
        val value = form.data

        errors shouldBe Seq(
          FormError(
            CheckClaimOrCancelDecisionForm.DecisionChoice,
            //TODO update after John will fix
            Seq("cxcxcxcx"),
            mutable.WrappedArray.empty
          )
        )
        value shouldBe formInput
        form.value shouldBe None
      }
    }

  }

}
