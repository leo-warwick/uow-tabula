package uk.ac.warwick.tabula.cm2

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.SingleMarking

class OnlineMarkingTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

  var assignmentId: String = _

  override val moreBefore: () => Unit = () => {
    // make an assignment and release it for marking
    withAssignmentWithWorkflow(SingleMarking, Seq(P.Marker1, P.Marker2)) { id =>
      assignmentId = id
      releaseForMarking(id)
    }
  }

  "Marker" should "be able to mark work and send to next step" in {
    as(P.Marker1) {
      When("I click to mark the assignment")
      eventually({
        val mark = partialLinkText("Mark").webElement
        mark.isDisplayed should be (true)
        click on mark
      })
      Then("I am redirected to the summary screen ")
      eventually(currentUrl should include(s"/admin/assignments/$assignmentId/marker/"))

      When("I expand the student")
      eventually(click on cssSelector(".toggle-icon-large.student-col").webElement)
      Then("Marking and feedback")
      eventually(pageSource contains "Marking and feedback" should be (true))

      When("I fill in the online feedback form with an invalid mark")
      textArea(xpath("//textarea[@rows='6']")).value = "An inspired treatise on the dangers posed by Ardea cinerea. They are truly one of the greatest threats our world faces."
      numberField("mark").value = "9000"
      textField("grade").value = "1"
      cssSelector("button[type=submit]").webElement.click()
      Then("I see a validation error")
      eventually(pageSource contains "Marks must be between 0 and 100" should be (true))

      When("I correct the mistake and submit the form")
      numberField("mark").value = "90"
      cssSelector(s"button[type=submit]").webElement.click()
      Then("The row will collapse")
      eventually(cssSelector(s"tr.collapsed").webElement.isDisplayed should be (true))

      go to Path("/coursework")
      When("I click to mark the assignment")
      eventually({
        val mark = partialLinkText("Mark").webElement
        mark.isDisplayed should be (true)
        click on mark
      })
      Then("I am redirected to the summary screen ")
      eventually(currentUrl should include(s"/admin/assignments/$assignmentId/marker/"))

      When("I expand the student again")
      eventually(cssSelector(".toggle-icon-large.student-col").webElement.isDisplayed shouldBe true)
      click on cssSelector(".toggle-icon-large.student-col").webElement
      Then("The feedback I entered before should be present")
      eventually(pageSource contains "Marking and feedback" should be (true))
      numberField("mark").value should be("90")
      textField("grade").value should be("1")
      textArea(xpath("//textarea[@rows='6']")).value should be("An inspired treatise on the dangers posed by Ardea cinerea. They are truly one of the greatest threats our world faces.")

      When("I select the feedback and click send to ...")
      cssSelector("input[name=markerFeedback]").webElement.click()
      partialLinkText("Confirm selected and send to").webElement.click()
      Then("I end up on the send to confirmation page")
      eventually(pageSource contains "Send to admin" should be (true))

      When("I confirm and send to admin")
      cssSelector("input[name=confirm]").webElement.click()
      cssSelector("input[type=submit]").webElement.click()
      Then("I end up back on the marker overview page")
      eventually(currentUrl should include(s"/admin/assignments/$assignmentId/marker/"))
      eventually(xpath("//span[@data-title=\"Marked\"]/i[contains(@class, \"fa-check-circle-o\")]").webElement.getAttribute("class").contains("text-success") should be (true))
    }
  }

}
