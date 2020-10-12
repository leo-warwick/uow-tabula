package uk.ac.warwick.tabula.groups.pages

import org.openqa.selenium.WebDriver
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.selenium.WebBrowser
import uk.ac.warwick.tabula.FunctionalTestProperties

class GroupsHomePage(implicit val webDriver: WebDriver) extends WebBrowser with Eventually with Matchers with ModuleAndGroupSetList {

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(30, Seconds), interval = Span(200, Millis))

  val url: String = FunctionalTestProperties.SiteRoot + "/groups/"

  def isCurrentPage: Boolean = {
    currentUrl should include("/groups/")
    pageTitle == "Tabula - Small Group Teaching"
  }


}
