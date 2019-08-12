package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import org.openqa.selenium.support.ui.Select
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest

class FeedbackAdjustmentsTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

  val adjustmentDescriptionText = "Your marks before adjustment were"

  private def adjustmentPage() = {
    When("I go to the adjustments page")
    eventually(className("collection-check-all").findElement.exists(_.isDisplayed) should be (true))
    click on className("collection-check-all")
    eventually(pageSource contains "Feedback" should be (true))
    click on linkText("Feedback")
    eventually(pageSource contains "Adjustments" should be (true))
    click on linkText("Adjustments")

    Then("I see a list of students")
    pageSource contains "Feedback adjustment" should be (true)
    pageSource contains "tabula-functest-student1" should be (true)
    pageSource contains "tabula-functest-student3" should be (true)
  }

  private def bulkAdjustmentPage(assignmentId: String) = {
    When("I click on the bulk adjustments button")
    click on linkText("Adjust in bulk")

    Then("I should see the bulk adjustment form")
    eventually(currentUrl should include(s"/coursework/admin/assignments/$assignmentId/feedback/bulk-adjustment"))

    Then("I upload a valid adjustments file")
    click on "file.upload"
    pressKeys(getClass.getResource("/adjustments.xlsx").getFile)

    And("submit the form")
    click on cssSelector(".btn-primary")

    Then("I should see the preview bulk adjustment page")
    eventually {
      pageSource contains "Preview bulk adjustment" should be (true)
    }

    Then("The hide from student checkbox should be selected by default")
    checkbox("privateAdjustment").isSelected should be(true)

  }

  "Admin" should "be able to make feedback adjustments" in {
    as(P.Admin1) {
      When("I go to the department admin page")
      go to Path("/coursework/admin/department/xxx")

      val module = eventually {
        getModule("XXX02").get
      }
      eventually(click on module.findElement(By.className("mod-code")))
      Then("I should see the premarked assignment CM2")
      eventually(pageSource contains "Premarked assignment CM2" should be (true))
      eventually(click on linkText("Premarked assignment CM2"))
      adjustmentPage

      When("I click on a student's ID")
      click on cssSelector("h6.toggle-icon")
      Then("I see the form and the student's current marks")
      eventually(pageSource contains "Original mark - 41" should be (true))

      When("I populate and submit the form")
      // as there is a hidden and disabled reason element on the same page we can't use the scala test singleSel
      val select = new Select(find(cssSelector("select[name=reason]")).get.underlying)
      select.selectByValue("Late submission penalty")
      textArea("comments").value = "Deducting 10 marks (5 marks per day)"
      numberField("adjustedMark").value = "31"
      find(cssSelector(s"#row-${P.Student1.usercode} button.btn-primary")).get.underlying.click()
      Then("the students marks get adjusted")

      eventually {
        id(s"row-${P.Student1.usercode}").webElement.isDisplayed should be(false)
      }

      When("I click on the student's ID again")
      click on cssSelector("h6.toggle-icon")
      Then("I see the form and the adusted mark")
      eventually(pageSource contains "Adjusted mark - 31" should be (true))

      click on partialLinkText("XXX02 Test Module 2")

      eventually(click on getModule("XXX02").get.findElement(By.className("mod-code")))

      When("I publish the feedback")
      // find feedback publishing link for cm2 related assignment (XXX02 module)
      val premarkedAssignmentFeedbackLink = eventually {
        id("main").webElement.findElement(By.xpath("//*[contains(text(),'Premarked assignment CM2')]"))
          .findElement(By.xpath("../../../../div[contains(@class, 'item-info')]")).findElement(By.linkText("Feedback needs publishing (2 of 2)"))
      }
      click on premarkedAssignmentFeedbackLink
      click on checkbox("confirm")
      cssSelector("div.submit-buttons button[type=submit]").findElement.get.underlying.click()
      Then("all is well in the world for all the Herons are in a deep slumber")
      eventually(pageSource contains "The feedback has been published." should be (true))
    }

    eventually(pageSource contains "Premarked assignment CM2" should be (true))
    eventually(click on partialLinkText("Premarked assignment CM2"))
    val assignmentId = currentUrl.split("/")(6)

    Then("The student can see the adjustment")
    as(P.Student1) {
      When("I visit the feedback page")
      go to Path(s"/coursework/submission/$assignmentId")
      Then("I can see the adjusted mark only")
      eventually(pageSource contains "Adjusted mark: 31" should be (true))
      pageSource contains "Mark: 41" should be (true)
    }

    When("Admin goes back in to make non-private adjustments")
    as(P.Admin1) {
      go to Path(s"/coursework/admin/assignments/$assignmentId")
      adjustmentPage()
      bulkAdjustmentPage(assignmentId)
      Then("I uncheck the hide from student checkbox")
      checkbox("privateAdjustment").clear()

      When("I submit the form")
      click on cssSelector("input.btn-primary")

      Then("I should get redirected back to the submissions summary page")
      eventually {
        currentUrl should include(s"/coursework/admin/assignments/$assignmentId/summary")
      }
    }

    Then("The student can see these adjustments")
    as(P.Student1) {
      When("I visit the feedback page")
      go to Path(s"/coursework/submission/$assignmentId")
      Then("I should see the adjustments")
      pageSource contains "Adjusted" should be (true)
      pageSource contains "Adjusted mark: 43" should be (true)
      pageSource contains "Adjusted grade: B" should be (true)
      pageSource contains adjustmentDescriptionText should be (true)
      pageSource contains "Mark: 41" should be (true)
      pageSource contains "Mark: 31" should be (true)
    }

    Then("Admin goes back in to make private adjustments")
    as(P.Admin1) {
      go to Path(s"/coursework/admin/assignments/$assignmentId")
      adjustmentPage()
      bulkAdjustmentPage(assignmentId)

      When("I submit the form")
      click on cssSelector("input.btn-primary")

      Then("I should get redirected back to the submissions summary page")
      eventually {
        currentUrl should include(s"/coursework/admin/assignments/$assignmentId/summary")
      }
    }

    Then("The student cannot see private adjustments or any previous adjustments")
    as(P.Student1) {
      When("I visit the feedback page")
      go to Path(s"/coursework/submission/$assignmentId")
      Then("I cannot see any adjustments as the last one was private")
      pageSource contains "Adjusted" should be (false)
      pageSource contains "Mark: 43" should be (true)
      pageSource contains "Grade: B" should be (true)
      pageSource contains adjustmentDescriptionText should be (false)
      pageSource contains "Mark: 41" should be (false)
      pageSource contains "Mark: 31" should be (false)
    }

  }
}