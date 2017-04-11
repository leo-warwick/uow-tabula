package uk.ac.warwick.tabula.commands.exams.grids

import java.awt.Color

import org.apache.poi.ss.usermodel.{FontUnderline, HorizontalAlignment, VerticalAlignment}
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.exams.grids.NormalLoadLookup

object GenerateExamGridExporter {

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

	def apply(
		department: Department,
		academicYear: AcademicYear,
		course: Course,
		routes: Seq[Route],
		yearOfStudy: Int,
		yearWeightings: Seq[CourseYearWeighting],
		normalLoadLookup: NormalLoadLookup,
		entities: Seq[ExamGridEntity],
		leftColumns: Seq[ChosenYearExamGridColumn],
		perYearColumns: Map[StudentCourseYearDetails.YearOfStudy, Seq[PerYearExamGridColumn]],
		rightColumns: Seq[ChosenYearExamGridColumn],
		chosenYearColumnValues: Map[ChosenYearExamGridColumn, Map[ExamGridEntity, ExamGridColumnValue]],
		perYearColumnValues: Map[PerYearExamGridColumn, Map[ExamGridEntity, Map[StudentCourseYearDetails.YearOfStudy, Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]]]]],
		showComponentMarks: Boolean,
		yearOrder: Ordering[Int] = Ordering.Int
	): XSSFWorkbook = {
		val workbook = new XSSFWorkbook()

		// Styles
		val cellStyleMap = getCellStyleMap(workbook)

		val sheet = workbook.createSheet(academicYear.toString.replace("/","-"))

		summaryAndKey(sheet, cellStyleMap, department, academicYear, course, routes, yearOfStudy, yearWeightings, normalLoadLookup, entities.size, isStudentCount = true)

		val yearRow = sheet.createRow(sheet.getLastRowNum + 1)
		val categoryRow = sheet.createRow(sheet.getLastRowNum + 1)
		val headerRow = sheet.createRow(sheet.getLastRowNum + 1)
		val secondaryValueRow = sheet.createRow(sheet.getLastRowNum + 1)
		val entityRows = entities.map(entity => entity -> {
			if (showComponentMarks) {
				Map[ExamGridColumnValueType, XSSFRow](
					ExamGridColumnValueType.Overall -> sheet.createRow(sheet.getLastRowNum + 1),
					ExamGridColumnValueType.Assignment -> sheet.createRow(sheet.getLastRowNum + 1),
					ExamGridColumnValueType.Exam -> sheet.createRow(sheet.getLastRowNum + 1)
				)
			} else {
				Map[ExamGridColumnValueType, XSSFRow](ExamGridColumnValueType.Overall -> sheet.createRow(sheet.getLastRowNum + 1))
			}
		}).toMap

		val chosenYearColumnCategories = rightColumns.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category)
		val perYearColumnCategories = perYearColumns.mapValues(_.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category))

		var currentColumnIndex = 3 // Move to the right of the key
		var categoryRowMaxCellWidth = 0
		var headerRowMaxCellWidth = 0

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
			entities.foreach(entity =>
				if (chosenYearColumnValues.get(leftColumn).exists(_.get(entity).isDefined)) {
					val entityCell = entityRows(entity)(ExamGridColumnValueType.Overall).createCell(currentColumnIndex)
					chosenYearColumnValues(leftColumn)(entity).populateCell(entityCell, cellStyleMap)
					if (showComponentMarks) {
						sheet.addMergedRegion(new CellRangeAddress(entityCell.getRowIndex, entityCell.getRowIndex + 2, entityCell.getColumnIndex, entityCell.getColumnIndex))
					}
				}
			)
			// And finally...
			sheet.setColumnWidth(currentColumnIndex, leftColumn.excelColumnWidth)
			currentColumnIndex = currentColumnIndex + 1
		})

		if (!showComponentMarks) {
			// Add a spacer
			sheet.setColumnWidth(currentColumnIndex, ExamGridColumnOption.ExcelColumnSizes.Spacer)
			currentColumnIndex = currentColumnIndex + 1
		}

		perYearColumns.keys.toSeq.sorted(yearOrder).foreach(year => {
			if (showComponentMarks) {
				entityRows.foreach { case (_, rowMap) => rowMap.foreach { case (valueType, row) =>
					val cell = row.createCell(currentColumnIndex)
					cell.setCellValue(valueType.label)
				}}
				sheet.setColumnWidth(currentColumnIndex, ExamGridColumnOption.ExcelColumnSizes.Spacer)
				currentColumnIndex = currentColumnIndex + 1
			}

			// Year row
			val yearCell = yearRow.createCell(currentColumnIndex)
			yearCell.setCellValue(s"Year $year")
			yearCell.setCellStyle(cellStyleMap(Header))
			sheet.addMergedRegion(new CellRangeAddress(yearCell.getRowIndex, yearCell.getRowIndex, yearCell.getColumnIndex, yearCell.getColumnIndex + Math.max(perYearColumns(year).size - 1, 0)))

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
						sheet.addMergedRegion(new CellRangeAddress(categoryCell.getRowIndex, categoryCell.getRowIndex, categoryCell.getColumnIndex, categoryCell.getColumnIndex + perYearColumnCategories(year)(hasCategory.category).size - 1))
					case _ =>
				}
				// Header row
				val headerCell = headerRow.createCell(currentColumnIndex)
				headerCell.setCellValue(perYearColumn.title)
				sheet.autoSizeColumn(currentColumnIndex)
				headerRowMaxCellWidth = Math.max(headerRowMaxCellWidth, sheet.getColumnWidth(currentColumnIndex))

				if(perYearColumn.boldTitle)
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
				entities.foreach(entity =>
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
				)
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
					sheet.addMergedRegion(new CellRangeAddress(categoryCell.getRowIndex, categoryCell.getRowIndex, categoryCell.getColumnIndex, categoryCell.getColumnIndex + chosenYearColumnCategories(hasCategory.category).size - 1))
				case _ =>
			}
			// Header row
			val headerCell = headerRow.createCell(currentColumnIndex)
			headerCell.setCellValue(rightColumn.title)

			if(rightColumn.boldTitle)
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
			entities.foreach(entity =>
				if (chosenYearColumnValues.get(rightColumn).exists(_.get(entity).isDefined)) {
					val entityCell = entityRows(entity)(ExamGridColumnValueType.Overall).createCell(currentColumnIndex)
					chosenYearColumnValues(rightColumn)(entity).populateCell(entityCell, cellStyleMap)
					if (showComponentMarks) {
						sheet.addMergedRegion(new CellRangeAddress(entityCell.getRowIndex, entityCell.getRowIndex + 2, entityCell.getColumnIndex, entityCell.getColumnIndex))
					}
				}
			)
			// And finally...
			sheet.setColumnWidth(currentColumnIndex, rightColumn.excelColumnWidth)
			currentColumnIndex = currentColumnIndex + 1
		})

		categoryRow.setHeight(Math.min(4000, categoryRowMaxCellWidth * 0.5).toShort)
		headerRow.setHeight(Math.min(4000, headerRowMaxCellWidth * 0.5).toShort)

		workbook
	}

	private def summaryAndKey(
		sheet: XSSFSheet,
		cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle],
		department: Department,
		academicYear: AcademicYear,
		course: Course,
		routes: Seq[Route],
		yearOfStudy: Int,
		yearWeightings: Seq[CourseYearWeighting],
		normalLoadLookup: NormalLoadLookup,
		count: Int,
		isStudentCount: Boolean
	): Unit = {
		def keyValueCells(key: String, value: String, rowIndex: Int) = {
			val row = sheet.createRow(rowIndex)
			val keyCell = row.createCell(0)
			keyCell.setCellValue(key)
			keyCell.setCellStyle(cellStyleMap(Header))
			val valueCell = row.createCell(1)
			valueCell.setCellValue(value)
			row
		}
		keyValueCells("Department:", department.name, 0)
		keyValueCells("Academic year:", academicYear.toString, 1)
		keyValueCells("Course:", s"${course.code.toUpperCase} ${course.name}", 2)
		routes.size match {
			case 0 => keyValueCells("Routes:", "All routes", 3)
			case 1 => keyValueCells("Route:", s"${routes.head.code.toUpperCase} ${routes.head.name}", 3)
			case n => keyValueCells("Routes:", s"$n routes", 3)
		}
		keyValueCells("Year of study:", yearOfStudy.toString, 4)
		val yearWeightingRow = keyValueCells("Year weightings:", yearWeightings.map(cyw => s"Year ${cyw.yearOfStudy} = ${cyw.weightingAsPercentage}%").mkString("\n"), 5)
		yearWeightingRow.setHeight((yearWeightingRow.getHeight * (yearWeightings.size - 1)).toShort)
		val normalCATSLoadRow = keyValueCells("Normal CATS load:", normalLoadLookup.routes.sortBy(_.code).map(r => s"${r.code.toUpperCase}: ${normalLoadLookup(r).underlying.toString}").mkString("\n"), 6)
		normalCATSLoadRow.setHeight((normalCATSLoadRow.getHeight * (normalLoadLookup.routes.size - 1)).toShort)
		if (isStudentCount) {
			keyValueCells("Student Count:", count.toString, 7)
		} else {
			keyValueCells("Count:", count.toString, 7)
		}
		keyValueCells("Grid Generated:", DateTime.now.toString, 8)

		{
			val row = sheet.createRow(9)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("#")
			keyCell.setCellStyle(cellStyleMap(Fail))
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Failed module")
		}
		{
			val row = sheet.createRow(10)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("#")
			keyCell.setCellStyle(cellStyleMap(Overcat))
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Used in overcatting calculation")
		}
		{
			val row = sheet.createRow(11)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("#")
			keyCell.setCellStyle(cellStyleMap(ActualMark))
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Agreed mark missing, using actual")
		}
		{
			val row = sheet.createRow(12)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("X")
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Agreed mark and actual mark missing")
		}
		{
			val row = sheet.createRow(13)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("")
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Blank indicates module not taken by student")
		}
		{
			val row = sheet.createRow(14)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("AB")
			keyCell.setCellStyle(cellStyleMap(BoldText))
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Bold module name indicates a duplicate table entry")
		}

		sheet.autoSizeColumn(0)
		sheet.autoSizeColumn(1)
	}

	private def getCellStyleMap(workbook: XSSFWorkbook): Map[GenerateExamGridExporter.Style, XSSFCellStyle] = {
		val headerStyle = {
			val cs = workbook.createCellStyle()
			val boldFont = workbook.createFont()
			boldFont.setFontHeight(10)
			boldFont.setBold(true)
			cs.setFont(boldFont)
			cs.setVerticalAlignment(VerticalAlignment.CENTER)
			cs
		}

		val headerRotatedStyle = {
			val cs = workbook.createCellStyle()
			val boldFont = workbook.createFont()
			boldFont.setFontHeight(10)
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
			val redFont = workbook.createFont()
			redFont.setFontHeight(10)
			redFont.setColor(new XSSFColor(new Color(175, 39, 35)))
			redFont.setUnderline(FontUnderline.DOUBLE)
			cs.setFont(redFont)
			cs
		}

		val overcatStyle = {
			val cs = workbook.createCellStyle()
			val greenFont = workbook.createFont()
			greenFont.setFontHeight(10)
			greenFont.setColor(new XSSFColor(new Color(89, 110, 49)))
			greenFont.setUnderline(FontUnderline.SINGLE)
			cs.setFont(greenFont)
			cs
		}

		val overriddenStyle = {
			val cs = workbook.createCellStyle()
			val blueFont = workbook.createFont()
			blueFont.setFontHeight(10)
			blueFont.setColor(new XSSFColor(new Color(32, 79, 121)))
			cs.setFont(blueFont)
			cs
		}

		val actualMarkStyle = {
			val cs = workbook.createCellStyle()
			val blueFont = workbook.createFont()
			blueFont.setFontHeight(10)
			blueFont.setColor(new XSSFColor(new Color(35, 155, 146)))
			blueFont.setItalic(true)
			cs.setFont(blueFont)
			cs
		}

		val failAndActualMarkStyle = {
			val cs = workbook.createCellStyle()
			val redFont = workbook.createFont()
			redFont.setFontHeight(10)
			redFont.setColor(new XSSFColor(new Color(175, 39, 35)))
			redFont.setUnderline(FontUnderline.DOUBLE)
			redFont.setItalic(true)
			cs.setFont(redFont)
			cs
		}

		val overcatAndActualMarkStyle = {
			val cs = workbook.createCellStyle()
			val greenFont = workbook.createFont()
			greenFont.setFontHeight(10)
			greenFont.setColor(new XSSFColor(new Color(89, 110, 49)))
			greenFont.setItalic(true)
			greenFont.setUnderline(FontUnderline.SINGLE)
			cs.setFont(greenFont)
			cs
		}

		val boldText = {
			val cs = workbook.createCellStyle()
			val boldFont = workbook.createFont()
			boldFont.setFontHeight(10)
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
