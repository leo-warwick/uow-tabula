package uk.ac.warwick.tabula.commands.reports.profiles

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.jobs.reports.ProfileExportJob
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.jobs.{AutowiringJobServiceComponent, JobInstance, JobServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.JavaConverters._

object ProfileExportReportCommand {
	def apply(department: Department, academicYear: AcademicYear, user: CurrentUser) =
		new ProfileExportReportCommandInternal(department, academicYear, user)
			with AutowiringJobServiceComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[JobInstance]
			with ProfileExportReportValidation
			with Unaudited
			with ProfileExportReportPermissions
			with ProfileExportReportCommandState
}


class ProfileExportReportCommandInternal(val department: Department, val academicYear: AcademicYear, val user: CurrentUser)
	extends CommandInternal[JobInstance] {

	self: JobServiceComponent with ProfileExportReportCommandState =>

	override def applyInternal(): JobInstance = {
		jobService.add(Option(user), ProfileExportJob(students.asScala, academicYear))
	}

}

trait ProfileExportReportValidation extends SelfValidating {

	self: ProfileExportReportCommandState with ProfileServiceComponent =>

	override def validate(errors: Errors) {
		if (students.isEmpty) {
			errors.rejectValue("students", "reports.profiles.export.noStudents")
		} else {
			val memberMap = profileService.getAllMembersWithUniversityIds(students.asScala).groupBy(_.universityId).mapValues(_.head)
			val notMembers = students.asScala.filter(uniId => memberMap.get(uniId).isEmpty)
			if (notMembers.nonEmpty) {
				errors.rejectValue("students", "reports.profiles.export.notMembers", Array(notMembers.mkString(", ")), "")
			}
			val outsideDepartment = memberMap.values.toSeq.filterNot(member => member.affiliatedDepartments.flatMap(_.subDepartmentsContaining(member)).contains(department))
			if (outsideDepartment.nonEmpty) {
				errors.rejectValue("students", "reports.profiles.export.outsideDepartment", Array(outsideDepartment.map(m => s"${m.fullName.getOrElse("")} (${m.universityId})").mkString(", ")), "")
			}
		}
	}

}

trait ProfileExportReportPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ProfileExportReportCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.Reports, department)
	}

}

trait ProfileExportReportCommandState {
	def department: Department
	def academicYear: AcademicYear
	def user: CurrentUser

	var students: JList[String] = JArrayList()
}
