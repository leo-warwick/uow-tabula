package uk.ac.warwick.tabula.exams.grids.documents

import java.io.ByteArrayOutputStream

import com.google.common.io.ByteSource
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridPassListExporter
import uk.ac.warwick.tabula.data.model.{Department, FileAttachment, UpstreamRouteRuleLookup}
import uk.ac.warwick.tabula.exams.grids.StatusAdapter
import uk.ac.warwick.tabula.exams.grids.documents.ExamGridDocument._
import uk.ac.warwick.tabula.services.AutowiringProgressionServiceComponent
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringNormalCATSLoadServiceComponent, AutowiringUpstreamRouteRuleServiceComponent, NormalLoadLookup}

import scala.jdk.CollectionConverters._

object PassListDocument extends ExamGridDocumentPrototype {
  override val identifier: String = "PassList"

  def options(confidential: Boolean): Map[String, Any] = Map("confidential" -> confidential)
}

@Component
class PassListDocument extends ExamGridDocument
  with AutowiringProgressionServiceComponent
  with AutowiringNormalCATSLoadServiceComponent
  with AutowiringUpstreamRouteRuleServiceComponent {
  override val identifier: String = PassListDocument.identifier

  override val contentType: String = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

  override def apply(
    department: Department,
    academicYear: AcademicYear,
    selectCourseCommand: SelectCourseCommand,
    gridOptionsCommand: GridOptionsCommand,
    checkOvercatCommand: CheckOvercatCommand,
    options: Map[String, Any],
    status: StatusAdapter
  ): FileAttachment = {
    val isConfidential: Boolean = options.get("confidential").fold(false)(_.asInstanceOf[Boolean])

    val entities = selectCourseCommand.apply()

    val document = ExamGridPassListExporter(
      entities,
      selectCourseCommand.department,
      selectCourseCommand.courses.asScala.toSeq,
      selectCourseCommand.yearOfStudy,
      selectCourseCommand.academicYear,
      progressionService,
      NormalLoadLookup(selectCourseCommand.academicYear, selectCourseCommand.yearOfStudy, normalCATSLoadService),
      UpstreamRouteRuleLookup(selectCourseCommand.academicYear, upstreamRouteRuleService),
      isConfidential,
      calculateYearMarks = gridOptionsCommand.calculateYearMarks,
      selectCourseCommand.isLevelGrid,
      gridOptionsCommand.applyBenchmark
    )

    val file = new FileAttachment()

    file.name = "%sPass list for %s %s %s %s.docx".format(
      if (isConfidential) "Confidential " else "",
      selectCourseCommand.department.name,
      selectCourseCommand.courses.size match {
        case 1 => selectCourseCommand.courses.get(0).code
        case n => s"$n courses"
      },
      selectCourseCommand.routes.size match {
        case 0 => "All routes"
        case 1 => selectCourseCommand.routes.get(0).code.toUpperCase
        case n => s"$n routes"
      },
      selectCourseCommand.academicYear.toString.replace("/", "-")
    )

    val out = new ByteArrayOutputStream()
    document.write(out)
    out.close()

    file.uploadedData = ByteSource.wrap(out.toByteArray)

    file
  }
}
