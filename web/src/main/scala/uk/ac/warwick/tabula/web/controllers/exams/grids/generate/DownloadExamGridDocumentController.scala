package uk.ac.warwick.tabula.web.controllers.exams.grids.generate

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{GetMapping, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.AutowiringFileDaoComponent
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.jobs.exams.GenerateExamGridDocumentJob
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}

@Controller
@RequestMapping(path = Array("/exams/grids/{department}/{academicYear}/generate/documents/{jobId}/download"))
class DownloadExamGridDocumentController extends ExamsController
	with AutowiringJobServiceComponent with AutowiringFileDaoComponent {
	@GetMapping
	def download(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable jobId: String): RenderableFile =
		jobService.getInstance(jobId)
			.filter(_.user == user)
			.filter(_.jobType == GenerateExamGridDocumentJob.identifier)
			.filter(_.getString("department") == department.code)
			.filter(_.succeeded)
			.map(_.getString("fileId"))
			.flatMap(fileDao.getFileById)
			.map(file => new RenderableAttachment(file, name = Some(file.name)))
			.getOrElse(throw new ItemNotFoundException())
}
