package uk.ac.warwick.tabula.services.attendancemonitoring

import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.{MeetingRecordDao, MeetingRecordDaoComponent}
import uk.ac.warwick.tabula.services.{RelationshipService, RelationshipServiceComponent, TermService, TermServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.util.termdates.Term.TermType
import uk.ac.warwick.util.termdates.TermImpl

class AttendanceMonitoringMeetingRecordServiceTest extends TestBase with Mockito {

	trait ServiceTestSupport extends MeetingRecordDaoComponent with AttendanceMonitoringServiceComponent
		with RelationshipServiceComponent with TermServiceComponent {

		val meetingRecordDao: MeetingRecordDao = smartMock[MeetingRecordDao]
		var relationshipService: RelationshipService = smartMock[RelationshipService]
		val termService: TermService = smartMock[TermService]
		val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
	}

	trait Fixture {
		val service = new AbstractAttendanceMonitoringMeetingRecordService with ServiceTestSupport

		val academicYear2013 = AcademicYear(2013)
		val autumnTerm = new TermImpl(null, dateTime(2013, 1, 7), null, TermType.autumn)
		service.termService.getTermFromDateIncludingVacations(any[DateTime]) returns autumnTerm

		val student: StudentMember = Fixtures.student("1234")

		val agent = "agent"
		val agentMember: StaffMember = Fixtures.staff(agent, agent)

		val tutorRelationshipType = StudentRelationshipType("personalTutor", "tutor", "personal tutor", "personal tutee")
		val supervisorRelationshipType = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisee")

		val meetingRelationship = ExternalStudentRelationship(agent, tutorRelationshipType, student, DateTime.now)

		service.relationshipService
			.getStudentRelationshipTypeById(tutorRelationshipType.id) returns Option(tutorRelationshipType)
		service.relationshipService
			.getStudentRelationshipTypeById(supervisorRelationshipType.id) returns Option(supervisorRelationshipType)
		service.relationshipService.getRelationships(tutorRelationshipType, student) returns Seq(meetingRelationship)

		val meeting = new MeetingRecord
		meeting.relationship = meetingRelationship
		meeting.format = MeetingFormat.FaceToFace
		meeting.creator = Fixtures.student("student", "student")
		meeting.meetingDate = dateTime(2013, 1, 7)
		meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))

		val meetingPoint = new AttendanceMonitoringPoint
		meetingPoint.startDate = meeting.meetingDate.minusDays(2).toLocalDate
		meetingPoint.endDate = meeting.meetingDate.plusDays(2).toLocalDate
		meetingPoint.pointType = AttendanceMonitoringPointType.Meeting
		meetingPoint.meetingRelationships = Seq(meeting.relationship.relationshipType)
		meetingPoint.relationshipService = service.relationshipService
		meetingPoint.meetingFormats = Seq(meeting.format)

		service.attendanceMonitoringService.listStudentsPoints(student, None, academicYear2013) returns Seq(meetingPoint)
		service.attendanceMonitoringService.getCheckpoints(Seq(meetingPoint), Seq(student)) returns Map()
		service.attendanceMonitoringService.studentAlreadyReportedThisTerm(student, meetingPoint) returns false
		service.attendanceMonitoringService.setAttendance(student, Map(meetingPoint -> AttendanceState.Attended), student.userId, autocreated = true) returns
			((Seq(Fixtures.attendanceMonitoringCheckpoint(meetingPoint, student, AttendanceState.Attended)), Seq[AttendanceMonitoringCheckpointTotal]()))

	}

	@Test
	def notApproved() { new Fixture {
		meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))
		service.getCheckpoints(meeting).size should be (0)
	}}

	@Test
	def noSuchStudent() { new Fixture {
		val thisMeeting = new MeetingRecord
		thisMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
		val thisMeetingRelationship = new ExternalStudentRelationship
		thisMeetingRelationship.agent = agent
		thisMeetingRelationship.relationshipType = supervisorRelationshipType
		thisMeeting.relationship = thisMeetingRelationship
		service.getCheckpoints(thisMeeting).size should be (0)
	}}

	@Test
	def nonMeetingPoint() { new Fixture {
		meetingPoint.pointType = AttendanceMonitoringPointType.Standard
		service.getCheckpoints(meeting).size should be (0)
	}}

	@Test
	def wrongRelationship() { new Fixture {
		meetingPoint.meetingRelationships = Seq(supervisorRelationshipType)
		service.getCheckpoints(meeting).size should be (0)
	}}

	@Test
	def wrongFormat() { new Fixture {
		meetingPoint.meetingFormats = Seq(MeetingFormat.Email)
		service.getCheckpoints(meeting).size should be (0)
	}}

	@Test
	def existingCheckpoint() { new Fixture {
		service.attendanceMonitoringService.getCheckpoints(Seq(meetingPoint), Seq(student)) returns
			Map(student -> Map(meetingPoint -> Fixtures.attendanceMonitoringCheckpoint(meetingPoint, student, AttendanceState.MissedAuthorised)))
		service.getCheckpoints(meeting).size should be (0)
	}}

	@Test
	def beforePoint() { new Fixture {
		meetingPoint.startDate = meeting.meetingDate.plusDays(1).toLocalDate
		service.getCheckpoints(meeting).size should be (0)
	}}

	@Test
	def afterPoint() { new Fixture {
		meetingPoint.endDate = meeting.meetingDate.minusDays(1).toLocalDate
		service.getCheckpoints(meeting).size should be (0)
	}}

	@Test
	def notEnoughMeetingsApproved() { new Fixture {
		meetingPoint.meetingQuantity = 2

		val otherMeeting = new MeetingRecord
		otherMeeting.id = "123"
		otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))
		otherMeeting.meetingDate = meeting.meetingDate
		service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

		service.getCheckpoints(meeting).size should be (0)
	}}

	@Test
	def notEnoughMeetingsCorrectFormat() { new Fixture {
		meetingPoint.meetingQuantity = 2

		val otherMeeting = new MeetingRecord
		otherMeeting.id = "123"
		otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
		otherMeeting.relationship = meetingRelationship
		otherMeeting.meetingDate = meeting.meetingDate
		otherMeeting.format = MeetingFormat.Email
		service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)
		service.getCheckpoints(meeting).size should be (0)
	}}

	@Test
	def notEnoughMeetingsBeforePoint() { new Fixture {
		meetingPoint.meetingQuantity = 2

		val otherMeeting = new MeetingRecord
		otherMeeting.id = "123"
		otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
		otherMeeting.relationship = meetingRelationship
		otherMeeting.meetingDate = meetingPoint.startDate.minusDays(1).toDateTimeAtStartOfDay
		otherMeeting.format = meeting.format
		service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)
		service.getCheckpoints(meeting).size should be (0)
	}}

	@Test
	def notEnoughMeetingsAfterPoint() { new Fixture {
		meetingPoint.meetingQuantity = 2

		val otherMeeting = new MeetingRecord
		otherMeeting.id = "123"
		otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
		otherMeeting.relationship = meetingRelationship
		otherMeeting.meetingDate = meetingPoint.endDate.plusDays(1).toDateTimeAtStartOfDay
		otherMeeting.format = meeting.format
		service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)
		service.getCheckpoints(meeting).size should be (0)
	}}

	@Test
	def validMeetings() { new Fixture {
		meetingPoint.meetingQuantity = 2
		val otherMeeting = new MeetingRecord
		otherMeeting.id = "123"
		otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
		otherMeeting.relationship = meetingRelationship
		otherMeeting.meetingDate = meeting.meetingDate
		otherMeeting.format = meeting.format
		service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)
		service.getCheckpoints(meeting).size should be (1)
	}}

	@Test
	def alreadyReported() { new Fixture {
		service.attendanceMonitoringService.studentAlreadyReportedThisTerm(student, meetingPoint) returns true

		service.getCheckpoints(meeting).size should be (0)
	}}

	@Test
	def notApprovedButCreatedByAgent() { new Fixture {
		meetingPoint.meetingQuantity = 2
		meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))
		meeting.creator = agentMember
		val otherMeeting = new MeetingRecord
		otherMeeting.id = "123"
		otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))
		otherMeeting.relationship = meetingRelationship
		otherMeeting.meetingDate = meeting.meetingDate
		otherMeeting.format = meeting.format
		otherMeeting.creator = agentMember
		service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)
		service.getCheckpoints(meeting).size should be (1)
	}}
}
