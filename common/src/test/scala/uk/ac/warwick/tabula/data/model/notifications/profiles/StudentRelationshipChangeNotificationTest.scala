package uk.ac.warwick.tabula.data.model.notifications.profiles

import uk.ac.warwick.tabula.data.model.{Member, Notification, StudentRelationship}
import uk.ac.warwick.tabula.profiles.TutorFixture
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class StudentRelationshipChangeNotificationTest extends TestBase with Mockito with FreemarkerRendering with TutorFixture {

	@Test def titleStudent() = withUser("0672089", "cuscav") {
		val notification = Notification.init(new StudentRelationshipChangeToStudentNotification, currentUser.apparentUser, relationship: StudentRelationship)
		notification.title should be ("Personal tutor allocation change")
	}

	@Test def titleOldTutor() = withUser("0672089", "cuscav") {
		val notification = Notification.init(new StudentRelationshipChangeToOldAgentNotification, currentUser.apparentUser, relationship: StudentRelationship)
		notification.title should be ("Change to personal tutees")
	}

	@Test def titleNewTutor() = withUser("0672089", "cuscav") {
		val notification = Notification.init(new StudentRelationshipChangeToNewAgentNotification, currentUser.apparentUser, relationship: StudentRelationship)
		notification.title should be ("Allocation of new personal tutees")
	}

	def createNewTutorNotification(relationship:StudentRelationship, actor:User, oldTutor: Option[Member]): StudentRelationshipChangeToNewAgentNotification = {
		val n = new StudentRelationshipChangeToNewAgentNotification
		n.agent = actor
		n.oldAgentIds.value = Seq(oldTutor.get.universityId)
		n.addItems(Seq(relationship))
		n
	}

	def createOldTutorNotification(relationship:StudentRelationship, actor:User, oldTutor: Option[Member]): StudentRelationshipChangeToOldAgentNotification = {
		val n = new StudentRelationshipChangeToOldAgentNotification
		n.profileService = mockProfileService

		n.agent = actor
		n.oldAgentIds.value = Seq(oldTutor.get.universityId)
		n.addItems(Seq(relationship))
		n
	}

	def createTuteeNotification(relationship:StudentRelationship, actor:User, oldTutor: Option[Member]): StudentRelationshipChangeToStudentNotification = {
		val n = new StudentRelationshipChangeToStudentNotification
		n.profileService = smartMock[ProfileService]
		n.agent = actor
		n.oldAgentIds.value = Seq(oldTutor.get.universityId)
		n.addItems(Seq(relationship))
		n
	}

	@Test
	def urlIsProfilePage():Unit = new TutorFixture {
		val n: StudentRelationshipChangeToNewAgentNotification = createNewTutorNotification(relationship, actor, Some(oldTutor))
		n.url should be(s"/profiles/view/student/${relationship.relationshipType.urlPart}")
		n.urlTitle should be ("view the student profile for Test Student")
	}

	@Test
	def recipientsContainsSingleUser():Unit = new TutorFixture {
		val n: StudentRelationshipChangeToOldAgentNotification = createOldTutorNotification(relationship, actor, Some(oldTutor))
		n.recipients should be (Seq(recipient))
		n.urlTitle should be ("view the student profile for Test Student")
	}

	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new TutorFixture {
		val n: StudentRelationshipChangeToStudentNotification = createTuteeNotification(relationship, actor,  Some(oldTutor))
		n.profileService = mockProfileService
		n.content.template should be ("/WEB-INF/freemarker/notifications/profiles/student_change_relationship_notification.ftl")
		n.content.model("student") should be (Some(student))
		n.content.model("path") should be (s"/profiles/view/student/${relationship.relationshipType.urlPart}")
		n.content.model("newAgents") should be (Seq(newTutor))
		n.urlTitle should be ("view your student profile")
	}

}
