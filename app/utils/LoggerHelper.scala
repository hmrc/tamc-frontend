/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.Logging

trait LoggerHelper extends Logging {

  def warn(message: String, throwable: Throwable): Unit = {
    logger.warn(message, throwable)
  }

  def warn(message: String): Unit = {
    logger.warn(message)
  }

  def error(message: String, throwable: Throwable): Unit = {
    logger.error(message, throwable)
  }

  def info(message: String): Unit = {
    logger.info(message)
  }
}
