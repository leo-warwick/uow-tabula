package uk.ac.warwick.tabula.data.model.notifications.profiles

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.NewMeetingRecordApprovalNotification
import uk.ac.warwick.tabula.data.model.{MeetingRecord, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget, ScopelessPermission}
import uk.ac.warwick.tabula.services.{ProfileService, SecurityService}
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaBeansWrapper, ScalaFreemarkerConfiguration, UrlMethodModel}
import uk.ac.warwick.tabula.{CurrentUser, Fixtures, Mockito, TestBase}

class MeetingRecordApprovalNotificationTest extends TestBase with Mockito with FreemarkerRendering {

	val securityService: SecurityService = mock[SecurityService]
	securityService.can(any[CurrentUser], any[ScopelessPermission]) returns true
	securityService.can(any[CurrentUser], any[Permission], any[PermissionsTarget]) returns true

	val profileService: ProfileService = mock[ProfileService]

	val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()
	freeMarkerConfig.getObjectWrapper.asInstanceOf[ScalaBeansWrapper].securityService = securityService

	val urlMethodModel = new UrlMethodModel
	//urlMethodModel.context = "/profiles"
	urlMethodModel.toplevelUrl = "https://tabula.warwick.ac.uk"

	freeMarkerConfig.setSharedVariable("url", urlMethodModel)

	@Test def itWorks() {
		val department = Fixtures.department("es", "Engineering")

		val agent = Fixtures.staff("1234567", "estaff", department)
		agent.firstName = "Karen"
		agent.lastName = "Bradbury"

		val student = Fixtures.student("1218503", "esustu", department)
		student.mostSignificantCourse = Fixtures.studentCourseDetails(student, department)
		student.mostSignificantCourse.sprCode = "1218503/1"

		profileService.getStudentBySprCode("1218503/1") returns Some(student)

		withUser(agent.userId, agent.universityId) {
			val meeting = new MeetingRecord
			meeting.id = "fd269caf-c739-4a44-8f3e-27e79110c73d"
			meeting.creator = agent
			meeting.title = "End of term progress meeting"
			meeting.meetingDate = new DateTime(2013, DateTimeConstants.DECEMBER, 5, 12, 0, 0, 0)

			val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
			meeting.relationship = StudentRelationship(agent, relationshipType, student, DateTime.now)

			val notification = new NewMeetingRecordApprovalNotification
			notification.agent = agent.asSsoUser
			notification.addItems(Seq(meeting))

			val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
			notificationContent should be (
				"""Karen Bradbury has created a record of your personal tutor meeting:

End of term progress meeting at 5 December 2013
""")
		}
	}

}