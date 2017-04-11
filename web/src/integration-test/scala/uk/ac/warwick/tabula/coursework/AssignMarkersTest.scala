package uk.ac.warwick.tabula.coursework

import org.openqa.selenium.By
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest

class AssignMarkersTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	final val moduleCode = "XXX02"
	final val assignmentName = "Marking workflow for Dummies"

	private def setup(assignmentId: String) {
		submitAssignment(P.Student1, moduleCode, assignmentName, assignmentId, "/file1.txt", mustBeEnrolled = false)
		submitAssignment(P.Student2, moduleCode, assignmentName, assignmentId, "/file2.txt", mustBeEnrolled = false)
	}

	"Online marker" should "be able to see a sequence of submissions to mark" in {
		addSingleMarkingWorkflow() {
			workflowId =>

			go to Path("/coursework")
			def settings(params: Seq[String]) = {
				allFeatures(Seq(P.Student1.usercode, P.Student2.usercode))
				singleSel("markingWorkflow").value = workflowId
			}

			withAssignment(moduleCode = moduleCode, assignmentName = assignmentName, settings = settings, members = Seq(P.Student1.usercode, P.Student2.usercode), assistants = Seq(P.Marker1.usercode)) {

				Given("We have two students with submitted assignments")
				assignmentId => setup(assignmentId)

				as(P.Admin1) {
					When("I go to the summary view")
					go to Path("/coursework/admin/module/" + moduleCode.toLowerCase + "/assignments/" + assignmentId + "/summary")
					And("I click on the assign markers link")
					val toolbar = findAll(cssSelector(".btn-toolbar")).next().underlying
					toolbar.findElement(By.partialLinkText("Marking")).click()
					Then("I see the marking dropdown")
					pageSource contains s"Assign markers" should be {true}

					go to Path("/coursework/admin/module/" + moduleCode.toLowerCase + "/assignments/" + assignmentId + "/assign-markers")
					Then("I see the assign marking UI")
					pageSource contains s"Assign students to markers" should be {true}

					When("I randomly assign the markers")
					find(cssSelector(".random ")).get.underlying.click()
					Then("The two markers should be assigned")
					eventually(find(cssSelector(".drag-count")).get.underlying.getText should be ("2"))

					When("I submit the marker allocation")
					find(cssSelector(".btn-primary")).get.underlying.click()
					Then("I am redirected to the dept admin screen ")
					eventually(currentUrl should include("/coursework/admin/department/xxx"))
					go to Path("/coursework/admin/module/" + moduleCode.toLowerCase + "/assignments/" + assignmentId + "/summary")

				}
			}
		}
	}

}
