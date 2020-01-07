/*
 * Copyright 2020 HM Revenue & Customs
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

package utils.viewHelpers

import org.jsoup.nodes.{Document, Element}
import org.scalatest.matchers.{MatchResult, Matcher}

import scala.collection.JavaConversions._

trait JSoupMatchers {

  class TagWithTextMatcher(expectedContent: String, tag: String) extends Matcher[Document] {
    def apply(document: Document): MatchResult = {
      val elements: List[String] = document.getElementsByTag(tag).toList.map(_.text)

      lazy val elementContents = elements.mkString("\t", "\n\t", "")

      MatchResult(
        elements.contains(expectedContent),
        s"[$expectedContent] not found in '$tag' elements:[\n$elementContents]",
        s"'$tag' element found with text [$expectedContent]"
      )
    }
  }

  class ElementWithAttributeValueMatcher(expectedContent: String, attribute: String) extends Matcher[Element] {
    def apply(left: Element): MatchResult = {
      val attribVal = left.attr(attribute)
      val attributes = left.attributes().asList().mkString("\t", "\n\t", "")

      MatchResult(
        attribVal == expectedContent,
        s"""[$attribute="$expectedContent"] is not a member of the element's attributes:[\n$attributes]""",
        s"""[$attribute="$expectedContent"] is a member of the element's attributes:[\n$attributes]"""
      )
    }

  }


  class CssSelectorWithTextMatcher(expectedContent: String, selector: String) extends Matcher[Document] {
    def apply(left: Document): MatchResult = {
      val elements: List[String] =
        left
          .select(selector)
          .toList
          .map(_.text)

      lazy val elementContents = elements.mkString("\t", "\n\t", "")

      MatchResult(
        elements.contains(expectedContent),
        s"[$expectedContent] not found in elements with '$selector' selector:[\n$elementContents]",
        s"[$expectedContent] element found with '$selector' selector and text [$expectedContent]"
      )
    }
  }

  def haveHeadingWithText(expectedText: String) = new TagWithTextMatcher(expectedText, "h1")

  def haveHeadingH2WithText(expectedText: String) = new TagWithTextMatcher(expectedText, "h2")

  def haveParagraphWithText(expectedText: String) = new TagWithTextMatcher(expectedText, "p")

  def haveLinkURL(expectedUrl: String) = new ElementWithAttributeValueMatcher(expectedUrl, "href")

  def havePreHeadingWithText(expectedText: String) = new CssSelectorWithTextMatcher(expectedText, "header>p")

}
