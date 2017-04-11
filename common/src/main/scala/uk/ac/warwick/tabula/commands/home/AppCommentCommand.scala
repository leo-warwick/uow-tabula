package uk.ac.warwick.tabula.commands.home

import java.util.concurrent.Future

import freemarker.template.{Configuration, Template}
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.UnicodeEmails
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent, RedirectingMailSender}
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.mail.WarwickMailSender

object AppCommentCommand {

	object Recipients {
		val DeptAdmin = "deptAdmin"
		val WebTeam = "webTeam"
	}

	def apply(user: CurrentUser) =
		new AppCommentCommandInternal(user)
			with AutowiringModuleAndDepartmentServiceComponent
			with Command[Future[JBoolean]]
			with AppCommentValidation
			with AppCommentDescription
			with AppCommentCommandState
			with AppCommentCommandRequest
			with ReadOnly with Public {

			var mailSender: RedirectingMailSender = Wire[RedirectingMailSender]("studentMailSender")
			var adminMailAddress: String = Wire.property("${mail.admin.to}")
			var freemarker: Configuration = Wire.auto[Configuration]
			var deptAdminTemplate: Template = freemarker.getTemplate("/WEB-INF/freemarker/emails/appfeedback-deptadmin.ftl")
			var webTeamTemplate: Template = freemarker.getTemplate("/WEB-INF/freemarker/emails/appfeedback.ftl")
		}
}


class AppCommentCommandInternal(val user: CurrentUser) extends CommandInternal[Future[JBoolean]]
	with FreemarkerRendering with UnicodeEmails {

	self: AppCommentCommandRequest with AppCommentCommandState with ModuleAndDepartmentServiceComponent =>

	if (user != null && user.loggedIn) {
		name = user.apparentUser.getFullName
		email = user.apparentUser.getEmail
		usercode = user.apparentUser.getUserId
	}

	override def applyInternal(): Future[JBoolean] = {
		val deptAdmin: Option[User] = {
			Option(user) match {
				case Some(loggedInUser) if loggedInUser.loggedIn =>
					moduleAndDepartmentService.getDepartmentByCode(user.apparentUser.getDepartmentCode).flatMap(_.owners.users.headOption)
				case _ =>
					None
			}
		}
		val mail = createMessage(mailSender) { mail =>
			if (recipient == AppCommentCommand.Recipients.DeptAdmin && deptAdmin.isDefined) {
				mail.setTo(deptAdmin.get.getEmail)
				mail.setFrom(adminMailAddress)
				mail.setSubject(encodeSubject("Tabula feedback"))
				mail.setText(renderToString(deptAdminTemplate, Map(
					"user" -> user,
					"info" -> this
				)))
			} else if (recipient == AppCommentCommand.Recipients.WebTeam) {
				mail.setTo(adminMailAddress)
				mail.setFrom(adminMailAddress)
				mail.setSubject(encodeSubject("Tabula feedback"))
				mail.setText(renderToString(webTeamTemplate, Map(
					"user" -> user,
					"info" -> this
				)))
			} else {
				throw new IllegalArgumentException
			}
		}

		mailSender.send(mail)
	}

}

trait AppCommentValidation extends SelfValidating {

	self: AppCommentCommandRequest =>

	override def validate(errors: Errors) {
		if (!message.hasText) {
			errors.rejectValue("message", "NotEmpty")
		}
		if (!recipient.maybeText.exists(r => r == AppCommentCommand.Recipients.DeptAdmin || r == AppCommentCommand.Recipients.WebTeam)) {
			errors.reject("", "Unknown recipient")
		}
	}

}

trait AppCommentDescription extends Describable[Future[JBoolean]] {

	self: AppCommentCommandRequest =>

	override lazy val eventName = "AppComment"

	override def describe(d: Description) {}

	override def describeResult(d: Description): Unit = d.properties(
		"name" -> name,
		"email" -> email,
		"message" -> message
	)
}

trait AppCommentCommandState {
	def user: CurrentUser
	def mailSender: WarwickMailSender
	def adminMailAddress: String
	def freemarker: Configuration
	def deptAdminTemplate: Template
	def webTeamTemplate: Template
}

trait AppCommentCommandRequest {
	var name: String = _
	var email: String = _
	var usercode: String = _
	var url: String = _
	var message: String = _
	var browser: String = _
	var os: String = _
	var resolution: String = _
	var ipAddress: String = _
	var recipient: String = _
}
