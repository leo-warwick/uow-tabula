package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Assignment, Submission}

trait SubmissionDao {
	def saveSubmission(submission: Submission)
	def getSubmissionByUniId(assignment: Assignment, uniId: String): Option[Submission]
	def getSubmissionsByAssignment(assignment: Assignment): Seq[Submission]
	def getSubmission(id: String): Option[Submission]

	def delete(submission: Submission): Unit
}

abstract class AbstractSubmissionDao extends SubmissionDao with HelperRestrictions {
	self: SessionComponent =>

	def saveSubmission(submission: Submission) = {
		session.saveOrUpdate(submission)
		session.flush()
	}

	def getSubmissionByUniId(assignment: Assignment, uniId: String) = {
		session.newCriteria[Submission]
			.add(is("assignment", assignment))
			.add(is("universityId", uniId))
			.uniqueResult
	}

	def getSubmissionsByAssignment(assignment: Assignment) : Seq[Submission] = {
		session.newCriteria[Submission]
			.add(is("assignment", assignment)).seq
	}

	def getSubmission(id: String) = session.getById[Submission](id)

	def delete(submission: Submission) {
		submission.assignment.submissions.remove(submission)
		session.delete(submission)
		// force delete now, just for the cases where we re-insert in the same session
		// (i.e. when a student is resubmitting work). [HFC-385#comments]
		session.flush()
	}
}

@Repository("submissionDao")
class SubmissionDaoImpl extends AbstractSubmissionDao with Daoisms

trait SubmissionDaoComponent {
	def submissionDao: SubmissionDao
}

trait AutowiringSubmissionDaoComponent extends SubmissionDaoComponent {
	override val submissionDao = Wire[SubmissionDao]
}
