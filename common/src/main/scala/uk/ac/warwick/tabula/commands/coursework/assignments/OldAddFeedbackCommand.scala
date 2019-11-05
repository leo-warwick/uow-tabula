package uk.ac.warwick.tabula.commands.coursework.assignments

import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.notifications.coursework.FeedbackChangeNotification

import scala.jdk.CollectionConverters._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.commands.{Notifies, Description, UploadedFile}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions._
import org.springframework.validation.Errors

class OldAddFeedbackCommand(module: Module, assignment: Assignment, marker: User, currentUser: CurrentUser)
  extends OldUploadFeedbackCommand[Seq[Feedback]](module, assignment, marker) with Notifies[Seq[Feedback], Feedback] {

  PermissionCheck(Permissions.AssignmentFeedback.Manage, assignment)

  override def applyInternal(): Seq[Feedback] = transactional() {

    def saveFeedback(student: User, file: UploadedFile): Option[Feedback] = {
      val feedback = assignment.findFeedback(student.getUserId).getOrElse({
        val newFeedback = new AssignmentFeedback
        newFeedback.assignment = assignment
        newFeedback.uploaderId = marker.getUserId
        newFeedback._universityId = student.getWarwickId
        newFeedback.usercode = student.getUserId
        newFeedback.released = false
        newFeedback.createdDate = DateTime.now
        newFeedback.updatedDate = DateTime.now
        newFeedback
      })

      val newAttachments = feedback.addAttachments(file.attached.asScala.toSeq)

      if (newAttachments.nonEmpty) {
        session.saveOrUpdate(feedback)
        Some(feedback)
      } else {
        None
      }
    }

    (for (item <- items.asScala; student <- item.student) yield {
      val feedback = saveFeedback(student, item.file)
      feedback.foreach(zipService.invalidateIndividualFeedbackZip)
      feedback
    }).toList.flatten
  }

  override def validateExisting(item: OldFeedbackItem, errors: Errors) {
    // warn if feedback for this student is already uploaded
    assignment.feedbacks.asScala.find { feedback => feedback._universityId == item.uniNumber && feedback.hasAttachments } match {
      case Some(feedback) =>
        // set warning flag for existing feedback and check if any existing files will be overwritten
        item.submissionExists = true
        item.isPublished = feedback.released
        checkForDuplicateFiles(item, feedback)
      case None =>
    }
  }

  private def checkForDuplicateFiles(item: OldFeedbackItem, feedback: Feedback) {
    case class duplicatePair(attached: FileAttachment, feedback: FileAttachment)

    val attachmentNames = item.file.attached.asScala.map(_.name)
    val withSameName = for (
      attached <- item.file.attached.asScala;
      feedback <- feedback.attachments.asScala
      if attached.name == feedback.name
    ) yield new duplicatePair(attached, feedback)

    item.duplicateFileNames = withSameName.filterNot(p => p.attached.isDataEqual(p.feedback)).map(_.attached.name).toSet
    item.ignoredFileNames = withSameName.map(_.attached.name).toSet -- item.duplicateFileNames
    item.isModified = (attachmentNames.toSet -- item.ignoredFileNames).nonEmpty
  }

  def describe(d: Description): Unit = d
    .assignment(assignment)
    .studentIds(items.asScala.toSeq.map(_.uniNumber))
    .studentUsercodes(items.asScala.toSeq.flatMap(_.student.map(_.getUserId)))

  override def describeResult(d: Description, feedbacks: Seq[Feedback]): Unit =
    d.assignment(assignment)
     .feedbacks(feedbacks)
     .fileAttachments(feedbacks.flatMap(_.attachments.asScala))

  def emit(updatedFeedback: Seq[Feedback]): Seq[FeedbackChangeNotification] = {
    updatedFeedback.filter(_.released).flatMap { feedback =>
      HibernateHelpers.initialiseAndUnproxy(feedback) match {
        case assignmentFeedback: AssignmentFeedback =>
          Option(Notification.init(new FeedbackChangeNotification, marker, assignmentFeedback, assignment))
        case _ =>
          None
      }
    }
  }
}
