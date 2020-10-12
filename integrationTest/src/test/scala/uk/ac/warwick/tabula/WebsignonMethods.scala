package uk.ac.warwick.tabula

import org.openqa.selenium.WebDriver
import org.scalatest.Assertions
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar._
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.selenium.WebBrowser
import uk.ac.warwick.tabula.WebsignonMethods._

import scala.util.matching.Regex

object WebsignonMethods {
  def parseSignedInDetail(html: String): String = {
    val SignedInAs = new Regex("""(?s).+(Signed in as [\-_\w0-9 ]+).+""")
    html match {
      case SignedInAs(line) => line.trim
      case _ => "couldn't parse anything useful from the HTML"
    }
  }
}

trait WebsignonMethods extends Matchers with Eventually {

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(30, Seconds), interval = Span(200, Millis))

  import WebBrowser._ // include methods like "go to"
  implicit val webDriver: WebDriver // let the trait know this will be implemented

  // nested objects so we can say "signIn as(user) to (url)".
  // with Java punctuation it would be "(signIn.as(user)).to(url)".
  //
  // (currently requires that the user's first name is the usercode, to check signed-in-ness)
  object signIn {
    def as(details: LoginDetails): SigningInPhase = {
      SigningInPhase(details)
    }

    case class SigningInPhase(details: LoginDetails) {

      def to(url: String): Unit = {
        // Sets session cookies if this user's logged in once before.
        //WebsignonMethods.sessions.retrieve(details.usercode, webDriver)

        go to url

        if (pageTitle.contains("Sign in")) {
          if (find("userName").isDefined) {
            // Signed-out, but with forced sign-in
            login(url)
            // Go through method again to verify sign-in
          } else {
            // Access refused
            click on linkText("Sign in with a different username.")
            login(url)
          }
          to(url)
        } else if (className("id7-page-header").findElement.isDefined) {
          // ID7
          val ssoLink = className("sso-link").findElement
          if (ssoLink.isDefined && ssoLink.get.attribute("class").exists(_.contains("sign-out"))) {
            // Already signed in
            if (ssoLink.get.attribute("data-name").exists(_.contains(details.usercode))) {
              // Already signed in as the right user, so we're done
            } else {
              // Signed in as someone else
              click on ssoLink.get
              eventually {
                (cssSelector("div.sign-out a").findElement.isDefined && cssSelector("div.sign-out a").findElement.get.isDisplayed) should be (true)
              }
              click on cssSelector("div.sign-out a")
              eventually {
                // The page contains two sso-link class elements so pick up the right one. The header div only contains sign-in class
                val cssSel = cssSelector(".id7-page-header a.sso-link")
                cssSel.findElement.isDefined && cssSel.findElement.get.attribute("class").exists(_.contains("sign-in")) should be (true)
                click on cssSel
              }

              login(url)
            }
          } else {
            // Not signed in
            click on ssoLink.get
            login(url)
          }

          val newSsoLink = className("sso-link").findElement.get
          if (newSsoLink.attribute("class").exists(_.contains("sign-out")) && newSsoLink.attribute("data-name").exists(_.contains(details.usercode))) {
            // NOW we're done
          } else if (pageSource contains "Access refused") {
            Assertions.fail("Signed in as " + details.description + " but access refused to " + url)
          } else {
            Assertions.fail("Tried to sign in as " + details.description + " but failed.")
          }

        } else {
          // ID6
          // FIXME doesn't handle being signed in as another user
          // TODO doesn't check that SSO sends you back to the right page
          // FIXME requires firstName==usercode
          if (pageSource contains ("Signed in as " + details.usercode)) {
            // we're done
          } else {

            if (pageSource contains "Signed in as ") {
              // signed in as someone else; sign out first
              click on linkText("Sign out")
            } else if (pageTitle startsWith "Sign in - Access refused") {
              //signed in as someone else who doesn't have permissions to view the page
              //  - follow the "sign in as another user" link
              click on linkText("Sign in with a different username.")
            }

            // sign in if we've not already been taken to that page
            if (!pageTitle.contains("Sign in")) {
              if (linkText("Sign in").findElement.isDefined) {
                click on linkText("Sign in")
              } else if (linkText("Sign out").findElement.isDefined) {
                val detail = parseSignedInDetail(pageSource)
                fail(s"Tried to sign out to sign in as ${details.usercode}, but still appear to be signed in! ($detail)")
              } else {
                fail("No Sign in or out links! URL:" + currentUrl)
              }
            }

            login(url)

            if (pageSource contains ("Signed in as " + details.usercode)) {
              // NOW we're done
            } else if (pageSource contains "Access refused") {
              Assertions.fail("Signed in as " + details.description + " but access refused to " + url)
            } else {
              Assertions.fail("Tried to sign in as " + details.description + " but failed.")
            }
          }
        }

      }

      private def login(url: String): Unit = {
        // wait for the page to load
        eventually(timeout(10.seconds), interval(200.millis))(
          pageTitle should include("Sign in")
        )
        textField("userName").value = details.usercode
        pwdField("password").value = details.password
        submit()

        if (currentUrl.startsWith("https://websignon")) {
          fail("Should've been signed in but aren't - credentials error?")
        }

        // Sign-out operations redirect you to the context root, so we may now be on the wrong page...
        if (currentUrl != url) {
          go to url
        }
      }
    }

  }


}
