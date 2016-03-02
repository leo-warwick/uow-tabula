package uk.ac.warwick.tabula.services

import java.util.zip.{ZipEntry, ZipInputStream}

import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipArchiveInputStream}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.data.SHAFileHasherComponent
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Closeables._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.objectstore.AutowiringObjectStorageServiceComponent
import uk.ac.warwick.userlookup.{AnonymousUser, User}

import scala.collection.JavaConverters._

@Service
class ZipService extends ZipCreator with AutowiringObjectStorageServiceComponent with SHAFileHasherComponent with Logging {

	@Autowired var features: Features = _
	@Autowired var userLookup: UserLookupService = _

	val idSplitSize = 4

	logger.info("Creating ZipService")

	def partition(id: String): String = id.replace("-", "").grouped(idSplitSize).mkString("/")

	def resolvePath(feedback: Feedback): String = "feedback/" + partition(feedback.id)
	def resolvePath(submission: Submission): String = "submission/" + partition(submission.id)
	def resolvePathForFeedback(assignment: Assignment) = "all-feedback/" + partition(assignment.id)
	def resolvePathForSubmission(assignment: Assignment) = "all-submissions/" + partition(assignment.id)

	def showStudentName(assignment: Assignment): Boolean = assignment.module.adminDepartment.showStudentName

	def invalidateFeedbackZip(assignment: Assignment) = invalidate(resolvePathForFeedback(assignment))
	def invalidateSubmissionZip(assignment: Assignment) = invalidate(resolvePathForSubmission(assignment))
	def invalidateIndividualFeedbackZip(feedback: Feedback) = invalidate(resolvePath(feedback))

	def getFeedbackZip(feedback: Feedback): RenderableFile =
		getZip(resolvePath(feedback), getFeedbackZipItems(feedback))

	def getSubmissionZip(submission: Submission): RenderableFile =
		getZip(resolvePath(submission), getSubmissionZipItems(submission))

	private def getFeedbackZipItems(feedback: Feedback): Seq[ZipItem] =
		feedback.attachments.asScala.map { (attachment) =>
			ZipFileItem(feedback.universityId + " - " + attachment.name, attachment.dataStream, attachment.actualDataLength)
		}

	private def getMarkerFeedbackZipItems(markerFeedback: MarkerFeedback): Seq[ZipItem] =
		markerFeedback.attachments.asScala.filter { _.hasData }.map { attachment =>
			ZipFileItem(markerFeedback.feedback.universityId + " - " + attachment.name, attachment.dataStream, attachment.actualDataLength)
		}

	/**
	 * A zip of feedback with a folder for each student.
	 */
	def getAllFeedbackZips(assignment: Assignment): RenderableFile =
		getZip(resolvePathForFeedback(assignment),
			assignment.feedbacks.asScala flatMap getFeedbackZipItems //flatmap - take the lists of items, and flattens them to one single list
		)

	/**
	 * Find all file attachment fields and any attachments in them, as a single list.
	 * TODO This doesn't check for duplicate file names
	 */
	def getSubmissionZipItems(submission: Submission): Seq[ZipItem] = {
		val allAttachments = submission.allAttachments
		val user = userLookup.getUserByUserId(submission.userId)
		val assignment = submission.assignment
		val code = assignment.module.code

		val submissionZipItems: Seq[ZipItem] = for (attachment <- allAttachments) yield {
			val userIdentifier = if(!showStudentName(assignment) || (user==null || user.isInstanceOf[AnonymousUser])) {
				submission.universityId
			} else {
				s"${user.getFullName} - ${submission.universityId}"
			}

			ZipFileItem(code + " - " + userIdentifier + " - " + attachment.name, attachment.dataStream, attachment.actualDataLength)
		}

		if (features.feedbackTemplates){
			val feedbackSheets = generateFeedbackSheet(submission)
			feedbackSheets ++ submissionZipItems
		}
		else
			submissionZipItems
	}

	/**
	 * Get a zip containing these submissions. If there is more than one submission
	 * for a user, the zip _might_ work but look weird.
	 */
	def getSomeSubmissionsZip(submissions: Seq[Submission], progressCallback: (Int, Int) => Unit = {(_,_) => }): RenderableFile =
		createUnnamedZip(submissions flatMap getSubmissionZipItems, progressCallback)

	/**
		* Get a zip containing these feedbacks.
	*/
	def getSomeFeedbacksZip(feedbacks: Seq[Feedback], progressCallback: (Int, Int) => Unit = {(_,_) => }): RenderableFile =
		createUnnamedZip(feedbacks flatMap getFeedbackZipItems, progressCallback)

	/**
	 * Get a zip containing these marker feedbacks.
	 */
	def getSomeMarkerFeedbacksZip(markerFeedbacks: Seq[MarkerFeedback]): RenderableFile =
		createUnnamedZip(markerFeedbacks flatMap getMarkerFeedbackZipItems)

	/**
	 * A zip of submissions with a folder for each student.
	 */
	def getAllSubmissionsZip(assignment: Assignment): RenderableFile =
		getZip(resolvePathForSubmission(assignment),
			assignment.submissions.asScala flatMap getSubmissionZipItems)

	/**
	 * A zip of feedback templates for each student registered on the assignment
	 * assumes a feedback template exists
	 */
	def getMemberFeedbackTemplates(users: Seq[User], assignment: Assignment): RenderableFile = {
		val templateFile = assignment.feedbackTemplate.attachment
		val zipItems:Seq[ZipItem] = for (user <- users) yield {
			val filename = assignment.module.code + " - " + user.getWarwickId + " - " + templateFile.name
			ZipFileItem(filename, templateFile.dataStream, templateFile.actualDataLength)
		}
		createUnnamedZip(zipItems)
	}

	/**
	 * Returns a sequence with a single ZipItem (the feedback template) or an empty
	 * sequence if no feedback template exists
	 */
	def generateFeedbackSheet(submission: Submission): Seq[ZipItem] = {
		// wrap template in an option to deal with nulls
		Option(submission.assignment.feedbackTemplate) match {
			case Some(t) => Seq(ZipFileItem(submission.zipFileName(t.attachment), t.attachment.dataStream, t.attachment.actualDataLength))
			case None => Seq()
		}
	}

	def getSomeMeetingRecordAttachmentsZip(meetingRecord: AbstractMeetingRecord): RenderableFile =
		createUnnamedZip(getMeetingRecordZipItems(meetingRecord))

	private def getMeetingRecordZipItems(meetingRecord: AbstractMeetingRecord): Seq[ZipItem] =
		meetingRecord.attachments.asScala.map { (attachment) =>
			ZipFileItem(attachment.name, attachment.dataStream, attachment.actualDataLength)
		}

	def getSomeMemberNoteAttachmentsZip(memberNote: MemberNote): RenderableFile =
		createUnnamedZip(getMemberNoteZipItems(memberNote))

	private def getMemberNoteZipItems(memberNote: MemberNote): Seq[ZipItem] =
		memberNote.attachments.asScala.map { (attachment) =>
			ZipFileItem(attachment.name, attachment.dataStream, attachment.actualDataLength)
		}

	def getProfileExportZip(results: Map[String, Seq[FileAttachment]]): RenderableFile = {
		createUnnamedZip(results.map{case(uniId, files) =>
			ZipFolderItem(uniId, files.zipWithIndex.map{case(file, index) =>
				if (index == 0)
					ZipFileItem(file.name, file.dataStream, file.actualDataLength)
				else
					ZipFileItem(file.id + "-" + file.name, file.dataStream, file.actualDataLength)
			})
		}.toSeq)
	}
}

trait ZipServiceComponent {
	def zipService: ZipService
}

trait AutowiringZipServiceComponent extends ZipServiceComponent {
	var zipService = Wire[ZipService]
}

object Zips {

	/**
	 * Provides an iterator for ZipEntry items which will be closed when you're done with them.
	 * The object returned from the function is converted to a list to guarantee that it's evaluated before closing.
	 */
	def iterator[A](zip: ZipArchiveInputStream)(fn: (Iterator[ZipArchiveEntry]) => Iterator[A]): List[A] = ensureClose(zip) {
		fn(Iterator.continually { zip.getNextZipEntry }.takeWhile { _ != null }).toList
	}

	def map[A](zip: ZipInputStream)(fn: (ZipEntry) => A): Seq[A] = ensureClose(zip) {
		Iterator.continually { zip.getNextEntry }.takeWhile { _ != null }.map { (item) =>
			val t = fn(item)
			zip.closeEntry()
			t
		}.toList // use toList to evaluate items now, before we actually close the stream
	}

}