/*
 * Copyright 2024 HM Revenue & Customs
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

package helpers

import controllers.auth.PertaxAuthAction
import play.api.mvc.{Request, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FakePertaxAuthAction @Inject()(implicit val executionContext: ExecutionContext) extends PertaxAuthAction {
  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    Future.successful(None)
  }
}
