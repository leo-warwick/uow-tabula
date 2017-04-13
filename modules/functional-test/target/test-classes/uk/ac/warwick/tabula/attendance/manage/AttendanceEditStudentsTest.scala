package uk.ac.warwick.tabula.attendance.manage

import org.joda.time.{DateTimeConstants, DateTime}
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.FunctionalTestAcademicYear
import uk.ac.warwick.tabula.attendance.AttendanceFixture

class AttendanceEditStudentsTest extends AttendanceFixture with GivenWhenThen {

	val isSitsInFlux: Boolean = DateTime.now.getMonthOfYear >= DateTimeConstants.JUNE && DateTime.now.getMonthOfYear < DateTimeConstants.OCTOBER

	"A Member of staff" should "be able to edit students on a scheme" in {
		Given("I am logged in as Admin1")
		signIn as P.Admin1 to Path("/")

		When(s"I go to /attendance/manage/xxx/$thisAcademicYearString")
		go to Path(s"/attendance/manage/xxx/$thisAcademicYearString")

		And("I choose to edit the students on a scheme")
		click on linkText("1 student")

		Then("I see the students currently on the scheme")
		eventually(currentUrl should endWith(s"students"))
		pageSource should include("1 students on this scheme")
		pageSource should include("0 from SITS")

		When("I add a student manually")
		click on cssSelector(".manually-added .section-title")
		eventually {
			findAll(cssSelector(".manually-added input[name=manuallyAddForm]")).forall { _.isDisplayed } should be {true}
		}
		click on cssSelector("input[name=manuallyAddForm]")
		eventually(pageSource should include("Add students manually"))
		click on cssSelector("textarea[name=massAddUsers]")
		pressKeys("tabula-functest-student2")
		click on cssSelector("form input.btn.btn-primary")

		Then("I see the manually added student")
		eventually(
			findAll(cssSelector(".manually-added table.manage-student-table tbody tr")).size should be (2)
		)
		pageSource should include("2 students on this scheme")
		pageSource should include("(0 from SITS, plus 2 added manually)")

		When("I choose a route")
		click on cssSelector(".find-students .section-title")
		eventually {
			findAll(cssSelector(".find-students div.student-filter")).forall { _.isDisplayed } should be {true}
		}
		click on cssSelector("span[data-placeholder='All routes']")
		eventually {
			findAll(cssSelector("input[name=routes]")).forall { _.isDisplayed } should be {true}
		}
		click on cssSelector("input[name=routes]")

		And("I click on Find")
		click on cssSelector("button[name=findStudents]")

		Then("I see the students")
		eventually(
			findAll(cssSelector(".find-students table.manage-student-table tbody tr")).size should be (2)
		)
		pageSource should include("3 students on this scheme")
		pageSource should include("(1 from SITS, plus 2 added manually)")

		// No linking to SITS between June and October
		if (!isSitsInFlux) {

			When("I choose to link to SITS")
			click on cssSelector("input[name=linkToSits]")

		} else {

			findAll(cssSelector("input[name=linkToSits]")).size should be (0)

		}

		And("I save the scheme")
		click on cssSelector("form input.btn.btn-primary[name=persist]")

		Then("I am redirected to the manage home page")
		eventually(currentUrl should endWith(s"/attendance/manage/xxx/$thisAcademicYearString"))
		pageSource should include(s"Manage monitoring points for ${FunctionalTestAcademicYear.current.toString}")

		When("I choose to edit the students on the same scheme")
		click on linkText("3 students")

		Then("I see the students currently on the scheme")
		eventually(currentUrl should endWith(s"students"))
		pageSource should include("3 students on this scheme")

		if (!isSitsInFlux) {

			When("I reset both manually added students")
			click on cssSelector(".manually-added .section-title")
			eventually {
				findAll(cssSelector(".manually-added input[name=manuallyAddForm]")).forall { _.isDisplayed } should be {true}
			}
			cssSelector(".manually-added input[name=resetStudentIds]").findAllElements.foreach(input => click on input)
			click on cssSelector("input[name=resetMembership]")

			Then("Only the SITS students remain")
			eventually {
				findAll(cssSelector(".find-students table.manage-student-table tbody tr")).size should be(2)
				findAll(cssSelector(".manually-added table.manage-student-table tbody tr")).size should be(0)
			}
			pageSource should include("2 students on this scheme")

			When("I exclude the SITS students")
			click on cssSelector(".find-students .section-title")
			eventually {
				findAll(cssSelector(".find-students div.student-filter")).forall { _.isDisplayed } should be {true}
			}
			cssSelector(".find-students input[name=excludeIds]").findAllElements.foreach(input => click on input)
			click on cssSelector("input[name=manuallyExclude]")

			Then("No students remain")
			eventually {
				findAll(cssSelector(".find-students table.manage-student-table tbody tr")).size should be(2)
				findAll(cssSelector(".find-students table.manage-student-table tbody tr.exclude")).size should be(2)
				findAll(cssSelector(".manually-added table.manage-student-table tbody tr")).size should be(2)
			}

		} else {

			When("I reset all manually added students")
			click on cssSelector(".manually-added .section-title")
			eventually {
				findAll(cssSelector(".manually-added input[name=manuallyAddForm]")).forall { _.isDisplayed } should be {true}
			}
			cssSelector(".manually-added input[name=resetStudentIds]").findAllElements.foreach(input => click on input)
			click on cssSelector("input[name=resetMembership]")

			Then("No students remain")
			eventually {
				findAll(cssSelector(".manually-added table.manage-student-table tbody tr")).size should be(0)
			}

		}

		When("I save the scheme")
		click on cssSelector("form input.btn.btn-primary[name=persist]")

		Then("I am redirected to the manage home page")
		eventually(currentUrl should endWith(s"/attendance/manage/xxx/$thisAcademicYearString"))
		pageSource should include(s"Manage monitoring points for ${FunctionalTestAcademicYear.current.toString}")
		pageSource should include("0 students")

	}
}
