package uk.ac.warwick.tabula.data

import org.hibernate.criterion.{Order, Restrictions}
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Assignment, OriginalityReport}

trait UrkundDaoComponent {
	def urkundDao: UrkundDao
}

trait AutowiringUrkundDaoComponent extends UrkundDaoComponent{
	val urkundDao: UrkundDao = Wire[UrkundDao]
}

trait UrkundDao {
	def findReportToSubmit: Option[OriginalityReport]
	def findReportToRetreive: Option[OriginalityReport]
	def listOriginalityReports(assignment: Assignment): Seq[OriginalityReport]
}

@Repository
class UrkundDaoImpl extends UrkundDao with Daoisms {

	override def findReportToSubmit: Option[OriginalityReport] = {
		session.newCriteria[OriginalityReport]
			.createAlias("attachment", "attachment") // Don't need these aliases
			.createAlias("attachment.submissionValue", "submissionValue") // but it ensures the submission
			.createAlias("submissionValue.submission", "submission") // hasn't been deleted
		  .add(Restrictions.lt("nextSubmitAttempt", DateTime.now))
			.add(Restrictions.isNull("nextResponseAttempt"))
			.addOrder(Order.asc("nextSubmitAttempt"))
			.setMaxResults(1)
			.uniqueResult
	}

	override def findReportToRetreive: Option[OriginalityReport] = {
		session.newCriteria[OriginalityReport]
			.createAlias("attachment", "attachment") // Don't need these aliases
			.createAlias("attachment.submissionValue", "submissionValue") // but it ensures the submission
			.createAlias("submissionValue.submission", "submission") // hasn't been deleted
			.add(Restrictions.isNull("nextSubmitAttempt"))
			.add(Restrictions.lt("nextResponseAttempt", DateTime.now))
			.addOrder(Order.asc("nextResponseAttempt"))
			.setMaxResults(1)
			.uniqueResult
	}

	override def listOriginalityReports(assignment: Assignment): Seq[OriginalityReport] = {
		session.newCriteria[OriginalityReport]
			.createAlias("attachment", "attachment")
		  .createAlias("attachment.submissionValue", "submissionValue")
			.createAlias("submissionValue.submission", "submission")
			.add(is("submission.assignment", assignment))
		  .seq
	}

}
