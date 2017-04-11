package uk.ac.warwick.tabula.commands.scheduling.imports

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseYearDetailsDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling.{Tier4VisaImporterComponent, CasUsageImporterComponent, AutowiringCasUsageImporterComponent, AutowiringTier4VisaImporterComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ImportTier4ForStudentCommand {
	def apply(student: StudentMember, yearOnwards: AcademicYear) =
		new ImportTier4ForStudentCommandInternal(student, yearOnwards)
			with ComposableCommand[Unit]
			with ImportTier4ForStudentCommandPermissions
			with AutowiringCasUsageImporterComponent
			with AutowiringTier4VisaImporterComponent
			with AutowiringStudentCourseYearDetailsDaoComponent
			with Unaudited
}

class ImportTier4ForStudentCommandInternal(student: StudentMember, yearOnwards: AcademicYear) extends CommandInternal[Unit] {

	self: CasUsageImporterComponent with Tier4VisaImporterComponent with StudentCourseYearDetailsDaoComponent =>

	def applyInternal(): Unit = {
		val newCasUsed = casUsageImporter.isCasUsed(student.universityId)
		val newTier4Visa = tier4VisaImporter.hasTier4Visa(student.universityId)

		student.freshOrStaleStudentCourseYearDetailsFrom(yearOnwards).map {
			var hasChanged = false
			scyd => {
				if (scyd.casUsed != newCasUsed) {
					scyd.casUsed = newCasUsed
					hasChanged = true
				}
				if (scyd.tier4Visa != newTier4Visa) {
					scyd.tier4Visa = newTier4Visa
					hasChanged = true
				}
				if (hasChanged) studentCourseYearDetailsDao.saveOrUpdate(scyd)
			}
		}
	}
}

trait ImportTier4ForStudentCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}
