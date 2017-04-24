package uk.ac.warwick.tabula.coursework


import org.openqa.selenium.By
import uk.ac.warwick.tabula.BrowserTest

import scala.collection.JavaConverters._

class CourseworkFeedbackTemplatesTest extends BrowserTest with CourseworkFixtures {

	def currentCount() = {
		if (id("feedback-template-list").findElement.isEmpty) 0
		else id("feedback-template-list").webElement.findElement(By.tagName("tbody")).findElements(By.tagName("tr")).size
	}

	"Department admin" should "be able to manage feedback templates" in as(P.Admin1) {

		click on linkText("Go to the Test Services admin page")

		def openFeedbackTemplates() = {
			click on (cssSelector(".dept-settings a.dropdown-toggle"))

			val feedbackTemplatesLink = cssSelector(".dept-settings .dropdown-menu").webElement.findElement(By.partialLinkText("Feedback templates"))
			eventually {
				feedbackTemplatesLink.isDisplayed should be(true)
			}
			click on (feedbackTemplatesLink)
		}

		openFeedbackTemplates()

		var currCnt = currentCount()

		def uploadNewTemplate(file: String) {

			currCnt = currentCount()

			ifPhantomJSDriver(
				operation = { d =>
					// This hangs forever for some reason in PhantomJS if you use the normal pressKeys method
					d.executePhantomJS("var page = this; page.uploadFile('input[type=file]', '" + getClass.getResource(file).getFile + "');")
				},
				otherwise = { _ =>
					click on getInputByLabel("Upload feedback forms").get
					pressKeys(getClass.getResource(file).getFile)
				}
			)

			click on cssSelector(".btn-primary")

			eventually {
				// We make sure that we haven't left the page
				currentUrl should endWith("/settings/feedback-templates/")

				// Check that we have one more row in the feedback template list
				id("feedback-template-list").webElement.findElement(By.tagName("tbody")).findElements(By.tagName("tr")).size should be(currCnt + 1)
			}
		}

		uploadNewTemplate("/file1.txt")
		currCnt = currentCount()

		// Just so we have two to work with, let's upload a second file as well
		uploadNewTemplate("/file2.txt")
		currCnt = currentCount()

		// TODO Check that clicking the download links work

		// Edit template 1. The rows actually appear in db insert order, so we need to find the right row first
		{
			val tbody = id("feedback-template-list").webElement.findElement(By.tagName("tbody"))
			val row = tbody.findElements(By.tagName("tr")).asScala.find({
				_.findElement(By.tagName("td")).getText == "file1.txt"
			})
			row should be('defined)

			click on (row.get.findElement(By.partialLinkText("Edit")))
		}

		eventuallyAjax {
			find("feedback-template-model") map {
				_.isDisplayed
			} should be(Some(true))

			val ifr = find(cssSelector(".modal-body iframe"))
			ifr map {
				_.isDisplayed
			} should be(Some(true))
		}

		switch to frame(find(cssSelector(".modal-body iframe")).get)

		// Set a name and description TODO check update
		textField("name").value = "extension template"
		textArea("description").value = "my extension template"
		submit

		switch to defaultContent

		var beforeDelete = currentCount()

		// This works, but it doesn't reload the page automatically properly. Do it manually
		reloadPage

		{
			val tbody = id("feedback-template-list").webElement.findElement(By.tagName("tbody"))

			val names = tbody.findElements(By.tagName("tr")).asScala.map({
				_.findElement(By.tagName("td")).getText
			}).toSet[String]
			names should be(Set("extension template", "file2.txt"))
		}

		// Delete the file2.txt template
		{
			beforeDelete = currentCount()

			val tbody = id("feedback-template-list").webElement.findElement(By.tagName("tbody"))
			val row = tbody.findElements(By.tagName("tr")).asScala.find({
				_.findElement(By.tagName("td")).getText == "file2.txt"
			})
			row should be('defined)

			click on (row.get.findElement(By.partialLinkText("Delete")))
		}

		eventuallyAjax {
			find("feedback-template-model") map {
				_.isDisplayed
			} should be(Some(true))

			val ifr = find(cssSelector(".modal-body iframe"))
			ifr map {
				_.isDisplayed
			} should be(Some(true))
		}

		switch to frame(find(cssSelector(".modal-body iframe")).get)

		executeScript("jQuery('#deleteFeedbackTemplateCommand').submit()")

		switch to defaultContent

		// This works, but it doesn't reload the page automatically properly. Do it manually
		reloadPage

		id("feedback-template-list").webElement.findElement(By.tagName("tbody")).findElements(By.tagName("tr")).size should be (beforeDelete-1)

  }

}