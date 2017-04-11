package uk.ac.warwick.tabula.web.controllers.profiles.profile

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.timetables.AutowiringScientiaConfigurationComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs

@Controller
@RequestMapping(Array("/profiles/view"))
class ViewProfileTimetableController extends AbstractViewProfileController
	with AutowiringScientiaConfigurationComponent with SystemClockComponent {

	@RequestMapping(Array("/{member}/timetable"))
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
				Mav("profiles/profile/timetable",
					"academicYears" -> scientiaConfiguration.academicYears
				).crumbs(breadcrumbsStaff(member, ProfileBreadcrumbs.Profile.TimetableIdentifier): _*)
		}
	}

	@RequestMapping(Array("/course/{studentCourseDetails}/{academicYear}/timetable"))
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
		Mav("profiles/profile/timetable",
			"member" -> studentCourseDetails.student
		).crumbs(breadcrumbsStudent(activeAcademicYear, studentCourseDetails, ProfileBreadcrumbs.Profile.TimetableIdentifier): _*)
			.secondCrumbs(secondBreadcrumbs(activeAcademicYear, studentCourseDetails)(scyd => Routes.Profile.timetable(scyd)): _*)
	}

}
