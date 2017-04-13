package uk.ac.warwick.tabula.profiles

/**
 * Tests for the pages that list students with and without tutors and supervisors for a given department
 *
 */
class TuteesListTest extends SubDepartmentFixture{

	// see also SubDepartmentFixture.before()
	before{
		beforeEverything()

		createStaffMember(P.Marker1.usercode, deptCode = TEST_DEPARTMENT_CODE)

		Given("Student1 is a first-year undergraduate, registered on a module in xxx")
		createModule("xxx","xxx198","Top level module")
		registerStudentsOnModule(Seq(P.Student1),"xxx198")

		And("Student2 is a second-year undergraduate, registered on a module in xxx-ug")
		createModule("xxx-ug","xug19","Top level module")
		registerStudentsOnModule(Seq(P.Student2),"xug19")

		Given("Student3 is a postgraduate, registered on a module in xxx")
		createModule("xxx","xpg12","Postgrad module")
		registerStudentsOnModule(Seq(P.Student3),"xpg12")

		Given("Marker 1 is the personal tutor for Student 3")
		createStudentRelationship(P.Student3,  P.Marker1, "tutor")

	}

	"Marker 1" should "be able to see their list of tutees" in {
		Given("Student3 has Marker 1 for a personal tutor")

		When("The marker goes to the profiles home page")
		signIn as P.Marker1 to Path("/profiles/tutor/students")

		Then("There is a tutee")
		pageSource should include("related_student")
		pageSource should include("tabula-functest-student3")


	}

}
