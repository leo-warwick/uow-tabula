package uk.ac.warwick.tabula.data

import org.hibernate.criterion.{Order, Restrictions}
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Assignment, OriginalityReport}
import uk.ac.warwick.tabula.services.turnitinlti.TurnitinLtiService

trait TurnitinLtiQueueDaoComponent {
	def turnitinLtiQueueDao: TurnitinLtiQueueDao
}

trait AutowriringTurnitinLtiQueueDaoComponent extends TurnitinLtiQueueDaoComponent{
	val turnitinLtiQueueDao: TurnitinLtiQueueDao = Wire[TurnitinLtiQueueDao]
}

trait TurnitinLtiQueueDao {
	def findAssignmentToProcess: Option[Assignment]
	def findReportToProcessForSubmission: Option[OriginalityReport]
	def findReportToProcessForReport(longAwaitedOnly: Boolean): Option[OriginalityReport]
	def listCompletedAssignments: Seq[Assignment]
	def listFailedAssignments: Seq[Assignment]
	def listOriginalityReports(assignment: Assignment): Seq[OriginalityReport]

}

@Repository
class TurnitinLtiQueueDaoImpl extends TurnitinLtiQueueDao with Daoisms {

	def findAssignmentToProcess: Option[Assignment] = {
		session.newCriteria[Assignment]
			.add(is("submitToTurnitin", true))
			.add(Restrictions.lt("lastSubmittedToTurnitin", DateTime.now.minusSeconds(TurnitinLtiService.SubmitAssignmentWaitInSeconds)))
		  .add(Restrictions.isNull("turnitinId"))
			.add(Restrictions.lt("submitToTurnitinRetries", TurnitinLtiService.SubmitAssignmentMaxRetries))
			.addOrder(Order.asc("lastSubmittedToTurnitin"))
			.setMaxResults(1)
			.uniqueResult
	}

	def findReportToProcessForSubmission: Option[OriginalityReport] = {
		session.newCriteria[OriginalityReport]
			.createAlias("attachment", "attachment")
			.createAlias("attachment.submissionValue", "submissionValue")
			.createAlias("submissionValue.submission", "submission")
			.createAlias("submission.assignment", "assignment")
			.add(Restrictions.isNotNull("assignment.turnitinId"))
			.add(Restrictions.isNull("turnitinId"))
			.add(Restrictions.lt("lastSubmittedToTurnitin", DateTime.now.minusSeconds(TurnitinLtiService.SubmitAttachmentWaitInSeconds)))
			.add(Restrictions.lt("submitToTurnitinRetries", TurnitinLtiService.SubmitAttachmentMaxRetries))
			.addOrder(Order.asc("lastSubmittedToTurnitin"))
			.setMaxResults(1)
			.uniqueResult
	}

	def findReportToProcessForReport(longAwaitedOnly: Boolean): Option[OriginalityReport] = {
		val c = session.newCriteria[OriginalityReport]
			.createAlias("attachment", "attachment")
			.createAlias("attachment.submissionValue", "submissionValue")
			.createAlias("submissionValue.submission", "submission")
			.createAlias("submission.assignment", "assignment")
			.add(Restrictions.isNotNull("assignment.turnitinId"))
			.add(Restrictions.isNotNull("turnitinId"))
			.add(is("reportReceived", false))
			.add(Restrictions.lt("lastReportRequest", DateTime.now.minusSeconds(TurnitinLtiService.ReportRequestWaitInSeconds)))
			.add(Restrictions.lt("reportRequestRetries", TurnitinLtiService.ReportRequestMaxRetries))
			.addOrder(Order.asc("lastReportRequest"))
			.setMaxResults(1)

		if (longAwaitedOnly) {
			c.add(Restrictions.lt("lastSubmittedToTurnitin", DateTime.now.minusSeconds(TurnitinLtiService.LongAwaitedReportWaitInSeconds)))
		}

		c.uniqueResult
	}

	def listCompletedAssignments: Seq[Assignment] = {
		val pendingAssignments = session.newCriteria[Assignment]
			.add(is("submitToTurnitin", true))
			.add(Restrictions.isNotNull("turnitinId"))
			.seq

		val incompleteReports = session.newCriteria[OriginalityReport]
			.createAlias("attachment", "attachment")
		  .createAlias("attachment.submissionValue", "submissionValue")
			.createAlias("submissionValue.submission", "submission")
			.createAlias("submission.assignment", "assignment")
		  .add(is("assignment.submitToTurnitin", true))
			.add(Restrictions.isNotNull("assignment.turnitinId"))
		  .add(Restrictions.conjunction(
				is("reportReceived", false),
				Restrictions.lt("submitToTurnitinRetries", TurnitinLtiService.SubmitAttachmentMaxRetries),
				Restrictions.lt("reportRequestRetries", TurnitinLtiService.ReportRequestMaxRetries)
			)).seq

		val incompletePendingAssignments = incompleteReports.map(_.attachment.submissionValue.submission.assignment).distinct
		pendingAssignments.diff(incompletePendingAssignments)
	}

	def listFailedAssignments: Seq[Assignment] = {
		session.newCriteria[Assignment]
			.add(is("submitToTurnitin", true))
			.add(Restrictions.isNull("turnitinId"))
			.add(is("submitToTurnitinRetries", TurnitinLtiService.SubmitAssignmentMaxRetries))
		  .seq
	}

	def listOriginalityReports(assignment: Assignment): Seq[OriginalityReport] = {
		session.newCriteria[OriginalityReport]
			.createAlias("attachment", "attachment")
			.createAlias("attachment.submissionValue", "submissionValue")
			.createAlias("submissionValue.submission", "submission")
			.add(is("submission.assignment", assignment))
			.seq
	}

}