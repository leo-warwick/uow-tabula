package uk.ac.warwick.tabula.web.controllers.exams.grids.generate

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.jobs.exams.GenerateExamGridDocumentJob
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}
import uk.ac.warwick.tabula.services.jobs.{AutowiringJobServiceComponent, JobInstance}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/generate/documents/{jobId}/progress"))
class ExamGridDocumentProgressController extends ExamsController
	with AutowiringJobServiceComponent {

	@ModelAttribute("job")
	def job(@PathVariable jobId: String, @PathVariable department: Department): JobInstance =
		jobService.getInstance(jobId)
			.filter(_.user == user)
			.filter(_.jobType == GenerateExamGridDocumentJob.identifier)
			.filter(_.getString("department") == department.code)
		 	.getOrElse(throw new ItemNotFoundException())

	@GetMapping
	def progress(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): Mav = Mav("exams/grids/generate/documentProgress")

	@PostMapping
	def jobProgress(@ModelAttribute("job") job: JobInstance): Mav =
		Mav(new JSONView(Map(
			"id" -> job.id,
			"status" -> job.status,
			"progress" -> job.progress,
			"finished" -> job.finished,
			"succeeded" -> job.succeeded
		))).noLayout()
}
