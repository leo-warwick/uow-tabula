package uk.ac.warwick.tabula.commands.coursework.feedback
import javax.mail.internet.InternetAddress
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.helpers.NoUser
import uk.ac.warwick.userlookup.User
import javax.mail.MessagingException
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.FeedbackService

abstract class RecipientReportItem(val universityId: String, val user: User, val good: Boolean)
case class MissingUser(id: String) extends RecipientReportItem(id, null, false)
case class BadEmail(u: User) extends RecipientReportItem(u.getWarwickId, u, false)
case class GoodUser(u: User) extends RecipientReportItem(u.getWarwickId, u, true)

case class RecipientCheckReport(users: List[RecipientReportItem]) {
	def hasProblems: Boolean = users.exists(!_.good)
	def problems: List[RecipientReportItem] = users.filter(!_.good)
}

/**
 * A standalone command to go through all the feedback for an assignment, looking up
 * all the students and reporting back on whether it looks like they have a working
 * email address. Used to show to the admin user which users may not receive an email
 * when feedback is published.
 */
class FeedbackRecipientCheckCommand(val module: Module, val assignment: Assignment) extends Command[RecipientCheckReport] with Unaudited with ReadOnly {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.AssignmentFeedback.Read, assignment)

	var feedbackService: FeedbackService = Wire.auto[FeedbackService]

	override def applyInternal(): RecipientCheckReport = {
		val items: Seq[RecipientReportItem] =
			for ((usercode, user) <- feedbackService.getUsersForFeedback(assignment))
				yield resolve(usercode, user)
		RecipientCheckReport(items.toList)
	}

	def resolve(usercode: String, user: User): RecipientReportItem = user match {
		case FoundUser(u) =>
			if (u.getEmail.hasText && isGoodEmail(u.getEmail)) {
				GoodUser(u)
			} else {
				BadEmail(u)
			}
		case NoUser(u) => MissingUser(usercode)
	}

	def isGoodEmail(email: String): Boolean = {
		try {
			new InternetAddress(email).validate()
			true
		} catch {
			case e: MessagingException => false
		}
	}
}