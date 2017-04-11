package uk.ac.warwick.tabula.commands.profiles.relationships

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.DescriptionImpl
import uk.ac.warwick.tabula.data.model.notifications.profiles.{StudentRelationshipChangeToOldAgentNotification, StudentRelationshipChangeToStudentNotification}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.profiles.TutorFixture
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.services.{RelationshipService, RelationshipServiceComponent}

class EditStudentRelationshipCommandTest extends TestBase with Mockito {

	@Test
	def describeShouldIncludeNewTutorAndStudent() { new TutorFixture {
		private val thisStudentCourseDetails = studentCourseDetails
		private val thisRelationshipType = tutorRelationshipType
		private val command = new EditStudentRelationshipDescription
			with EditStudentRelationshipCommandState
			with EditStudentRelationshipCommandRequest {
			override val studentCourseDetails: StudentCourseDetails = thisStudentCourseDetails
			override val relationshipType: StudentRelationshipType = thisRelationshipType
			override val user: CurrentUser = NoCurrentUser()
		}

		command.oldAgent = oldTutor
		command.newAgent = newTutor
		val desc = new DescriptionImpl
		command.describe(desc)

		desc.allProperties should be (Map(
			"students" -> List("student"),
			"sprCode" -> "0000001/1",
			"oldAgent" -> "0000002",
			"newAgent" -> "0000001"
		))
	}}

	@Test
	def emitShouldCreateNotificationToTutee() { new TutorFixture {
		private val thisStudentCourseDetails = studentCourseDetails
		private val thisRelationshipType = tutorRelationshipType
		val command = new EditStudentRelationshipCommandNotifications
			with EditStudentRelationshipCommandState
			with EditStudentRelationshipCommandRequest {
			override def studentCourseDetails: StudentCourseDetails = thisStudentCourseDetails
			override def relationshipType: StudentRelationshipType = thisRelationshipType
			override def user: CurrentUser = NoCurrentUser()
		}

		command.oldAgent = oldTutor
		command.newAgent = newTutor
		command.notifyStudent = true

		private val notifications = command.emit(Seq(relationship))
		notifications.size should be(1)
		notifications.exists(n => n.recipients.exists(u => u.getWarwickId == "student")) should be (true)
	}}

	@Test
	def emitShouldCreateNotificationToOldTutor() { new TutorFixture{
		private val thisStudentCourseDetails = studentCourseDetails
		private val thisRelationshipType = tutorRelationshipType
		val command = new EditStudentRelationshipCommandNotifications
			with EditStudentRelationshipCommandState
			with EditStudentRelationshipCommandRequest {
			override def studentCourseDetails: StudentCourseDetails = thisStudentCourseDetails
			override def relationshipType: StudentRelationshipType = thisRelationshipType
			override def user: CurrentUser = NoCurrentUser()
		}

		command.oldAgent = oldTutor
		command.newAgent = newTutor
		command.notifyOldAgent = true

		private val notifications = command.emit(Seq(relationship))
		notifications.size should be(1)
	}}

	@Test
	def emitShouldCreateNotificationToNewTutor() { new TutorFixture{
		private val thisStudentCourseDetails = studentCourseDetails
		private val thisRelationshipType = tutorRelationshipType
		val command = new EditStudentRelationshipCommandNotifications
			with EditStudentRelationshipCommandState
			with EditStudentRelationshipCommandRequest {
			override def studentCourseDetails: StudentCourseDetails = thisStudentCourseDetails
			override def relationshipType: StudentRelationshipType = thisRelationshipType
			override def user: CurrentUser = NoCurrentUser()
		}

		command.oldAgent = oldTutor
		command.newAgent = newTutor
		command.notifyNewAgent = true

		private val notifications = command.emit(Seq(relationship))
		notifications.size should be(1)
	}}

	@Test
	def emitShouldNotNotifyOldTutorIfTheyDontExist() { new TutorFixture{
		private val thisStudentCourseDetails = studentCourseDetails
		private val thisRelationshipType = tutorRelationshipType
		val command = new EditStudentRelationshipCommandNotifications
			with EditStudentRelationshipCommandState
			with EditStudentRelationshipCommandRequest {
			override def studentCourseDetails: StudentCourseDetails = thisStudentCourseDetails
			override def relationshipType: StudentRelationshipType = thisRelationshipType
			override def user: CurrentUser = NoCurrentUser()
		}

		command.newAgent = newTutor
		command.notifyNewAgent = true
		command.notifyOldAgent = true
		command.notifyStudent = true

		private val notifications = command.emit(Seq(relationship))
		notifications.size should be(2)
	}}

	@Test
	def emitShouldNotifyOnRemove() { new TutorFixture{
		private val thisStudentCourseDetails = studentCourseDetails
		private val thisRelationshipType = tutorRelationshipType
		val command = new EditStudentRelationshipCommandNotifications
			with EditStudentRelationshipCommandState
			with EditStudentRelationshipCommandRequest {
			override def studentCourseDetails: StudentCourseDetails = thisStudentCourseDetails
			override def relationshipType: StudentRelationshipType = thisRelationshipType
			override def user: CurrentUser = NoCurrentUser()
		}

		command.oldAgent = oldTutor
		command.newAgent = newTutor
		command.notifyOldAgent = true
		command.notifyStudent = true

		private val notifications = command.emit(Seq(relationship))
		notifications.size should be(2)
		notifications.exists(_.isInstanceOf[StudentRelationshipChangeToOldAgentNotification]) should be (true)
		notifications.exists(_.isInstanceOf[StudentRelationshipChangeToStudentNotification]) should be (true)
	}}

	@Test
	def testApplyNoExistingRels() { withFakeTime(DateTime.now) { new TutorFixture {
		// apply should return all the modified relationships

		// no existing relationships for the SCD - add a new tutor and get back the new relationship:
		val command = new EditStudentRelationshipCommandInternal(studentCourseDetails, tutorRelationshipType, NoCurrentUser())
			with RelationshipServiceComponent
			with EditStudentRelationshipCommandRequest {
			override val relationshipService: RelationshipService = mockRelationshipService
		}

		command.newAgent = newTutor

		private val rels = command.applyInternal()
		rels.size should be (1)
		rels.head.agent should be (newTutor.universityId)
		rels.head.studentMember should be (Some(studentCourseDetails.student))
	}}}

	@Test
	def testApplyExistingRel() { withFakeTime(DateTime.now) { new TutorFixture {
		// an existing relationship with a different tutor:
		val command = new EditStudentRelationshipCommandInternal(studentCourseDetails, tutorRelationshipType, NoCurrentUser())
			with RelationshipServiceComponent
			with EditStudentRelationshipCommandRequest {
			override val relationshipService: RelationshipService = mockRelationshipService
		}
		command.oldAgent = oldTutor
		command.newAgent = newTutor

		private val rels = command.applyInternal()
		rels.size should be (2)
		rels.exists(r => r.agent == newTutor.universityId) should be (true)
		rels.exists(r => r.studentMember.contains(studentCourseDetails.student)) should be (true)
	}}}

	@Test
	def testApplyReplaceWithSelf() { new TutorFixture {
		// replace an agent with themselves, ie do nothing
		val command = new EditStudentRelationshipCommandInternal(studentCourseDetails, tutorRelationshipType, NoCurrentUser())
			with RelationshipServiceComponent
			with EditStudentRelationshipCommandRequest {
			override val relationshipService: RelationshipService = mockRelationshipService
		}
		command.oldAgent = oldTutor
		command.newAgent = oldTutor

		private val rels = command.applyInternal()
		rels.size should be(0)
	}}

	@Test
	def testApplyMultipleOldTutorsReplaceOne() { withFakeTime(DateTime.now) { new TutorFixture {
		// multiple old tutors, just replace one
		val oldTutor2 = new StaffMember
		oldTutor2.universityId = "00000022"

		mockProfileService.getMemberByUniversityId("00000022") returns Some(oldTutor2)

		val relationshipOld2 = new MemberStudentRelationship
		relationshipOld2.studentMember = student
		relationshipOld2.agentMember = oldTutor2

		mockRelationshipService.findCurrentRelationships(tutorRelationshipType, studentCourseDetails) returns Seq(relationshipOld, relationshipOld2)

		val command = new EditStudentRelationshipCommandInternal(studentCourseDetails, tutorRelationshipType, NoCurrentUser())
			with RelationshipServiceComponent
			with EditStudentRelationshipCommandRequest {
			override val relationshipService: RelationshipService = mockRelationshipService
		}
		command.oldAgent = oldTutor
		command.newAgent = newTutor

		val rels3: Seq[StudentRelationship] = command.applyInternal()

		// so we should have replaced oldTutor with newTutor and oldTutor2 should remain untouched
		rels3.size should be (2)
		rels3.exists(r => r.agent == newTutor.universityId) should be (true)
		rels3.exists(r => r.studentMember.contains(studentCourseDetails.student)) should be (true)
	}}}

}
