package uk.ac.warwick.tabula.profiles

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.web.{FeaturesDriver, FixturesDriver}

/**
 * Tests for the pages that list students with and without tutors and supervisors for a given department
 *
 */
class TutorAllocationTest extends BrowserTest with FeaturesDriver with FixturesDriver with GivenWhenThen {

	before {
		go to Path("/fixtures/setup")
		pageSource should include("Fixture setup successful")

		createStaffMember(P.Marker1.usercode, deptCode = "xxx")
		createStaffMember(P.Marker2.usercode, deptCode = "xxx")
		createStaffMember(P.Marker3.usercode, deptCode = "xxx")

		createCourse("Ux123","Test UG Course")

		createStudentMember(P.Student1.usercode, deptCode = "xxx", courseCode = "Ux123")
		createStudentMember(P.Student2.usercode, deptCode = "xxx", courseCode = "Ux123")
		createStudentMember(P.Student3.usercode, deptCode = "xxx", courseCode = "Ux123")
		createStudentMember(P.Student4.usercode, deptCode = "xxx", courseCode = "Ux123")
		createStudentMember(P.Student5.usercode, deptCode = "xxx", courseCode = "Ux123")

		createStudentRelationship(P.Student1, P.Marker1)
		createStudentRelationship(P.Student1, P.Marker2)
		createStudentRelationship(P.Student2, P.Marker2)
	}

	"Admin" should "be able to update allocation" in {
		Given("I am logged in as Admin1")
		signIn as P.Admin1 to Path("/")

		When("I go to /profiles/department/xxx/tutor/allocate")
		go to Path("/profiles/department/xxx/tutor/allocate")

		Then("I see a list of the unallocated students")
		eventually(currentUrl should include("/profiles/department/xxx/tutor/allocate"))
		pageSource should include("3 Unallocated students found")
		findAll(cssSelector(".students table tbody tr")).size should be (3)

		And("I see a list of the allocated students")
		findAll(cssSelector(".entities table tbody tr")).size should be (2)

		When("I select all of the unallocated students")
		findAll(cssSelector(".students tbody tr td.check input")).foreach(c => clickOn(c))
		eventually{
			findAll(cssSelector(".students tbody tr td.check input")).toSeq.forall(_.isSelected) should be {true}
		}

		And("I select all of the entities")
		findAll(cssSelector(".entities tbody tr td.check input")).foreach(c => clickOn(c))
		eventually{
			findAll(cssSelector(".entities tbody tr td.check input")).toSeq.forall(_.isSelected) should be {true}
		}

		And("I choose to distribute selected")
		click on cssSelector("button[name=action][value=DistributeSelected]")

		Then("All the students are allocated")
		eventually{
			pageSource should include("0 Unallocated students found")
			findAll(cssSelector(".students table tbody tr")).size should be (0)
		}

		When("I select the first entity")
		checkbox(cssSelector(".entities tbody tr td.check input")).select()
		eventually{
			find(cssSelector("button[name=action][value=RemoveFromAll]")).get.isEnabled should be {true}
		}

		And("I remove all the students")
		click on cssSelector("button[name=action][value=RemoveFromAll]")

		Then("Some students are unallocated")
		eventually{
			findAll(cssSelector(".students table tbody tr")).size should be > 0
		}
		val unallocated = findAll(cssSelector(".students table tbody tr")).size

		When("I select the first unallocated student")
		checkbox(cssSelector(".students tbody tr td.check input")).select()

		And("I choose the first entity")
		checkbox(cssSelector(".entities tbody tr td.check input")).select()

		And("I choose to distribute to a single entity")
		eventually{
			find(cssSelector("button[name=action][value=DistributeSelected]")).get.isEnabled should be {true}
		}
		click on cssSelector("button[name=action][value=DistributeSelected]")

		Then("There is one fewer unallocated")
		eventually{
			findAll(cssSelector(".students table tbody tr")).size should be (unallocated - 1)
		}

		When("I choose to continue")
		click on cssSelector("form.preview button")

		Then("I see a preview of the changes")
		eventually{
			currentUrl should include("/profiles/department/xxx/tutor/allocate/preview")
			findAll(cssSelector(".removals table tbody tr")).size should be (1)
			findAll(cssSelector(".additions table tbody tr")).size should be (2)
			findAll(cssSelector("input[name^=removals]")).size should be (1)
			findAll(cssSelector("input[name^=additions]")).size should be (2)
		}

		When("I choose to confirm")
		click on cssSelector("form div.submit-buttons button.btn-primary")

		// Couldn't get the modal to work properly in HtmlUnit, so just submit the form
		ifHtmlUnitDriver(
			operation = { _ => id("command").webElement.submit() },
			otherwise = { _ =>
				Then("I am prompted to choose the notifications")
				eventually{
					id("notify-modal").webElement.isDisplayed should be {true}
				}

				When("I choose to save the changes")
				click on cssSelector("form div.modal-footer button.btn-primary")
			}
		)

		Then("The changes are saved and I am redirected")
		eventually {
			currentUrl should include("/profiles/department/xxx/tutor")
		}
	}

}
