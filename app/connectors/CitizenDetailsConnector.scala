package connectors

import details.PersonDetails
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}
import utils.WSHttp

import scala.concurrent.Future


object CitizenDetailsConnector extends CitizenDetailsConnector with ServicesConfig {
  override def httpGet: HttpGet = WSHttp

  override def citizenDetailsUrl: String = baseUrl("citizen-details")
}

trait CitizenDetailsConnector {
  def httpGet: HttpGet

  def citizenDetailsUrl: String

  def citizenDetailsFromNino(nino: Nino)(implicit hc: HeaderCarrier): Future[PersonDetails] =
    httpGet.GET[PersonDetails](s"$citizenDetailsUrl/citizen-details/$nino/designatory-details")

}
