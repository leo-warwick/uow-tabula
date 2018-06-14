package uk.ac.warwick.tabula.commands.exams.grids


import org.apache.poi.ss.usermodel._
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.grids.StatusAdapter
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.exams.grids.columns.modules.{ModuleExamGridColumn, ModuleReportsColumn}
import uk.ac.warwick.tabula.services.exams.grids.NormalLoadLookup

object GenerateExamGridShortFormExporter extends TaskBenchmarking {

	import ExamGridExportStyles._

	def apply(
		department: Department,
		academicYear: AcademicYear,
		courses: Seq[Course],
		routes: Seq[Route],
		yearOfStudy: Int,
		yearWeightings: Map[Course, Seq[CourseYearWeighting]],
		normalLoadLookup: NormalLoadLookup,
		entities: Seq[ExamGridEntity],
		leftColumns: Seq[ChosenYearExamGridColumn],
		perYearColumns: Map[StudentCourseYearDetails.YearOfStudy, Seq[PerYearExamGridColumn]],
		rightColumns: Seq[ChosenYearExamGridColumn],
		chosenYearColumnValues: Map[ChosenYearExamGridColumn, Map[ExamGridEntity, ExamGridColumnValue]],
		perYearColumnValues: Map[PerYearExamGridColumn, Map[ExamGridEntity, Map[StudentCourseYearDetails.YearOfStudy, Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]]]]],
		moduleColumnsPerEntity: Map[ExamGridEntity, Map[YearOfStudy, Seq[Option[ModuleExamGridColumn]]]],
		perYearModuleMarkColumns: Map[YearOfStudy, Seq[ModuleExamGridColumn]],
		perYearModuleReportColumns: Map[YearOfStudy, Seq[ModuleReportsColumn]],
		maxYearColumnSize: Map[YearOfStudy, Int],
		showComponentMarks: Boolean,
		yearOrder: Ordering[Int] = Ordering.Int,
		mergedCells: Boolean = true,
		status: StatusAdapter
	): Workbook = {
		// Allow randomly accessing rows at any point during generation, don't flush
		val workbook = new SXSSFWorkbook(null, -1)

		// Styles
		val cellStyleMap = getCellStyleMap(workbook)

		val sheet = workbook.createSheet(academicYear.toString.replace("/","-"))
		sheet.trackAllColumnsForAutoSizing()

		ExamGridSummaryAndKey.summaryAndKey(sheet, cellStyleMap, department, academicYear, courses, routes, yearOfStudy, yearWeightings, normalLoadLookup, entities.size, isStudentCount = true)

		val categoryRow = sheet.createRow(sheet.getLastRowNum + 1)
		val headerRow = sheet.createRow(sheet.getLastRowNum + 1)
		val entityRows = entities.map(entity => entity -> {
			val entityHeader = sheet.createRow(sheet.getLastRowNum + 1)
			val valueRows = if (showComponentMarks) {
				Map[ExamGridColumnValueType, Row](
					ExamGridColumnValueType.Overall -> sheet.createRow(sheet.getLastRowNum + 1),
					ExamGridColumnValueType.Assignment -> sheet.createRow(sheet.getLastRowNum + 1),
					ExamGridColumnValueType.Exam -> sheet.createRow(sheet.getLastRowNum + 1)
				)
			} else {
				Map[ExamGridColumnValueType, Row](ExamGridColumnValueType.Overall -> sheet.createRow(sheet.getLastRowNum + 1))
			}
			(entityHeader, valueRows)
		}).toMap

		val chosenYearColumnCategories = rightColumns.collect { case c: HasExamGridColumnCategory => c }.groupBy(_.category)

		var currentColumnIndex = 3 // Move to the right of the key
		var categoryRowMaxCellWidth = 0
		var headerRowMaxCellWidth = 0
		var entityHeaderRowMaxCellWidth = 0

		benchmarkTask("leftColumns") {
			leftColumns.foreach(leftColumn => {
				// Nothing in category row
				// Header row
				val headerCell = headerRow.createCell(currentColumnIndex)
				headerCell.setCellValue(leftColumn.title)
				sheet.autoSizeColumn(currentColumnIndex)
				headerCell.setCellStyle(cellStyleMap(Header))

				// Nothing in secondary value row
				// Entity rows
				entities.foreach(entity =>
					if (chosenYearColumnValues.get(leftColumn).exists(_.get(entity).isDefined)) {
						val (header, valueRows) = entityRows(entity)
						val entityCell = header.createCell(currentColumnIndex)
						chosenYearColumnValues(leftColumn)(entity).populateCell(entityCell, cellStyleMap)
						if(mergedCells) {
							val lastRow = if(showComponentMarks) entityCell.getRowIndex + 3 else entityCell.getRowIndex + 1
							sheet.addMergedRegion(new CellRangeAddress(entityCell.getRowIndex, lastRow, entityCell.getColumnIndex, entityCell.getColumnIndex))
						} else {
							valueRows.values.foreach(row => {
								val entityCell = row.createCell(currentColumnIndex)
								chosenYearColumnValues(leftColumn)(entity).populateCell(entityCell, cellStyleMap)
							})
						}
					}
				)
				// And finally...
				sheet.setColumnWidth(currentColumnIndex, leftColumn.excelColumnWidth)
				currentColumnIndex = currentColumnIndex + 1
			})
		}

		if (!showComponentMarks) {
			// Add a spacer
			sheet.setColumnWidth(currentColumnIndex, ExamGridColumnOption.ExcelColumnSizes.Spacer)
			currentColumnIndex = currentColumnIndex + 1
		}

		benchmarkTask("perYearColumns") {
			perYearColumns.keys.toSeq.sorted(yearOrder).foreach(year => {
				if (showComponentMarks) {
					entityRows.foreach { case (_, (header, rowMap)) => rowMap.foreach { case (valueType, row) =>
						val cell = row.createCell(currentColumnIndex)
						cell.setCellValue(valueType.label)
					}}
					sheet.setColumnWidth(currentColumnIndex, ExamGridColumnOption.ExcelColumnSizes.Spacer)
					currentColumnIndex = currentColumnIndex + 1
				}

				// for each module column
				for (moduleColumnIndex <- 1 to maxYearColumnSize(year)) {
					// Year heading
					if (moduleColumnIndex == 1) {
						val headerCell = headerRow.createCell(currentColumnIndex)
						headerCell.setCellValue(s"Year $year")
						headerCell.setCellStyle(cellStyleMap(Header))
						headerRowMaxCellWidth = Math.max(headerRowMaxCellWidth, sheet.getColumnWidth(currentColumnIndex))
						val yearWidth = maxYearColumnSize(year) - 1
						if (yearWidth > 0)
							sheet.addMergedRegion(new CellRangeAddress(headerCell.getRowIndex, headerCell.getRowIndex, headerCell.getColumnIndex, headerCell.getColumnIndex + yearWidth))
					}

					// Entity rows
					entities.foreach(entity => benchmarkTask(s"entityRow${entity.universityId}"){
						val (header, valueRows) = entityRows(entity)
						moduleColumnsPerEntity(entity)(year).lift(moduleColumnIndex - 1).foreach(col => {
							val marks = col.map(c => perYearColumnValues.getOrElse(c, Map()).getOrElse(entity,Map()).getOrElse(year,Map())).getOrElse(Map())
							col match {
								// has marks for this module
								case Some(column) if marks.nonEmpty =>
									val headerCell = header.createCell(currentColumnIndex)
									val title = s"${column.title} - ${column.secondaryValue} ${column.categoryShortForm}"
									headerCell.setCellValue(title)
									entityHeaderRowMaxCellWidth = Math.max(entityHeaderRowMaxCellWidth, sheet.getColumnWidth(currentColumnIndex))
									headerCell.setCellStyle(cellStyleMap(Rotated))

									if (showComponentMarks) {
										val overallCell = valueRows(ExamGridColumnValueType.Overall).createCell(currentColumnIndex)
										marks(ExamGridColumnValueType.Overall).head.populateCell(overallCell, cellStyleMap)
										val assignmentCell = valueRows(ExamGridColumnValueType.Assignment).createCell(currentColumnIndex)
										ExamGridColumnValue.merge(marks(ExamGridColumnValueType.Assignment)).populateCell(assignmentCell, cellStyleMap)
										val examsCell = valueRows(ExamGridColumnValueType.Exam).createCell(currentColumnIndex)
										ExamGridColumnValue.merge(marks(ExamGridColumnValueType.Exam)).populateCell(examsCell, cellStyleMap)
									} else {
										val entityCell = valueRows(ExamGridColumnValueType.Overall).createCell(currentColumnIndex)
										marks(ExamGridColumnValueType.Overall).head.populateCell(entityCell, cellStyleMap)
									}

								// blank cell(s)
								case _ =>
									if (showComponentMarks) {
										valueRows(ExamGridColumnValueType.Overall).createCell(currentColumnIndex)
										valueRows(ExamGridColumnValueType.Assignment).createCell(currentColumnIndex)
										valueRows(ExamGridColumnValueType.Exam).createCell(currentColumnIndex)
									} else {
										valueRows(ExamGridColumnValueType.Overall).createCell(currentColumnIndex)
									}
							}
						})
					})

					// and finally ..
					perYearModuleMarkColumns(year).headOption.foreach(c => sheet.setColumnWidth(currentColumnIndex, c.excelColumnWidth))
					currentColumnIndex = currentColumnIndex + 1
				}

				// Module report columns
				perYearModuleReportColumns(year).foreach(reportColumn => {
					// header row
					val headerCell = headerRow.createCell(currentColumnIndex)
					headerCell.setCellValue(reportColumn.title)
					headerCell.setCellStyle(cellStyleMap(Rotated))
					headerRowMaxCellWidth = Math.max(headerRowMaxCellWidth, sheet.getColumnWidth(currentColumnIndex))
					// Entity rows
					entities.foreach(entity => {
						// empty header
						val (header, valueRows) = entityRows(entity)
						header.createCell(currentColumnIndex)
						val overallCell = valueRows(ExamGridColumnValueType.Overall).createCell(currentColumnIndex)
						if (perYearColumnValues.get(reportColumn).exists(_.get(entity).exists(_.get(year).isDefined))) {
							perYearColumnValues(reportColumn)(entity)(year)(ExamGridColumnValueType.Overall).head.populateCell(overallCell, cellStyleMap)
						}
					})
					// and finally ..
					sheet.setColumnWidth(currentColumnIndex, reportColumn.excelColumnWidth)
					currentColumnIndex = currentColumnIndex + 1
				})
			})
		}

		benchmarkTask("rightColumns") {
			var currentCategory = ""
			rightColumns.foreach(rightColumn => {
				// Category row
				rightColumn match {
					case hasCategory: HasExamGridColumnCategory if hasCategory.category != currentCategory =>
						currentCategory = hasCategory.category

						val categoryCell = categoryRow.createCell(currentColumnIndex)
						categoryCell.setCellValue(hasCategory.category)
						sheet.autoSizeColumn(currentColumnIndex)
						categoryRowMaxCellWidth = Math.max(categoryRowMaxCellWidth, sheet.getColumnWidth(currentColumnIndex))
						categoryCell.setCellStyle(cellStyleMap(HeaderRotated))

						// Guard against trying to create a merged region with only one cell in it
						val startColumn = categoryCell.getColumnIndex
						val endColumn = categoryCell.getColumnIndex + chosenYearColumnCategories(hasCategory.category).size - 1

						if (endColumn > startColumn)
							sheet.addMergedRegion(new CellRangeAddress(categoryCell.getRowIndex, categoryCell.getRowIndex, startColumn, endColumn))
					case _ =>
				}

				// Header row
				val headerCell = headerRow.createCell(currentColumnIndex)
				headerCell.setCellValue(rightColumn.title)
				sheet.autoSizeColumn(currentColumnIndex)
				headerRowMaxCellWidth = Math.max(headerRowMaxCellWidth, sheet.getColumnWidth(currentColumnIndex))

				if (rightColumn.boldTitle)
					headerCell.setCellStyle(cellStyleMap(HeaderRotated))
				else
					headerCell.setCellStyle(cellStyleMap(Rotated))


				// Entity rows
				entities.foreach(entity => {
					val (header, valueRows) = entityRows(entity)
					if (chosenYearColumnValues.get(rightColumn).exists(_.get(entity).isDefined)) {
						val entityCell = header.createCell(currentColumnIndex)
						chosenYearColumnValues(rightColumn)(entity).populateCell(entityCell, cellStyleMap)
						if(mergedCells) {
							val lastRow = if(showComponentMarks) entityCell.getRowIndex + 3 else entityCell.getRowIndex + 1
							sheet.addMergedRegion(new CellRangeAddress(entityCell.getRowIndex, lastRow, entityCell.getColumnIndex, entityCell.getColumnIndex))
						} else {
							valueRows.values.foreach(row => {
								val entityCell = row.createCell(currentColumnIndex)
								chosenYearColumnValues(rightColumn)(entity).populateCell(entityCell, cellStyleMap)
							})
						}
					}
				})

				// And finally...
				sheet.setColumnWidth(currentColumnIndex, rightColumn.excelColumnWidth)
				currentColumnIndex = currentColumnIndex + 1
			})
		}

		categoryRow.setHeight(Math.min(4000, categoryRowMaxCellWidth * 0.5).toShort)
		headerRow.setHeight(Math.min(4000, headerRowMaxCellWidth * 0.5).toShort)
		entityRows.values.map{ case (header, _) => header }.foreach(entityHeader =>
			entityHeader.setHeight(Math.min(4000, entityHeaderRowMaxCellWidth * 0.5).toShort)
		)

		workbook
	}

}
