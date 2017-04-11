package uk.ac.warwick.tabula.groups.pages

import org.openqa.selenium.WebDriver
import org.scalatest.selenium.WebBrowser
import uk.ac.warwick.tabula.FunctionalTestProperties
import org.scalatest.Matchers
import uk.ac.warwick.tabula.EventuallyAjax

class GroupsHomePage (implicit val webDriver:WebDriver) extends WebBrowser with EventuallyAjax with Matchers with ModuleAndGroupSetList {
	val url: String = FunctionalTestProperties.SiteRoot + "/groups/"

	def isCurrentPage: Boolean =  {
		currentUrl should include("/groups/")
		pageTitle == "Tabula - Small Group Teaching"
	}



}
