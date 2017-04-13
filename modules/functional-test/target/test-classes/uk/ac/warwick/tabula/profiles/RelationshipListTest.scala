package uk.ac.warwick.tabula.profiles

/**
 * Tests for the pages that list students with and without tutors and supervisors for a given department
 *
 */
class RelationshipListTest extends SubDepartmentFixture{

	// see also SubDepartmentFixture.before()
	before{
		beforeEverything()

		Given("Student1 is a first-year undergraduate, registered on a module in xxx")
		createModule("xxx","xxx198","Top level module")
		registerStudentsOnModule(Seq(P.Student1),"xxx198")

		And("Student2 is a second-year undergraduate, registered on a module in xxx-ug")
		createModule("xxx-ug","xug19","Top level module")
		registerStudentsOnModule(Seq(P.Student2),"xug19")

		And("Student3 is a postgraduate, registered on a module in xxx")
		createModule("xxx","xpg12","Postgrad module")
		registerStudentsOnModule(Seq(P.Student3),"xpg12")

		And("There is a relationship between Student1 and Marker1")
		createStudentRelationship(P.Student1, P.Marker1)
	}

	"A departmental administrator" should "be able to see lists of students in his department" in {

		When("The departmental administrator goes to the profiles home page")
		signIn as P.Admin1 to Path("/profiles")

		Then("There is a link to administer department xxx")
		find(cssSelector("#profile-dept-admin h5")).get.underlying.getText should be("Test Services")

		And("The tutor page shows one student with a tutor")
		// too lazy to write the code to drive the drop-down "Manage" button on the profiles page.
		go to Path("/profiles/department/xxx/tutor")
		pageSource should include("1 personal tutee")

		And("The tutor page shows one student without a tutor")
		// Student3 is a PGR and they are not expected to have a personal tutor, so is filtered out
		pageSource should include("View 1 student with no personal tutor")
	}

	"A sub-departmental administrator" should "be able to see lists of students in his sub-department" in {
		When("The sub-departmental administrator goes to the profiles home page")
		signIn as P.Admin3 to Path("/profiles")

		Then("There is a link to administer department xxx-ug")
		find(cssSelector("#profile-dept-admin h5")).get.underlying.getText should be("Test Services - UG")

		And("The tutor page shows one student with a tutor")
		go to Path("/profiles/department/xxx-ug/tutor")
		pageSource should include("1 personal tutee")

		And("The tutor page shows one student without a tutor")
		pageSource should include("View 1 student with no personal tutor")
	}

}
