package uk.ac.warwick.tabula.exams.grids.documents

import java.io.ByteArrayOutputStream

import com.google.common.io.ByteSource
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridAuditCommand.SelectCourseCommand
import uk.ac.warwick.tabula.commands.exams.grids._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.grids.StatusAdapter
import uk.ac.warwick.tabula.exams.grids.columns.modules.{ModuleExamGridColumn, ModuleReportsColumn}
import uk.ac.warwick.tabula.exams.grids.data.{GeneratesExamGridData, GridData}
import uk.ac.warwick.tabula.exams.grids.documents.ExamGridDocument._
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringNormalCATSLoadServiceComponent, AutowiringUpstreamRouteRuleServiceComponent}
import uk.ac.warwick.tabula.services.objectstore.AutowiringObjectStorageServiceComponent
import uk.ac.warwick.tabula.services.{AutowiringCourseAndRouteServiceComponent, AutowiringMaintenanceModeServiceComponent, AutowiringModuleRegistrationServiceComponent, AutowiringProgressionServiceComponent}

import scala.collection.JavaConverters._

object ExcelGridDocument extends ExamGridDocumentPrototype {
	override val identifier: String = "ExcelGrid"

	def options(mergedCells: Boolean): Map[String, Any] = Map("mergedCells" -> mergedCells)
}

@Component
class ExcelGridDocument extends ExamGridDocument
	with GeneratesExamGridData
	with AutowiringUpstreamRouteRuleServiceComponent
	with AutowiringProgressionServiceComponent
	with AutowiringNormalCATSLoadServiceComponent
	with AutowiringObjectStorageServiceComponent
	with AutowiringCourseAndRouteServiceComponent
	with AutowiringMaintenanceModeServiceComponent
	with AutowiringModuleRegistrationServiceComponent
	with TaskBenchmarking {
	override val identifier: String = ExcelGridDocument.identifier

	override val contentType: String = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

	def buildCoreRequiredModuleLookup(
		academicYear: AcademicYear,
		yearOfStudy: Int,
		levelCode: String
	): CoreRequiredModuleLookup = {
		if (Option(yearOfStudy).nonEmpty) {
			new CoreRequiredModuleLookupImpl(academicYear, yearOfStudy, moduleRegistrationService)
		} else if (levelCode != null) {
			new CoreRequiredModuleLookupImpl(academicYear, Level.toYearOfStudy(levelCode), moduleRegistrationService)
		} else {
			null
		}
	}

	override def apply(
		department: Department,
		academicYear: AcademicYear,
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand,
		checkOvercatCommand: CheckOvercatCommand,
		options: Map[String, Any],
		status: StatusAdapter
	): FileAttachment = {
		val mergedCells: Boolean = options.get("mergedCells").fold(false)(_.asInstanceOf[Boolean])

		val coreRequiredModuleLookup = buildCoreRequiredModuleLookup(academicYear, selectCourseCommand.yearOfStudy, selectCourseCommand.levelCode)

		val GridData(entities, studentInformationColumns, perYearColumns, summaryColumns, weightings, normalLoadLookup, _) = benchmarkTask("GridData") {
			checkAndApplyOvercatAndGetGridData(
				selectCourseCommand,
				gridOptionsCommand,
				checkOvercatCommand,
				coreRequiredModuleLookup
			)
		}

		status.stageCount = 4

		val chosenYearColumnValues = benchmarkTask("chosenYearColumnValues") {
			status.stageNumber = 1
			val columns = Seq(studentInformationColumns, summaryColumns).flatten

			columns.zipWithIndex.map { case (c, i) =>
				status.setMessage(s"Populating column: ${c.title}")
				status.setProgress(i, columns.size)

				c -> c.values
			}.toMap
		}

		val perYearColumnValues = benchmarkTask("perYearColumnValues") {
			status.stageNumber = 2
			val columns = perYearColumns.values.flatten.toSeq

			columns.zipWithIndex.map { case (c, i) =>
				status.setMessage(s"Populating column: ${c.title}")
				status.setProgress(i, columns.size)

				c -> c.values
			}.toMap
		}

		status.stageNumber = 3
		status.setMessage("Building the spreadsheet")

		val workbook = if (gridOptionsCommand.showFullLayout) {
			GenerateExamGridExporter(
				department = department,
				academicYear = academicYear,
				courses = selectCourseCommand.courses.asScala,
				routes = selectCourseCommand.routes.asScala,
				yearOfStudy = selectCourseCommand.yearOfStudy,
				yearWeightings = weightings,
				normalLoadLookup = normalLoadLookup,
				entities = entities,
				leftColumns = studentInformationColumns,
				perYearColumns = perYearColumns,
				rightColumns = summaryColumns,
				chosenYearColumnValues = chosenYearColumnValues,
				perYearColumnValues = perYearColumnValues,
				showComponentMarks = gridOptionsCommand.showComponentMarks,
				mergedCells = mergedCells,
				status = status
			)
		} else {
			val perYearModuleMarkColumns = benchmarkTask("perYearModuleMarkColumns") {
				perYearColumns.map { case (year, columns) => year -> columns.collect { case marks: ModuleExamGridColumn => marks } }
			}
			val perYearModuleReportColumns = benchmarkTask("perYearModuleReportColumns") {
				perYearColumns.map { case (year, columns) => year -> columns.collect { case marks: ModuleReportsColumn => marks } }
			}

			val maxYearColumnSize = benchmarkTask("maxYearColumnSize") {
				perYearModuleMarkColumns.map { case (year, columns) =>
					val maxModuleColumns = (entities.map(entity => columns.count(c => !c.isEmpty(entity, year))) ++ Seq(1)).max
					year -> maxModuleColumns
				}
			}

			// for each entity have a list of all modules with marks and padding at the end for empty cells
			val moduleColumnsPerEntity = benchmarkTask("moduleColumnsPerEntity") {
				entities.map(entity => {
					entity -> perYearModuleMarkColumns.map { case (year, modules) =>
						val hasValue: Seq[Option[ModuleExamGridColumn]] = modules.filter(m => !m.isEmpty(entity, year)).map(Some.apply)
						val padding: Seq[Option[ModuleExamGridColumn]] = (1 to maxYearColumnSize(year) - hasValue.size).map(_ => None)
						year -> (hasValue ++ padding)
					}
				}).toMap
			}

			GenerateExamGridShortFormExporter(
				department = department,
				academicYear = academicYear,
				courses = selectCourseCommand.courses.asScala,
				routes = selectCourseCommand.routes.asScala,
				yearOfStudy = selectCourseCommand.yearOfStudy,
				yearWeightings = weightings,
				normalLoadLookup = normalLoadLookup,
				entities = entities,
				leftColumns = studentInformationColumns,
				perYearColumns = perYearColumns,
				rightColumns = summaryColumns,
				chosenYearColumnValues = chosenYearColumnValues,
				perYearColumnValues = perYearColumnValues,
				moduleColumnsPerEntity = moduleColumnsPerEntity,
				perYearModuleMarkColumns = perYearModuleMarkColumns,
				perYearModuleReportColumns = perYearModuleReportColumns,
				maxYearColumnSize,
				showComponentMarks = gridOptionsCommand.showComponentMarks,
				mergedCells = mergedCells,
				status = status
			)
		}

		status.stageNumber = 4
		status.setMessage("Finalising the spreadsheet")
		status.setProgress(0)

		val file = new FileAttachment

		file.name = "Exam grid for %s %s %s %s.xlsx".format(
			department.name,
			selectCourseCommand.courses.size match {
				case 1 => selectCourseCommand.courses.get(0).code
				case n => s"$n courses"
			},
			selectCourseCommand.routes.size match {
				case 0 => "All routes"
				case 1 => selectCourseCommand.routes.get(0).code.toUpperCase
				case n => s"$n routes"
			},
			academicYear.toString.replace("/", "-")
		)

		val out = new ByteArrayOutputStream()
		workbook.write(out)
		out.close()

		file.uploadedData = ByteSource.wrap(out.toByteArray)

		file
	}
}
