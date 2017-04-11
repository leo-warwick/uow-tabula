package uk.ac.warwick.tabula.commands.exams.grids

import org.apache.poi.xwpf.usermodel.{Borders, ParagraphAlignment, XWPFDocument, XWPFParagraph}
import org.joda.time.DateTime
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.exams.grids.NormalLoadLookup
import uk.ac.warwick.tabula.services.{ProgressionResult, ProgressionService}
import uk.ac.warwick.tabula.{AcademicYear, DateFormats}

import scala.collection.JavaConverters._

object ExamGridPassListExporter extends TaskBenchmarking with AddConfidentialWatermarkToDocument {

	def apply(
		entities: Seq[ExamGridEntity],
		department: Department,
		course: Course,
		yearOfStudy: YearOfStudy,
		academicYear: AcademicYear,
		progressionService: ProgressionService,
		normalLoadLookup: NormalLoadLookup,
		routeRulesLookup: UpstreamRouteRuleLookup,
		isConfidential: Boolean
	): XWPFDocument = {

		val doc = new XWPFDocument()
		if (isConfidential) {
			createAndFormatParagraph(doc, alignCenter = false)
		} else {
			val p = createAndFormatParagraph(doc)
			p.createRun().setText("For Display")
		}

		createAndFormatParagraph(doc).createRun().setText("University of Warwick")
		createAndFormatParagraph(doc).createRun().setText("%s year examinations JUNE %s".format(
			yearOfStudyToString(yearOfStudy).capitalize,
			academicYear.endYear
		))
		createAndFormatParagraph(doc).createRun().setText("Pass List")
		createAndFormatParagraph(doc).createRun().setText("The following candidates will proceed to the %s year of study:".format(yearOfStudyToString(yearOfStudy + 1)))
		createAndFormatParagraph(doc).createRun().setText(department.name)
		createAndFormatParagraph(doc).createRun().setText(s"${course.code.toUpperCase} ${course.name}")

		createAndFormatParagraph(doc).setBorderBottom(Borders.SINGLE)

		createAndFormatParagraph(doc)

		val passedEntites = {
			entities.filter(entity =>
				progressionService.suggestedResult(
					entity.years(yearOfStudy).studentCourseYearDetails.get,
					normalLoadLookup(entity.years(yearOfStudy).route),
					routeRulesLookup(entity.years(yearOfStudy).route)
				) match {
					case ProgressionResult.Proceed | ProgressionResult.PossiblyProceed | ProgressionResult.Pass => true
					case _ => false
				}
			)
		}

		val entityTable = doc.createTable(passedEntites.size, 3)
		// Set table width
		val entityTableWidth = entityTable.getCTTbl.getTblPr.getTblW
		entityTableWidth.setW(BigInt(8640).bigInteger)
		entityTableWidth.setType(STTblWidth.DXA)
		// Set column widths using grid
		entityTable.getCTTbl.addNewTblGrid().addNewGridCol().setW(BigInt(3456).bigInteger)
		entityTable.getCTTbl.getTblGrid.addNewGridCol().setW(BigInt(3456).bigInteger)
		entityTable.getCTTbl.getTblGrid.addNewGridCol().setW(BigInt(1728).bigInteger)
		// Remove table border
		entityTable.getCTTbl.getTblPr.unsetTblBorders()
		// Stop rows breaking over pages
		entityTable.getRows.asScala.foreach(_.setCantSplitRow(true))

		passedEntites.sortBy(e => (e.lastName, e.firstName, e.universityId)).zipWithIndex.foreach { case (entity, index) =>
			val row = entityTable.getRow(index)
			row.getCell(0).setText(entity.lastName)
			row.getCell(1).setText(entity.firstName)
			row.getCell(2).setText(entity.universityId)
		}

		entityTable.getRows.asScala.flatMap(_.getTableCells.asScala.flatMap(_.getParagraphs.asScala)).foreach { p =>
			val spacing = p.getCTP.addNewPPr().addNewSpacing()
			spacing.setAfter(BigInt(113).bigInteger)
		}

		createAndFormatParagraph(doc, alignCenter = false)

		val signatoryTable = doc.createTable(6, 3)
		// Set table width
		val signatoryTableWidth = signatoryTable.getCTTbl.getTblPr.getTblW
		signatoryTableWidth.setW(BigInt(8640).bigInteger)
		signatoryTableWidth.setType(STTblWidth.DXA)
		// Set column widths using grid
		signatoryTable.getCTTbl.addNewTblGrid().addNewGridCol().setW(BigInt(3820).bigInteger)
		signatoryTable.getCTTbl.getTblGrid.addNewGridCol().setW(BigInt(1000).bigInteger)
		signatoryTable.getCTTbl.getTblGrid.addNewGridCol().setW(BigInt(3820).bigInteger)
		// Remove table border
		signatoryTable.getCTTbl.getTblPr.unsetTblBorders()
		// Stop rows breaking over pages
		signatoryTable.getRows.asScala.foreach(_.setCantSplitRow(true))

		val signatoryTableHeader = signatoryTable.getRow(0)
		signatoryTableHeader.getCell(0).setText("Signed")
		signatoryTableHeader.getCell(0).getParagraphs.get(0).setAlignment(ParagraphAlignment.CENTER)
		signatoryTableHeader.getCell(2).setText("Position")
		signatoryTableHeader.getCell(2).getParagraphs.get(0).setAlignment(ParagraphAlignment.CENTER)

		(1 until 6).foreach { index =>
			val row = signatoryTable.getRow(index)
			row.getCell(0).getParagraphs.get(0).setBorderBottom(Borders.DOTTED)
			row.getCell(2).getParagraphs.get(0).setBorderBottom(Borders.DOTTED)
		}

		signatoryTable.getRows.asScala.flatMap(_.getTableCells.asScala.flatMap(_.getParagraphs.asScala)).foreach { p =>
			val spacing = p.getCTP.addNewPPr().addNewSpacing()
			spacing.setBefore(BigInt(113).bigInteger)
			spacing.setAfter(BigInt(113).bigInteger)
		}

		createAndFormatParagraph(doc)

		createAndFormatParagraph(doc, alignCenter = false).createRun().setText(DateFormats.NotificationDateOnly.print(DateTime.now))

		if (isConfidential) {
			addWatermark(doc)
		}

		doc
	}

	private def createAndFormatParagraph(doc: XWPFDocument, alignCenter: Boolean = true): XWPFParagraph = {
		val p = doc.createParagraph()
		p.getCTP.addNewPPr().addNewSpacing().setAfter(BigInt(113).bigInteger)
		if (alignCenter) {
			p.setAlignment(ParagraphAlignment.CENTER)
		}
		p
	}

	private def yearOfStudyToString(yearOfStudy: YearOfStudy): String = {
		yearOfStudy match {
			case 1 => "first"
			case 2 => "second"
			case 3 => "third"
			case 4 => "fourth"
			case n => s"${n}th"
		}
	}

}
