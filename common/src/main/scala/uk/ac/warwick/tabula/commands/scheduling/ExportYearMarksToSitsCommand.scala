package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseYearDetailsDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling.{AutowiringExportYearMarksToSitsServiceComponent, ExportYearMarksToSitsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ExportYearMarksToSitsCommand {
	def apply() =
		new ExportYearMarksToSitsCommandInternal
			with ComposableCommand[Seq[StudentCourseYearDetails]]
			with AutowiringStudentCourseYearDetailsDaoComponent
			with AutowiringExportYearMarksToSitsServiceComponent
			with ExportYearMarksToSitsDescription
			with ExportYearMarksToSitsPermissions
}

class ExportYearMarksToSitsCommandInternal extends CommandInternal[Seq[StudentCourseYearDetails]] with Logging {

	self: StudentCourseYearDetailsDaoComponent with ExportYearMarksToSitsServiceComponent =>

	override def applyInternal(): Seq[StudentCourseYearDetails] = {
		val scydsToExport = transactional(readOnly = true) {
			studentCourseYearDetailsDao.listForYearMarkExport
		}
		scydsToExport.flatMap(scyd => transactional(){
			val result = exportYearMarksToSitsService.exportToSits(scyd)
			if (result) {
				scyd.agreedMarkUploadedDate = DateTime.now
				studentCourseYearDetailsDao.saveOrUpdate(scyd)
				logger.info("Exported year mark %s to SITS for %s for %s".format(
					scyd.agreedMark.toString,
					scyd.studentCourseDetails.scjCode,
					scyd.academicYear.toString
				))
				Option(scyd)
			} else {
				logger.error("Could not export year mark %s to SITS for %s for %s".format(
					scyd.agreedMark.toString,
					scyd.studentCourseDetails.scjCode,
					scyd.academicYear.toString
				))
				None
			}
		})
	}

}

trait ExportYearMarksToSitsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Marks.UploadToSits)
	}

}

trait ExportYearMarksToSitsDescription extends Describable[Seq[StudentCourseYearDetails]] {

	override lazy val eventName = "ExportYearMarksToSits"

	override def describe(d: Description) {}
	override def describeResult(d: Description, result: Seq[StudentCourseYearDetails]) {
		d.property("marks exported", result.size)
		d.property("marks", result.map(scyd => scyd.id -> (scyd.academicYear, scyd.yearOfStudy, scyd.agreedMark)))
	}
}
