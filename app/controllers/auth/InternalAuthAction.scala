/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.auth

import com.google.inject.Inject
import config.ApplicationConfig
import play.api.Logging
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, IAAction, Resource, ResourceLocation, ResourceType, Retrieval}
import uk.gov.hmrc.internalauth.client.Predicate.Permission

import scala.concurrent.ExecutionContext

class InternalAuthAction @Inject()(
                                    appConfig: ApplicationConfig,
                                    internalAuth: BackendAuthComponents
                                  )(implicit
                                    val executionContext: ExecutionContext
                                  ) extends Logging {

  private val permission: Permission =
    Permission(
      resource = Resource(
        resourceType = ResourceType(appConfig.internalAuthResourceType),
        resourceLocation = ResourceLocation("*")
      ),
      action = IAAction("ADMIN")
    )

  //noinspection ScalaStyle
  def apply() =
    internalAuth.authorizedAction(permission, Retrieval.username)
}
