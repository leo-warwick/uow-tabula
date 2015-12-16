package uk.ac.warwick.tabula.commands.profiles

import org.joda.time.DateTimeConstants
import org.junit.Before
import uk.ac.warwick.tabula.data.model.MeetingApprovalState.Pending
import uk.ac.warwick.tabula.data.model.MeetingFormat.FaceToFace
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.events.EventHandling
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{CurrentUser, Fixtures, Mockito, PersistenceTestBase}

trait MeetingRecordTests extends PersistenceTestBase with Mockito {

	val aprilFool = dateTime(2013, DateTimeConstants.APRIL)
	val marchHare = dateTime(2013, DateTimeConstants.MARCH)

	val ps = mock[ProfileService]

	EventHandling.enabled = false

	val meetingRecordService = smartMock[MeetingRecordService]
	val maintenanceModeService = smartMock[MaintenanceModeService]
	maintenanceModeService.enabled returns false
	val notificationService = smartMock[NotificationService]
	val scheduledNotificationService = smartMock[ScheduledNotificationService]
	val securityService = smartMock[SecurityService]

	var student:StudentMember = _
	var creator: StaffMember = _
	var relationship: StudentRelationship = _
	var meeting: MeetingRecord = _

	val user = smartMock[CurrentUser]
	user.universityId returns "9876543"

	@Before
	def setUp() {
		creator = transactional { tx =>
			val m = new StaffMember("9876543")
			m.userId = "staffmember"
			session.save(m)
			m
		}

		student = transactional { tx =>
			val m = Fixtures.student(universityId="1170836", userId="studentmember")
			session.save(m)
			m
		}

		relationship = transactional { tx =>
			val relationship = ExternalStudentRelationship("Professor A Tutor", StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee"), student)
			session.save(relationship)
			relationship
		}

		meeting = {
			val mr = new MeetingRecord
			mr.creator = creator
			mr.relationship = relationship
			mr.title = "A title"
			mr.format = FaceToFace
			mr.meetingDate = aprilFool
			mr.description = "Lovely words"
			mr.securityService = securityService

			val approval = new MeetingRecordApproval
			approval.approver = relationship.studentMember.get
			approval.creationDate = aprilFool
			approval.state = Pending
			approval.meetingRecord = mr

			mr.approvals.add(approval)
			mr
		}
	}
}
