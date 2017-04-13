package uk.ac.warwick.tabula.coursework

import uk.ac.warwick.tabula.BrowserTest
import org.openqa.selenium.By

class CourseworkExtensionsTest extends BrowserTest with CourseworkFixtures {

	val ExtensionGuidelines = "You must be dying of something awful"
	val ExtensionInfoUrl = "http://warwick.ac.uk/"

	"Department admin" should "be able to enable extensions" in as(P.Admin1) {
		click on linkText("Go to the Test Services admin page")

		def openExtensionSettings() = {
			click on (cssSelector(".dept-settings a.dropdown-toggle"))

			val extensionsLink = cssSelector(".dept-settings .dropdown-menu").webElement.findElement(By.partialLinkText("Extension settings"))
			eventually {
				extensionsLink.isDisplayed should be (true)
			}
			click on (extensionsLink)
		}

		openExtensionSettings()

		checkbox("allowExtensionRequests").select()

		eventually {
			textArea("extensionGuidelineSummary").isDisplayed should be (true)
		}

		textArea("extensionGuidelineSummary").value = ExtensionGuidelines
		textField("extensionGuidelineLink").value = ExtensionInfoUrl
		textField("extensionManagers").value = P.ExtensionManager1.usercode

		submit()

		// Ensure that we've been redirected back to the dept admin page
		currentUrl should endWith ("/department/xxx/")

		// Check that when we go back to the page, all of the settings have been populated
		openExtensionSettings()

		checkbox("allowExtensionRequests").isSelected should be (true)
		textArea("extensionGuidelineSummary").isDisplayed should be (true)
		textArea("extensionGuidelineSummary").value should be(ExtensionGuidelines)
		textField("extensionGuidelineLink").value should be (ExtensionInfoUrl)
		textField("extensionManagers").value should be (P.ExtensionManager1.usercode)
	}

}