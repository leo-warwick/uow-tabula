package uk.ac.warwick.tabula

import org.openqa.selenium.{By, WebDriver}
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.selenium.WebBrowser

trait BreadcrumbsMatcher extends Matchers {

  this: WebBrowser =>

  def breadCrumbsMatch(crumbsToMatch: Seq[String])(implicit webDriver: WebDriver): Unit = {
    val crumbs = findAll(cssSelector("ul#primary-navigation li")).toSeq
    val crumbText = crumbs.map(e => e.underlying.findElement(By.tagName("a")).getText)
    withClue(s"$crumbText should be $crumbsToMatch}") {
      crumbs.size should be(crumbsToMatch.size)
      crumbText should be(crumbsToMatch)
    }
  }

  def breadCrumbsMatchID7(crumbsToMatch: Seq[String])(implicit webDriver: WebDriver): Unit = {
    val crumbs = findAll(cssSelector(".navbar-secondary ul li.nav-breadcrumb")).filter(_.isDisplayed).toSeq
    val crumbText = crumbs.map(e => e.underlying.findElement(By.tagName("a")).getText)
    withClue(s"$crumbText should be $crumbsToMatch}") {
      crumbs.size should be(crumbsToMatch.size)
      crumbText should be(crumbsToMatch)
    }
  }
}
