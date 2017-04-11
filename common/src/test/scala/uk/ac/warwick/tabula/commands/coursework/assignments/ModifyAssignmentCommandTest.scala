package uk.ac.warwick.tabula.commands.coursework.assignments

import javax.sql.DataSource

import org.hamcrest.Matchers._
import org.hibernate.{Session, SessionFactory}
import org.joda.time.{DateTime, DateTimeConstants}
import org.junit.{After, Before}
import org.springframework.validation.BindException
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.CurrentSITSAcademicYear
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.forms.ExtensionState.Unreviewed
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.events.EventListener
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.userlookup.{AnonymousUser, User}
import uk.ac.warwick.util.queue.Queue

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

// scalastyle:off magic.number
class ModifyAssignmentCommandTest extends TestBase with Mockito with FunctionalContextTesting {

	import ModifyAssignmentCommandTest.MinimalCommandContext

	var userDatabase: Seq[User] = Seq(
		("0000000", "aaslat", "aaaaa"),
		("0000001", "baslat", "aaaab")
	) map { case(warwickId, userId, code) =>
		val user = new User(code)
		user.setWarwickId(warwickId)
		user.setUserId(userId)
		user.setFoundUser(true)
		user.setFullName("Roger " + code.head.toUpper + code.tail)
		user
	}

	var userLookup: UserLookupService = smartMock[UserLookupService]
	userLookup.getUsersByUserIds(any[JList[String]]) answers { _ match { case ids: JList[String @unchecked] =>
		val users = ids.asScala.map(id=>(id,new User(id)))
		JHashMap(users:_*)
	}}

	userLookup.getUserByUserId(any[String]) answers { id =>
		userDatabase find {_.getUserId == id} getOrElse new AnonymousUser
	}
	userLookup.getUserByWarwickUniId(any[String]) answers { id =>
		userDatabase find {_.getWarwickId == id} getOrElse new AnonymousUser
	}

	class MockNotificationService extends NotificationService {
		var notifications: ListBuffer[Notification[_,_]] = new ListBuffer[Notification[_,_]]()

		override def push(n: Notification[_,_]) {
			notifications += n
		}
	}

	trait Fixture extends CurrentSITSAcademicYear {
		val user = new User("cusxad")
		val currentUser = new CurrentUser(user, user)
		val module: Module = Fixtures.module(code="ls101")
		val assignment: Assignment = Fixtures.assignment("test")
		assignment.academicYear = academicYear
		assignment.closeDate = DateTime.now.plusDays(30)
		assignment.members = UserGroup.ofUsercodes
		val extension1 = new Extension
		val extension2 = new Extension
		val sometime: DateTime = new DateTime().minusWeeks(1)

		extension1._universityId = "1234567"
		extension1.usercode = "custard"
		extension1.requestedOn = sometime
		extension1.requestedExpiryDate = sometime.plusWeeks(8)
		extension1.reason = "Truculence."
		extension1.assignment = assignment

		extension2._universityId = "7654321"
		extension2.usercode = "swotty"
		extension2.requestedOn = sometime
		extension2.requestedExpiryDate = sometime.plusWeeks(8)
		extension2.assignment = assignment
		extension2.approve()
		extension2.reviewedOn = sometime.plusDays(5)

		assignment.allowExtensions = true
		assignment.extensions.add(extension1)
		assignment.extensions.add(extension2)
		assignment.module = module

		module.assignments.add(assignment)

		val mockAssignmentService: AssessmentService = smartMock[AssessmentService]
		assignment.assignmentService = mockAssignmentService
		mockAssignmentService.getAssignmentByNameYearModule(assignment.name, assignment.academicYear, assignment.module) returns Seq(assignment)

		val mockAssignmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]
		assignment.assessmentMembershipService = mockAssignmentMembershipService
		mockAssignmentMembershipService.determineMembershipUsers(any[Seq[UpstreamAssessmentGroup]], any[Option[UnspecifiedTypeUserGroup]]) returns Seq()

		val mockExtensionService: ExtensionService = smartMock[ExtensionService]
		assignment.extensionService = mockExtensionService
		mockExtensionService.countUnapprovedExtensions(assignment) returns 1
		mockExtensionService.getUnapprovedExtensions(assignment) returns Seq(extension1)
	}

	@Test def validateNullDates() { new Fixture {
		val cmd = new AddAssignmentCommand(module)
		var errors = new BindException(cmd, "command")
		cmd.openEnded = false

		// Both null two errors
		cmd.openDate = null
		cmd.closeDate = null
		cmd.validate(errors)
		errors.getErrorCount should be (2)

		// open null one error
		errors = new BindException(cmd, "command")
		cmd.openDate = null
		cmd.closeDate = new DateTime(2012, DateTimeConstants.JANUARY, 10, 0, 0)
		cmd.validate(errors)
		errors.getErrorCount should be (1)

		// Close null one error
		errors = new BindException(cmd, "command")
		cmd.openDate = new DateTime(2012, DateTimeConstants.JANUARY, 10, 0, 0)
		cmd.closeDate = null
		cmd.validate(errors)
		errors.getErrorCount should be (1)

		// But if we're open ended then no error
		errors = new BindException(cmd, "command")
		cmd.openEnded = true
		cmd.validate(errors)
		errors.getErrorCount should be (0)

	}}

	@Test def validateCloseDate() { new Fixture {
		val cmd = new AddAssignmentCommand(module)
		val errors = new BindException(cmd, "command")

		// No error, close date after open date
		cmd.openDate = new DateTime(2012, DateTimeConstants.JANUARY, 10, 0, 0)
		cmd.closeDate = cmd.openDate.plusDays(1)
		cmd.validate(errors)
		errors.getErrorCount should be (0)

		// Close date is before open date; but open ended so still no error
		cmd.closeDate = cmd.openDate.minusDays(1)
		cmd.openEnded = true
		cmd.validate(errors)
		errors.getErrorCount should be (0)

		// But if we're not open ended
		cmd.openEnded = false
		cmd.validate(errors)
		errors.getErrorCount should be (1)
		withClue("correct error code") { errors.getGlobalError.getCode should be ("closeDate.early") }
	}}

	@Test def validateName() { new Fixture {
		// TAB-1263
		val cmd = new AddAssignmentCommand(module)
		cmd.service = mockAssignmentService
		cmd.service.getAssignmentByNameYearModule("New assignment", academicYear, module) returns Seq()

		var errors = new BindException(cmd, "command")

		// No error, different name
		cmd.name = "New assignment"
		cmd.validate(errors)
		errors.getErrorCount should be (0)

		// Error, existing name
		cmd.name = "test"
		cmd.validate(errors)
		errors.getErrorCount should not be 0
		withClue("correct error code") { errors.getFieldErrors("name").asScala.map(_.getCode).head should be ("name.duplicate.assignment") }

		// Archive existing, should stop error
		module.assignments.get(0).archive()
		errors = new BindException(cmd, "command")

		cmd.name = "test"
		cmd.validate(errors)
		errors.getErrorCount should be (0)
	}}

	@Before
	def before() {
		Wire.ignoreMissingBeans = true
	}

	@After
	def afterTheFeast() {
		Wire.ignoreMissingBeans = false
	}

	@Test def includeAndExcludeUsers(): Unit = inContext[MinimalCommandContext] { new Fixture {
		val cmd = new EditAssignmentCommand(module, assignment, currentUser)
		cmd.service = mockAssignmentService
		cmd.assessmentMembershipService = mockAssignmentMembershipService
		cmd.userLookup = userLookup

		def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
			case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
			case ug: UserGroup => ug.userLookup = userLookup
		}

		wireUserLookup(cmd.members)

		// have one user, add a new one and check re-add does nothing
		cmd.members.add(new User("aaslat"))
		cmd.includeUsers = JList("baslat", "aaslat")
		cmd.afterBind()
		cmd.members.users.size should be(2)
		cmd.members.excludes.size should be(0)

		// remove one
		cmd.excludeUsers = JList("aaslat")
		cmd.afterBind()
		cmd.members.users.size should be(1)
		cmd.members.excludes.size should be(0)

		// now exclude (blacklist) it
		cmd.excludeUsers = JList("aaslat")
		cmd.afterBind()
		cmd.members.users.size should be(1)
		cmd.members.excludes.size should be(1)

		// now unexclude it
		cmd.includeUsers = JList("aaslat")
		cmd.afterBind()
		cmd.members.users.size should be(1)
		cmd.members.excludes.size should be(0)
	}}

	@Test def purgeExtensionRequests(): Unit = inContext[MinimalCommandContext] { new Fixture {
		assignment.extensions.size should be (2)
		assignment.countUnapprovedExtensions should be (1)

		val cmd = new EditAssignmentCommand(module, assignment, currentUser)

		cmd.service = mockAssignmentService
		cmd.assessmentMembershipService = mockAssignmentMembershipService
		cmd.maintenanceModeService = smartMock[MaintenanceModeService]
		cmd.listener = smartMock[EventListener]
		cmd.scheduledNotificationService = smartMock[ScheduledNotificationService]

		cmd.allowExtensions = false
		cmd.notificationService = new MockNotificationService()
		val ns: MockNotificationService = cmd.notificationService.asInstanceOf[MockNotificationService]
		val ass: Assignment = cmd.apply()
		if (extension1.state != Unreviewed) mockExtensionService.countUnapprovedExtensions(assignment) returns 0
		ass.countUnapprovedExtensions should be (0)
		ns.notifications.size should be (2)
		ns.notifications.map(_.verb).toSeq.sorted should be (Seq("reject", "respond").sorted)
	}}

}

object ModifyAssignmentCommandTest {
	class MinimalCommandContext extends FunctionalContext with Mockito {
		bean(){
			val sessionFactory = smartMock[SessionFactory]
			val session = smartMock[Session]
			sessionFactory.getCurrentSession returns session
			sessionFactory.openSession() returns session
			sessionFactory
		}
		bean("dataSource"){mock[DataSource]}
		bean(){mock[TriggerService]}
	}
}