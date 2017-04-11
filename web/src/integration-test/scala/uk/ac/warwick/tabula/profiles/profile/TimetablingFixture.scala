package uk.ac.warwick.tabula.profiles.profile

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.web.FeaturesDriver
import uk.ac.warwick.tabula.{BrowserTest, FunctionalTestAcademicYear}


trait TimetablingFixture extends BrowserTest with TimetableDriver  with FeaturesDriver with GivenWhenThen{

	val TEST_MODULE_CODE = "xxx654"
	val TEST_GROUPSET_NAME = "Timetable Test Groupset"
  val TEST_ROUTE_CODE="xx123"
	val TEST_DEPARTMENT_CODE="xxx"
	val TEST_COURSE_CODE="Ux123"
	val TEST_MODULE_NAME="Timetabling Module"

	var testGroupSetId:String=_

	val academicYear: FunctionalTestAcademicYear = FunctionalTestAcademicYear.current

	val examTimetablesWasEnabled: Boolean = isFeatureEnabled("personalExamTimetables")

	before {
		Given("The test department exists")
		go to Path("/fixtures/setup")

		pageSource should include("Fixture setup successful")

		And("the personal timetables feature is enabled")
		enableFeature("personalTimetables")

		And("exam timetables are disabled")
		if (examTimetablesWasEnabled) disableFeature("personalExamTimetables")

		And("student1 has a membership record")
		createRoute(TEST_ROUTE_CODE, TEST_DEPARTMENT_CODE, "TimetableTest Route")
		createCourse(TEST_COURSE_CODE,"TimetableTest Course")
		createStudentMember(
			P.Student1.usercode,
			routeCode = TEST_ROUTE_CODE,
			courseCode = TEST_COURSE_CODE,
			deptCode = TEST_DEPARTMENT_CODE,
			academicYear = academicYear.startYear.toString
		)

		And("a module exists with a related SmallGroupSet")
		createModule(TEST_DEPARTMENT_CODE, TEST_MODULE_CODE, TEST_MODULE_NAME)
		testGroupSetId = createSmallGroupSet(
			TEST_MODULE_CODE,
			TEST_GROUPSET_NAME,
			academicYear = academicYear.startYear.toString
		)

		And("marker1 has a membership record")
		createStaffMember(P.Marker1.usercode, deptCode = TEST_DEPARTMENT_CODE)
	}

	after {
		if (examTimetablesWasEnabled) enableFeature("personalExamTimetables")
	}

}
