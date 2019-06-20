package uk.ac.warwick.tabula.cm2

import org.joda.time.DateTime
import org.openqa.selenium.By
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.SingleMarking
import uk.ac.warwick.tabula.{AcademicYear, BrowserTest}

class ReplaceMarkerReusableWorkflowTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

  private val currentYear = AcademicYear.now()

  private def openModifyMarkerScreen(): Unit = {

    if (!currentUrl.contains("/department/xxx")) {
      When("I go the admin page")
      click on linkText("Test Services")
    }

    if (!currentUrl.contains("/department/xxx/$currentYear/markingworkflows")) {

      Then("I should be able to click on the Marking workflows option")
      val toolbar = findAll(className("dept-toolbar")).next().underlying
      click on toolbar.findElement(By.partialLinkText("Marking workflows"))

      val getCurrentYear = linkText(currentYear.previous.toString)
      click on getCurrentYear

      eventually {
        Then(s"I should be on the ${currentYear.previous} version of the page")
        currentUrl should include(s"/${currentYear.previous.startYear}/markingworkflows")
      }
      // only for single use?
      val addToYear = id("main").webElement.findElement(By.linkText(s"Add to $currentYear"))
      click on addToYear
    }

    eventually {
      Then("I should reach the marking workflows page")
      currentUrl should include(s"/${currentYear.startYear}/markingworkflows")
    }

  }

  private def replaceIncompleteMarker(): Unit = {
    When("I select to modify the Old single marker workflow")
    val currentMarker = id("main").webElement.findElements(By.tagName("td")).get(2).getText == "Marker: tabula-functest-marker1 user"
    currentMarker should be(true)

    val modifyBtn = id("main").webElement.findElement(By.cssSelector("td a.btn-default"))
    click on modifyBtn

    eventually {
      Then("I should reach the modify workflows options page")
      currentUrl should include("/edit")
    }

    val replaceLink = id("main").webElement.findElement(By.partialLinkText("Replace marker"))
    click on replaceLink

    eventually {
      Then("I should reach the modify workflows page")
      currentUrl should include("/replace")
    }

    When("I replace the marker with new marker")
    val markerToReplace2 = id("oldMarker").webElement
    click on markerToReplace2

    val oldMarker2 = markerToReplace2.findElements(By.tagName("option")).get(1)
    oldMarker2.isDisplayed should be(true)
    click on oldMarker2

    val newMarker = id("main").webElement.findElement(By.cssSelector("input[name=newMarker]"))
    newMarker.sendKeys("tabula-functest-marker2")

    val saveButton = cssSelector("input.btn-primary")
    click on saveButton

    eventually {
      Then("I should reach the modify workflows page again")
      currentUrl should include(s"/xxx/${currentYear.startYear}/markingworkflows")
    }

    And("Marker should be updated")
    val addedMarker = id("main").webElement.findElements(By.tagName("td")).get(2).getText == "Marker: tabula-functest-marker2 user"
    addedMarker should be(true)

  }

  private def replaceCompletedMarker(): Unit = {
    When("I select to modify the Single marker workflow")
    val currentMarker = id("main").webElement.findElements(By.tagName("td")).get(6).getText == "Marker: tabula-functest-marker1 user"
    currentMarker should be(true)

    val modifyBtn = id("main").webElement.findElements(By.cssSelector("td a.btn-default")).get(2)
    click on modifyBtn

    eventually {
      Then("I should reach the modify workflows options page")
      currentUrl should include("/edit")
    }

    val replaceLink = id("main").webElement.findElement(By.partialLinkText("Replace marker"))
    click on replaceLink

    eventually {
      Then("I should reach the modify workflows page")
      currentUrl should include("/replace")
    }

    When("I replace the marker with new marker")
    val markerToReplace = id("oldMarker").webElement
    click on markerToReplace

    val oldMarker = markerToReplace.findElement(By.cssSelector("option[value^=tabula-functest-marker]"))
    eventually {
      oldMarker.isDisplayed should be(true)
      click on oldMarker
    }

    val newMarker = id("main").webElement.findElement(By.cssSelector("input[name=newMarker]"))
    newMarker.sendKeys("tabula-functest-marker3")

    And("Confirm to replaced completed marking assignments")
    val replaceFinished = id("main").webElement.findElement(By.id("includeCompleted1"))
    click on replaceFinished

    replaceFinished.isSelected should be(true)

    And("Confirm awareness of needing to notify students")
    val confirmNotify = id("main").webElement.findElement(By.id("confirm1"))
    click on confirmNotify

    confirmNotify.isSelected should be(true)

    val saveButton = cssSelector("input.btn-primary")
    click on saveButton

    eventually {
      Then("I should reach the modify workflows page again")
      currentUrl should include(s"/xxx/${currentYear.startYear}/markingworkflows")
    }
    And("Marker should be updated")
    val addedMarker = id("main").webElement.findElements(By.tagName("td")).get(6).getText == "Marker: tabula-functest-marker3 user"
    addedMarker should be(true)

  }

  "Department admin" should "be able to modify markers in reusable workflows" in as(P.Admin1) {

    openModifyMarkerScreen()
    replaceIncompleteMarker()
    openModifyMarkerScreen()
    replaceCompletedMarker()

  }
}
