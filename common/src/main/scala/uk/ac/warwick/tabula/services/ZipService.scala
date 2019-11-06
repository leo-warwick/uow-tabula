package uk.ac.warwick.tabula.services

import java.util.zip.{ZipEntry, ZipInputStream}

import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipArchiveInputStream}
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.commands.coursework.DownloadFeedbackAsPdfCommand
import uk.ac.warwick.tabula.commands.profiles.PhotosWarwickMemberPhotoUrlGeneratorComponent
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringFileDaoComponent, SHAFileHasherComponent}
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.pdf.FreemarkerXHTMLPDFGeneratorWithFileStorageComponent
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.objectstore.AutowiringObjectStorageServiceComponent
import uk.ac.warwick.tabula.web.views.AutowiredTextRendererComponent
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, AutowiringTopLevelUrlComponent}
import uk.ac.warwick.userlookup.{AnonymousUser, User}

import scala.jdk.CollectionConverters._
import scala.concurrent.Future
import scala.util.Using

@Service
class ZipService
  extends ZipCreator
    with AutowiringObjectStorageServiceComponent
    with SHAFileHasherComponent
    with FreemarkerXHTMLPDFGeneratorWithFileStorageComponent
    with AutowiredTextRendererComponent
    with PhotosWarwickMemberPhotoUrlGeneratorComponent
    with AutowiringFileDaoComponent
    with AutowiringTopLevelUrlComponent
    with AutowiringFeaturesComponent
    with AutowiringUserLookupComponent
    with Logging
    with TaskBenchmarking {

  val idSplitSize = 4

  logger.info("Creating ZipService")

  def partition(id: String): String = id.replace("-", "").grouped(idSplitSize).mkString("/")

  private def resolvePath(feedback: Feedback): String = "feedback/" + partition(feedback.id)

  private def resolvePathForStudent(feedback: Feedback): String = "student-feedback/" + partition(feedback.id)

  private def resolvePath(submission: Submission): String = "submission/" + partition(submission.id)

  private def resolvePathForSubmission(assignment: Assignment) = "all-submissions/" + partition(assignment.id)

  def invalidateSubmissionZip(assignment: Assignment): Future[Unit] = invalidate(resolvePathForSubmission(assignment))

  def invalidateIndividualFeedbackZip(feedback: Feedback): Future[Unit] = {
    Future.sequence(Seq(
      invalidate(resolvePath(feedback)),
      invalidate(resolvePathForStudent(feedback))
    )).map(_ => ())
  }

  def getFeedbackZip(feedback: Feedback): Future[RenderableFile] =
    getZip(resolvePath(feedback), getFeedbackZipItems(feedback))
      .map(_.withSuggestedFilename(s"feedback-${feedback.studentIdentifier}.zip"))

  def getSubmissionZip(submission: Submission): Future[RenderableFile] =
    getZip(resolvePath(submission), getSubmissionZipItems(submission))
      .map(_.withSuggestedFilename(s"submission-${submission.studentIdentifier}.zip"))

  private def getFeedbackZipItems(feedback: Feedback): Seq[ZipItem] = {
    (Seq(getOnlineFeedbackPdf(feedback)) ++ feedback.attachments.asScala).map { attachment =>
      ZipFileItem(feedback.studentIdentifier + " - " + attachment.name, attachment.asByteSource, attachment.actualDataLength)
    }
  }

  private def getOnlineFeedbackPdf(feedback: Feedback): FileAttachment = {
    pdfGenerator.renderTemplateAndStore(
      DownloadFeedbackAsPdfCommand.feedbackDownloadTemple,
      "feedback.pdf",
      Map(
        "feedback" -> feedback,
        "studentId" -> feedback.studentIdentifier
      )
    )
  }

  private def getMarkerFeedbackZipItems(markerFeedback: MarkerFeedback): Seq[ZipItem] =
    markerFeedback.attachments.asScala.toSeq.filter(_.hasData).map { attachment =>
      ZipFileItem(markerFeedback.feedback.studentIdentifier + " - " + attachment.name, attachment.asByteSource, attachment.actualDataLength)
    }

  /**
    * Find all file attachment fields and any attachments in them, as a single list.
    * TODO This doesn't check for duplicate file names
    */
  def getSubmissionZipItems(submission: Submission): Seq[ZipItem] = benchmarkTask(s"Create zip item for $submission") {
    val attachments = submission.allAttachments
    val user = userLookup.getUserByUserId(submission.usercode)
    val assignment = submission.assignment
    val moduleCode = assignment.module.code

    val userIdentifier = if (!assignment.showStudentNames || user == null || user.isInstanceOf[AnonymousUser]) {
      submission.studentIdentifier
    } else {
      s"${user.getFullName} - ${submission.studentIdentifier}"
    }

    val submissionZipItems = attachments.map(a => ZipFileItem(s"$moduleCode - $userIdentifier - ${a.name}", a.asByteSource, a.actualDataLength))

    if (features.feedbackTemplates) {
      val feedbackSheets = generateFeedbackSheet(submission)
      feedbackSheets ++ submissionZipItems
    } else {
      submissionZipItems
    }
  }

  /**
    * Get a zip containing these submissions. If there is more than one submission
    * for a user, the zip _might_ work but look weird.
    */
  def getSomeSubmissionsZip(submissions: Seq[Submission], progressCallback: (Int, Int) => Unit = { (_, _) => }): Future[RenderableFile] = benchmarkTask("Create zip") {
    createUnnamedZip(submissions.flatMap(getSubmissionZipItems), progressCallback).map(_.withSuggestedFilename("submissions.zip"))
  }

  /**
    * Get a zip containing these feedbacks.
    */
  def getSomeFeedbacksZip(feedbacks: Seq[Feedback], progressCallback: (Int, Int) => Unit = { (_, _) => }): Future[RenderableFile] =
    createUnnamedZip(feedbacks.flatMap(getFeedbackZipItems), progressCallback).map(_.withSuggestedFilename("feedback.zip"))

  /**
    * Get a zip containing these marker feedbacks.
    */
  def getSomeMarkerFeedbacksZip(markerFeedbacks: Seq[MarkerFeedback]): Future[RenderableFile] =
    createUnnamedZip(markerFeedbacks.flatMap(getMarkerFeedbackZipItems)).map(_.withSuggestedFilename("marker-feedback.zip"))

  /**
    * A zip of submissions with a folder for each student.
    */
  def getAllSubmissionsZip(assignment: Assignment): Future[RenderableFile] =
    getZip(resolvePathForSubmission(assignment),
      assignment.submissions.asScala.toSeq.flatMap(getSubmissionZipItems))

  /**
    * A zip of feedback templates for each student registered on the assignment
    * assumes a feedback template exists
    */
  def getMemberFeedbackTemplates(users: Seq[User], assignment: Assignment): Future[RenderableFile] = {
    val templateFile = assignment.feedbackTemplate.attachment
    val zipItems: Seq[ZipItem] = for (user <- users) yield {
      val filename = assignment.module.code + " - " + user.getWarwickId + " - " + templateFile.name
      ZipFileItem(filename, templateFile.asByteSource, templateFile.actualDataLength)
    }
    createUnnamedZip(zipItems).map(_.withSuggestedFilename("feedback-templates.zip"))
  }

  /**
    * Returns a sequence with a single ZipItem (the feedback template) or an empty
    * sequence if no feedback template exists
    */
  def generateFeedbackSheet(submission: Submission): Seq[ZipItem] = {
    // wrap template in an option to deal with nulls
    Option(submission.assignment.feedbackTemplate) match {
      case Some(t) => Seq(ZipFileItem(submission.zipFileName(t.attachment), t.attachment.asByteSource, t.attachment.actualDataLength))
      case None => Seq()
    }
  }

  def getSomeMeetingRecordAttachmentsZip(meetingRecord: AbstractMeetingRecord): Future[RenderableFile] =
    createUnnamedZip(getMeetingRecordZipItems(meetingRecord)).map(_.withSuggestedFilename("meeting.zip"))

  private def getMeetingRecordZipItems(meetingRecord: AbstractMeetingRecord): Seq[ZipItem] =
    meetingRecord.attachments.asScala.toSeq.map { attachment =>
      ZipFileItem(attachment.name, attachment.asByteSource, attachment.actualDataLength)
    }

  def getSomeMemberNoteAttachmentsZip(memberNote: MemberNote): Future[RenderableFile] =
    createUnnamedZip(getMemberNoteZipItems(memberNote)).map(_.withSuggestedFilename("note.zip"))

  private def getMemberNoteZipItems(memberNote: MemberNote): Seq[ZipItem] =
    memberNote.attachments.asScala.toSeq.map { attachment =>
      ZipFileItem(attachment.name, attachment.asByteSource, attachment.actualDataLength)
    }

  def getProfileExportZip(results: Map[String, Seq[FileAttachment]]): Future[RenderableFile] = {
    createUnnamedZip(results.map { case (uniId, files) =>
      ZipFolderItem(uniId, files.zipWithIndex.map { case (file, index) =>
        if (index == 0)
          ZipFileItem(file.name, file.asByteSource, file.actualDataLength)
        else
          ZipFileItem(file.id + "-" + file.name, file.asByteSource, file.actualDataLength)
      })
    }.toSeq).map(_.withSuggestedFilename("profiles.zip"))
  }
}

trait ZipServiceComponent {
  def zipService: ZipService
}

trait AutowiringZipServiceComponent extends ZipServiceComponent {
  var zipService: ZipService = Wire[ZipService]
}

object Zips {

  /**
    * Provides an iterator for ZipEntry items which will be closed when you're done with them.
    * The object returned from the function is converted to a list to guarantee that it's evaluated before closing.
    */
  def iterator[A](zis: ZipArchiveInputStream)(fn: Iterator[ZipArchiveEntry] => Iterator[A]): List[A] = Using.resource(zis) { zip =>
    fn {
      Iterator.continually(zip.getNextZipEntry)
        .takeWhile(_ != null)
    }.toList
  }

  def map[A](zis: ZipInputStream)(fn: ZipEntry => A): Seq[A] = Using.resource(zis) { zip =>
    Iterator.continually(zip.getNextEntry)
      .takeWhile(_ != null)
      .map { item =>
        val t = fn(item)
        zip.closeEntry()
        t
      }
      .toList // use toList to evaluate items now, before we actually close the stream
  }

}
