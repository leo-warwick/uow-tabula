package uk.ac.warwick.tabula.dev.web.commands

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.{Gender, StudentCourseDetails, StudentCourseYearDetails, StudentMember}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, CourseAndRouteService, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class StudentMemberFixtureCommand extends CommandInternal[StudentMember] with Logging {
	this: UserLookupComponent =>

	var userId: String = _
	var genderCode: String = _
	var routeCode: String = ""
	var courseCode:String=""
	var yearOfStudy: Int = 1
	var deptCode:String=""
	var academicYear: AcademicYear = _

	var memberDao: MemberDao = Wire[MemberDao]
	var courseAndRouteService: CourseAndRouteService = Wire[CourseAndRouteService]
  var deptDao: DepartmentDao = Wire[DepartmentDao]
	var statusDao: SitsStatusDao = Wire[SitsStatusDao]
	var studentCourseDetailsDao: StudentCourseDetailsDao = Wire[StudentCourseDetailsDao]

	def applyInternal(): StudentMember = {
		val userLookupUser = userLookup.getUserByUserId(userId)
		assert(userLookupUser != null)

			val existing = memberDao.getByUniversityId(userLookupUser.getWarwickId)
			val route = if (routeCode != "") courseAndRouteService.getRouteByCode(routeCode) else None
			val course = if (courseCode!= "") courseAndRouteService.getCourseByCode(courseCode) else None
			val dept = if (deptCode!= "") deptDao.getByCode(deptCode) else None
			val currentStudentStatus = statusDao.getByCode("C").get


			val newMember = new StudentMember
			newMember.universityId = userLookupUser.getWarwickId
			newMember.userId = userId
			newMember.gender = Gender.fromCode(genderCode)
			newMember.firstName = userLookupUser.getFirstName
			newMember.lastName = userLookupUser.getLastName
			newMember.inUseFlag = "Active"
			newMember.mobileNumber = "07000123456"
			if (dept.isDefined) newMember.homeDepartment = dept.get

			val scd = new StudentCourseDetails(newMember, userLookupUser.getWarwickId + "/" + yearOfStudy)
			scd.mostSignificant = true
			scd.sprCode = scd.scjCode
			scd.statusOnRoute = currentStudentStatus
			scd.beginDate = new LocalDate().minusYears(2)

			if (route.isDefined) scd.currentRoute = route.get
			if (course.isDefined) scd.course = course.get
			if (dept.isDefined)  scd.department = dept.get

			val scydAcademicYear = {
				if (null != academicYear) academicYear
				else AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
			}

			// actually, if the specified academic year is before now, perhaps we should create and
			// attach a StudentCourseYearDetails for every year between then and now
			val yd = new StudentCourseYearDetails(scd, 1, scydAcademicYear)
			yd.yearOfStudy = yearOfStudy
			if (dept.isDefined) yd.enrolmentDepartment = dept.get
			yd.enrolledOrCompleted = true
			scd.attachStudentCourseYearDetails(yd)

			transactional() {
				existing foreach {
					memberDao.delete
				}
			}

			transactional() {
				newMember.attachStudentCourseDetails(scd)
				memberDao.saveOrUpdate(newMember)
			}

			transactional() {
				newMember.mostSignificantCourse = scd
				memberDao.saveOrUpdate(newMember)
			}

			newMember
	}
}

object StudentMemberFixtureCommand {
	def apply(): StudentMemberFixtureCommand with ComposableCommand[StudentMember] with AutowiringUserLookupComponent with Unaudited with PubliclyVisiblePermissions = {
		new StudentMemberFixtureCommand with ComposableCommand[StudentMember]
			with AutowiringUserLookupComponent
			with Unaudited
			with PubliclyVisiblePermissions
	}
}
