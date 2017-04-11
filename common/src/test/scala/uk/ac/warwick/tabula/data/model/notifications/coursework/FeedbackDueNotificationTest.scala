package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.sql.DataSource

import org.hibernate.{Session, SessionFactory}
import org.joda.time.{DateTime, DateTimeConstants}
import org.junit.{After, Before}
import org.springframework.beans.factory.config.{BeanDefinition, ConfigurableListableBeanFactory}
import org.springframework.context.ConfigurableApplicationContext
import uk.ac.warwick.spring.SpringConfigurer
import uk.ac.warwick.tabula.data.model.{Module, Notification, UserGroup}
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.tabula.roles.ModuleManagerRoleDefinition
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.tabula.JavaImports._

class FeedbackDueNotificationTest extends TestBase with Mockito with FreemarkerRendering {

	val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()
	val extensionService: ExtensionService = smartMock[ExtensionService]
	val userLookup = new MockUserLookup()
	val moduleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
	val permissionsService: PermissionsService = smartMock[PermissionsService]

	@Before def setupAppContext(): Unit = {
		val applicationContext = smartMock[ConfigurableApplicationContext]
		val beanFactory = smartMock[ConfigurableListableBeanFactory]

		applicationContext.getBeanFactory returns beanFactory

		val beanDefinition = smartMock[BeanDefinition]
		beanFactory.getBeanDefinition(any[String]) returns beanDefinition
		beanDefinition.isAutowireCandidate returns true

		applicationContext.getBean("assignmentService") returns smartMock[AssessmentService]
		applicationContext.getBean("assignmentMembershipService") returns smartMock[AssessmentMembershipService]
		applicationContext.getBean("feedbackService") returns smartMock[FeedbackService]
		applicationContext.getBean("extensionService") returns extensionService
		applicationContext.getBean("submissionService") returns smartMock[SubmissionService]
		val sessionFactory = smartMock[SessionFactory]
		val session = smartMock[Session]
		sessionFactory.getCurrentSession returns session
		sessionFactory.openSession() returns session
		applicationContext.getBeansOfType(classOf[SessionFactory]) returns JMap("sessionFactory" -> sessionFactory)
		applicationContext.getBean("dataSource") returns smartMock[DataSource]

		applicationContext.getBean("userLookup") returns userLookup
		applicationContext.getBeansOfType(classOf[UserLookupService]) returns JMap[String, UserLookupService]("userLookup" -> userLookup)

		applicationContext.getBeansOfType(classOf[PermissionsService]) returns JMap("permissionsService" -> permissionsService)
		applicationContext.getBeansOfType(classOf[NotificationService]) returns JMap("notificationService" -> smartMock[NotificationService])
		applicationContext.getBeansOfType(classOf[ModuleAndDepartmentService]) returns JMap("moduleAndDepartmentService" -> moduleAndDepartmentService)

		SpringConfigurer.applicationContext = applicationContext
	}

	@After def removeAppContext(): Unit = {
		SpringConfigurer.applicationContext = null
	}

	@Test def titleDueGeneral() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val notification = Notification.init(new FeedbackDueGeneralNotification, new AnonymousUser, assignment)
		notification.title should be ("CS118: Feedback for \"5,000 word essay\" is due to be published")
	}

	@Test def outputGeneral() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		assignment.extensionService = smartMock[ExtensionService]

		val notification = Notification.init(new FeedbackDueGeneralNotification, new AnonymousUser, assignment)

		val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
		notificationContent should be (
			"""Feedback for CS118 Programming for Computer Scientists 5,000 word essay is due in 21 working days on 14 October 2014."""
		)
	}

	// TAB-3303
	@Test def outputGeneralPlural() = withFakeTime(new DateTime(2015, DateTimeConstants.FEBRUARY, 16, 17, 0, 0, 0)) {
		val assignment = Fixtures.assignment("Implementation and Report")
		assignment.module = Fixtures.module("cs404", "Agent Based Systems")
		assignment.closeDate = new DateTime(2015, DateTimeConstants.JANUARY, 20, 12, 0, 0, 0)

		assignment.extensionService = smartMock[ExtensionService]

		val notification = Notification.init(new FeedbackDueGeneralNotification, new AnonymousUser, assignment)

		val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
		notificationContent should be (
			"""Feedback for CS404 Agent Based Systems Implementation and Report is due in 1 working day on 17 February 2015."""
		)
	}

	@Test def titleDueExtension() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val extension = Fixtures.extension()
		extension.assignment = assignment
		extension._universityId = "1234567"
		extension.usercode = "1234567"
		extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)

		val notification = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension)
		notification.title should be ("CS118: Feedback for 1234567 for \"5,000 word essay\" is due to be published")
	}

	@Test def outputExtension() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 0, 0, 0)
		val submission = Fixtures.submission("1234567","1234567")
		submission.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)
		assignment.submissions.add(submission)
		val extension = Fixtures.extension()
		extension.assignment = assignment
		extension._universityId = "1234567"
		extension.usercode = "1234567"
		extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
		extension.approve()

		val notification = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension)

		val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
		notificationContent should be (
			"""1234567 had an extension until 17 September 2014 for the assignment CS118 Programming for Computer Scientists 5,000 word essay. Their feedback is due in 22 working days on 15 October 2014."""
		)
	}

	// TAB-3303
	@Test def outputExtensionPlural() = withFakeTime(new DateTime(2015, DateTimeConstants.FEBRUARY, 16, 17, 0, 0, 0)) {
		val assignment = Fixtures.assignment("Implementation and Report")
		assignment.module = Fixtures.module("cs404", "Agent Based Systems")
		assignment.closeDate = new DateTime(2015, DateTimeConstants.JANUARY, 20, 12, 0, 0, 0)

		val submission = Fixtures.submission("1234567","1234567")
		submission.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 20, 13, 0, 0, 0)
		assignment.submissions.add(submission)

		val extension = Fixtures.extension()
		extension.assignment = assignment
		extension._universityId = "1234567"
		extension.usercode = "1234567"
		extension.expiryDate = new DateTime(2015, DateTimeConstants.JANUARY, 20, 17, 0, 0, 0)
		extension.approve()

		val notification = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension)

		val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
		notificationContent should be (
			"""1234567 had an extension until 20 January 2015 for the assignment CS404 Agent Based Systems Implementation and Report. Their feedback is due in 1 working day on 17 February 2015."""
		)
	}

	@Test def recipientsExtension() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val submission = Fixtures.submission("1234567", "1234567")
		submission.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
		assignment.submissions.add(submission)

		val extension = Fixtures.extension()
		extension.assignment = assignment
		extension._universityId = "1234567"
		extension.usercode = "1234567"
		extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 18, 9, 0, 0, 0)
		extension.approve()
		extension.feedbackDeadline should not be 'empty

		val managers = UserGroup.ofUsercodes
		userLookup.registerUsers("cuscav", "cusebr")
		managers.addUserId("cuscav")
		managers.addUserId("cusebr")

		moduleAndDepartmentService.getModuleByCode(assignment.module.code) returns Some(assignment.module)
		permissionsService.ensureUserGroupFor[Module](assignment.module, ModuleManagerRoleDefinition) returns managers

		assignment.submissions.add(Fixtures.submission("1234567", "1234567"))

		val notification = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension)
		notification.deadline should not be 'empty
		notification.recipients should not be 'empty
	}

	@Test def recipientsExtensionNoSubmissions() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val extension = Fixtures.extension()
		extension.assignment = assignment
		extension._universityId = "1234567"
		extension.usercode = "1234567"
		extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)

		val notification = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension)
		notification.recipients should be ('empty)
	}

	@Test def recipientsGeneralAlreadyReleased() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val managers = UserGroup.ofUsercodes
		userLookup.registerUsers("cuscav", "cusebr")
		managers.addUserId("cuscav")
		managers.addUserId("cusebr")

		moduleAndDepartmentService.getModuleByCode(assignment.module.code) returns Some(assignment.module)
		permissionsService.ensureUserGroupFor[Module](assignment.module, ModuleManagerRoleDefinition) returns managers

		assignment.submissions.add(Fixtures.submission("0000001", "0000001"))
		assignment.submissions.add(Fixtures.submission("0000002", "0000002"))

		assignment.needsFeedbackPublishing should be {true}

		val notification = Notification.init(new FeedbackDueGeneralNotification, new AnonymousUser, assignment)
		notification.recipients should not be 'empty

		// Feedback added
		assignment.feedbacks.add(Fixtures.assignmentFeedback("0000001", "0000001").tap { f => f.assignment = assignment; f.actualMark = Some(80) })
		assignment.feedbacks.add(Fixtures.assignmentFeedback("0000002", "0000002").tap { f => f.assignment = assignment; f.actualMark = Some(70) })
		notification.recipients should not be 'empty

		// Feedback partially released
		assignment.feedbacks.get(0).released = true
		notification.recipients should not be 'empty

		// Feedback fully released
		assignment.feedbacks.get(1).released = true
		assignment.needsFeedbackPublishing should be {false}
		notification.recipients should be ('empty)
	}

	@Test def recipientsExtensionAlreadyReleased() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 14, 9, 0, 0, 0)

		val submission1 = Fixtures.submission("0000001", "0000001")
		val submission2 = Fixtures.submission("0000002", "0000002")
		submission1.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 0, 0, 0)
		submission2.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 0, 0, 0)
		assignment.submissions.add(submission1)
		assignment.submissions.add(submission2)

		val managers = UserGroup.ofUsercodes
		userLookup.registerUsers("cuscav", "cusebr")
		managers.addUserId("cuscav")
		managers.addUserId("cusebr")

		moduleAndDepartmentService.getModuleByCode(assignment.module.code) returns Some(assignment.module)
		permissionsService.ensureUserGroupFor[Module](assignment.module, ModuleManagerRoleDefinition) returns managers

		val extension = Fixtures.extension()
		extension.assignment = assignment
		extension._universityId = "1234567"
		extension.usercode = "1234567"
		extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
		extension.approve()
		extension.feedbackDeadline should be ('empty)

		val submission3 = Fixtures.submission("1234567", "1234567")
		submission3.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 0, 0, 0)
		assignment.submissions.add(submission3)

		val notification = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension)
		notification.deadline should not be 'empty
		notification.recipients should not be 'empty

		// Feedback added
		assignment.feedbacks.add(Fixtures.assignmentFeedback("1234567", "1234567").tap { f => f.assignment = assignment; f.actualMark = Some(80) })
		notification.recipients should not be 'empty

		// Feedback fully released
		assignment.feedbacks.get(0).released = true
		assignment.needsFeedbackPublishing should be {true} // We still haven't released the others
		notification.recipients should be ('empty)
	}

}
