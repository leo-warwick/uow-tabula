package uk.ac.warwick.tabula.data.model.forms
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment, Submission}

class FormValueTest extends PersistenceTestBase {

	@Test def makePermanentOnPersist() {
		val field = new FileField()
		val value = new FileFormValue(field)
		val attachment = new FileAttachment()
		attachment.temporary.booleanValue should be {true}

		value.file.attached.add(attachment)

		val saved = new SavedFormValue()
		value.persist(saved)
		saved.attachments.size should be (1)
		saved.attachments.iterator.next.temporary.booleanValue should be {false}
	}

	@Test def tab3614() = transactional{ tx=>
		// TAB-667
		val orphanAttachment = flushing(session) {
			val attachment = new FileAttachment

			session.save(attachment)
			attachment
		}

		val (ssv, ssvAttachment1, ssvAttachment2) = flushing(session) {
			val submission = new Submission
			submission._universityId = "0000001"
			submission.usercode = "steve"

			val assignment = new Assignment
			session.save(assignment)

			submission.assignment = assignment

			session.save(submission)

			val attachment1 = new FileAttachment
			val attachment2 = new FileAttachment

			val ssv = SavedFormValue.withAttachments(submission, "name", Set(attachment1, attachment2))

			session.save(ssv)
			(ssv, attachment1, attachment2)
		}

		// Ensure everything's been persisted
		orphanAttachment.id should not be null
		ssv.id should not be null
		ssvAttachment1.id should not be null
		ssvAttachment2.id should not be null

		// Can fetch everything from db
		flushing(session) {
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[SavedFormValue], ssv.id) should be (ssv)
			session.get(classOf[FileAttachment], ssvAttachment1.id) should be (ssvAttachment1)
			session.get(classOf[FileAttachment], ssvAttachment2.id) should be (ssvAttachment2)
		}

		flushing(session) { session.delete(ssv) }

		// Ensure we can't fetch the extension, but all the other objects are returned
		flushing(session) {
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[SavedFormValue], ssv.id) should be (null)
			session.get(classOf[FileAttachment], ssvAttachment1.id) should be (ssvAttachment1)
			session.get(classOf[FileAttachment], ssvAttachment2.id) should be (ssvAttachment2)
		}
	}
}