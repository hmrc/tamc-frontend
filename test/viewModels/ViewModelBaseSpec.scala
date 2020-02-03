package viewModels

import controllers.actions.{AuthenticatedActionRefiner, UnauthenticatedActionTransformer}
import models.{Recipient, Role, Transferor}
import org.joda.time.LocalDate
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import test_utils.{MockAuthenticatedAction, MockFormPartialRetriever, MockTemplateRenderer, MockUnauthenticatedAction}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time.TaxYear
import utils.TamcViewModelTest
import views.helpers.TextGenerators

class ViewModelBaseSpec extends TamcViewModelTest {

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides(bind[AuthenticatedActionRefiner].to[MockAuthenticatedAction])
    .overrides(bind[UnauthenticatedActionTransformer].to[MockUnauthenticatedAction])
    .overrides(bind[TemplateRenderer].toInstance(MockTemplateRenderer))
    .overrides(bind[FormPartialRetriever].toInstance(MockFormPartialRetriever)
    ).configure(
    "metrics.jvm" -> false,
    "metrics.enabled" -> false
  ).build()

  def transformDate(date: LocalDate, isWelsh: Boolean = false): String = {
    TextGenerators.ukDateTransformer(Some(date), isWelsh)
  }

  def bulletStatements(role: Role, currentTaxYear: TaxYear, isCurrentYearDivorced: Boolean)(implicit messages: Messages): Seq[String] = {
    lazy val currentTaxYearEnd: String = transformDate(currentTaxYear.finishes)
    lazy val nextTaxYearStart: String = transformDate(currentTaxYear.next.starts)
    lazy val endOfPreviousTaxYear: String = transformDate(currentTaxYear.previous.finishes)
    lazy val taxYearEndForGivenYear: LocalDate => String = divorceDate => transformDate(TaxYear.taxYearFor(divorceDate).finishes)

    //TODO remove duplicate case into case _ =>
    (role, isCurrentYearDivorced) match {
      case (Recipient, true) => {
        Seq(messages("pages.divorce.explanation.recipient.current.bullet1", currentTaxYearEnd),
          messages("pages.divorce.explanation.recipient.current.bullet2", nextTaxYearStart))
      }
      case (Recipient, false) => {
        Seq(messages("pages.divorce.explanation.previous.bullet1", endOfPreviousTaxYear),
          messages("pages.divorce.explanation.previous.bullet2"))
      }
      case (Transferor, true) => {
        Seq(messages("pages.divorce.explanation.previous.bullet1", endOfPreviousTaxYear),
          messages("pages.divorce.explanation.previous.bullet2"))
      }
      case (Transferor, false) => {
        Seq(messages("pages.divorce.explanation.previous.bullet1", taxYearEndForGivenYear),
          messages("pages.divorce.explanation.previous.bullet2"))
      }
    }
  }
}
