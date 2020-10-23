package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.scheduling.ExportRecordedDecisionsToSitsCommand._
import uk.ac.warwick.tabula.data.model.RecordedDecision
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.marks.{AutowiringDecisionServiceComponent, DecisionServiceComponent}
import uk.ac.warwick.tabula.services.scheduling.{AutowiringExportDecisionsToSitsServiceComponent, ExportDecisionsToSitsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

object ExportRecordedDecisionsToSitsCommand {
  type Result = Seq[RecordedDecision]
  type Command = Appliable[Result]

  def apply(): Command =
    new ExportRecordedDecisionsToSitsCommandInternal()
      with ComposableCommand[Result]
      with ExportRecordedDecisionsToSitsCommandPermissions
      with ExportRecordedDecisionsToSitsDescription
      with AutowiringExportDecisionsToSitsServiceComponent
      with AutowiringDecisionServiceComponent
      with AutowiringTransactionalComponent
}

abstract class ExportRecordedDecisionsToSitsCommandInternal
  extends CommandInternal[Result]
    with Logging {
  self: ExportDecisionsToSitsServiceComponent
    with DecisionServiceComponent
    with TransactionalComponent =>

  override def applyInternal(): Result = transactional() {
    decisionService.allNeedingWritingToSits.flatMap { decision =>
      val rowsUpdated = exportDecisionsToSitsService.updateDecision(decision)

      rowsUpdated match {
        case 0 =>
          logger.warn(s"Upload to SITS for decision $decision failed - updated zero rows")
          None

        case d if d > 1 =>
          throw new IllegalStateException(s"Unexpected SITS update! Only expected to insert one row, but $d rows were updated for decision $decision")

        case 1 =>
          logger.info(s"Decision uploaded to SITS - $decision")
          decision.needsWritingToSitsSince = None
          decision.lastWrittenToSits = Some(DateTime.now)
          Some(decisionService.saveOrUpdate(decision))
      }
    }
  }
}

trait ExportRecordedDecisionsToSitsCommandPermissions extends RequiresPermissionsChecking {
  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Marks.UploadToSits)
  }
}

trait ExportRecordedDecisionsToSitsDescription extends Describable[Result] {
  override lazy val eventName: String = "ExportRecordedDecisionsToSits"

  override def describe(d: Description): Unit = {}

  override def describeResult(d: Description, result: Result): Unit =
    d.property("decisions", result)
}
