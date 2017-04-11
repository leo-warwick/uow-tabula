package uk.ac.warwick.tabula.web.controllers.profiles.profile

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.coursework.assignments.MarkerAssignmentsSummaryCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs

@Controller
@RequestMapping(Array("/profiles/view"))
class ViewProfileMarkingController extends AbstractViewProfileController {

	@RequestMapping(Array("/{member}/marking"))
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
				commonView(member).crumbs(breadcrumbsStaff(member, ProfileBreadcrumbs.Profile.MarkingIdentifier): _*)
		}
	}

	@RequestMapping(Array("/course/{studentCourseDetails}/{academicYear}/marking"))
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
		commonView(studentCourseDetails.student)
			.crumbs(breadcrumbsStudent(activeAcademicYear, studentCourseDetails, ProfileBreadcrumbs.Profile.MarkingIdentifier): _*)
			.secondCrumbs(secondBreadcrumbs(activeAcademicYear, studentCourseDetails)(scyd => Routes.Profile.marking(scyd)): _*)
	}

	private def commonView(member: Member): Mav = {
		val command = restricted(MarkerAssignmentsSummaryCommand(member))
		Mav("profiles/profile/marking",
			"hasPermission" -> command.nonEmpty,
			"command" -> command,
			"result" -> command.map(_.apply()).orNull,
			"isSelf" -> (user.universityId.maybeText.getOrElse("") == member.universityId)
		)
	}

}
