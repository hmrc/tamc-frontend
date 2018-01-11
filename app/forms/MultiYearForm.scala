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

package forms

import models.MultiYearInput
import models.Gender
import models.RegistrationFormInput
import play.api.data.FormError
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.of
import play.api.data.Forms.list
import play.api.data.Forms.number
import play.api.data.Forms.optional
import play.api.data.Mapping
import play.api.data.format.Formatter
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.data.validation.ValidationError
import uk.gov.hmrc.domain.Nino
import org.joda.time.LocalDate
import models.MultiYearInput
import play.api.data.validation.Constraint
import play.api.data.RepeatedMapping

object MultiYearForm {

  object HackedRepeatedMapping {
    def indexes(key: String, data: Map[String, String]): Seq[Int] = {
      val KeyPattern = ("^" + java.util.regex.Pattern.quote(key) + """\[(\d+)\].*$""").r
      data.toSeq.collect { case (KeyPattern(index), _) => index.toInt }.sorted.distinct
    }
  }

  case class HackedRepeatedMapping[T](wrapped: Mapping[T], val key: String = "", val constraints: Seq[Constraint[List[T]]] = Nil, extraYears: List[Int]) extends Mapping[List[T]] {

    override val format: Option[(String, Seq[Any])] = wrapped.format

    def verifying(addConstraints: Constraint[List[T]]*): Mapping[List[T]] = {
      this.copy(constraints = constraints ++ addConstraints.toSeq)
    }

    private def findMissingFields(data: Map[String, String]): Seq[Either[Seq[FormError], T]] =
      extraYears.filter { year => data.get(s"year[${year}]").isEmpty}.map { 
        year =>  Left(Seq(FormError(s"year[${year}]",s"pages.form.extra-year.field-required",Seq(year.toString, (year+1).toString))))}
    
    def bind(data: Map[String, String]): Either[Seq[FormError], List[T]] = {
      val allErrorsOrItems: Seq[Either[Seq[FormError], T]] = 
        RepeatedMapping.indexes(key, data).map(i => wrapped.withPrefix(key + "[" + i + "]").bind(data)) ++ findMissingFields(data)

      
      
      if (allErrorsOrItems.forall(_.isRight)) {
        Right(allErrorsOrItems.map(_.right.get).toList).right.flatMap(applyConstraints)
      } else {
        Left(allErrorsOrItems.collect { case Left(errors) => errors }.flatten)
      }
    }

    def unbind(value: List[T]): Map[String, String] = {
      val datas = value.zipWithIndex.map { case (t, i) => wrapped.withPrefix(key + "[" + i + "]").unbind(t) }
      datas.foldLeft(Map.empty[String, String])(_ ++ _)
    }

    def unbindAndValidate(value: List[T]): (Map[String, String], Seq[FormError]) = {
      val (datas, errors) = value.zipWithIndex.map { case (t, i) => wrapped.withPrefix(key + "[" + i + "]").unbindAndValidate(t) }.unzip
      (datas.foldLeft(Map.empty[String, String])(_ ++ _), errors.flatten ++ collectErrors(value))
    }

    def withPrefix(prefix: String): Mapping[List[T]] = {
      addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)
    }

    val mappings: Seq[Mapping[_]] = wrapped.mappings

  }

  def hackedList[A](mapping: Mapping[A], extraYears: List[Int]): Mapping[List[A]] = HackedRepeatedMapping(mapping, extraYears = extraYears)

  def multiYearForm(extraYears: List[Int] = List()) = Form[MultiYearInput](
    mapping(
      "year" -> hackedList(number, extraYears))(MultiYearInput.apply)(MultiYearInput.unapply))
}
