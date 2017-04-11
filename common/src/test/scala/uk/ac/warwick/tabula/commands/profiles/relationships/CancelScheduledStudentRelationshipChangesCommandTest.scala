package uk.ac.warwick.tabula.commands.profiles.relationships

import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.profiles.{CancelledBulkStudentRelationshipChangeToAgentNotification, CancelledStudentRelationshipChangeToNewAgentNotification, CancelledStudentRelationshipChangeToOldAgentNotification, CancelledStudentRelationshipChangeToStudentNotification}
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.{CurrentUser, Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class CancelScheduledStudentRelationshipChangesCommandTest extends TestBase with Mockito {

	trait Fixture {
		protected val thisDepartment: Department = Fixtures.department("its")
		protected val thisRelationshipType = StudentRelationshipType("tutor","tutor","tutor","tutee")
		protected val student1: StudentMember = Fixtures.student("1234")
		protected val student2: StudentMember = Fixtures.student("2345")
		protected val agent1: StaffMember = Fixtures.staff("4567")
		protected val agent2: StaffMember = Fixtures.staff("5678")
		protected val agent3: StaffMember = Fixtures.staff("6789")
		private val user1 = new User("cuscav")
		private val thisUser = new CurrentUser(user1, user1)
		protected val notifier = new CancelScheduledStudentRelationshipChangesCommandNotifications
			with ManageStudentRelationshipsState
			with UpdateScheduledStudentRelationshipChangesCommandRequest {
			override val department: Department = thisDepartment
			override val relationshipType: StudentRelationshipType = thisRelationshipType
			override val user: CurrentUser = thisUser
		}
		protected val mockProfileService: ProfileService = smartMock[ProfileService]
		mockProfileService.getMemberByUniversityId(student1.universityId) returns Option(student1)
		mockProfileService.getMemberByUniversityId(student2.universityId) returns Option(student2)
	}

	@Test
	def singleRelationshipNotifyStudent(): Unit = {
		// Cancelled add
		new Fixture {
			private val relationship = StudentRelationship(agent1, thisRelationshipType, student1, DateTime.now.plusDays(1))
			notifier.notifyStudent = true
			notifier.relationships = JArrayList(relationship)
			private val notifications = notifier.emit(Seq(relationship))
			notifications.size should be (1)
			private val notification = notifications.head.asInstanceOf[CancelledStudentRelationshipChangeToStudentNotification]
			notification.cancelledAdditionsIds.value should be (Seq(agent1.universityId))
			notification.cancelledRemovalsIds.value should be (Seq())
			notification.scheduledDate.get should be (relationship.startDate)
			notification.profileService = mockProfileService
			notification.student.get should be (student1)
		}
		// Cancelled remove
		new Fixture {
			private val relationship = StudentRelationship(agent1, thisRelationshipType, student1, DateTime.now.minusDays(1))
			relationship.endDate = DateTime.now.plusDays(1)
			notifier.notifyStudent = true
			notifier.relationships = JArrayList(relationship)
			private val notifications = notifier.emit(Seq(relationship))
			notifications.size should be (1)
			private val notification = notifications.head.asInstanceOf[CancelledStudentRelationshipChangeToStudentNotification]
			notification.cancelledAdditionsIds.value should be (Seq())
			notification.cancelledRemovalsIds.value should be (Seq(agent1.universityId))
			notification.scheduledDate.get should be (relationship.endDate)
			notification.profileService = mockProfileService
			notification.student.get should be (student1)
		}
		// Cancelled replace
		new Fixture {
			private val cancelledRelationship = StudentRelationship(agent1, thisRelationshipType, student1, DateTime.now.plusDays(1))
			private val replacedRelationship = StudentRelationship(agent2, thisRelationshipType, student1, DateTime.now.minusDays(1))
			replacedRelationship.endDate = cancelledRelationship.startDate
			notifier.previouslyReplacedRelationships.put(cancelledRelationship, Set(replacedRelationship))
			notifier.notifyStudent = true
			notifier.relationships = JArrayList(cancelledRelationship)
			// Called by the validator
			notifier.scheduledDate
			// Done by apply
			replacedRelationship.endDate = null
			private val notifications = notifier.emit(Seq(cancelledRelationship))
			private val notification = notifications.head.asInstanceOf[CancelledStudentRelationshipChangeToStudentNotification]
			notification.cancelledAdditionsIds.value should be (Seq(agent1.universityId))
			notification.cancelledRemovalsIds.value should be (Seq(agent2.universityId))
			notification.scheduledDate.get should be (cancelledRelationship.startDate)
			notification.profileService = mockProfileService
			notification.student.get should be (student1)
		}
	}

	trait MultipleChangesFixture extends Fixture {
		protected val changeDate: DateTime = DateTime.now.plusDays(1)
		protected val agent1student1EndingRel = StudentRelationship(agent1, thisRelationshipType, student1, DateTime.now.minusDays(1))
		agent1student1EndingRel.endDate = changeDate
		protected val agent2student1StartingRel = StudentRelationship(agent2, thisRelationshipType, student1, changeDate)
		notifier.previouslyReplacedRelationships.put(agent2student1StartingRel, Set(agent1student1EndingRel))
		protected val agent3student1EndingRel = StudentRelationship(agent3, thisRelationshipType, student1, DateTime.now.minusDays(1))
		agent3student1EndingRel.endDate = changeDate
		protected val agent3student2StartingRel = StudentRelationship(agent3, thisRelationshipType, student2, changeDate)
		protected val appliedRelationships = Seq(agent2student1StartingRel, agent3student1EndingRel, agent3student2StartingRel)
	}

	@Test
	def multipleNotifyStudents(): Unit = {
		new MultipleChangesFixture {
			notifier.notifyStudent = true
			notifier.relationships = JArrayList(appliedRelationships)
			private val notifications = notifier.emit(appliedRelationships)

			notifications.size should be (2)
			private val studentNotifications = notifications.map { notification =>
				val studentNotifcation = notification.asInstanceOf[CancelledStudentRelationshipChangeToStudentNotification]
				studentNotifcation.scheduledDate.get should be (changeDate)
				studentNotifcation.profileService = mockProfileService
				studentNotifcation
			}
			private val student1Notification = studentNotifications.find(_.student.contains(student1)).get
			private val student2Notification = studentNotifications.find(_.student.contains(student2)).get
			student1Notification.cancelledAdditionsIds.value should be (Seq(agent2.universityId))
			student1Notification.cancelledRemovalsIds.value.sorted should be (Seq(agent1.universityId, agent3.universityId))
			student1Notification.student.get should be (student1)
			student2Notification.cancelledAdditionsIds.value should be (Seq(agent3.universityId))
			student2Notification.cancelledRemovalsIds.value should be (Seq())
			student2Notification.student.get should be (student2)
		}
	}

	@Test
	def multipleNotifyOldAgents(): Unit = {
		new MultipleChangesFixture {
			notifier.notifyOldAgent = true
			notifier.relationships = JArrayList(appliedRelationships)
			private val notifications = notifier.emit(appliedRelationships)

			notifications.size should be (2)

			private val agent1Notification = notifications.collect { case n: CancelledStudentRelationshipChangeToOldAgentNotification => n }.head
			agent1Notification.cancelledRemovalsIds.value should be (Seq(agent1.universityId))
			agent1Notification.cancelledAdditionsIds.value should be (Seq(agent2.universityId))
			agent1Notification.scheduledDate.get should be (changeDate)
			agent1Notification.profileService = mockProfileService
			agent1Notification.student.get should be (student1)

			private val agent3Notification = notifications.collect { case n: CancelledBulkStudentRelationshipChangeToAgentNotification => n }.head
			agent3Notification.recipientUniversityId should be (agent3.universityId)
			agent3Notification.cancelledRemovalsIds.value should be (Seq(student1.universityId))
			agent3Notification.cancelledAdditionsIds.value should be (Seq(student2.universityId))
			agent3Notification.scheduledDate.get should be (changeDate)
		}
	}

	@Test
	def multipleNotifyNewAgents(): Unit = {
		new MultipleChangesFixture {
			notifier.notifyNewAgent = true
			notifier.relationships = JArrayList(appliedRelationships)
			private val notifications = notifier.emit(appliedRelationships)

			notifications.size should be (2)

			private val agent2Notification = notifications.collect { case n: CancelledStudentRelationshipChangeToNewAgentNotification => n }.head
			agent2Notification.cancelledRemovalsIds.value should be (Seq(agent1.universityId))
			agent2Notification.cancelledAdditionsIds.value should be (Seq(agent2.universityId))
			agent2Notification.scheduledDate.get should be (changeDate)
			agent2Notification.profileService = mockProfileService
			agent2Notification.student.get should be (student1)

			private val agent3Notification = notifications.collect { case n: CancelledBulkStudentRelationshipChangeToAgentNotification => n }.head
			agent3Notification.recipientUniversityId should be (agent3.universityId)
			agent3Notification.cancelledRemovalsIds.value should be (Seq(student1.universityId))
			agent3Notification.cancelledAdditionsIds.value should be (Seq(student2.universityId))
			agent3Notification.scheduledDate.get should be (changeDate)
		}
	}

}