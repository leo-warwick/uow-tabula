package uk.ac.warwick.tabula.coursework

import scala.collection.JavaConverters._
import org.scalatest.{GivenWhenThen, BeforeAndAfter, BeforeAndAfterAll}
import uk.ac.warwick.tabula.BrowserTest
import org.openqa.selenium.By

class CourseworkDisplaySettingsTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	"Department admin" should "be able to set display settings for a department" in as(P.Admin1) {
		click on linkText("Go to the Test Services admin page")
		openDisplaySettings()
		
		checkbox("showStudentName").select()
		
		submit()
		
		// Ensure that we've been redirected back to the dept admin page
		currentUrl should endWith ("/department/xxx/")
		
		// Check that when we go back to the page, all of the settings have been populated
		openDisplaySettings()
		
		checkbox("showStudentName").isSelected should be (true)
	}

	def openDisplaySettings() = {
		click on (cssSelector(".dept-settings a.dropdown-toggle"))

		val displayLink = cssSelector(".dept-settings .dropdown-menu").webElement.findElement(By.partialLinkText(" Settings"))
		eventually {
			displayLink.isDisplayed should be (true)
		}
		click on (displayLink)
	}

}