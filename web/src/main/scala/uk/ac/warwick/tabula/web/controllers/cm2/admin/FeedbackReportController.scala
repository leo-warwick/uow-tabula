package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.departments.FeedbackReportCommand
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.jobs.JobService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}
import uk.ac.warwick.tabula.{CurrentUser, DateFormats}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/reports/feedback"))
class FeedbackReportController extends CourseworkController {

	validatesSelf[FeedbackReportCommand]

	var jobService: JobService = Wire.auto[JobService]

	@ModelAttribute def command(@PathVariable(value = "department") dept: Department, user: CurrentUser) =
		new FeedbackReportCommand(mandatory(dept), mandatory(user))

	@RequestMapping(method=Array(HEAD, GET), params = Array("!jobId"))
	def requestReport(cmd:FeedbackReportCommand, errors:Errors):Mav = {
		val formatter = DateTimeFormat.forPattern(DateFormats.DateTimePickerPattern)
		Mav(s"$urlPrefix/admin/assignments/feedbackreport/report_range",
			"startDate" ->  formatter.print(new DateTime().minusMonths(3)),
			"endDate" ->  formatter.print(new DateTime())
		).noLayout()
	}

	@RequestMapping(method = Array(POST), params = Array("!jobId"))
	def generateReport(@Valid cmd: FeedbackReportCommand, errors: Errors): Mav = {
		if(errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val jobId = cmd.apply().id
			val successUrl = Routes.admin.feedbackReports(cmd.department) + "?jobId=" + jobId
			Mav(new JSONView(Map("status" -> "success", "result" -> successUrl)))
		}
	}

	@RequestMapping(params = Array("jobId"))
	def checkProgress(@RequestParam jobId: String): Mav = {
		val job = jobService.getInstance(jobId)
		Mav(s"$urlPrefix/admin/assignments/feedbackreport/progress", "job" -> job).noLayoutIf(ajax)
	}

}