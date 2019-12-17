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
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent, RedirectingMailSender, UserSettingsService}
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
      var settingsService: UserSettingsService = Wire.auto[UserSettingsService]
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
    val mail = createMessage(mailSender, multipart = false) { mail =>
      if (recipient == AppCommentCommand.Recipients.DeptAdmin) {
        val userEmails = Option(user)
          .filter(_.loggedIn)
          .flatMap(u => moduleAndDepartmentService.getDepartmentByCode(u.apparentUser.getDepartmentCode))
          .map(_.owners.users)
          .getOrElse(throw new IllegalArgumentException)
          .filter(da => settingsService.getByUserId(da.getUserId).exists(_.deptAdminReceiveStudentComments))
          .map(_.getEmail)

        require(userEmails.nonEmpty) // User should not have submitted form if no dept. admins approve

        mail.setTo(userEmails.toArray)
        mail.setFrom(adminMailAddress)
        mail.setSubject(encodeSubject("Tabula help"))
        mail.setText(renderToString(deptAdminTemplate, Map(
          "user" -> user,
          "info" -> this
        )))
      } else if (recipient == AppCommentCommand.Recipients.WebTeam) {
        mail.setTo(adminMailAddress)
        mail.setFrom(adminMailAddress)
        mail.setSubject(encodeSubject("Tabula support"))
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

  override def validate(errors: Errors): Unit = {
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

  override def describe(d: Description): Unit = {}

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

  def settingsService: UserSettingsService
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
