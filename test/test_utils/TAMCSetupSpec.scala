package test_utils

import controllers.actions.{AuthenticatedActionRefiner, UnauthenticatedActionTransformer}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.renderer.TemplateRenderer

trait TAMCSetupSpec extends UnitSpec with I18nSupport with GuiceOneAppPerSuite with MockitoSugar with WireMockHelper {

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides(bind[AuthenticatedActionRefiner].to[MockAuthenticatedAction])
    .overrides(bind[UnauthenticatedActionTransformer].to[MockUnauthenticatedAction])
    .overrides(bind[TemplateRenderer].toInstance(MockTemplateRenderer))
    .overrides(bind[FormPartialRetriever].toInstance(MockFormPartialRetriever)
    ).configure(
    "metrics.jvm" -> false,
    "metrics.enabled" -> false,
    "microservice.services.marriage-allowance.port" -> server.port()
  ).build()

  def instanceOf[T](implicit evidence: scala.reflect.ClassTag[T]): T = app.injector.instanceOf[T]

  implicit val request: Request[AnyContent] = FakeRequest()

  implicit def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

}
