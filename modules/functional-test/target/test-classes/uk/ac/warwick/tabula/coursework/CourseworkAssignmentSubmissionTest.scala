package uk.ac.warwick.tabula.coursework

import uk.ac.warwick.tabula.BrowserTest

class CourseworkAssignmentSubmissionTest extends BrowserTest with CourseworkFixtures {

	// TAB-413, TAB-415
	"Student" should "be able to submit assignment after validation errors without re-uploading file" in {
		withAssignment("xxx01", "Fully featured assignment") { assignmentId =>
			as(P.Student1) {
				click on linkText("Fully featured assignment")
				currentUrl should endWith(assignmentId + "/")

				click on (getInputByLabel("File").orNull)
				pressKeys(getClass.getResource("/file1.txt").getFile)

				new TextField(getInputByLabel("Word count").orNull).value = "1000"

				// Don't click the plagiarism detection button yet
				submit()

				pageSource contains "Thanks, we've received your submission." should be (false)

				id("plagiarismDeclaration.errors").webElement.isDisplayed() should be (true)
				pageSource contains "You must confirm that this submission is all your own work." should be (true)

				// Click the button and submit again
				checkbox("plagiarismDeclaration").select()

				submit()

				pageSource contains "Thanks, we've received your submission." should be (true)

				linkText("file1.txt").webElement.isDisplayed() should be (true)
			}
		}
	}

	"Student" should "be able to submit assignment" in {
		withAssignment("xxx01", "Fully featured assignment") { assignmentId =>
			submitAssignment(P.Student1, "xxx01", "Fully featured assignment", assignmentId, "/file1.txt")
		}
	}

}