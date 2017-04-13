package uk.ac.warwick.tabula.coursework

import org.openqa.selenium.support.ui.Select
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest

class FeedbackAdjustmentsTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	val adjustmentDescriptionText = "Your marks before adjustment were"

	"Admin" should "be able to make feedback adjustments" in {

		var assignmentId: String = null

		as(P.Admin1) {
			When("I go to the department admin page")
			go to Path("/coursework/admin/department/xxx/#module-xxx02")
			Then("I should see the premarked assignment")
			eventually(pageSource contains "Premarked assignment" should be {true})
			click on linkText("2 submissions and 2 items of feedback")

			When("I go to the adjustments page")
			// For some dumb reason all of the hrefs in the toolbar links are stripped out by htmlunit lame!
			// If this can be fixed then the following three lines are a better way of getting to the adjustment page
			// val toolbar = eventually(findAll(cssSelector(".btn-toolbar")).next().underlying)
			// eventually(toolbar.findElement(By.partialLinkText("Feedback")).click())
			// eventually(toolbar.findElement(By.partialLinkText("Adjustments")).click())

			assignmentId = currentUrl.split("/").reverse.toList match {
				case _ :: id :: list => id
				case _ => ""
			}
			go to Path(s"/coursework/admin/module/xxx02/assignments/$assignmentId/feedback/adjustments")
			Then("I see a list of students")
			pageSource contains "Feedback adjustment" should be {true}
			pageSource contains "tabula-functest-student1" should be {true}
			pageSource contains "tabula-functest-student3" should be {true}

			When("I click on a student's ID")
			click on cssSelector("h6.toggle-icon")
			Then("I see the form and the student's current marks")
			eventuallyAjax(pageSource contains "Original mark - 41" should be {true})

			When("I populate and submit the form")
			// as there is a hidden and disabled reason element on the same page we can't use the scala test singleSel
			val select = new Select(find(cssSelector("select[name=reason]")).get.underlying)
			select.selectByValue("Late submission penalty")
			textArea("comments").value = "Deducting 10 marks (5 marks per day)"
			textField("adjustedMark").value = "31"
			find(cssSelector(s"#content-${P.Student1.warwickId} input.btn-primary")).get.underlying.click()
			Then("the students marks get adjusted")

			When("I click on the student's ID again")
			click on cssSelector("h6.toggle-icon")
			Then("I see the form and the adusted marks are pre-populated")
			eventuallyAjax(textField("adjustedMark").value should be ("31"))

			When("I publish the feedback")
			go to Path(s"/coursework/admin/module/xxx02/assignments/$assignmentId/publish")
			checkbox("confirm").select
			find(cssSelector("input.btn-primary")).get.underlying.click()
			Then("all is well in the world for all the Herons are in a deep slumber")
			eventually(pageSource contains "Published feedback for Premarked assignment" should be {true})
		}

		Then("The student can see the adjustment")
		as(P.Student1) {
			When("I visit the feedback page")
			go to Path(s"/coursework/module/xxx02/$assignmentId")
			Then("I can see the adjusted mark only")
			pageSource contains "Adjusted mark: 31" should be {true}
			pageSource contains "Mark: 41" should be {true}
		}

		When("Admin goes back in to make non-private adjustments")
		as(P.Admin1) {
			go to Path(s"/coursework/admin/module/xxx02/assignments/$assignmentId/feedback/adjustments")
			Then("I see a list of students")
			pageSource contains "Feedback adjustment" should be {true}
			pageSource contains "tabula-functest-student1" should be {true}
			pageSource contains "tabula-functest-student3" should be {true}

			When("I click on the bulk adjustments button")
			click on linkText("Adjust in bulk")

			Then("I should see the bulk adjustment form")
			eventually(currentUrl should include(s"/coursework/admin/module/xxx02/assignments/$assignmentId/feedback/bulk-adjustment"))

			Then("I upload a valid adjustments file")
			click on "file.upload"
			pressKeys(getClass.getResource("/adjustments.xlsx").getFile)

			And("submit the form")
			click on cssSelector(".btn-primary")

			Then("I should see the preview bulk adjustment page")
			eventually {
				pageSource contains "Preview bulk adjustment" should be {true}
			}

			Then("The hide from student checkbox should be selected by default")
			checkbox("privateAdjustment").isSelected should be (true)

			Then("I uncheck the hide from student checkbox")
			checkbox("privateAdjustment").clear()

			When("I submit the form")
			click on cssSelector("input.btn-primary")

			Then("I should get redirected back to the submissions summary page")
			eventually {
				currentUrl should include(s"/coursework/admin/module/xxx02/assignments/$assignmentId/summary")
			}
		}

		Then("The student can see these adjustments")
		as(P.Student1) {
			When("I visit the feedback page")
			go to Path(s"/coursework/module/xxx02/$assignmentId")
			Then("I should see the adjustments")
			pageSource contains "Adjusted" should be {true}
			pageSource contains "Adjusted mark: 43" should be {true}
			pageSource contains "Adjusted grade: B" should be {true}
			pageSource contains adjustmentDescriptionText should be {true}
			pageSource contains "Mark: 41" should be {true}
			pageSource contains "Mark: 31" should be {true}
		}

		Then("Admin goes back in to make private adjustments")
		as(P.Admin1) {
			go to Path(s"/coursework/admin/module/xxx02/assignments/$assignmentId/feedback/adjustments")
			Then("I see a list of students")
			pageSource contains "Feedback adjustment" should be {true}
			pageSource contains "tabula-functest-student1" should be {true}
			pageSource contains "tabula-functest-student3" should be {true}

			When("I click on the bulk adjustments button")
			click on linkText("Adjust in bulk")

			Then("I should see the bulk adjustment form")
			eventually(currentUrl should include(s"/coursework/admin/module/xxx02/assignments/$assignmentId/feedback/bulk-adjustment"))

			Then("I upload a valid adjustments file")
			click on "file.upload"
			pressKeys(getClass.getResource("/adjustments.xlsx").getFile)

			And("submit the form")
			click on cssSelector(".btn-primary")

			Then("I should see the preview bulk adjustment page")
			eventually {
				pageSource contains "Preview bulk adjustment" should be {true}
			}

			Then("The hide from student checkbox should be selected by default")
			checkbox("privateAdjustment").isSelected should be (true)

			When("I submit the form")
			click on cssSelector("input.btn-primary")

			Then("I should get redirected back to the submissions summary page")
			eventually {
				currentUrl should include(s"/coursework/admin/module/xxx02/assignments/$assignmentId/summary")
			}
		}

		Then("The student cannot see private adjustments or any previous adjustments")
		as(P.Student1) {
			When("I visit the feedback page")
			go to Path(s"/coursework/module/xxx02/$assignmentId")
			Then("I cannot see any adjustments as the last one was private")
			pageSource contains "Adjusted" should be {false}
			pageSource contains "Mark: 43" should be {true}
			pageSource contains "Grade: B" should be {true}
			pageSource contains adjustmentDescriptionText should be {false}
			pageSource contains "Mark: 41" should be {false}
			pageSource contains "Mark: 31" should be {false}
		}

	}
}
