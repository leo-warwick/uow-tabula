package uk.ac.warwick.tabula.commands.exams.grids

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model.UpstreamRouteRuleLookup
import uk.ac.warwick.tabula.exams.grids.columns.ExamGridYearMarksToUse
import uk.ac.warwick.tabula.services.exams.grids.NormalLoadLookup
import uk.ac.warwick.tabula.services.{FinalYearGrade, ProgressionService}

import scala.jdk.CollectionConverters._

object ExamGridMarksRecordExporter extends TaskBenchmarking with AddConfidentialWatermarkToDocument {

  def apply(
    entities: Seq[ExamGridEntity],
    progressionService: ProgressionService,
    normalLoadLookup: NormalLoadLookup,
    routeRulesLookup: UpstreamRouteRuleLookup,
    isConfidential: Boolean,
    yearMarksToUse: ExamGridYearMarksToUse,
    isLevelGrid: Boolean,
    applyBenchmark: Boolean,
  ): XWPFDocument = {

    val doc = new XWPFDocument()
    doc.createParagraph()

    def renderEntity(entity: ExamGridEntity): Unit = benchmarkTask("renderEntity") {
      val route = entity.validYears(entity.validYears.keys.last).route
      val course = entity.validYears(entity.validYears.keys.last).studentCourseYearDetails.map(_.studentCourseDetails.course)
        .getOrElse(throw new IllegalStateException(s"No course for ${entity.universityId}"))
      val p1 = doc.getLastParagraph
      val r1 = p1.createRun()
      r1.setText(s"Marks record for ${entity.firstName} ${entity.lastName}")
      doc.createParagraph().createRun().setText(s"${course.code.toUpperCase} ${course.name}, ${route.code.toUpperCase} ${route.name}")

      doc.createParagraph()

      entity.validYears.keys.toSeq.sorted.foreach { yearOfStudy =>
        doc.createParagraph().createRun().setText("%s year examinations".format(yearOfStudy match {
          case 1 => "First"
          case 2 => "Second"
          case 3 => "Third"
          case 4 => "Fourth"
          case n => s"${n}th"
        }))

        val year = entity.validYears(yearOfStudy)
        val moduleTable = doc.createTable(year.moduleRegistrations.size + 1, 4)
        // Set table width
        val moduleTableWidth = moduleTable.getCTTbl.getTblPr.getTblW
        moduleTableWidth.setW(BigInt(8640).bigInteger)
        moduleTableWidth.setType(STTblWidth.DXA)
        // Set column widths using grid
        moduleTable.getCTTbl.addNewTblGrid().addNewGridCol().setW(BigInt(4320).bigInteger)
        moduleTable.getCTTbl.getTblGrid.addNewGridCol().setW(BigInt(1440).bigInteger)
        moduleTable.getCTTbl.getTblGrid.addNewGridCol().setW(BigInt(1440).bigInteger)
        moduleTable.getCTTbl.getTblGrid.addNewGridCol().setW(BigInt(1440).bigInteger)
        // Add headers
        moduleTable.getRow(0).getCell(0).setText("Module name")
        moduleTable.getRow(0).getCell(1).setText("Weight")
        moduleTable.getRow(0).getCell(2).setText("Percentage")
        moduleTable.getRow(0).getCell(3).setText("Grade")
        // Remove table border
        moduleTable.getCTTbl.getTblPr.unsetTblBorders()
        // Stop rows breaking over pages
        moduleTable.getRows.asScala.foreach(_.setCantSplitRow(true))

        // Add data
        year.moduleRegistrations.zipWithIndex.foreach { case (mr, index) =>
          val row = moduleTable.getRow(index + 1)
          row.getCell(0).setText(s"${mr.module.code.toUpperCase} ${mr.module.name}")
          row.getCell(1).setText(mr.cats.toPlainString)
          if (mr.agreedMark.nonEmpty || mr.agreedGrade.nonEmpty) {
            row.getCell(2).setText(mr.agreedMark.map(_.toString).getOrElse("-"))
            row.getCell(3).setText(mr.agreedGrade.getOrElse(""))
          } else if (mr.actualMark.nonEmpty || mr.actualGrade.nonEmpty) {
            row.getCell(2).setText(mr.actualMark.map(_.toString).getOrElse("-"))
            row.getCell(3).setText(mr.actualGrade.getOrElse(""))
          }
        }

        doc.createParagraph()

        val yearMark: Option[BigDecimal] = {
          lazy val uploadedYearMark: Option[BigDecimal] =
            Option(BigDecimal(year.studentCourseYearDetails.get.agreedMark))

          lazy val calculatedYearMark: Option[BigDecimal] =
            progressionService.getYearMark(year, normalLoadLookup(year.route), routeRulesLookup(year.route, year.level), entity.yearWeightings).toOption

          yearMarksToUse match {
            case _ if entity.years.keys.last == yearOfStudy =>
              calculatedYearMark

            case ExamGridYearMarksToUse.CalculateYearMarks =>
              calculatedYearMark

            case ExamGridYearMarksToUse.UploadedYearMarksOnly =>
              uploadedYearMark

            case ExamGridYearMarksToUse.UploadedYearMarksIfAvailable =>
              uploadedYearMark.orElse(calculatedYearMark)
          }
        }
        doc.createParagraph().createRun().setText(s"Mark for the year: ${yearMark.map(_.underlying.toPlainString).getOrElse("X")}")

        val routeRules = entity.validYears.view.mapValues(ey => routeRulesLookup(ey.route, ey.level)).toMap
        progressionService.suggestedFinalYearGrade(year, normalLoadLookup(year.route), routeRules, yearMarksToUse, isLevelGrid, applyBenchmark, entity.yearWeightings) match {
          case FinalYearGrade.Ignore =>
          case grade => doc.createParagraph().createRun().setText(s"Classification: ${grade.description}")
        }

        doc.createParagraph()
      }
    }

    def processEntities(remainingEntities: List[ExamGridEntity]): Unit = {
      remainingEntities match {
        case Nil =>
        case entity :: tail =>
          renderEntity(entity)
          if (tail.nonEmpty) {
            val newParagraph = doc.createParagraph()
            newParagraph.setPageBreak(true)
          }
          processEntities(tail)
      }
    }

    processEntities(entities.toList)

    // Set paragraph spacing to after 0.2cm
    benchmarkTask("paragraphSpacing") {
      val allParagraphs = doc.getParagraphs.asScala ++ doc.getTables.asScala.flatMap(_.getRows.asScala.flatMap(_.getTableCells.asScala.flatMap(_.getParagraphs.asScala)))
      allParagraphs.foreach(_.getCTP.addNewPPr().addNewSpacing().setAfter(BigInt(113).bigInteger))
    }

    if (isConfidential) {
      addWatermark(doc)
    }

    doc
  }

}
