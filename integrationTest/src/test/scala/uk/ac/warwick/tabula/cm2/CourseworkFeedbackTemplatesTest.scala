package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import uk.ac.warwick.tabula.BrowserTest

import scala.jdk.CollectionConverters._

class CourseworkFeedbackTemplatesTest extends BrowserTest with CourseworkFixtures {

  //check to see how many template files are currently showing on screen
  def currentCount(): Int = {
    if (id("feedback-template-list").findElement.isEmpty) 0
    else id("feedback-template-list").webElement.findElement(By.tagName("tbody")).findElements(By.tagName("tr")).size
  }

  "Department admin" should "be able to manage feedback templates" in as(P.Admin1) {
    click on linkText("Test Services")

    def openFeedbackTemplates(): Unit = {
      click on partialLinkText("Feedback")

      val feedbackTemplatesLink = id("main").webElement.findElement(By.linkText("Feedback templates"))
      eventually {
        feedbackTemplatesLink.isDisplayed should be(true)
      }
      click on feedbackTemplatesLink
    }

    Then("I should be able to open feedback templates page")
    openFeedbackTemplates()

    var currCnt = currentCount()

    def uploadNewTemplate(file: String): Unit = {
      val currentCount =
        if (id("feedback-template-list").findElement.isEmpty) 0
        else id("feedback-template-list").webElement.findElement(By.tagName("tbody")).findElements(By.tagName("tr")).size

      find(id("file.upload")).get.underlying.sendKeys(getClass.getResource(file).getFile)

      click on cssSelector(".btn-primary")

      eventually {

        // We make sure that we haven't left the page
        currentUrl should endWith("/settings/feedback-templates/")

        // Check that we have one more row in the feedback template list
        id("feedback-template-list").webElement.findElement(By.tagName("tbody")).findElements(By.tagName("tr")).size should be(currentCount + 1)

      }
    }

    Then("I should be able to upload a new template")
    uploadNewTemplate("/file1.txt")
    currCnt = currentCount()

    And("a row should be displayed for the new template")
    currCnt should be(1)

    Then("I should be able to upload a second template")
    // Just so we have two to work with, let's upload a second file as well
    uploadNewTemplate("/file2.txt")
    currCnt = currentCount()

    And("a row should be displayed for the new template")
    currCnt should be(2)

    def downloadTemplate(file: String): Unit = {

      val tbody = id("feedback-template-list").webElement.findElement(By.tagName("tbody"))
      val row = tbody.findElements(By.tagName("tr")).asScala.find({
        _.findElement(By.tagName("td")).getText == file
      })
      row should be(defined)

      val providedFileUrl: String = row.get.findElement(By.cssSelector(".btn-primary")).getAttribute("href")
      providedFileUrl should endWith(file)

      eventually {

        // make sure that we haven't left the page
        currentUrl should endWith("/settings/feedback-templates/")

      }
    }

    Then("I should be able to download the first template")
    downloadTemplate("file1.txt")
    currCnt = currentCount()

    And("count should still be the same")
    currCnt should be(2)

    // Edit template 1. The rows actually appear in db insert order, so we need to find the right row first
    def editTemplate(file: String): Unit = {
      val tbody = id("feedback-template-list").webElement.findElement(By.tagName("tbody"))
      val row = tbody.findElements(By.tagName("tr")).asScala.find({
        _.findElement(By.tagName("td")).getText == file
      })

      click on row.get.findElement(By.partialLinkText("Edit"))

      eventually {
        find("feedback-template-model").map(_.isDisplayed) should be(Some(true))

        val ifr = find(cssSelector(".modal-body iframe"))
        ifr.map(_.isDisplayed) should be(Some(true))
      }

      val iframe = frame(find(cssSelector(".modal-body iframe")).get)
      switch to iframe

      eventually{
        textField(name("name")).isDisplayed should be(true)
        textField(name("name")).isEnabled should be(true)
      }

      // Set a name and description TODO check update
      textField("name").value = "extension template"
      textArea("description").value = "my extension template"

      switch to defaultContent

      eventually {
        cssSelector("#feedback-template-model .btn-primary").webElement.isDisplayed shouldBe true
        cssSelector("#feedback-template-model .btn-primary").webElement.isEnabled shouldBe true
        click on cssSelector("#feedback-template-model .btn-primary")
      }

      // Wait for the modal to go away
      eventually {
        find("feedback-template-model").map(_.isDisplayed) should be(Some(false))
      }
    }

    Then("I should be able to edit the first template")
    editTemplate("file1.txt")

    And("Values should have been updated")
    var tbody = id("feedback-template-list").webElement.findElement(By.tagName("tbody"))
    var row = tbody.findElements(By.tagName("tr")).asScala.find(_.findElement(By.tagName("td")).getText == "extension template")
    val file1names = row.map(_.findElements(By.tagName("td")).asScala).getOrElse(Seq()).map(_.getText).toSet
    file1names should be(Set("extension template", "my extension template", "None", "Download Edit Delete"))

    var beforeDelete = currentCount()

    // This works, but it doesn't reload the page automatically properly. Do it manually
    reloadPage()

    And("The second template should not have changed")
    tbody = id("feedback-template-list").webElement.findElement(By.tagName("tbody"))
    row = tbody.findElements(By.tagName("tr")).asScala.find({
      _.findElement(By.tagName("td")).getText == "file2.txt"
    })
    val file2names = row.map(_.findElements(By.tagName("td")).asScala).getOrElse(Seq()).map(_.getText).toSet
    file2names should be(Set("file2.txt", "", "None", "Download Edit Delete"))
    currCnt = currentCount()

    And("count should still be the same")
    currCnt should be(2)

    // Delete the file2.txt template
    def deleteFile(file: String): Unit = {
      val tbody = id("feedback-template-list").webElement.findElement(By.tagName("tbody"))
      val row = tbody.findElements(By.tagName("tr")).asScala.find({
        _.findElement(By.tagName("td")).getText == file
      })
      row should be(Symbol("defined"))

      click on row.get.findElement(By.partialLinkText("Delete"))
    }

    Then("I should be able to delete the second file")
    deleteFile("file2.txt")

    eventually {
      find("feedback-template-model").map(_.isDisplayed) should be(Some(true))

      val ifr = find(cssSelector(".modal-body iframe"))
      ifr.map(_.isDisplayed) should be(Some(true))
    }

    val iframe = frame(find(cssSelector(".modal-body iframe")).get)
    switch to iframe
    eventually(pageSource contains "Are you sure that you want to delete this feedback template?" should be (true))

    switch to defaultContent

    click on cssSelector("#feedback-template-model .btn-primary")

    // Wait for the modal to go away
    eventually {
      find("feedback-template-model").map(_.isDisplayed) should be(Some(false))
    }

    And("File should have been deleted so count should be 1 less")
    eventually(id("feedback-template-list").webElement.findElement(By.tagName("tbody")).findElements(By.tagName("tr")).size should be(currCnt - 1))
  }
}
