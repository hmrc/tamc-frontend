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

package utils

import helpers.NbspString
import org.jsoup.nodes.{Document, Element}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers

trait ViewTestUtils extends Matchers {

  type LazyDocument = () => Document

  def checkDocTitle(expectedTitle: String)(implicit doc: LazyDocument): Assertion = {
    doc().title() shouldBe expectedTitle
  }

  def checkPageTitle(expectedTitle: String, selector: String = "#page-title", replaceNbsp: Boolean = false)(implicit doc: LazyDocument): Assertion = {
    val result = doc().select(selector).text()
    val finalText = (if (replaceNbsp) result.replaceNbsp else result)
    assert(finalText.contains(expectedTitle), s"The title of the page, '$finalText', did not match '$expectedTitle'.")
  }

  def checkTextInElement(expectedText: String, selector: String, replaceNbsp: Boolean = false)(implicit doc: LazyDocument): Assertion = {
    val element = doc().select(selector).text()
    (if (replaceNbsp) element.replaceNbsp else element) shouldBe expectedText
  }

  def checkElementExists(selector: String)(implicit doc: LazyDocument): Assertion = {
    assert(doc().select(selector).size() > 0, s"The element with the selector '$selector' does exist.")
  }

  def checkElementDoesNotExist(selector: String)(implicit doc: LazyDocument): Assertion = {
    doc().select(selector).size() shouldBe 0
  }

  def checkElementContainsClass(clazz: String, selector: String)(implicit doc: LazyDocument): Assertion = {
    assert(doc().select(selector).hasClass(clazz), s"The element with selector '$selector' does not contain the class '$clazz'.")
  }

  def checkElementForAttribute(attribute: String, selector: String)(implicit doc: LazyDocument): Assertion = {
    assert(doc().select(selector).hasAttr(attribute), s"The element with the selector '$selector' does not contain the attribute '$attribute'.")
  }

  def checkInputValue(value: String, selector: String)(implicit doc: LazyDocument): Assertion = {
    assert(doc().select(selector).`val`() == value, s"The element with the selector '$selector' does not contain the value '$value'.")
  }

  def selectFirst(selector: String)(implicit doc: LazyDocument): Element = {
    doc().selectFirst(selector)
  }
}
