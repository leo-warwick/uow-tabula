package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking, PermissionsCheckingMethods}
import uk.ac.warwick.tabula.permissions.Permissions.Profiles
import uk.ac.warwick.tabula.AcademicYear

object ViewModuleRegistrationsCommand {
	def apply(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear) =
		new ViewModuleRegistrationsCommandInternal(studentCourseDetails, academicYear) with
			ComposableCommand[Seq[ModuleRegistration]] with
			ViewModuleRegistrationsCommandPermissions with
			ReadOnly with Unaudited
}

trait ViewModuleRegistrationsCommandState {
	val studentCourseDetails: StudentCourseDetails
	val academicYear: AcademicYear
	var studentCourseYearDetails: StudentCourseYearDetails = _
}

class ViewModuleRegistrationsCommandInternal(val studentCourseDetails: StudentCourseDetails, val academicYear: AcademicYear)
	extends CommandInternal[Seq[ModuleRegistration]] with ViewModuleRegistrationsCommandState {

	def applyInternal() = {
			studentCourseYearDetails = studentCourseDetails.freshStudentCourseYearDetails.filter(_.academicYear == academicYear).seq.head
			studentCourseYearDetails.moduleRegistrations
	}
}

trait ViewModuleRegistrationsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewModuleRegistrationsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Profiles.Read.ModuleRegistration.Core, studentCourseDetails)
	}
}
