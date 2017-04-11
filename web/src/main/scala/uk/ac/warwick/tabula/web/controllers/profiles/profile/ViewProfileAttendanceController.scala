package uk.ac.warwick.tabula.web.controllers.profiles.profile

import java.text.DateFormatSymbols

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.profile.AttendanceProfileCommand
import uk.ac.warwick.tabula.commands.groups.ListStudentGroupAttendanceCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.attendance.MonthNames
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs

@Controller
@RequestMapping(Array("/profiles/view"))
class ViewProfileAttendanceController extends AbstractViewProfileController {

	@RequestMapping(Array("/{member}/attendance"))
	def viewByMemberMapping(
		@PathVariable member: Member,
		@ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]
	): Mav = {
		mandatory(member) match {
			case student: StudentMember if student.mostSignificantCourseDetails.isDefined =>
				viewByCourse(student.mostSignificantCourseDetails.get, activeAcademicYear)
			case student: StudentMember if student.freshOrStaleStudentCourseDetails.nonEmpty =>
				viewByCourse(student.freshOrStaleStudentCourseDetails.lastOption.get, activeAcademicYear)
			case _ =>
				Redirect(Routes.Profile.identity(member))
		}
	}

	@RequestMapping(Array("/course/{studentCourseDetails}/{academicYear}/attendance"))
	def viewByCourseMapping(
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		val activeAcademicYear: Option[AcademicYear] = Some(mandatory(academicYear))
		viewByCourse(studentCourseDetails, activeAcademicYear)
	}

	private def viewByCourse(
		studentCourseDetails: StudentCourseDetails,
		activeAcademicYear: Option[AcademicYear]
	): Mav = {
		val student = studentCourseDetails.student

		val monitoringPointAttendanceCommand = restricted(AttendanceProfileCommand(mandatory(student), mandatory(activeAcademicYear)))
		val seminarAttendanceCommand = restricted(ListStudentGroupAttendanceCommand(mandatory(student), mandatory(activeAcademicYear)))

		Mav("profiles/profile/attendance_student",
			"student" -> student,
			"hasMonitoringPointAttendancePermission" -> monitoringPointAttendanceCommand.nonEmpty,
			"hasSeminarAttendancePermission" -> seminarAttendanceCommand.nonEmpty,

			"monitoringPointAttendanceCommandResult" -> monitoringPointAttendanceCommand.map(_.apply()).orNull,
			"seminarAttendanceCommandResult" -> seminarAttendanceCommand.map(_.apply()).orNull,
			"isSelf" -> (user.universityId.maybeText.getOrElse("") == student.universityId),
			"allCheckpointStates" -> AttendanceState.values.sortBy(state => state.description),
			"monthNames" -> MonthNames(activeAcademicYear.get),
			"academicYear" -> activeAcademicYear
		).crumbs(breadcrumbsStudent(activeAcademicYear, studentCourseDetails, ProfileBreadcrumbs.Profile.AttendanceIdentifier): _*)
			.secondCrumbs(secondBreadcrumbs(activeAcademicYear, studentCourseDetails)(scyd => Routes.Profile.attendance(scyd)): _*)

	}

}
