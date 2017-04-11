package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.FeedbackForSits
import uk.ac.warwick.tabula.data.model.FeedbackForSitsStatus.{Failed, Successful}
import uk.ac.warwick.tabula.data.{AutowiringFeedbackForSitsDaoComponent, FeedbackForSitsDaoComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.scheduling.{AutowiringExportFeedbackToSitsServiceComponent, ExportFeedbackToSitsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}

object ExportFeedbackToSitsCommand {
	def apply() = new ExportFeedbackToSitsCommand
		with ComposableCommand[Seq[FeedbackForSits]]
		with ExportFeedbackToSitsCommandPermissions
		with ExportFeedbackToSitsCommandDescription
		with AutowiringFeedbackForSitsDaoComponent
		with AutowiringExportFeedbackToSitsServiceComponent
		with AutowiringFeaturesComponent
}

class ExportFeedbackToSitsCommand extends CommandInternal[Seq[FeedbackForSits]] with Logging {

	self: FeedbackForSitsDaoComponent with ExportFeedbackToSitsServiceComponent with FeaturesComponent =>

	override def applyInternal(): Seq[FeedbackForSits] = transactional() {

		val feedbacksToLoad = feedbackForSitsDao.feedbackToLoad
		var feedbacksLoaded: Seq[FeedbackForSits] = Seq()

		logger.info(s"Found ${feedbacksToLoad.size} feedback to upload to SITS")

		// for each mark/grade
		for (feedbackToLoad <- feedbacksToLoad) {
			val feedback = feedbackToLoad.feedback
			val feedbackId = feedback.id
			val department = feedback.module.adminDepartment

			if (!department.canUploadMarksToSitsForYear(feedback.academicYear, feedback.module))
				logger.warn(f"Not uploading feedback $feedbackId as department ${department.code} is closed")
			else {

				// first check to see if there is one and only one matching row
				val rowCount = exportFeedbackToSitsService.countMatchingSasRecords(feedbackToLoad)

				if (rowCount == 0) {
					feedbackToLoad.status = Failed
					logger.warn(f"Not updating SITS CAM_SAS for feedback $feedbackId - found zero rows")
				} else if (rowCount > 1) {
					feedbackToLoad.status = Failed
					logger.warn(f"Not updating SITS CAM_SAS for feedback $feedbackId - found multiple rows")
				} else {
					feedbacksLoaded = feedbacksLoaded :+ uploadFeedbackToSits(feedbackToLoad)
				}
				feedbackForSitsDao.saveOrUpdate(feedbackToLoad)
			}
		}

		feedbacksLoaded
	}


	def uploadFeedbackToSits(feedbackToLoad: FeedbackForSits): FeedbackForSits = {
		val feedback = feedbackToLoad.feedback
		val feedbackId = feedback.id
		val studentId = feedback.studentIdentifier

		//  update - expecting to update one row
		val expectedRowCount = exportFeedbackToSitsService.exportToSits(feedbackToLoad)

		if (expectedRowCount == 0) {
			feedbackToLoad.status = Failed
			logger.warn(f"Not updating SITS CAM_SAS for feedback $feedbackId - found zero rows")
		} else if (expectedRowCount == 1) {
			// record what's been done in the FeedbackToLoad object
			feedbackToLoad.status = Successful
			feedbackToLoad.dateOfUpload = DateTime.now

			feedback.latestMark.foreach( mark => feedbackToLoad.actualMarkLastUploaded = mark)
			feedback.latestGrade.foreach( grade => feedbackToLoad.actualGradeLastUploaded = grade)
		} else {
			throw new IllegalStateException(s"Unexpected SITS update!  Only expected to update one row, but $expectedRowCount rows were updated " +
				s"in CAM_SAS for student $studentId, feedback $feedbackId")
		}

		feedbackToLoad
	}
}

trait ExportFeedbackToSitsCommandPermissions extends RequiresPermissionsChecking {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Marks.UploadToSits)
	}
}

trait ExportFeedbackToSitsCommandDescription extends Describable[Seq[FeedbackForSits]] {
	override def describe(d: Description) {}
	override def describeResult(d: Description, result: Seq[FeedbackForSits]) {
		d.property("feedbackForSits", result.map(feedback => Map(
			"id" -> feedback.id,
			"feedback" -> feedback.feedback.id,
			"mark" -> feedback.actualMarkLastUploaded,
			"grade" -> feedback.actualGradeLastUploaded
		)))
	}
}