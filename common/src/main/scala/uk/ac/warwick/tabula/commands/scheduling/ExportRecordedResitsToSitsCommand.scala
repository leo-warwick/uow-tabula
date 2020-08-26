package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.scheduling.ExportRecordedResitToSitsCommand._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.data.model.{RecordedResit, UpstreamAssessmentGroup, UpstreamAssessmentGroupMember, UpstreamAssessmentGroupMemberAssessmentType}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.services.marks.{AutowiringResitServiceComponent, ResitServiceComponent}
import uk.ac.warwick.tabula.services.scheduling.{AutowiringExportFeedbackToSitsServiceComponent, AutowiringExportResitsToSitsServiceComponent, ExportFeedbackToSitsServiceComponent, ExportResitsToSitsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

object ExportRecordedResitToSitsCommand {
  type Result = Seq[RecordedResit]
  type Command = Appliable[Result]

  def apply(): Command =
    new ExportRecordedResitToSitsCommandInternal()
      with ComposableCommand[Result]
      with ExportRecordedResitToSitsCommandPermissions
      with ExportRecordedResitToSitsDescription
      with AutowiringExportResitsToSitsServiceComponent
      with AutowiringResitServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringTransactionalComponent
}

abstract class ExportRecordedResitToSitsCommandInternal
  extends CommandInternal[Result]
    with Logging {
  self: ExportResitsToSitsServiceComponent
    with ResitServiceComponent
    with ModuleAndDepartmentServiceComponent
    with TransactionalComponent =>

  override def applyInternal(): Result = transactional() {
    val resitsToUpload = resitService.allNeedingWritingToSits

    val uploaded = resitsToUpload.flatMap { resit =>
      val canUploadResitsToSitsForYear =
        moduleAndDepartmentService.getModuleBySitsCode(resit.moduleCode).forall { module =>
          module.adminDepartment.canUploadMarksToSitsForYear(resit.academicYear, module)
        }

      if (!canUploadResitsToSitsForYear) {
        logger.warn(s"Not uploading resit $resit as department for ${resit.moduleCode} is closed for ${resit.academicYear}")
        None
      } else {

        val rowsUpdated = if (resit.lastWrittenToSits.isDefined) {
          exportResitsToSitsService.updateResit(resit)
        } else {
          val rseq = exportResitsToSitsService.getNextResitSequence(resit)
          resit.resitSequence = Option(rseq)
          exportResitsToSitsService.createResit(resit, rseq)
        }

        rowsUpdated match {
          case 0 =>
            logger.warn(s"Upload to SITS for resit $resit failed - updated zero rows")
            None

          case r if r > 1 =>
            throw new IllegalStateException(s"Unexpected SITS update! Only expected to insert one row, but $r rows were updated for resit $resit")

          case 1 =>
            logger.info(s"Resit uploaded to SITS -  $resit")
            resit.needsWritingToSitsSince = None
            resit.lastWrittenToSits = Some(DateTime.now)
            Some(resitService.saveOrUpdate(resit))
        }
      }
    }
    uploaded
  }
}

trait ExportRecordedResitToSitsCommandPermissions extends RequiresPermissionsChecking {
  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Marks.UploadToSits)
  }
}

trait ExportRecordedResitToSitsDescription extends Describable[Result] {
  override lazy val eventName: String = "ExportRecordedResitToSits"

  override def describe(d: Description): Unit = {}

  override def describeResult(d: Description, result: Result): Unit =
    d.property("resit", result)
}
