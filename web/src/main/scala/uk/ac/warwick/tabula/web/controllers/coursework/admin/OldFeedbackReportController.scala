package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.{CurrentUser, DateFormats}
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.data.model.Department
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.coursework.departments.FeedbackReportCommand
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.services.jobs.JobService
import javax.validation.Valid

import org.springframework.context.annotation.Profile
import uk.ac.warwick.spring.Wire

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/department/{dept}/reports/feedback"))
class OldFeedbackReportController extends OldCourseworkController {

	validatesSelf[FeedbackReportCommand]

	var jobService: JobService = Wire.auto[JobService]

	@ModelAttribute def command(@PathVariable(value = "dept") dept: Department, user: CurrentUser) =
		new FeedbackReportCommand(dept, user)

	@RequestMapping(method=Array(HEAD, GET), params = Array("!jobId"))
	def requestReport(cmd:FeedbackReportCommand, errors:Errors):Mav = {
		val formatter = DateTimeFormat.forPattern(DateFormats.DateTimePickerPattern)
		Mav(s"$urlPrefix/admin/assignments/feedbackreport/report_range",
			"department" -> cmd.department,
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