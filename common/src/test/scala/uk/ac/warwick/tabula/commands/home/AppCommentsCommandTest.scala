package uk.ac.warwick.tabula.commands.home

import java.util.Properties
import javax.mail.Message.RecipientType
import javax.mail.Session
import javax.mail.internet.{MimeMessage, MimeMultipart}

import freemarker.template.Template
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{Department, UserGroup}
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.web.views.ScalaFreemarkerConfiguration
import uk.ac.warwick.tabula.{MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.mail.WarwickMailSender

class AppCommentsCommandTest extends TestBase with Mockito {

	val mockMailSender: WarwickMailSender = smartMock[WarwickMailSender]
	val mockModuleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
	val mockUserLookup = new MockUserLookup()

	val session: Session = Session.getDefaultInstance(new Properties)
	val mimeMessage = new MimeMessage(session)
	mockMailSender.createMimeMessage() returns mimeMessage

	val adminEmail = "stabula@warwick.ac.uk"

	val owner = new User("owner")
	owner.setEmail("owner@warwick.ac.uk")
	mockUserLookup.registerUserObjects(owner)
	val dept = new Department {
		code = "its"
		override lazy val owners: UserGroup = UserGroup.ofUsercodes
		owners.userLookup = mockUserLookup
	}
	dept.owners.add(owner)

	mockModuleAndDepartmentService.getDepartmentByCode(dept.code) returns Option(dept)

	trait Fixture {
		val cmd = new AppCommentCommandInternal(currentUser) with AppCommentCommandRequest
			with AppCommentCommandState with ModuleAndDepartmentServiceComponent {

			override val mailSender: WarwickMailSender = mockMailSender
			override val freemarker: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()
			override val moduleAndDepartmentService: ModuleAndDepartmentService = mockModuleAndDepartmentService
			override val adminMailAddress: String = adminEmail
			override val deptAdminTemplate: Template = freemarker.getTemplate("/WEB-INF/freemarker/emails/appfeedback-deptadmin.ftl")
			override val webTeamTemplate: Template = freemarker.getTemplate("/WEB-INF/freemarker/emails/appfeedback.ftl")
		}

		val validator = new AppCommentValidation with AppCommentCommandRequest
	}

	@Test
	def populateFromNoUser() { new Fixture {
		cmd.usercode should be (null)
		cmd.name should be (null)
		cmd.email should be (null)
	}}

	@Test
	def populateWithUser() = withUser("cuscav") {
		currentUser.apparentUser.setFullName("Billy Bob")
		currentUser.apparentUser.setEmail("billybob@warwick.ac.uk")
		new Fixture {
			cmd.usercode should not be 'empty
			cmd.name should not be 'empty
			cmd.email should not be 'empty
		}
	}

	@Test
	def validatePasses() { new Fixture {
		validator.message = "I'm coming for you"
		validator.recipient = AppCommentCommand.Recipients.WebTeam

		val errors = new BindException(validator, "command")
		validator.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test
	def validateNoMessage() { new Fixture {
		validator.message = "   "
		validator.recipient = AppCommentCommand.Recipients.WebTeam

		val errors = new BindException(validator, "command")
		validator.validate(errors)

		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("message")
		errors.getFieldError.getCode should be ("NotEmpty")
	}}

	@Test
	def sendToDeptAdminWithNothing() { new Fixture {
		// As they aren't signed in, this should throw (checked by validation)
		cmd.message = "I'm coming for you"
		cmd.recipient = AppCommentCommand.Recipients.DeptAdmin

		intercept[IllegalArgumentException] {
			cmd.applyInternal()
		}
	}}

	@Test
	def sendToDeptAdminFullyPopulated() { withUser("cuscav") {
		currentUser.apparentUser.setFullName("Billy Bob")
		currentUser.apparentUser.setEmail("billybob@warwick.ac.uk")
		currentUser.apparentUser.setDepartmentCode(dept.code)

		new Fixture {
			cmd.message = "I'm coming for you"
			cmd.url = "http://stabula.warwick.ac.uk/my/page"
			cmd.browser = "Chrome"
			cmd.ipAddress = "137.205.194.132"
			cmd.os = "Window"
			cmd.resolution = "New years"
			cmd.recipient = AppCommentCommand.Recipients.DeptAdmin

			cmd.applyInternal()
			verify(mockMailSender, times(1)).send(mimeMessage)

			mimeMessage.getRecipients(RecipientType.TO).map {_.toString} should be (Array(owner.getEmail))
			mimeMessage.getFrom.map {_.toString} should be (Array(adminEmail))
			mimeMessage.getSubject should be ("Tabula feedback")

			// Check properties have been set
			val text: String = mimeMessage.getContent match {
				case string: String => string
				case multipart: MimeMultipart => multipart.getBodyPart(0).getContent.toString
			}

			text should include ("I'm coming for you")
			text should include ("Name: Billy Bob")
			text should include ("Email: billybob@warwick.ac.uk")
			text should include ("Usercode: cuscav")
		}
	}}

	@Test
	def sendToWebTeamWithNothing() { new Fixture {
		// Only message is required, so this should work even if the user doesn't fill anything out
		cmd.message = "I'm coming for you"
		cmd.recipient = AppCommentCommand.Recipients.WebTeam

		cmd.applyInternal()
		verify(mockMailSender, times(1)).send(mimeMessage)

		mimeMessage.getRecipients(RecipientType.TO).map {_.toString} should be (Array(adminEmail))
		mimeMessage.getFrom.map(_.toString) should be (Array(adminEmail))
		mimeMessage.getSubject should be ("Tabula feedback")

		// Check properties have been set
		val text: String = mimeMessage.getContent match {
			case string: String => string
			case multipart: MimeMultipart => multipart.getBodyPart(0).getContent.toString
		}

		text should include ("I'm coming for you")
		text should include ("Name: Not provided")
	}}

	@Test
	def sendToWebTeamFullyPopulated() { withUser("cuscav") {
		currentUser.apparentUser.setFullName("Billy Bob")
		currentUser.apparentUser.setEmail("billybob@warwick.ac.uk")
		currentUser.apparentUser.setDepartmentCode(dept.code)

		new Fixture {
			cmd.message = "I'm coming for you"
			cmd.url = "http://stabula.warwick.ac.uk/my/page"
			cmd.browser = "Chrome"
			cmd.ipAddress = "137.205.194.132"
			cmd.os = "Window"
			cmd.resolution = "New years"
			cmd.recipient = AppCommentCommand.Recipients.WebTeam

			cmd.applyInternal()
			verify(mockMailSender, times(1)).send(mimeMessage)

			mimeMessage.getRecipients(RecipientType.TO).map {_.toString} should be (Array(adminEmail))
			mimeMessage.getFrom.map {_.toString} should be (Array(adminEmail))
			mimeMessage.getSubject should be ("Tabula feedback")

			// Check properties have been set
			val text: String = mimeMessage.getContent match {
				case string: String => string
				case multipart: MimeMultipart => multipart.getBodyPart(0).getContent.toString
			}

			text should include ("I'm coming for you")
			text should include ("Name: Billy Bob")
			text should include ("Email: billybob@warwick.ac.uk")
			text should include ("Usercode: cuscav")
			text should include ("Current page: http://stabula.warwick.ac.uk/my/page")
			text should include ("Browser: Chrome")
			text should include ("OS: Window")
			text should include ("Screen resolution: New years")
			text should include ("IP address: 137.205.194.132")
		}
	}}

}