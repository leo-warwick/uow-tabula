package uk.ac.warwick.tabula.commands.profiles.profile

import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions.Profiles
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ViewModuleRegistrationsCommand {
	def apply(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear) =
		new ViewModuleRegistrationsCommandInternal(studentCourseDetails, academicYear)
			with ComposableCommand[Seq[ModuleRegistration]]
			with ViewModuleRegistrationsCommandPermissions
			with ReadOnly with Unaudited
}

class ViewModuleRegistrationsCommandInternal(val studentCourseDetails: StudentCourseDetails, val academicYear: AcademicYear)
	extends CommandInternal[Seq[ModuleRegistration]] with ViewModuleRegistrationsCommandState {

	def applyInternal(): Seq[ModuleRegistration] = {
		studentCourseYearDetails = studentCourseDetails.freshStudentCourseYearDetails.find(_.academicYear == academicYear).orElse(
			studentCourseDetails.freshOrStaleStudentCourseYearDetails.find(_.academicYear == academicYear)
		).getOrElse(throw new ItemNotFoundException())
		studentCourseYearDetails.moduleRegistrations
	}
}

trait ViewModuleRegistrationsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewModuleRegistrationsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Profiles.Read.ModuleRegistration.Core, studentCourseDetails)
	}
}

trait ViewModuleRegistrationsCommandState {
	val studentCourseDetails: StudentCourseDetails
	val academicYear: AcademicYear
	var studentCourseYearDetails: StudentCourseYearDetails = _
}
