package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue

class SubmissionTest extends PersistenceTestBase {

	@Test def allAttachments() {
		val submission = new Submission
		submission.allAttachments.size should be (0)
	}

	@Test def tab3614() = transactional{tx=>
		// TAB-667
		val orphanAttachment = flushing(session) {
			val attachment = new FileAttachment

			session.save(attachment)
			attachment
		}

		val (submission, submissionAttachment) = flushing(session) {
			val submission = new Submission
			submission._universityId = "0000001"
			submission.usercode = "steve"

			val assignment = new Assignment
			session.save(assignment)

			submission.assignment = assignment

			session.save(submission)

			val attachment = new FileAttachment
			val ssv = SavedFormValue.withAttachments(submission, "name", Set(attachment))
			submission.values.add(ssv)

			session.saveOrUpdate(submission)
			(submission, attachment)
		}

		// Ensure everything's been persisted
		orphanAttachment.id should not be null
		submission.id should not be null
		submissionAttachment.id should not be null

		// Can fetch everything from db
		flushing(session) {
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[Submission], submission.id) should be (submission)
			session.get(classOf[FileAttachment], submissionAttachment.id) should be (submissionAttachment)
		}

		flushing(session) { session.delete(submission) }

		// Ensure we can't fetch the submission, but all the other objects are returned
		flushing(session) {
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[Submission], submission.id) should be (null)
			session.get(classOf[FileAttachment], submissionAttachment.id) should be (submissionAttachment)
		}
	}
}