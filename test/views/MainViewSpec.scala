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

package views

import java.util.Locale

import org.jsoup.Jsoup
import models.auth.{BaseUserRequest, UserRequest}
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.Html
import utils.BaseTest

//scalastyle:off magic.number
class MainViewSpec extends BaseTest with Injecting {

  trait UniqueValues {
    val testName: String
    val profileAndSettingsLink: String

    val researchBannerHeaderSelector: String
    val researchBannerLinkSelector: String

    val accessibilityReferrerUrl: String
    val reportTechnicalProblemUrl: String
  }

  object OldMainUnique extends UniqueValues {
    override val testName: String = "old style"
    override val profileAndSettingsLink: String = "http://localhost:9232/personal-account/your-profile"

    override val researchBannerHeaderSelector: String = "h2"
    override val researchBannerLinkSelector: String = "a"

    override val accessibilityReferrerUrl: String = "9900%2Fsome-url"
    override val reportTechnicalProblemUrl: String = "http://localhost:9250/" +
      "contact/report-technical-problem?newTab=true&service=TAMC&referrerUrl=%2Fsome-url"
  }

  object NewMainUnique extends UniqueValues {
    override val testName: String = "new style"
    override val profileAndSettingsLink: String = "http://localhost:9232/personal-account/profile-and-settings"

    override val researchBannerHeaderSelector: String = ".hmrc-user-research-banner__title"
    override val researchBannerLinkSelector: String = ".hmrc-user-research-banner__link"

    override val accessibilityReferrerUrl: String = "12346%2Fmarriage-allowance"
    override val reportTechnicalProblemUrl: String = "http://localhost:9250/contact/report-technical-problem?newTab=true&service=TAMC&referrerUrl=%2Fsome-url"
  }

  object CommonValues {

    val pageTitle = "Fake Page Title - Apply for Marriage Allowance - GOV.UK"
    val pageHeader = "Apply for Marriage Allowance"
    val accountHome = "Account home"
    val messages = "Messages"
    val checkProgress = "Check progress"
    val profileAndSettings = "Profile and settings"
    val signOut = "Sign out"

    val localPersonalAccountLink = "http://localhost:9232/personal-account"
    val localMessagesLink = "http://localhost:9232/personal-account/messages"
    val localTrackProgressLink = "http://localhost:9100/track"
    val signOutUrl = "/marriage-allowance-application/logout"
    val keepAliveUrl = "/keep-alive"

    val urBannerHeader = "Help make GOV.UK better"
    val urBannerLinkText = "Sign up to take part in research (opens in new tab)"
    val urBannerLink =
      "https://signup.take-part-in-research.service.gov.uk/?utm_campaign=TAMCPTAbanner&utm_source=Other&utm_medium=gov.uk%20survey&t=HMRC&id=133"

    val accessibilityStatementText = "Accessibility statement"
    val accessibilityStatementBaseUrl =
      "http://localhost:12346/accessibility-statement/marriage-allowance?referrerUrl=http%3A%2F%2Flocalhost%3A"

    val reportATechnicalIssueText = "Is this page not working properly? (opens in new tab)"

    val scriptUrl = "http://localhost:9234/marriage-allowance/assets/javascript/tamc.js"
    val tamcBacklinkScriptUrl = "http://localhost:9234/marriage-allowance/pta-frontend/assets/tamc-backlink.js"
    val pollyfillScriptUrl = "http://localhost:9234/marriage-allowance/pta-frontend/assets/pollyfill.js"
  }

  case class TestObject(uniqueValues: UniqueValues, scaWrapper: Boolean)

  Seq(
    TestObject(OldMainUnique, scaWrapper = false),
    TestObject(NewMainUnique, scaWrapper = true)
  ).foreach { testObject =>

    implicit lazy val messages: MessagesImpl  = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])

    lazy val main = inject[Main]

    implicit val baseUserRequest: BaseUserRequest[_] = UserRequest(FakeRequest("GET", "/some-url"), None, isAuthenticated = true, Some(""), isSA = true)

    lazy val pageRender = main.apply(Some("Fake Page Title - Apply for Marriage Allowance - GOV.UK"))(Html("<p>Fake body</p>"))

    lazy val doc = {
      if (testObject.scaWrapper) {
        featureFlagSCAWrapperMock(true)
      } else {
        featureFlagSCAWrapperMock()
      }

      Jsoup.parse(pageRender.toString())
    }

    s"when using the ${testObject.uniqueValues.testName} for the main" which {

      "the page header should" should {

        lazy val pageHeader = doc.select(".hmrc-header__service-name")

        "contain the text 'Apply for Marriage Allowance'" in {
          pageHeader.text() shouldBe CommonValues.pageHeader
        }
      }

      "the page title" should {

        s"equal ${CommonValues.pageTitle}" in {
          doc.title() shouldBe CommonValues.pageTitle
        }

      }

      lazy val accountLinks = doc.select(".hmrc-account-menu__link")
      lazy val accountHome = accountLinks.first()
      lazy val messagesLink = accountLinks.get(3)
      lazy val checkProgressLink = accountLinks.get(4)
      lazy val profileAndSettingsLink = accountLinks.get(5)
      lazy val signOutLink = accountLinks.get(6)

      "the account link" should {

        "have the text Account Menu" in {
          accountHome.text() shouldBe CommonValues.accountHome
        }

        "have the link to the users personal account" in {
          accountHome.attr("href") shouldBe CommonValues.localPersonalAccountLink
        }

      }

      "the messages link" should {

        "have the text Messages" in {
          messagesLink.text() shouldBe CommonValues.messages
        }

        "have the link to the users personal account" in {
          messagesLink.attr("href") shouldBe CommonValues.localMessagesLink
        }

      }

      "the check progress link" should {

        "have the text Check progress" in {
          checkProgressLink.text() shouldBe CommonValues.checkProgress
        }

        "have the link to the users personal account" in {
          checkProgressLink.attr("href") shouldBe CommonValues.localTrackProgressLink
        }

      }

      "the profile and settings link" should {

        "have the text Profile and settings" in {
          profileAndSettingsLink.text() shouldBe CommonValues.profileAndSettings
        }

        "have the link to the users personal account" in {
          profileAndSettingsLink.attr("href") shouldBe testObject.uniqueValues.profileAndSettingsLink
        }

      }

      "the sign out link" should {

        "have the text Sign out" in {
          signOutLink.text() shouldBe CommonValues.signOut
        }

        "have the link to the sign out controller" in {
          signOutLink.attr("href") shouldBe CommonValues.signOutUrl
        }

      }

      "the research banner" should {

        lazy val researchBanner = doc.select(".hmrc-user-research-banner").first()

        "have the text 'Help make GOV.UK better'" in {
          val text = researchBanner.select(testObject.uniqueValues.researchBannerHeaderSelector).text()

          text shouldBe CommonValues.urBannerHeader
        }

        "have the link 'Sign up to take part in research (opens in new tab)'" which {

          lazy val link = researchBanner.select(testObject.uniqueValues.researchBannerLinkSelector)

          "has the correct text" in {
            link.text() shouldBe CommonValues.urBannerLinkText
          }

          "links to the correct URL" in {
            link.attr("href") shouldBe CommonValues.urBannerLink
          }

        }

      }

      "js scripts" should {

        lazy val scripts = doc.select("script").text()

        "still contain the tamc js script" in {
          scripts.contains(CommonValues.scriptUrl)
        }

        "still contains the tamc backlink scripts" in {
          scripts.contains(CommonValues.tamcBacklinkScriptUrl)
        }

        "still contains the pollyfill scripts" in {
          scripts.contains(CommonValues.pollyfillScriptUrl)
        }

      }

      "the accessibility link" should {

        lazy val accessibilityLink = doc
          .select("body > footer > div > div > div.govuk-footer__meta-item.govuk-footer__meta-item--grow > ul > li:nth-child(2) > a")

        "contain the text 'Accessibility statement'" in {
          accessibilityLink.text() shouldBe CommonValues.accessibilityStatementText
        }

        "contains the correct URL" in {
          val expectedUrl = CommonValues.accessibilityStatementBaseUrl + testObject.uniqueValues.accessibilityReferrerUrl

          accessibilityLink.attr("href") shouldBe expectedUrl
        }

      }

      "timeout dialogue" should {

        lazy val timeoutDialogueData = doc.select("[name=\"hmrc-timeout-dialog\"]")

        "have a timeout of 900" in {
          timeoutDialogueData.attr("data-timeout") shouldBe "900"
        }

        "have a countdown of 120" in {
          timeoutDialogueData.attr("data-countdown") shouldBe "120"
        }

        "have the correct keep alive url" in {
          timeoutDialogueData.attr("data-keep-alive-url") shouldBe CommonValues.keepAliveUrl
        }

        "have the correct sign out url" in {
          timeoutDialogueData.attr("data-sign-out-url") shouldBe CommonValues.signOutUrl
        }

      }

      "the link to report a problem" should {

        lazy val isThePageNotWorkingCorrectly = doc.select(".hmrc-report-technical-issue")

        "contain the correct text" in {
          isThePageNotWorkingCorrectly.text() shouldBe CommonValues.reportATechnicalIssueText
        }

        "contains the correct link" in {
          isThePageNotWorkingCorrectly.attr("href") shouldBe testObject.uniqueValues.reportTechnicalProblemUrl
        }

      }

    }

  }

}

