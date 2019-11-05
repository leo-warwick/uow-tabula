package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands.cm2.assignments.DownloadAllSubmissionsCommand._
import uk.ac.warwick.tabula.commands.{Description, _}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.{AutowiringZipServiceComponent, ZipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object DownloadAllSubmissionsCommand {
  type Result = RenderableFile
  type Command = Appliable[Result] with DownloadAllSubmissionsCommandState

  val AdminPermission = Permissions.Submission.Read

  def apply(assignment: Assignment, filename: String): Command =
    new DownloadAllSubmissionsCommandInternal(assignment, filename)
      with ComposableCommand[Result]
      with DownloadAllSubmissionsCommandPermissions
      with DownloadAllSubmissionsCommandDescription
      with AutowiringZipServiceComponent
      with ReadOnly
}

trait DownloadAllSubmissionsCommandState {
  def assignment: Assignment

  def filename: String
}

class DownloadAllSubmissionsCommandInternal(val assignment: Assignment, val filename: String)
  extends CommandInternal[Result] with DownloadAllSubmissionsCommandState {
  self: ZipServiceComponent =>

  override def applyInternal(): RenderableFile = Await.result(zipService.getAllSubmissionsZip(assignment), Duration.Inf)
}

trait DownloadAllSubmissionsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: DownloadAllSubmissionsCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(AdminPermission, mandatory(assignment))
}

trait DownloadAllSubmissionsCommandDescription extends Describable[Result] {
  self: DownloadAllSubmissionsCommandState =>

  override lazy val eventName: String = "DownloadAllSubmissions"

  override def describe(d: Description): Unit = d
    .assignment(assignment)
    .studentIds(assignment.submissions.asScala.toSeq.flatMap(_.universityId))
    .studentUsercodes(assignment.submissions.asScala.toSeq.map(_.usercode))
    .properties(
      "submissionCount" -> Option(assignment.submissions).map(_.size).getOrElse(0))
}
