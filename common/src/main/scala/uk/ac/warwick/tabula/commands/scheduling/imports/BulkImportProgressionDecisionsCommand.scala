package uk.ac.warwick.tabula.commands.scheduling.imports

import org.springframework.beans.PropertyAccessorFactory
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.scheduling.imports.BulkImportProgressionDecisionsCommand._
import uk.ac.warwick.tabula.data.model.ProgressionDecision
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.services.scheduling.{AutowiringProgressionDecisionImporterComponent, ProgressionDecisionImporterComponent, ProgressionDecisionRow}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringProgressionDecisionServiceComponent, ModuleAndDepartmentServiceComponent, ProgressionDecisionServiceComponent}

object BulkImportProgressionDecisionsCommand {
  case class Result(created: Int, updated: Int, deleted: Int)
  type Command = Appliable[Result]

  def apply(year: AcademicYear): Command =
    new BulkImportProgressionDecisionsCommandInternal
      with ComposableCommand[Result]
      with ImportSystemDataPermissions
      with PropertyCopying
      with AutowiringProgressionDecisionImporterComponent
      with AutowiringProgressionDecisionServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringTransactionalComponent
      with BulkImportProgressionDecisionsForAcademicYearDescription
      with BulkImportProgressionDecisionsForAcademicYearRequest {
      override val academicYear: AcademicYear = year
    }

  def apply(ids: Seq[String]): Command =
    new BulkImportProgressionDecisionsCommandInternal
      with ComposableCommand[Result]
      with ImportSystemDataPermissions
      with PropertyCopying
      with AutowiringProgressionDecisionImporterComponent
      with AutowiringProgressionDecisionServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringTransactionalComponent
      with BulkImportProgressionDecisionsForUniversityIdsDescription
      with BulkImportProgressionDecisionsForUniversityIdsRequest {
      override val universityIds: Seq[String] = ids
    }
}

abstract class BulkImportProgressionDecisionsCommandInternal extends CommandInternal[Result] with TaskBenchmarking with PropertyCopying {
  self: BulkImportProgressionDecisionsRequest
    with ProgressionDecisionServiceComponent
    with ModuleAndDepartmentServiceComponent
    with TransactionalComponent =>

  private val properties = Set(
    "academicYear", "resitPeriod"
  )

  private def copyProperties(row: ProgressionDecisionRow, decision: ProgressionDecision): Boolean = {
    val rowBean = PropertyAccessorFactory.forBeanPropertyAccess(row)
    val progressionDecisionBean = PropertyAccessorFactory.forBeanPropertyAccess(decision)

    copyBasicProperties(properties, rowBean, progressionDecisionBean) |
    copyOptionProperty(progressionDecisionBean,"outcome", row.outcome) |
    copyOptionProperty(progressionDecisionBean, "notes", row.notes) |
      copyOptionProperty(progressionDecisionBean, "minutes", row.minutes)
  }

  override def applyInternal(): Result = transactional() {
    val rows = allRows

    var created: Int = 0
    var updated: Int = 0

    // decisions that we found in SITS that already existed in Tabula
    val foundDecisions = benchmarkTask("Updating decisions") { rows.flatMap { row =>
      val existing = existingProgressionDecisions.get((row.sprCode, row.sequence))
      val decision = existing.getOrElse(row.toProgressionDecision)

      val hasChanged = copyProperties(row, decision)

      if (existing.isEmpty || hasChanged) {
        logger.debug(s"Saving changes for $decision because ${if (existing.isEmpty) "it's a new object" else "it's changed"}")

        progressionDecisionService.saveOrUpdate(decision)

        if (existing.isEmpty) created += 1
        else updated += 1
      }

      existing
    }}

    val deleted = benchmarkTask("Deleting decisions that weren't found in SITS") {
      val missingDecisions = existingProgressionDecisions.values.toSet -- foundDecisions
      missingDecisions.foreach { pd =>
        logger.debug(s"Deleting $pd")
        progressionDecisionService.delete(pd)
      }

      missingDecisions.size
    }

    Result(created, updated, deleted)
  }
}

trait BulkImportProgressionDecisionsRequest {
  def allRows: Seq[ProgressionDecisionRow]
  def existingProgressionDecisions: Map[(String, String), ProgressionDecision]
}

trait BulkImportProgressionDecisionsForAcademicYearRequest extends BulkImportProgressionDecisionsRequest with TaskBenchmarking {
  self: ProgressionDecisionImporterComponent
    with ProgressionDecisionServiceComponent =>

  def academicYear: AcademicYear

  def yearsToImport: Seq[AcademicYear] = academicYear.yearsSurrounding(3, 0)

  lazy val allRows: Seq[ProgressionDecisionRow] = progressionDecisionImporter.getProgressionDecisionRowsForAcademicYears(yearsToImport)

  lazy val existingProgressionDecisions: Map[(String, String), ProgressionDecision] = benchmarkTask("Fetching existing progression decisions") {
    progressionDecisionService.getByAcademicYears(yearsToImport)
      .map { pd => (pd.sprCode, pd.sequence) -> pd }
      .toMap
  }
}

trait BulkImportProgressionDecisionsForUniversityIdsRequest extends BulkImportProgressionDecisionsRequest with TaskBenchmarking {
  self: ProgressionDecisionImporterComponent
    with ProgressionDecisionServiceComponent =>

  def universityIds: Seq[String]

  lazy val allRows: Seq[ProgressionDecisionRow] = progressionDecisionImporter.getProgressionDecisionRowsForUniversityIds(universityIds)

  lazy val existingProgressionDecisions: Map[(String, String), ProgressionDecision] = benchmarkTask("Fetching existing progression decisions") {
    progressionDecisionService.getAgreedByUniversityIds(universityIds)
      .map { pd => (pd.sprCode, pd.sequence) -> pd }
      .toMap
  }
}

trait BulkImportProgressionDecisionsDescription extends Describable[Result] {
  override def describeResult(d: Description, result: Result): Unit =
    d.properties(
      "progressionDecisionsAdded" -> result.created,
      "progressionDecisionsChanged" -> result.updated,
      "progressionDecisionsDeleted" -> result.deleted,
    )
}

trait BulkImportProgressionDecisionsForAcademicYearDescription extends BulkImportProgressionDecisionsDescription {
  self: BulkImportProgressionDecisionsForAcademicYearRequest =>

  override lazy val eventName: String = "BulkImportProgressionDecisionsForAcademicYear"

  override def describe(d: Description): Unit =
    d.property("academicYears" -> yearsToImport.map(_.toString))
}

trait BulkImportProgressionDecisionsForUniversityIdsDescription extends BulkImportProgressionDecisionsDescription {
  self: BulkImportProgressionDecisionsForUniversityIdsRequest =>

  override lazy val eventName: String = "BulkImportProgressionDecisionsForUniversityIds"

  override def describe(d: Description): Unit =
    d.studentIds(universityIds)
}
