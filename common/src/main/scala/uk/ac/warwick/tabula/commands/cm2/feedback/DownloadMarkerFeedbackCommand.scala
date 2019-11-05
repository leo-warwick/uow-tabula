package uk.ac.warwick.tabula.commands.cm2.feedback

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.data.model.{Assignment, MarkerFeedback}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.services.{AutowiringZipServiceComponent, ZipServiceComponent}
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.jdk.CollectionConverters._
import scala.concurrent.Await
import scala.concurrent.duration.Duration


object DownloadMarkerFeedbackCommand {
  def apply(assignment: Assignment, markerFeedback: MarkerFeedback) = new DownloadMarkerFeedbackCommandInternal(assignment, markerFeedback)
    with ComposableCommand[Option[RenderableFile]]
    with DownloadMarkerFeedbackPermissions
    with DownloadMarkerFeedbackDescription
    with AutowiringZipServiceComponent
}

class DownloadMarkerFeedbackCommandInternal(val assignment: Assignment, val markerFeedback: MarkerFeedback) extends CommandInternal[Option[RenderableFile]]
  with DownloadMarkerFeedbackState {

  this: ZipServiceComponent =>

  def applyInternal(): Option[RenderableFile] = filename match {
    case name: String if name.hasText =>
      markerFeedback.attachments.asScala.find(_.name == filename).map(new RenderableAttachment(_) {
        override val filename = s"${markerFeedback.feedback.studentIdentifier}-$name"
      })
    case _ => Some(Await.result(zipService.getSomeMarkerFeedbacksZip(Seq(markerFeedback)), Duration.Inf))
  }

}

trait DownloadMarkerFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: DownloadMarkerFeedbackState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.AssignmentFeedback.Read, markerFeedback.feedback)
  }
}

trait DownloadMarkerFeedbackDescription extends Describable[Option[RenderableFile]] {
  self: DownloadMarkerFeedbackState =>

  override lazy val eventName: String = "DownloadMarkerFeedback"

  override def describe(d: Description): Unit =
    d.assignment(assignment)
     .property("filename", filename)
     .fileAttachments(markerFeedback.attachments.asScala.toSeq)

  override def describeResult(d: Description, result: Option[RenderableFile]): Unit =
    d.property("fileFound", result.isDefined)
}

trait DownloadMarkerFeedbackState {
  val assignment: Assignment
  val markerFeedback: MarkerFeedback
  var filename: String = _
}
