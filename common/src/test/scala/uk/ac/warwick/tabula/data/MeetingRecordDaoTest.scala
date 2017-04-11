package uk.ac.warwick.tabula.data

import org.joda.time.{DateTime, DateTimeConstants}
import org.junit.Before
import uk.ac.warwick.tabula.{Fixtures, PersistenceTestBase}
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.data.model.ExternalStudentRelationship

// scalastyle:off magic.number
class MeetingRecordDaoTest extends PersistenceTestBase {

	// This test for memberdao maybe shouldn't be involving meetingrecorddao.
	val memberDao = new AutowiringMemberDaoImpl
	val relationshipDao = new RelationshipDaoImpl
	val meetingDao = new MeetingRecordDaoImpl

	@Before
	def setup() {
		memberDao.sessionFactory = sessionFactory
		relationshipDao.sessionFactory = sessionFactory
		meetingDao.sessionFactory = sessionFactory
	}

	@Test def createAndList() = transactional { tx =>
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		relationshipDao.saveOrUpdate(relationshipType)

		val student = Fixtures.student(universityId = "1000001", userId="student")
		memberDao.saveOrUpdate(student)

		val creator = Fixtures.staff(universityId = "0000001", userId="staff1")
		val relationship = ExternalStudentRelationship("Professor A Tutor", relationshipType, student, DateTime.now)

		memberDao.saveOrUpdate(creator)
		relationshipDao.saveOrUpdate(relationship)

		val relSet: Set[StudentRelationship] = Set(relationship)

		val currentMember = new StaffMember
		currentMember.universityId = "0070790"

		meetingDao.list(relSet, Some(currentMember)).size should be (0)

		// create some meetings, out of order
		val middleMeeting = new MeetingRecord(creator, relationship)
		val earliestMeeting = new MeetingRecord(creator, relationship)
		val newestMeeting = new MeetingRecord(creator, relationship)

		meetingDao.saveOrUpdate(middleMeeting)
		meetingDao.saveOrUpdate(earliestMeeting)
		meetingDao.saveOrUpdate(newestMeeting)

		middleMeeting.meetingDate = dateTime(2013, DateTimeConstants.APRIL)
		earliestMeeting.meetingDate = dateTime(2013, DateTimeConstants.JANUARY)
		newestMeeting.meetingDate = dateTime(2013, DateTimeConstants.JUNE)

		meetingDao.saveOrUpdate(middleMeeting)
		meetingDao.saveOrUpdate(earliestMeeting)
		meetingDao.saveOrUpdate(newestMeeting)

		val savedMeetings = meetingDao.list(relSet, Some(currentMember))
		savedMeetings.size should be (3)
		savedMeetings.head should be (newestMeeting)
		savedMeetings.last should be (earliestMeeting)
	}
}