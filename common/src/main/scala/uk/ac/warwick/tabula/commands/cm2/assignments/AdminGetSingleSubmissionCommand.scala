package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.AdminGetSingleSubmissionCommand._
import uk.ac.warwick.tabula.data.model.{Assignment, Submission}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, AutowiringZipServiceComponent, UserLookupComponent, ZipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.AnonymousUser

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AdminGetSingleSubmissionCommand {
  type Result = RenderableFile
  type Command = Appliable[Result] with AdminGetSingleSubmissionCommandState

  val AdminPermission = Permissions.Submission.Read

  def single(assignment: Assignment, submission: Submission, filename: String) =
    new AdminGetSingleSubmissionFileCommandInternal(assignment, submission, filename)
      with ComposableCommand[Result]
      with AdminGetSingleSubmissionCommandPermissions
      with AdminGetSingleSubmissionFileCommandDescription
      with AutowiringUserLookupComponent
      with ReadOnly

  def zip(assignment: Assignment, submission: Submission) =
    new AdminGetSingleSubmissionAsZipCommandInternal(assignment, submission)
      with ComposableCommand[Result]
      with AdminGetSingleSubmissionCommandPermissions
      with AdminGetSingleSubmissionAsZipCommandDescription
      with ReadOnly
      with AutowiringZipServiceComponent
}

trait AdminGetSingleSubmissionCommandState {
  def assignment: Assignment

  def submission: Submission
}

trait AdminGetSingleSubmissionFilenameCommandState extends AdminGetSingleSubmissionCommandState {
  def filename: String
}

class AdminGetSingleSubmissionFileCommandInternal(val assignment: Assignment, val submission: Submission, val filename: String)
  extends CommandInternal[Result] with AdminGetSingleSubmissionFilenameCommandState {
  self: UserLookupComponent =>

  override def applyInternal(): RenderableFile = {
    val user = userLookup.getUserByUserId(submission.usercode)
    val moduleCode = assignment.module.code

    val userIdentifier = if (!assignment.showStudentNames || user == null || user.isInstanceOf[AnonymousUser]) {
      submission.studentIdentifier
    } else {
      s"${user.getFullName} - ${submission.studentIdentifier}"
    }

    submission.allAttachments
      .find(_.name == filename)
      .map(a => new RenderableAttachment(a, Some(s"$moduleCode - $userIdentifier - $filename")))
      .getOrElse {
        throw new ItemNotFoundException()
      }
  }
}

class AdminGetSingleSubmissionAsZipCommandInternal(val assignment: Assignment, val submission: Submission)
  extends CommandInternal[Result] with AdminGetSingleSubmissionCommandState {
  self: ZipServiceComponent =>

  override def applyInternal(): RenderableFile = Await.result(zipService.getSubmissionZip(submission), Duration.Inf)
}

trait AdminGetSingleSubmissionCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: AdminGetSingleSubmissionCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    mustBeLinked(submission, assignment)
    p.PermissionCheck(AdminPermission, mandatory(submission))
  }
}

trait AdminGetSingleSubmissionFileCommandDescription extends Describable[Result] {
  self: AdminGetSingleSubmissionFilenameCommandState =>

  override lazy val eventName: String = "AdminGetSingleSubmission"

  override def describe(d: Description): Unit =
    d.submission(submission).properties(
      "studentId" -> submission.studentIdentifier,
      "filename" -> filename
    )
}

trait AdminGetSingleSubmissionAsZipCommandDescription extends Describable[Result] {
  self: AdminGetSingleSubmissionCommandState =>

  override lazy val eventName: String = "AdminGetSingleSubmission"

  override def describe(d: Description): Unit =
    d.submission(submission).properties(
      "studentId" -> submission.studentIdentifier,
      "attachmentCount" -> submission.allAttachments.size
    )
}