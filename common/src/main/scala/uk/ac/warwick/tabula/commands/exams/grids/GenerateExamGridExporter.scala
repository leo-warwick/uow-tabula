package uk.ac.warwick.tabula.commands.exams.grids

import java.awt.Color

import org.apache.poi.ss.usermodel._
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.{XSSFColor, XSSFFont}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.grids.StatusAdapter
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.exams.grids.NormalLoadLookup

object GenerateExamGridExporter extends TaskBenchmarking {

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
		showComponentMarks: Boolean,
		yearOrder: Ordering[Int] = Ordering.Int,
		mergedCells: Boolean = true,
		status: StatusAdapter
	): Workbook = {
		var progress = 0
		val progressMax = entities.size * (leftColumns.size + perYearColumns.size * perYearColumns.values.map(_.size).sum + rightColumns.size)
		def stepProgress(): Unit = {
			progress = progress + 1
			status.setProgress(progress, progressMax)
		}

		// Allow randomly accessing rows at any point during generation, don't flush
		val workbook = new SXSSFWorkbook(null, -1)

		// Styles
		val cellStyleMap = getCellStyleMap(workbook)

		val sheet = workbook.createSheet(academicYear.toString.replace("/","-"))
		sheet.trackAllColumnsForAutoSizing()

		ExamGridSummaryAndKey.summaryAndKey(sheet, cellStyleMap, department, academicYear, courses, routes, yearOfStudy, yearWeightings, normalLoadLookup, entities.size, isStudentCount = true)

		val yearRow = sheet.createRow(sheet.getLastRowNum + 1)
		val categoryRow = sheet.createRow(sheet.getLastRowNum + 1)
		val headerRow = sheet.createRow(sheet.getLastRowNum + 1)
		val secondaryValueRow = sheet.createRow(sheet.getLastRowNum + 1)
		val entityRows = entities.map(entity => entity -> {
			if (showComponentMarks) {
				Map[ExamGridColumnValueType, Row](
					ExamGridColumnValueType.Overall -> sheet.createRow(sheet.getLastRowNum + 1),
					ExamGridColumnValueType.Assignment -> sheet.createRow(sheet.getLastRowNum + 1),
					ExamGridColumnValueType.Exam -> sheet.createRow(sheet.getLastRowNum + 1)
				)
			} else {
				Map[ExamGridColumnValueType, Row](ExamGridColumnValueType.Overall -> sheet.createRow(sheet.getLastRowNum + 1))
			}
		}).toMap

		val chosenYearColumnCategories = rightColumns.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category)
		val perYearColumnCategories = perYearColumns.mapValues(_.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category))

		var currentColumnIndex = 3 // Move to the right of the key
		var categoryRowMaxCellWidth = 0
		var headerRowMaxCellWidth = 0

		benchmarkTask("leftColumns") {
			leftColumns.foreach(leftColumn => {
				// Nothing in year row
				// Nothing in category row
				// Header row
				val headerCell = headerRow.createCell(currentColumnIndex)
				headerCell.setCellValue(leftColumn.title)
				headerCell.setCellStyle(cellStyleMap(Header))
				if (!leftColumn.isInstanceOf[HasExamGridColumnSecondaryValue]) {
					// rowspan = 2
					sheet.addMergedRegion(new CellRangeAddress(headerCell.getRowIndex, headerCell.getRowIndex + 1, headerCell.getColumnIndex, headerCell.getColumnIndex))
				}
				// Nothing in secondary value row
				// Entity rows
				entities.foreach(entity => {
					if (chosenYearColumnValues.get(leftColumn).exists(_.get(entity).isDefined)) {
						val entityCell = entityRows(entity)(ExamGridColumnValueType.Overall).createCell(currentColumnIndex)
						chosenYearColumnValues(leftColumn)(entity).populateCell(entityCell, cellStyleMap)
						if (showComponentMarks) {
							if (mergedCells) {
								sheet.addMergedRegion(new CellRangeAddress(entityCell.getRowIndex, entityCell.getRowIndex + 2, entityCell.getColumnIndex, entityCell.getColumnIndex))
							} else {
								entityRows(entity).filterKeys(_ != ExamGridColumnValueType.Overall).values.foreach(row => {
									val cell = row.createCell(currentColumnIndex)
									chosenYearColumnValues(leftColumn)(entity).populateCell(cell, cellStyleMap)
								})
							}
						}
					}
					stepProgress()
				})
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
					entityRows.foreach { case (_, rowMap) => rowMap.foreach { case (valueType, row) =>
						val cell = row.createCell(currentColumnIndex)
						cell.setCellValue(valueType.label)
					}
					}
					sheet.setColumnWidth(currentColumnIndex, ExamGridColumnOption.ExcelColumnSizes.Spacer)
					currentColumnIndex = currentColumnIndex + 1
				}

				// Year row
				val yearCell = yearRow.createCell(currentColumnIndex)
				yearCell.setCellValue(s"Year $year")
				yearCell.setCellStyle(cellStyleMap(Header))

				val startColumn = yearCell.getColumnIndex
				val endColumn = yearCell.getColumnIndex + Math.max(perYearColumns(year).size - 1, 0)

				if (endColumn > startColumn)
					sheet.addMergedRegion(new CellRangeAddress(yearCell.getRowIndex, yearCell.getRowIndex, startColumn, endColumn))

				var currentCategory = ""
				perYearColumns(year).foreach(perYearColumn => {
					// Category row
					perYearColumn match {
						case hasCategory: HasExamGridColumnCategory if hasCategory.category != currentCategory =>
							currentCategory = hasCategory.category
							val categoryCell = categoryRow.createCell(currentColumnIndex)
							categoryCell.setCellValue(hasCategory.category)
							sheet.autoSizeColumn(currentColumnIndex)
							categoryRowMaxCellWidth = Math.max(categoryRowMaxCellWidth, sheet.getColumnWidth(currentColumnIndex))
							categoryCell.setCellStyle(cellStyleMap(HeaderRotated))

							// Guard against trying to create a merged region with only one cell in it
							val startColumn = categoryCell.getColumnIndex
							val endColumn = categoryCell.getColumnIndex + perYearColumnCategories(year)(hasCategory.category).size - 1

							if (endColumn > startColumn)
								sheet.addMergedRegion(new CellRangeAddress(categoryCell.getRowIndex, categoryCell.getRowIndex, startColumn, endColumn))
						case _ =>
					}
					// Header row
					val headerCell = headerRow.createCell(currentColumnIndex)
					headerCell.setCellValue(perYearColumn.title)
					headerRowMaxCellWidth = Math.max(headerRowMaxCellWidth, sheet.getColumnWidth(currentColumnIndex))

					if (perYearColumn.boldTitle)
						headerCell.setCellStyle(cellStyleMap(HeaderRotated))
					else
						headerCell.setCellStyle(cellStyleMap(Rotated))

					if (!perYearColumn.isInstanceOf[HasExamGridColumnCategory]) {
						// rowspan = 2
						sheet.addMergedRegion(new CellRangeAddress(headerCell.getRowIndex, headerCell.getRowIndex + 1, headerCell.getColumnIndex, headerCell.getColumnIndex))
					}
					// Secondary value row
					perYearColumn match {
						case hasSecondaryValue: HasExamGridColumnSecondaryValue =>
							val secondaryValueCell = secondaryValueRow.createCell(currentColumnIndex)
							secondaryValueCell.setCellValue(hasSecondaryValue.secondaryValue)
						case _ =>
					}
					// Entity rows
					entities.foreach(entity => {
						if (perYearColumnValues.get(perYearColumn).exists(_.get(entity).exists(_.get(year).isDefined))) {
							if (showComponentMarks) {
								val overallCell = entityRows(entity)(ExamGridColumnValueType.Overall).createCell(currentColumnIndex)
								perYearColumnValues(perYearColumn)(entity)(year)(ExamGridColumnValueType.Overall).head.populateCell(overallCell, cellStyleMap)
								val assignmentCell = entityRows(entity)(ExamGridColumnValueType.Assignment).createCell(currentColumnIndex)
								ExamGridColumnValue.merge(perYearColumnValues(perYearColumn)(entity)(year)(ExamGridColumnValueType.Assignment)).populateCell(assignmentCell, cellStyleMap)
								val examsCell = entityRows(entity)(ExamGridColumnValueType.Exam).createCell(currentColumnIndex)
								ExamGridColumnValue.merge(perYearColumnValues(perYearColumn)(entity)(year)(ExamGridColumnValueType.Exam)).populateCell(examsCell, cellStyleMap)
							} else {
								val entityCell = entityRows(entity)(ExamGridColumnValueType.Overall).createCell(currentColumnIndex)
								perYearColumnValues(perYearColumn)(entity)(year)(ExamGridColumnValueType.Overall).head.populateCell(entityCell, cellStyleMap)
							}
						}
						stepProgress()
					})
					// And finally...
					sheet.setColumnWidth(currentColumnIndex, perYearColumn.excelColumnWidth)
					currentColumnIndex = currentColumnIndex + 1
				})

				if (perYearColumns(year).isEmpty) {
					currentColumnIndex = currentColumnIndex + 1
				}

				if (!showComponentMarks || perYearColumns.keys.toSeq.sorted(yearOrder).last == year) {
					// Add a spacer after each year
					sheet.setColumnWidth(currentColumnIndex, ExamGridColumnOption.ExcelColumnSizes.Spacer)
					currentColumnIndex = currentColumnIndex + 1
				}
			})
		}

		benchmarkTask("rightColumns") {
			var currentCategory = ""
			rightColumns.foreach(rightColumn => {
				// Nothing in year row
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

				if (rightColumn.boldTitle)
					headerCell.setCellStyle(cellStyleMap(HeaderRotated))
				else
					headerCell.setCellStyle(cellStyleMap(Rotated))

				if (!rightColumn.isInstanceOf[HasExamGridColumnSecondaryValue]) {
					// rowspan = 2
					sheet.addMergedRegion(new CellRangeAddress(headerCell.getRowIndex, headerCell.getRowIndex + 1, headerCell.getColumnIndex, headerCell.getColumnIndex))
				}
				// Secondary value row
				rightColumn match {
					case hasSecondaryValue: HasExamGridColumnSecondaryValue =>
						val secondaryValueCell = secondaryValueRow.createCell(currentColumnIndex)
						secondaryValueCell.setCellValue(hasSecondaryValue.secondaryValue)
					case _ =>
				}
				// Entity rows
				entities.foreach(entity => {
					if (chosenYearColumnValues.get(rightColumn).exists(_.get(entity).isDefined)) {
						val entityCell = entityRows(entity)(ExamGridColumnValueType.Overall).createCell(currentColumnIndex)
						chosenYearColumnValues(rightColumn)(entity).populateCell(entityCell, cellStyleMap)
						if (showComponentMarks) {
							if (mergedCells) {
								sheet.addMergedRegion(new CellRangeAddress(entityCell.getRowIndex, entityCell.getRowIndex + 2, entityCell.getColumnIndex, entityCell.getColumnIndex))
							} else {
								entityRows(entity).filterKeys(_ != ExamGridColumnValueType.Overall).values.foreach(row => {
									val cell = row.createCell(currentColumnIndex)
									chosenYearColumnValues(rightColumn)(entity).populateCell(cell, cellStyleMap)
								})
							}
						}
					}
					stepProgress()
				})
				// And finally...
				sheet.setColumnWidth(currentColumnIndex, rightColumn.excelColumnWidth)
				currentColumnIndex = currentColumnIndex + 1
			})
		}

		categoryRow.setHeight(Math.min(4000, categoryRowMaxCellWidth * 0.5).toShort)
		headerRow.setHeight(Math.min(4000, headerRowMaxCellWidth * 0.5).toShort)

		workbook
	}

}

object ExamGridExportStyles {

	sealed trait Style
	case object Header extends Style
	case object HeaderRotated extends Style
	case object Rotated extends Style
	case object Fail extends Style
	case object Overcat extends Style
	case object Overridden extends Style
	case object ActualMark extends Style
	case object FailAndActualMark extends Style
	case object OvercatAndActualMark extends Style
	case object BoldText extends Style


	def getCellStyleMap(workbook: Workbook): Map[Style, CellStyle] = {
		val headerStyle = {
			val cs = workbook.createCellStyle()
			val boldFont = workbook.createFont()
			boldFont.setFontHeightInPoints(10)
			boldFont.setBold(true)
			cs.setFont(boldFont)
			cs.setVerticalAlignment(VerticalAlignment.CENTER)
			cs
		}

		val headerRotatedStyle = {
			val cs = workbook.createCellStyle()
			val boldFont = workbook.createFont()
			boldFont.setFontHeightInPoints(10)
			boldFont.setBold(true)
			cs.setFont(boldFont)
			cs.setRotation(90)
			cs.setAlignment(HorizontalAlignment.CENTER)
			cs
		}

		val rotatedStyle = {
			val cs = workbook.createCellStyle()
			cs.setRotation(90)
			cs.setAlignment(HorizontalAlignment.CENTER)
			cs
		}

		val failStyle = {
			val cs = workbook.createCellStyle()
			val redFont = workbook.createFont().asInstanceOf[XSSFFont]
			redFont.setFontHeightInPoints(10)
			redFont.setColor(new XSSFColor(new Color(175, 39, 35)))
			redFont.setUnderline(FontUnderline.DOUBLE)
			cs.setFont(redFont)
			cs
		}

		val overcatStyle = {
			val cs = workbook.createCellStyle()
			val greenFont = workbook.createFont().asInstanceOf[XSSFFont]
			greenFont.setFontHeightInPoints(10)
			greenFont.setColor(new XSSFColor(new Color(89, 110, 49)))
			greenFont.setUnderline(FontUnderline.SINGLE)
			cs.setFont(greenFont)
			cs
		}

		val overriddenStyle = {
			val cs = workbook.createCellStyle()
			val blueFont = workbook.createFont().asInstanceOf[XSSFFont]
			blueFont.setFontHeightInPoints(10)
			blueFont.setColor(new XSSFColor(new Color(32, 79, 121)))
			cs.setFont(blueFont)
			cs
		}

		val actualMarkStyle = {
			val cs = workbook.createCellStyle()
			val blueFont = workbook.createFont().asInstanceOf[XSSFFont]
			blueFont.setFontHeightInPoints(10)
			blueFont.setColor(new XSSFColor(new Color(35, 155, 146)))
			blueFont.setItalic(true)
			cs.setFont(blueFont)
			cs
		}

		val failAndActualMarkStyle = {
			val cs = workbook.createCellStyle()
			val redFont = workbook.createFont().asInstanceOf[XSSFFont]
			redFont.setFontHeightInPoints(10)
			redFont.setColor(new XSSFColor(new Color(175, 39, 35)))
			redFont.setUnderline(FontUnderline.DOUBLE)
			redFont.setItalic(true)
			cs.setFont(redFont)
			cs
		}

		val overcatAndActualMarkStyle = {
			val cs = workbook.createCellStyle()
			val greenFont = workbook.createFont().asInstanceOf[XSSFFont]
			greenFont.setFontHeightInPoints(10)
			greenFont.setColor(new XSSFColor(new Color(89, 110, 49)))
			greenFont.setItalic(true)
			greenFont.setUnderline(FontUnderline.SINGLE)
			cs.setFont(greenFont)
			cs
		}

		val boldText = {
			val cs = workbook.createCellStyle()
			val boldFont = workbook.createFont()
			boldFont.setFontHeightInPoints(10)
			boldFont.setBold(true)
			cs.setFont(boldFont)
			cs
		}


		Map(
			Header -> headerStyle,
			HeaderRotated -> headerRotatedStyle,
			Rotated -> rotatedStyle,
			Fail -> failStyle,
			Overcat -> overcatStyle,
			Overridden -> overriddenStyle,
			ActualMark -> actualMarkStyle,
			FailAndActualMark -> failAndActualMarkStyle,
			OvercatAndActualMark -> overcatAndActualMarkStyle,
			BoldText -> boldText
		)
	}
}

object ExamGridSummaryAndKey {

	def summaryAndKey(
		sheet: Sheet,
		cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle],
		department: Department,
		academicYear: AcademicYear,
		courses: Seq[Course],
		routes: Seq[Route],
		yearOfStudy: Int,
		yearWeightings: Map[Course, Seq[CourseYearWeighting]],
		normalLoadLookup: NormalLoadLookup,
		count: Int,
		isStudentCount: Boolean
	): Unit = {
		def keyValueCells(key: String, value: String, rowIndex: Int) = {
			val row = sheet.createRow(rowIndex)
			val keyCell = row.createCell(0)
			keyCell.setCellValue(key)
			keyCell.setCellStyle(cellStyleMap(ExamGridExportStyles.Header))
			val valueCell = row.createCell(1)
			valueCell.setCellValue(value)
			row
		}
		val gridType =  department.examGridOptions.layout match {
			case "short" => "Short Grid"
			case _ => "Full Grid"
		}
		keyValueCells("Grid type:", gridType, 0)
		keyValueCells("Department:", department.name, 1)
		keyValueCells("Academic year:", academicYear.toString, 2)
		courses.size match {
			case 1 => keyValueCells("Course:", s"${courses.head.code} ${courses.head.name}", 3)
			case n if n > 0 => keyValueCells("Courses:", s"${courses.map(_.code).mkString(", ")}", 3)
		}
		routes.size match {
			case 0 => keyValueCells("Routes:", "All routes", 4)
			case 1 => keyValueCells("Route:", s"${routes.head.code.toUpperCase} ${routes.head.name}", 4)
			case n => keyValueCells("Routes:", s"$n routes", 4)
		}
		keyValueCells("Year of study:", yearOfStudy.toString, 5)

		yearWeightings.size match {
			case 0 => keyValueCells("Year weightings:", "", 6)
			case 1 =>
				val yearWeightingRow =
					keyValueCells("Year weightings:", yearWeightings.head._2.map(cyw => s"Year ${cyw.yearOfStudy} = ${cyw.weightingAsPercentage.toPlainString}%").mkString("\n"), 6)
				yearWeightingRow.setHeight((yearWeightingRow.getHeight * (yearWeightings.size - 1)).toShort)
			case n if n > 0 =>
				val weightingByCourse = yearWeightings.map{case (course, cyws) =>
					s"${course.code}: ${cyws.map(cyw => s"Year ${cyw.yearOfStudy} = ${cyw.weightingAsPercentage.toPlainString}%").mkString(", ")}"
				}.mkString("\n")
				val yearWeightingRow = keyValueCells("Year weightings:", weightingByCourse, 6)
				yearWeightingRow.setHeight((yearWeightingRow.getHeight * yearWeightings.size).toShort)
		}
		val normalCATSLoadRow = keyValueCells("Normal CATS load:", normalLoadLookup.routes.sortBy(_.code).map(r => s"${r.code.toUpperCase}: ${normalLoadLookup(r).underlying.toString}").mkString("\n"), 7)
		normalCATSLoadRow.setHeight((normalCATSLoadRow.getHeight * (normalLoadLookup.routes.size - 1)).toShort)
		if (isStudentCount) {
			keyValueCells("Student Count:", count.toString, 8)
		} else {
			keyValueCells("Count:", count.toString, 8)
		}
		keyValueCells("Grid Generated:", DateTime.now.toString, 9)

		{
			val row = sheet.createRow(10)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("#")
			keyCell.setCellStyle(cellStyleMap(ExamGridExportStyles.Fail))
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Failed module or component")
		}
		{
			val row = sheet.createRow(11)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("#")
			keyCell.setCellStyle(cellStyleMap(ExamGridExportStyles.Overcat))
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Used in overcatting calculation")
		}
		{
			val row = sheet.createRow(12)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("#")
			keyCell.setCellStyle(cellStyleMap(ExamGridExportStyles.ActualMark))
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Agreed mark missing, using actual")
		}
		{
			val row = sheet.createRow(13)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("[# (#)]")
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Resit mark (original mark)")
		}
		{
			val row = sheet.createRow(14)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("X")
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Agreed mark and actual mark missing")
		}
		{
			val row = sheet.createRow(15)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("")
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Blank indicates module not taken by student")
		}
		{
			val row = sheet.createRow(16)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("AB")
			keyCell.setCellStyle(cellStyleMap(ExamGridExportStyles.BoldText))
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Bold module name indicates a duplicate table entry")
		}

		sheet.autoSizeColumn(0)
		sheet.autoSizeColumn(1)
	}
}