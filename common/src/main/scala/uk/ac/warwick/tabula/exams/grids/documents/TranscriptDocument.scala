package uk.ac.warwick.tabula.exams.grids.documents

import java.io.ByteArrayOutputStream

import com.google.common.io.ByteSource
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridTranscriptExporter
import uk.ac.warwick.tabula.data.model.{Department, FileAttachment}
import uk.ac.warwick.tabula.exams.grids.StatusAdapter
import uk.ac.warwick.tabula.exams.grids.documents.ExamGridDocument._

object TranscriptDocument extends ExamGridDocumentPrototype {
	override val identifier: String = "Transcript"

	def options(confidential: Boolean): Map[String, Any] = Map("confidential" -> confidential)
}

@Component
class TranscriptDocument extends ExamGridDocument {
	override val identifier: String = TranscriptDocument.identifier

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

		val document = ExamGridTranscriptExporter(
			entities,
			isConfidential = isConfidential
		)

		val file = new FileAttachment()

		file.name = "%sTranscript for %s %s %s %s.docx".format(
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
