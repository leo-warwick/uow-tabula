package uk.ac.warwick.tabula.web.controllers.cm2

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.RequestAssignmentAccessCommand
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav

import scala.collection.mutable

@Controller
@RequestMapping(value = Array("/coursework/submission/{assignment}/request-access"))
class RequestAssignmentAccessController extends CourseworkController {

  hideDeletedItems

  // clumsy way to prevent a user spamming admins with emails.
  var requestedAccess: mutable.Queue[(String, String)] = mutable.Queue[(String, String)]()

  @ModelAttribute("requestAssignmentAccessCommand")
  def cmd(@PathVariable assignment: Assignment, user: CurrentUser): RequestAssignmentAccessCommand.Command =
    RequestAssignmentAccessCommand(mandatory(assignment), mandatory(user))

  @RequestMapping
  def nope(@PathVariable assignment: Assignment) = Redirect(Routes.assignment(mandatory(assignment)))

  @RequestMapping(method = Array(POST))
  def sendEmail(user: CurrentUser, @ModelAttribute("requestAssignmentAccessCommand") form: RequestAssignmentAccessCommand.Command, @PathVariable assignment: Assignment): Mav = {
    if (!user.loggedIn) {
      nope(assignment)
    } else {
      if (!alreadyEmailed(user, form, assignment)) {
        form.apply()
      }

      Redirect(Routes.assignment(assignment)).addObjects("requestedAccess" -> true)
    }
  }

  // if user+assignment is in the queue, they already sent an email recently so don't resend.
  // queue size is limited to 1000 so eventually they would be able to send again, but not rapidly.
  // They will still be able to send as many times as there are app JVMs (currently 2).
  def alreadyEmailed(user: CurrentUser, form: RequestAssignmentAccessCommand.Command, assignment: Assignment): Boolean = {
    val key = (user.apparentId, assignment.id)
    if (requestedAccess contains key) {
      true
    } else {
      requestedAccess.enqueue(key)
      while (requestedAccess.size > 1000)
        requestedAccess.dequeue()
      false
    }
  }

}
