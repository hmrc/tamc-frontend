/*
 * Copyright 2018 HM Revenue & Customs
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

package metrics

import com.codahale.metrics.{MetricRegistry, Timer}
import com.codahale.metrics.Timer.Context
import uk.gov.hmrc.play.graphite.MicroserviceMetrics


trait Metrics {
  def incrementSuccessCitizenDetail(): Unit

  def incrementFailedCitizenDetail(): Unit

  def citizenDetailStartTimer(): Timer.Context
}

object Metrics extends Metrics with MicroserviceMetrics {

  val registry: MetricRegistry = metrics.defaultRegistry

  override def incrementSuccessCitizenDetail(): Unit = registry.counter("citizen-detail-success").inc()

  override def incrementFailedCitizenDetail(): Unit = registry.counter("citizen-detail-failed").inc()

  override def citizenDetailStartTimer(): Context = registry.timer("citizen-detail-response-timer").time()

}
