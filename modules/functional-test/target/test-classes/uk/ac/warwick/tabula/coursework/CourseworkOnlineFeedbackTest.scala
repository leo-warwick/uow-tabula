package uk.ac.warwick.tabula.coursework

import org.joda.time.DateTime
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.{BrowserTest, Download}

class CourseworkOnlineFeedbackTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {
	final val moduleCode = "XXX01"
	final val assignmentName = "Online marking for Dummies"

	private def setup(assignmentId: String) {
		submitAssignment(P.Student1, moduleCode, assignmentName, assignmentId, "/file1.txt")
		submitAssignment(P.Student2, moduleCode, assignmentName, assignmentId, "/file2.txt")
	}

	"Online marker" should "be able to see a sequence of submissions to mark" in {
		withAssignment(moduleCode = moduleCode, assignmentName = assignmentName, assistants = Seq(P.Marker1.usercode)) {
			Given("We have two students with submitted assignments")
			assignmentId => setup(assignmentId)

				as(P.Marker1) {
					When("I go to the marking page")
					go to Path("/coursework/admin/module/" + moduleCode.toLowerCase + "/assignments/" + assignmentId + "/feedback/online")

					Then("I see the table of students")
					pageSource contains s"Online marking" should be {true}
					pageSource contains s"$assignmentName ($moduleCode)" should be {true}
					findAll(cssSelector(".content-container")).size should be(2)
					pageSource contains P.Student1.usercode should be{true}
					pageSource contains P.Student2.usercode should be{true}
				}
		}
	}

	"Online marker" should "be able to feedback on an absent submission" in {
		withAssignment(moduleCode = moduleCode, assignmentName = assignmentName, members = Seq(P.Student3.usercode), assistants = Seq(P.Marker1.usercode)) {
			Given("We have one registered student")
			assignmentId => as(P.Marker1) {
				When("I go to the marking page")
				go to Path("/coursework/admin/module/" + moduleCode.toLowerCase + "/assignments/" + assignmentId + "/feedback/online")

				Then("I see the table of students")
				pageSource contains s"Online marking" should be {true}
				pageSource contains s"$assignmentName ($moduleCode)" should be {true}
				findAll(cssSelector(".content-container")).size should be (1)
				pageSource contains P.Student3.usercode should be {true}
			}
		}
	}

	"Student" should "be able to view feedback on their submission" in {
		withAssignment(moduleCode = moduleCode, assignmentName = assignmentName, assistants = Seq(P.Marker1.usercode)) {

			createStudentMember(P.Student1.usercode, "F", "xx123", 1,"Ux123")

			Given("We have two students with submitted assignments")
			assignmentId => setup(assignmentId)

				And("The assignment is closed for further submission")
				updateAssignment("xxx", assignmentName, Some(DateTime.now.minusDays(2)), Some(DateTime.now.minusDays(1)))

				as(P.Marker1) {
					And("I go to the marking page")
					go to Path("/coursework/admin/module/" + moduleCode.toLowerCase + "/assignments/" + assignmentId + "/feedback/online")

					And("I give feedback for student 1")
					val toggleLink = findAll(cssSelector("h6.toggle-icon")).filter(e=>e.text.contains(P.Student1.usercode)).next()
					toggleLink.underlying.click()
					eventuallyAjax(getInputByLabel("Feedback") should be ('defined))

					textArea(cssSelector(s"#content-${P.Student1.usercode} textarea")).value = "That was RUBBISH"
					textField(cssSelector(s"#content-${P.Student1.usercode} input#mark")).value="12"
					textField(cssSelector(s"#content-${P.Student1.usercode} input#grade")).value="F"
					find(cssSelector(s"#content-${P.Student1.usercode} input.btn-primary")).get.underlying.click()

					And("I go to the marking page")
					go to Path("/coursework/admin/module/" + moduleCode.toLowerCase + "/assignments/" + assignmentId + "/feedback/online")

					And("I give feedback for student 2")
					val toggleLink2 = findAll(cssSelector("h6.toggle-icon")).filter(e=>e.text.contains(P.Student2.usercode)).next()
					toggleLink2.underlying.click()
					eventuallyAjax(getInputByLabel("Feedback") should be ('defined))

					textArea(cssSelector(s"#content-${P.Student2.usercode} textarea")).value = "That was Awesome"
					textField(cssSelector(s"#content-${P.Student2.usercode} input#mark")).value="98"
					textField(cssSelector(s"#content-${P.Student2.usercode} input#grade")).value="A"
					find(cssSelector(s"#content-${P.Student2.usercode} input.btn-primary")).get.underlying.click()
				}
			  as(P.Admin1) {

					// We don't support adjusting outside of a workflow yet
//					And("I adjust Student2's feedback")
//					go to Path("/coursework/admin/module/" + moduleCode.toLowerCase + "/assignments/" + assignmentId + "/summary")
//					checkbox(cssSelector(s".collection-checkbox[value='${P.Student2.warwickId}']")).select()
//
//					val feedbackToolbarLink = findAll(linkText("Feedback")).next().underlying
//					feedbackToolbarLink.click()
//					eventually(click on feedbackToolbarLink.findElement(By.partialLinkText("Adjustments")))
//
//					eventually(find(cssSelector(s"#content-${P.Student2.warwickId} .toggle-cell")).get.underlying.click())
//					eventuallyAjax(getInputByLabel("Reason for adjustment") should be ('defined))
//					singleSel(cssSelector(s"#reason")).value = "Plagarism penalty"
//					textArea(cssSelector(s"#comments")).value = "Plagarism - Student copied Herons"
//					textField(cssSelector(s"#adjustedMark")).value="79"
//					textField(cssSelector(s"#adjustedGrade")).value="A"
//					find(cssSelector(s"#content-${P.Student2.warwickId} input.btn-primary")).get.underlying.click()

					And("I publish feedback")
					go to Path("/coursework/admin/module/" + moduleCode.toLowerCase + "/assignments/" + assignmentId + "/publish")
					checkbox(cssSelector("#confirmCheck")).select()
					eventually(
						find(cssSelector("#publish-submit")).get.underlying should be ('enabled)
					)
					find(cssSelector("#publish-submit")).get.underlying.click()
				}

				When("I log in as student1 and go to the feedback page")
			  as(P.Student1){
					go to Path(s"/coursework/module/${moduleCode.toLowerCase}/$assignmentId")
					Then("I see the feedback")
					pageSource should include("That was RUBBISH")
				}
				And("I can download the results as a PDF")

				val pdfDownload = Download(Path(s"/coursework/module/${moduleCode.toLowerCase}/$assignmentId/${P.Student1.usercode}/feedback.pdf")).as(P.Student1)
				pdfDownload should be ('successful)
				pdfDownload.contentAsString should include("PDF")

				When("I log in as student2 and go to the feedback page")
				as(P.Student2){
					go to Path(s"/coursework/module/${moduleCode.toLowerCase}/$assignmentId")
					Then("I see the feedback")
					pageSource should include("98")
				}

				as(P.Admin1) {
					Then("The assignment is archived")
					go to Path("/coursework/admin/module/" + moduleCode.toLowerCase + "/assignments/" + assignmentId + "/archive")
					pageSource should include("Archive this assignment")
					find(cssSelector("input[type='submit'][value=Archive]")).get.underlying.click()
					eventually {
						pageSource should include("Successful")
					}
					go to Path("/coursework/admin/module/" + moduleCode.toLowerCase + "/assignments/" + assignmentId + "/archive")
					pageSource should include("Unarchive this assignment")
				}

				When("I log back in as student1 and go to the feedback page")
				as(P.Student1){
					go to Path(s"/coursework/module/${moduleCode.toLowerCase}/$assignmentId")
					Then("I still see the feedback, even though the assignmnet has now been archived")
					pageSource should include("That was RUBBISH")
				}
		}
	}
}