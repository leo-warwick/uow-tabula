package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.{PersistenceTestBase, Fixtures}
import org.joda.time.DateTimeConstants
import collection.JavaConversions._

// scalastyle:off magic.number
class MeetingRecordTest extends PersistenceTestBase {

	val aprilFool = dateTime(2013, DateTimeConstants.APRIL)

	@Test def deleteFileAttachmentOnDelete = transactional {ts=>
		val orphanAttachment = flushing(session) {
			val attachment = new FileAttachment
			session.save(attachment)
			attachment
		}

		val (creator, relationship) = flushing(session){
			val creator = new StaffMember(id = idFormat(1))
			creator.userId = idFormat(11)
			val relationship = new ExternalStudentRelationship
			session.save(creator)
			session.save(relationship)
			(creator, relationship)
		}

		val (meetingRecord, meetingRecordkAttachment) = flushing(session) {
			val meetingRecord = new MeetingRecord(creator, relationship)
			meetingRecord.id = idFormat(2)

			val attachment = new FileAttachment
			meetingRecord.attachments = List(attachment)

			session.save(meetingRecord)
			(meetingRecord, attachment)
		}

		// Ensure everything's been persisted
		orphanAttachment.id should not be (null)
		meetingRecord.id should not be (null)
		meetingRecordkAttachment.id should not be (null)

		// Can fetch everything from db
		flushing(session) {
			session.getById[FileAttachment](orphanAttachment.id) should be (Some(orphanAttachment))
			session.getById[MeetingRecord](meetingRecord.id) should be (Some(meetingRecord))
			session.getById[FileAttachment](meetingRecordkAttachment.id) should be (Some(meetingRecordkAttachment))
		}

		flushing(session) { session.delete(meetingRecord) }

		// Ensure we can't fetch the feedback or attachment, but all the other objects are returned
		flushing(session) {
			session.getById[FileAttachment](orphanAttachment.id) should be (Some(orphanAttachment))
			session.getById[MeetingRecord](meetingRecord.id) should be ('empty)
			session.getById[FileAttachment](meetingRecordkAttachment.id) should be ('empty)
		}
	}



	@Test def defaultConstructor = withFakeTime(aprilFool) {
		val meeting = new MeetingRecord

		meeting.creationDate should be (aprilFool)
		meeting.lastUpdatedDate should be (aprilFool)

		meeting.creator should be (null)
		meeting.relationship should be (null)
		meeting.meetingDate should be (null)
		meeting.format should be (null)
		meeting should be ('approved)
	}

	@Test def everydayConstructor = withFakeTime(aprilFool) {
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

		val student = Fixtures.student(universityId = "1000001", userId="student")

		val creator = new StaffMember
		val relationship = ExternalStudentRelationship("Professor A Tutor", relationshipType, student)

		val meeting = new MeetingRecord(creator, relationship)

		meeting.creationDate should be (aprilFool)
		meeting.lastUpdatedDate should be (aprilFool)

		meeting.creator should be (creator)
		meeting.relationship should be (relationship)
		meeting.meetingDate should be (null)
		meeting.format should be (null)
		meeting should be ('approved)
	}

	/** Zero-pad integer to a 7 digit string */
	def idFormat(i:Int) = "%07d" format i
}