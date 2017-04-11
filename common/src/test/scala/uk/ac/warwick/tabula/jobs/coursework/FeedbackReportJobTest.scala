package uk.ac.warwick.tabula.jobs.coursework

import uk.ac.warwick.tabula.jobs.Job
import uk.ac.warwick.tabula.jobs.JobTestHelp
import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.AssessmentService
import uk.ac.warwick.tabula.commands.coursework.departments.ReportWorld

// scalastyle:off magic.number
class FeedbackReportJobTest extends TestBase with Mockito with JobTestHelp with ReportWorld {
	val job = new FeedbackReportJob
	override def createJobs: Array[Job] = Array[Job](job)

	@Test def run {
		val job = new FeedbackReportJob
		job.assignmentService = mock[AssessmentService]
		job.departmentService = mock[ModuleAndDepartmentService]

		service.add(None, FeedbackReportJob(department, dateTime(2013, 1, 1), dateTime(2013, 8, 1)))
	}
}
