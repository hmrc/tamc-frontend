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

package models.auth

import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.domain.Nino

sealed abstract class UserRequest[A](val request: Request[A]) extends WrappedRequest[A](request)

final case class RequestWithAuthState[A](
                                          override val request: Request[A],
                                          authState: AuthState
                                        ) extends UserRequest[A](request)

final case class AuthenticatedUserRequest[A](
                                              override val request: Request[A],
                                              authState: AuthState,
                                              nino: Nino
                                            ) extends UserRequest[A](request)

