package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model.{Assignment, Notification}
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{Fixtures, TestBase}
import uk.ac.warwick.userlookup.User

class RequestAssignmentAccessNotificationTest extends TestBase with FreemarkerRendering {

	private trait Fixture {
		val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration

		val assignment: Assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
	}

	@Test def title(): Unit = new Fixture {
		val notification: RequestAssignmentAccessNotification = Notification.init(new RequestAssignmentAccessNotification, new User("cuscav"), assignment)
		notification.title should be ("CS118: Access requested for \"5,000 word essay\"")
	}

	@Test def student(): Unit = new Fixture {
		val user = new User("studentId")
		user.setFoundUser(true)
		user.setWarwickId("1234567")
		user.setStudent(true)
		user.setUserType("Student")
		user.setFullName("Student Full Name")
		user.setDepartment("Warwick Business School")
		user.setEmail("student@wbs.ac.uk")

		val notification: RequestAssignmentAccessNotification = Notification.init(new RequestAssignmentAccessNotification, user, assignment)

		val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
		notificationContent should be (
			"""Student Full Name (1234567), who is a student in Warwick Business School, has requested access to the assignment "5,000 word essay" for CS118.
				|
				|If you agree that they should be able to submit to this assignment, you should enrol them on the assignment via the assignment properties.
				|
				|You can contact the student via email at student@wbs.ac.uk.""".stripMargin
		)
	}

	@Test def staff(): Unit = new Fixture {
		val user = new User("staffId")
		user.setFoundUser(true)
		user.setWarwickId("1234567")
		user.setStaff(true)
		user.setUserType("Staff")
		user.setFullName("Staff Full Name")
		user.setDepartment("Warwick Business School")
		user.setEmail("staff@wbs.ac.uk")

		val notification: RequestAssignmentAccessNotification = Notification.init(new RequestAssignmentAccessNotification, user, assignment)

		val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
		notificationContent should be (
			"""Staff Full Name (1234567), who is a member of staff in Warwick Business School, has requested access to the assignment "5,000 word essay" for CS118.
				|
				|If you agree that they should be able to submit to this assignment, you should enrol them on the assignment via the assignment properties.
				|
				|You can contact the member of staff via email at staff@wbs.ac.uk.""".stripMargin
		)
	}

	@Test def external(): Unit = new Fixture {
		val user = new User("externalId")
		user.setFoundUser(true)
		user.setUserType("External")
		user.setFullName("External Full Name")
		user.setEmail("external@wbs.ac.uk")

		val notification: RequestAssignmentAccessNotification = Notification.init(new RequestAssignmentAccessNotification, user, assignment)

		val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
		notificationContent should be (
			"""External Full Name, who is an external user, has requested access to the assignment "5,000 word essay" for CS118.
				|
				|If you agree that they should be able to submit to this assignment, you should enrol them on the assignment via the assignment properties.
				|
				|You can contact the external user via email at external@wbs.ac.uk.""".stripMargin
		)
	}

}
