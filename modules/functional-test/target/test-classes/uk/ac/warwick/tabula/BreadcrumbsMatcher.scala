package uk.ac.warwick.tabula

import org.openqa.selenium.{WebDriver, By}
import org.scalatest.selenium.WebBrowser
import org.scalatest.Matchers

trait BreadcrumbsMatcher extends Matchers {

	this:WebBrowser=>

	def breadCrumbsMatch(crumbsToMatch:Seq[String])(implicit webDriver:WebDriver){
		val crumbs = findAll(cssSelector("ul#primary-navigation li")).toSeq
		val crumbText = crumbs.map(e=>e.underlying.findElement(By.tagName("a")).getText)
		withClue(s"$crumbText should be $crumbsToMatch}") {
			crumbs.size should be (crumbsToMatch.size)
			crumbText should be (crumbsToMatch)
		}
	}

	def breadCrumbsMatchID7(crumbsToMatch:Seq[String])(implicit webDriver:WebDriver){
		val crumbs = findAll(cssSelector(".navbar-secondary ul li.nav-breadcrumb")).filter(_.isDisplayed).toSeq
		val crumbText = crumbs.map(e=>e.underlying.findElement(By.tagName("a")).getText)
		withClue(s"$crumbText should be $crumbsToMatch}") {
			crumbs.size should be (crumbsToMatch.size)
			crumbText should be (crumbsToMatch)
		}
	}
}
