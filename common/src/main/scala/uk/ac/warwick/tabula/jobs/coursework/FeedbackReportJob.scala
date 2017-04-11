package uk.ac.warwick.tabula.jobs.coursework

import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.jobs.{Job, JobPrototype}
import org.joda.time.DateTime
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.helpers.{SpreadsheetHelpers, Logging}
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, AssessmentService}
import uk.ac.warwick.tabula.{CurrentUser, DateFormats}
import uk.ac.warwick.spring.Wire
import org.springframework.mail.javamail.MimeMessageHelper
import javax.annotation.Resource
import uk.ac.warwick.util.mail.WarwickMailSender
import org.springframework.beans.factory.annotation.Value
import java.io.ByteArrayOutputStream
import org.springframework.core.io.ByteArrayResource
import freemarker.template.Configuration
import uk.ac.warwick.tabula.services.coursework.feedbackreport.FeedbackReport
import uk.ac.warwick.tabula.data.Transactions._

object FeedbackReportJob {
	val identifier = "feedback-report"
	def apply(department: Department, start: DateTime, end: DateTime) = JobPrototype(identifier, Map(
		"department" -> department.id ,
		"startDate" -> DateFormats.CSVDate.print(start),
		"endDate" -> DateFormats.CSVDate.print(end)
	))
}

@Component
class FeedbackReportJob extends Job with Logging with FreemarkerRendering {

	val identifier: String = FeedbackReportJob.identifier

	implicit var freemarker: Configuration = Wire.auto[Configuration]
	var departmentService: ModuleAndDepartmentService = Wire.auto[ModuleAndDepartmentService]
	var assignmentService: AssessmentService = Wire.auto[AssessmentService]

	@Resource(name = "mailSender") var mailer: WarwickMailSender = _

	@Value("${mail.noreply.to}") var replyAddress: String = _
	@Value("${mail.admin.to}") var adminFromAddress: String = _
	val excelContentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
	val WaitingRetries = 50
	val WaitingSleep = 20000

	val ProgressGeneratedWorksheets = 5
	val ProgressBuiltAssignmentData = 60
	val ProgressPopulatedAssignments = 70
	val ProgressPopulatedModules = 80
	val ProgressFormattedSheets = 85
	val ProgressDone = 100

	var sendEmails = true

	def run(implicit job: JobInstance): Unit = {
		transactional() {
			new Runner(job).run()
		}
	}


	def renderJobDoneEmailText(user: CurrentUser, department: Department, startDate: DateTime, endDate: DateTime): String = {
		renderToString("/WEB-INF/freemarker/emails/feedback_report_done.ftl", Map(
			"department" -> department,
			"startDate" -> DateFormats.CSVDate.print(startDate),
			"endDate" -> DateFormats.CSVDate.print(endDate),
			"user" -> user
		))
	}

	// Job is a shared service, so rather than pass objects between methods,
	// let's use an inner class.
	class Runner(job: JobInstance) {
		implicit private val _job: JobInstance = job

		val department: Department = {
			val id = job.getString("department")
			departmentService.getDepartmentById(id) getOrElse {
				updateStatus("%s is not a valid department ID" format (id))
				throw obsoleteJob
			}
		}

		val startDate: DateTime = {
			val stringDate = job.getString("startDate")
			try {
				DateFormats.CSVDate.parseDateTime(stringDate)
			} catch {
				case e:IllegalArgumentException => {
					updateStatus("Start date format was incorrect")
					throw obsoleteJob
				}
			}
		}


		val endDate: DateTime = {
			val stringDate = job.getString("endDate")
			try {
				DateFormats.CSVDate.parseDateTime(stringDate)
			} catch {
				case e:IllegalArgumentException => {
					updateStatus("End date format was incorrect")
					throw obsoleteJob
				}
			}
		}

		def run() {
			val report = new FeedbackReport(department, startDate, endDate)

			updateStatus("Generating base worksheets")
			val assignmentSheet = report.generateAssignmentSheet(department)
			val moduleSheet = report.generateModuleSheet(department)
			updateProgress(ProgressGeneratedWorksheets)

			updateStatus("Building assignment data")
			report.buildAssignmentData()
			updateProgress(ProgressBuiltAssignmentData)

			updateStatus("Populating worksheets")
			report.populateAssignmentSheet(assignmentSheet)
			updateProgress(ProgressPopulatedAssignments)
			report.populateModuleSheet(moduleSheet)
			updateProgress(ProgressPopulatedModules)

			updateStatus("Formatting worksheets")
			SpreadsheetHelpers.formatWorksheet(assignmentSheet, FeedbackReport.AssignmentSheetSize)
			SpreadsheetHelpers.formatWorksheet(moduleSheet, FeedbackReport.ModuleSheetSize)
			updateProgress(ProgressFormattedSheets)

			// Adapter stuff to go from XSSF to the writer without having to save the spreadsheet to disk
			val out = new ByteArrayOutputStream
			report.workbook.write(out)
			out.close()
			val in = new ByteArrayResource(out.toByteArray)

			updateStatus("Sending emails")
			if (sendEmails) {
				debug("Sending an email to " + job.user.email)
				val mime = mailer.createMimeMessage()


				val helper = new MimeMessageHelper(mime, true)
				helper.setFrom(adminFromAddress)
				helper.setReplyTo(replyAddress)
				helper.setTo(job.user.email)
				helper.setSubject("Feedback report generated for %s" format (department.name))
				helper.setText(renderJobDoneEmailText(job.user, department, startDate, endDate))
				helper.addAttachment("Feedback Report.xlsx", in, excelContentType)

				mailer.send(mime)
			}

			updateProgress(ProgressDone)
			updateStatus("Generated report and sent to your inbox")
			job.succeeded = true
		}
	}

}
