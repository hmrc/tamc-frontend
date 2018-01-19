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

package utils

import uk.gov.hmrc.http.{HttpDelete, HttpGet, HttpPost, HttpPut}
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.http.ws.WSDelete
import uk.gov.hmrc.play.http.ws.WSGet
import uk.gov.hmrc.play.http.ws.WSPost
import uk.gov.hmrc.play.http.ws.WSPut

object WSHttp extends WSGet with HttpGet
  with WSPut with HttpPut
  with WSPost with HttpPost
  with WSDelete with HttpDelete
  with AppName with RunMode {
  override val hooks = NoneRequired
}
