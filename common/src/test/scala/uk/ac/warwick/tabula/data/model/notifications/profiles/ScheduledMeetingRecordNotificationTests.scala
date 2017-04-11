package uk.ac.warwick.tabula.data.model.notifications.profiles

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.profiles.relationships.meetings._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.{AddsIcalAttachmentToScheduledMeetingNotification, ScheduledMeetingRecordNotification}
import uk.ac.warwick.tabula.{CurrentUser, Fixtures, Mockito, TestBase}

trait ScheduledMeetingRecordNotificationFixture {
	val admin: StaffMember = Fixtures.staff("1170836", "cuslaj")
	admin.firstName = "Some"
	admin.lastName = "Admin"
	val staff: StaffMember = Fixtures.staff("9517535", "mctutor")
	staff.firstName = "Mc"
	staff.lastName = "Tutor"
	val student: StudentMember = Fixtures.student("1234567", "youth")
	student.firstName = "A"
	student.lastName = "Youth"
	val relationshipType = StudentRelationshipType("tutor", "tutor", "tutor", "tutee")
	val relationship = StudentRelationship(staff, relationshipType, student, DateTime.now)

	def scheduledMeeting(creator: Member): ScheduledMeetingRecord = {
		val scheduledMeeting = new ScheduledMeetingRecord(creator, relationship)
		scheduledMeeting.title = "my meeting"
		scheduledMeeting.description = "discuss things"
		scheduledMeeting.meetingDate = DateTime.now
		scheduledMeeting.format = MeetingFormat.FaceToFace
		scheduledMeeting
	}
}

class CreateScheduledMeetingRecordNotificationTest extends TestBase with Mockito {
	val notifier = new CreateScheduledMeetingRecordNotification {}

	@Test
	def testStudent() { new ScheduledMeetingRecordNotificationFixture {
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(student))
		notifications.length should be {1}
		notifications.head.recipients.head should be {staff.asSsoUser}
	}}

	@Test
	def testAgent() { new ScheduledMeetingRecordNotificationFixture {
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(staff))
		notifications.length should be {1}
		notifications.head.recipients.head should be {student.asSsoUser}
	}}

	@Test
	def testAdmin() { new ScheduledMeetingRecordNotificationFixture {
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(admin))
		notifications.length should be {2}
		notifications(0).recipients.head should be { student.asSsoUser }
		notifications(0).title should be (s"Tutor meeting with ${staff.fullName.get} created by ${admin.fullName.get}")
		notifications(1).recipients.head should be { staff.asSsoUser }
		notifications(1).title should be (s"Tutor meeting with ${student.fullName.get} created on your behalf by ${admin.fullName.get}")
	}}
}

class EditScheduledMeetingRecordNotificationTest extends TestBase with Mockito
	with ScheduledMeetingRecordNotificationFixture {

	def scheduledMeetingResult(agent: Member, isRescheduled: Boolean = false): ScheduledMeetingRecordResult = {
		val sm = scheduledMeeting(agent)
		ScheduledMeetingRecordResult(sm, isRescheduled)
	}

	@Test
	def testStudentCreatedByStaff() {
		val notifier = new EditScheduledMeetingRecordNotification with EditScheduledMeetingRecordState {
			override def editor: Member = student
			override def meetingRecord: ScheduledMeetingRecord = null
		}
		val notifications = notifier.emit(scheduledMeetingResult(staff))
		notifications.length should be {1}
		notifications.head.recipients.head should be {staff.asSsoUser}
	}

	@Test
	def testStudentCreatedByStudent() {
		val notifier = new EditScheduledMeetingRecordNotification with EditScheduledMeetingRecordState {
			override def editor: Member = student
			override def meetingRecord: ScheduledMeetingRecord = null
		}
		val notifications = notifier.emit(scheduledMeetingResult(student))
		notifications.length should be {1}
		notifications.head.recipients.head should be {staff.asSsoUser}
	}

	@Test
	def testStudentCreatedByAdmin() {
		val notifier = new EditScheduledMeetingRecordNotification with EditScheduledMeetingRecordState {
			override def editor: Member = student
			override def meetingRecord: ScheduledMeetingRecord = null
		}
		val notifications = notifier.emit(scheduledMeetingResult(admin))
		notifications.length should be {1}
		notifications.head.recipients.head should be {staff.asSsoUser}
	}

	@Test
	def testAgentCreatedByAgent() {
		val notifier = new EditScheduledMeetingRecordNotification with EditScheduledMeetingRecordState {
			override def editor: Member = staff
			override def meetingRecord: ScheduledMeetingRecord = null
		}
		val notifications = notifier.emit(scheduledMeetingResult(staff))
		notifications.length should be {1}
		notifications.head.recipients.head should be {student.asSsoUser}
	}

	@Test
	def testAgentCreatedByStudent() {
		val notifier = new EditScheduledMeetingRecordNotification with EditScheduledMeetingRecordState {
			override def editor: Member = staff
			override def meetingRecord: ScheduledMeetingRecord = null
		}
		val notifications = notifier.emit(scheduledMeetingResult(student))
		notifications.length should be {1}
		notifications.head.recipients.head should be {student.asSsoUser}
	}

	@Test
	def testAgentCreatedByAdmin() {
		val notifier = new EditScheduledMeetingRecordNotification with EditScheduledMeetingRecordState {
			override def editor: Member = staff
			override def meetingRecord: ScheduledMeetingRecord = null
		}
		val notifications = notifier.emit(scheduledMeetingResult(admin))
		notifications.length should be {1}
		notifications.head.recipients.head should be {student.asSsoUser}
	}

	@Test
	def testAdminCreatedByAgent() {
		val notifier = new EditScheduledMeetingRecordNotification with EditScheduledMeetingRecordState {
			override def editor: Member = admin
			override def meetingRecord: ScheduledMeetingRecord = null
		}
		val notifications = notifier.emit(scheduledMeetingResult(staff))
		notifications.length should be {2}
		notifications(0).recipients.head should be { student.asSsoUser }
		notifications(0).title should be (s"Tutor meeting with ${staff.fullName.get} updated by ${admin.fullName.get}")
		notifications(1).recipients.head should be { staff.asSsoUser }
		notifications(1).title should be (s"Tutor meeting with ${student.fullName.get} updated on your behalf by ${admin.fullName.get}")


		val notifications2 = notifier.emit(scheduledMeetingResult(staff, isRescheduled = true))
		notifications2.length should be {2}
		notifications2(0).recipients.head should be { student.asSsoUser }
		notifications2(0).title should be (s"Tutor meeting with ${staff.fullName.get} rescheduled by ${admin.fullName.get}")
		notifications2(1).recipients.head should be { staff.asSsoUser }
		notifications2(1).title should be (s"Tutor meeting with ${student.fullName.get} rescheduled on your behalf by ${admin.fullName.get}")
	}

	@Test
	def testAdminCreatedByStudent() {
		val notifier = new EditScheduledMeetingRecordNotification with EditScheduledMeetingRecordState {
			override def editor: Member = admin
			override def meetingRecord: ScheduledMeetingRecord = null
		}
		val notifications = notifier.emit(scheduledMeetingResult(student))
		notifications.length should be {2}
		notifications(0).recipients.head should be { student.asSsoUser }
		notifications(0).title should be (s"Tutor meeting with ${staff.fullName.get} updated by ${admin.fullName.get}")
		notifications(1).recipients.head should be { staff.asSsoUser }
		notifications(1).title should be (s"Tutor meeting with ${student.fullName.get} updated on your behalf by ${admin.fullName.get}")


		val notifications2 = notifier.emit(scheduledMeetingResult(student, isRescheduled = true))
		notifications2.length should be {2}
		notifications2(0).recipients.head should be { student.asSsoUser }
		notifications2(0).title should be (s"Tutor meeting with ${staff.fullName.get} rescheduled by ${admin.fullName.get}")
		notifications2(1).recipients.head should be { staff.asSsoUser }
		notifications2(1).title should be (s"Tutor meeting with ${student.fullName.get} rescheduled on your behalf by ${admin.fullName.get}")
	}

	@Test
	def testAdminCreatedByAdmin() {
		val notifier = new EditScheduledMeetingRecordNotification with EditScheduledMeetingRecordState {
			override def editor: Member = admin
			override def meetingRecord: ScheduledMeetingRecord = null
		}
		val notifications = notifier.emit(scheduledMeetingResult(admin))
		notifications.length should be {2}
		notifications(0).recipients.head should be { student.asSsoUser }
		notifications(0).title should be (s"Tutor meeting with ${staff.fullName.get} updated by ${admin.fullName.get}")
		notifications(1).recipients.head should be { staff.asSsoUser }
		notifications(1).title should be (s"Tutor meeting with ${student.fullName.get} updated on your behalf by ${admin.fullName.get}")


		val notifications2 = notifier.emit(scheduledMeetingResult(admin, isRescheduled = true))
		notifications2.length should be {2}
		notifications2(0).recipients.head should be { student.asSsoUser }
		notifications2(0).title should be (s"Tutor meeting with ${staff.fullName.get} rescheduled by ${admin.fullName.get}")
		notifications2(1).recipients.head should be { staff.asSsoUser }
		notifications2(1).title should be (s"Tutor meeting with ${student.fullName.get} rescheduled on your behalf by ${admin.fullName.get}")
	}
}

class DeleteScheduledMeetingRecordNotificationTest extends TestBase with Mockito {

	@Test
	def testStudentCreatedByStudent() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new DeleteScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(student.asSsoUser, student.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(student))
		notifications.length should be {1}
		notifications.head.recipients.head should be {staff.asSsoUser}
	}}

	@Test
	def testStudentCreatedByAgent() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new DeleteScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(student.asSsoUser, student.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(staff))
		notifications.length should be {1}
		notifications.head.recipients.head should be {staff.asSsoUser}
	}}

	@Test
	def testStudentCreatedByAdmin() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new DeleteScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(student.asSsoUser, student.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(admin))
		notifications.length should be {1}
		notifications.head.recipients.head should be {staff.asSsoUser}
	}}

	@Test
	def testAgentCreatedByStudent() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new DeleteScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(staff.asSsoUser, staff.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(student))
		notifications.length should be {1}
		notifications.head.recipients.head should be {student.asSsoUser}
	}}

	@Test
	def testAgentCreatedByAgent() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new DeleteScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(staff.asSsoUser, staff.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(staff))
		notifications.length should be {1}
		notifications.head.recipients.head should be {student.asSsoUser}
	}}

	@Test
	def testAgentCreatedByAdmin() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new DeleteScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(staff.asSsoUser, staff.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(admin))
		notifications.length should be {1}
		notifications.head.recipients.head should be {student.asSsoUser}
	}}

	@Test
	def testAdminCreatedByStudent() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new DeleteScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(admin.asSsoUser, admin.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(student))
		notifications.length should be {2}
		notifications(0).recipients.head should be { student.asSsoUser }
		notifications(0).title should be (s"Tutor meeting with ${staff.fullName.get} deleted by ${admin.fullName.get}")
		notifications(1).recipients.head should be { staff.asSsoUser }
		notifications(1).title should be (s"Tutor meeting with ${student.fullName.get} deleted on your behalf by ${admin.fullName.get}")
	}}

	@Test
	def testAdminCreatedByAgent() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new DeleteScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(admin.asSsoUser, admin.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(staff))
		notifications.length should be {2}
		notifications(0).recipients.head should be { student.asSsoUser }
		notifications(0).title should be (s"Tutor meeting with ${staff.fullName.get} deleted by ${admin.fullName.get}")
		notifications(1).recipients.head should be { staff.asSsoUser }
		notifications(1).title should be (s"Tutor meeting with ${student.fullName.get} deleted on your behalf by ${admin.fullName.get}")
	}}

	@Test
	def testAdminCreatedByAdmin() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new DeleteScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(admin.asSsoUser, admin.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(admin))
		notifications.length should be {2}
		notifications(0).recipients.head should be { student.asSsoUser }
		notifications(0).title should be (s"Tutor meeting with ${staff.fullName.get} deleted by ${admin.fullName.get}")
		notifications(1).recipients.head should be { staff.asSsoUser }
		notifications(1).title should be (s"Tutor meeting with ${student.fullName.get} deleted on your behalf by ${admin.fullName.get}")
	}}
}

class RestoreScheduledMeetingRecordNotificationTest extends TestBase with Mockito {

	@Test
	def testStudentCreatedByStudent() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new RestoreScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(student.asSsoUser, student.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(student))
		notifications.length should be {1}
		notifications.head.recipients.head should be {staff.asSsoUser}
	}}

	@Test
	def testStudentCreatedByAgent() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new RestoreScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(student.asSsoUser, student.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(staff))
		notifications.length should be {1}
		notifications.head.recipients.head should be {staff.asSsoUser}
	}}

	@Test
	def testStudentCreatedByAdmin() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new RestoreScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(student.asSsoUser, student.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(admin))
		notifications.length should be {1}
		notifications.head.recipients.head should be {staff.asSsoUser}
	}}

	@Test
	def testAgentCreatedByStudent() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new RestoreScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(staff.asSsoUser, staff.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(student))
		notifications.length should be {1}
		notifications.head.recipients.head should be {student.asSsoUser}
	}}

	@Test
	def testAgentCreatedByAgent() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new RestoreScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(staff.asSsoUser, staff.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(staff))
		notifications.length should be {1}
		notifications.head.recipients.head should be {student.asSsoUser}
	}}

	@Test
	def testAgentCreatedByAdmin() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new RestoreScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(staff.asSsoUser, staff.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(admin))
		notifications.length should be {1}
		notifications.head.recipients.head should be {student.asSsoUser}
	}}

	@Test
	def testAdminCreatedByStudent() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new RestoreScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(admin.asSsoUser, admin.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(student))
		notifications.length should be {2}
		notifications(0).recipients.head should be { student.asSsoUser }
		notifications(0).title should be (s"Tutor meeting with ${staff.fullName.get} rescheduled by ${admin.fullName.get}")
		notifications(1).recipients.head should be { staff.asSsoUser }
		notifications(1).title should be (s"Tutor meeting with ${student.fullName.get} rescheduled on your behalf by ${admin.fullName.get}")
	}}

	@Test
	def testAdminCreatedByAgent() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new RestoreScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(admin.asSsoUser, admin.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(staff))
		notifications.length should be {2}
		notifications(0).recipients.head should be { student.asSsoUser }
		notifications(0).title should be (s"Tutor meeting with ${staff.fullName.get} rescheduled by ${admin.fullName.get}")
		notifications(1).recipients.head should be { staff.asSsoUser }
		notifications(1).title should be (s"Tutor meeting with ${student.fullName.get} rescheduled on your behalf by ${admin.fullName.get}")
	}}

	@Test
	def testAdminCreatedByAdmin() { new ScheduledMeetingRecordNotificationFixture {
		val notifier = new RestoreScheduledMeetingRecordNotification with RemoveMeetingRecordState {
			override def meetingRecord: AbstractMeetingRecord = null
			override def user: CurrentUser = new CurrentUser(admin.asSsoUser, admin.asSsoUser)
		}
		val notifications: Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = notifier.emit(scheduledMeeting(admin))
		notifications.length should be {2}
		notifications(0).recipients.head should be { student.asSsoUser }
		notifications(0).title should be (s"Tutor meeting with ${staff.fullName.get} rescheduled by ${admin.fullName.get}")
		notifications(1).recipients.head should be { staff.asSsoUser }
		notifications(1).title should be (s"Tutor meeting with ${student.fullName.get} rescheduled on your behalf by ${admin.fullName.get}")
	}}

}
