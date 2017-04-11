package uk.ac.warwick.tabula.commands.profiles

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.profiles.ViewRelatedStudentsCommand.Result
import uk.ac.warwick.tabula.data.ScalaRestriction
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{Mockito, TestBase}


class ViewRelatedStudentsCommandTest extends TestBase with Mockito {

	val staffMember = new StaffMember("test")

	trait Fixture {
		val testDepartment = new Department
		testDepartment.fullName = "Department of Architecture and Explosions"
		testDepartment.code = "DA"

		val course = new Course
		course.code = "DA1"
		course.name = "Beginners Building Things"

		val testRoute1, testRoute2 = new Route

		testRoute1.code = "DA101"
		testRoute1.name = "101 Explosives"

		testRoute2.code = "DA102"
		testRoute2.name = "102 Clearing up"

		val courseDetails1, courseDetails2 = new StudentCourseDetails
		val student1 = new StudentMember
		student1.universityId = "0110011"

		val student2 = new StudentMember
		student2.universityId = "0110022"

		courseDetails1.department = testDepartment
		courseDetails1.currentRoute = testRoute1
		courseDetails1.course = course
		courseDetails1.student = student1

		courseDetails2.department = testDepartment
		courseDetails2.currentRoute = testRoute2
		courseDetails2.course = course
		courseDetails2.student = student2
	}

	@Test
	def listsAllStudentsWithTutorRelationship() { new Fixture {
		val mockProfileService: ProfileService = mock[ProfileService]
		val mockMeetingRecordService: MeetingRecordService = mock[MeetingRecordService]
		val mockRelationshipService: RelationshipService = mock[RelationshipService]

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		val restrictions : Seq[ScalaRestriction] = Seq()

		val relationship1 = Seq(StudentRelationship(staffMember, relationshipType, courseDetails1.student, DateTime.now))
		val relationship2 = Seq(StudentRelationship(staffMember, relationshipType, courseDetails2.student, DateTime.now))

		mockProfileService.getSCDsByAgentRelationshipAndRestrictions(relationshipType, staffMember, restrictions) returns Seq(courseDetails1, courseDetails2)

		mockRelationshipService.getRelationships(relationshipType, courseDetails1.student) returns relationship1
		mockRelationshipService.getRelationships(relationshipType, courseDetails2.student) returns relationship2

		mockMeetingRecordService.list(relationship1.toSet, Some(staffMember)) returns Seq()
		mockMeetingRecordService.list(relationship2.toSet, Some(staffMember)) returns Seq()

		mockMeetingRecordService.countPendingApprovals(courseDetails1.student.universityId) returns 0
		mockMeetingRecordService.countPendingApprovals(courseDetails2.student.universityId) returns 0

		val command = new ViewRelatedStudentsCommandInternal(staffMember, relationshipType) with ProfileServiceComponent with MeetingRecordServiceComponent with RelationshipServiceComponent {
			var profileService: ProfileService = mockProfileService
			var meetingRecordService: MeetingRecordService = mockMeetingRecordService
			var relationshipService: RelationshipService = mockRelationshipService
		}

		val result: Result = command.applyInternal()

		result.entities should be (Seq(courseDetails1, courseDetails2))
	}}

	@Test
	def listsAllStudentsWithSupervisorRelationshipWithMeetingRecord() { new Fixture {
		val mockProfileService: ProfileService = mock[ProfileService]
		val mockMeetingRecordService: MeetingRecordService = mock[MeetingRecordService]
		val mockRelationshipService: RelationshipService = mock[RelationshipService]

		val restrictions : Seq[ScalaRestriction] = Seq()
		val relationshipType = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisee")

		val relationship1 = StudentRelationship(staffMember, relationshipType, courseDetails1.student, DateTime.now)
		val relationship2 = StudentRelationship(staffMember, relationshipType, courseDetails2.student, DateTime.now)

		val meetingRecord = new MeetingRecord
		meetingRecord.relationship = relationship1

		val proposedApproval = new MeetingRecordApproval
		proposedApproval.meetingRecord = meetingRecord
		proposedApproval.approver = courseDetails1.student
		proposedApproval.state = MeetingApprovalState.Pending

		meetingRecord.approvals.add(proposedApproval)

		mockProfileService.getSCDsByAgentRelationshipAndRestrictions(relationshipType, staffMember, restrictions) returns Seq(courseDetails1, courseDetails2)

		mockRelationshipService.getRelationships(relationshipType, courseDetails1.student) returns Seq(relationship1)
		mockRelationshipService.getRelationships(relationshipType, courseDetails2.student) returns Seq(relationship2)


		mockMeetingRecordService.list(Seq(relationship1).toSet, Some(staffMember)) returns Seq(meetingRecord)
		mockMeetingRecordService.list(Seq(relationship2).toSet, Some(staffMember)) returns Seq()

		mockMeetingRecordService.countPendingApprovals(courseDetails1.student.universityId) returns 1
		mockMeetingRecordService.countPendingApprovals(courseDetails2.student.universityId) returns 0


		val command = new ViewRelatedStudentsCommandInternal(staffMember, relationshipType) with ProfileServiceComponent with MeetingRecordServiceComponent with RelationshipServiceComponent {
			var profileService: ProfileService = mockProfileService
			var meetingRecordService: MeetingRecordService = mockMeetingRecordService
			var relationshipService: RelationshipService = mockRelationshipService
		}

		val result: Result = command.applyInternal()

		result.entities should be (Seq(courseDetails1, courseDetails2))
		val studentMeetingRecord: Option[MeetingRecord] = result.lastMeetingWithTotalPendingApprovalsMap(courseDetails1.student.universityId)._1
		val studentPendingApprovalCount: Int = result.lastMeetingWithTotalPendingApprovalsMap(courseDetails1.student.universityId)._2
		studentMeetingRecord should be (Some(meetingRecord))
		studentPendingApprovalCount should be (1)
	}}

	@Test
	def helperFunctions() { new Fixture {
		val mockProfileService: ProfileService = mock[ProfileService]
		val mockMeetingRecordService: MeetingRecordService = mock[MeetingRecordService]
		val mockRelationshipService: RelationshipService = mock[RelationshipService]

		val relationshipType = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisee")

		mockProfileService.getSCDsByAgentRelationshipAndRestrictions(relationshipType, staffMember, Nil) returns Seq(courseDetails1, courseDetails2)

		val command = new ViewRelatedStudentsCommandInternal(staffMember, relationshipType) with ProfileServiceComponent with MeetingRecordServiceComponent with RelationshipServiceComponent{
			var profileService: ProfileService = mockProfileService
			var meetingRecordService: MeetingRecordService = mockMeetingRecordService
			var relationshipService: RelationshipService = mockRelationshipService
		}

		command.allCourses should be (Seq(courseDetails1, courseDetails2))
		command.allDepartments should be (Seq(testDepartment))
		command.allRoutes should be (Seq(testRoute1, testRoute2))
	}}

}
