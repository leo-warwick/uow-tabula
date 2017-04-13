package uk.ac.warwick.tabula

import collection.JavaConverters._

import org.scalatest.selenium.WebBrowser
import org.scalatest.Assertions
import org.scalatest.Matchers
import org.openqa.selenium.{Cookie, WebDriver}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar._

import WebsignonMethods._
import scala.util.matching.Regex
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import com.gargoylesoftware.htmlunit
import com.gargoylesoftware.htmlunit.WebClient

object WebsignonMethods {
	def parseSignedInDetail(html: String): String = {
		val SignedInAs = new Regex("""(?s).+(Signed in as [\-_\w0-9 ]+).+""")
		html match {
			case SignedInAs(line) => line.trim
			case _ => "couldn't parse anything useful from the HTML"
		}
	}

	// Doesn't work yet.
	//val sessions = new SessionCache
}

/**
 * Maintains a JVM-level cache of each user's cookies, so signing in
 * as a user should be faster after the first time in a run - we can
 * just switch their cookies back in, simulating each user being online
 * at the same time in different browsers.
 */
class SessionCache {
	private var map = Map[String, Set[Cookie]]()

	// Get HTMLUnit WebClient if one is available.
	def getWebClient(driver: WebDriver): Option[WebClient] = driver match {
		case hud: HtmlUnitDriver =>
			val getWebClient = hud.getClass.getDeclaredMethod("getWebClient")
			getWebClient.setAccessible(true)
			Some(getWebClient.invoke(hud).asInstanceOf[WebClient])
//		case _ =>
	}

	// Clears ALL cookies, not just the current page's.
	def clearCookies(webDriver: WebDriver) {
		getWebClient(webDriver).foreach { wc =>
			wc.getCookieManager.clearCookies()
		}
	}

	/** If we have stored cookies and are using HtmlUnit, we can
		* dig in to the cookie manager and reinsert those cookies
		* without having to sign in again.
		*/
	def retrieve(usercode: String, webDriver: WebDriver) {
		getWebClient(webDriver).foreach { wc =>
			map.get(usercode).foreach { cookies =>
				val cm = wc.getCookieManager
				cm.clearCookies()
				cookies.foreach { cookie =>
					cm.addCookie(new htmlunit.util.Cookie(
						cookie.getDomain,
						cookie.getName,
						cookie.getValue,
						cookie.getPath,
						cookie.getExpiry,
						false
					))
				}
			}
		}

	}

	def store(usercode: String, webDriver: WebDriver) {
		map += (usercode -> webDriver.manage().getCookies.asScala.toSet)
	}
}

trait WebsignonMethods extends Matchers  with Eventually{
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

			def to(url: String) {
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
								(cssSelector("div.sign-out a").findElement.isDefined && cssSelector("div.sign-out a").findElement.get.isDisplayed) should be {true}
							}
							click on cssSelector("div.sign-out a")
							eventually {
								(className("sso-link").findElement.isDefined && className("sso-link").findElement.get.attribute("class").exists(_.contains("sign-in"))) should be {true}
							}
							click on className("sso-link")
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
						}else if (pageTitle startsWith "Sign in - Access refused"){
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
								fail("No Sign in or out links! URL:"+currentUrl)
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

			private def login(url: String) = {
				// wait for the page to load
				eventually(timeout(10.seconds), interval(200.millis))(
					pageTitle should include("Sign in")
				)
				textField("userName").value = details.usercode
				pwdField("password").value = details.password
				submit()

				// Sign-out operations redirect you to the context root, so we may now be on the wrong page...
				if (currentUrl != url){
					go to url
				}
			}
    }
	}



}