package uk.ac.warwick.tabula.web.controllers.cm2

import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.{Assignment, Member, RuntimeMember}
import uk.ac.warwick.tabula.web.controllers.BaseController

abstract class CourseworkController extends BaseController with CourseworkBreadcrumbs {
	final def optionalCurrentMember: Option[Member] = user.profile
	final def currentMember: Member = optionalCurrentMember getOrElse new RuntimeMember(user)

	def mustBeCM2(assignment: Assignment): Assignment =
		if (!assignment.cm2Assignment) throw new ItemNotFoundException(assignment, "Tried to edit a CM1 assignment using the CM2 edit pages")
		else assignment
}

/*
 * You reach these endpoints by selecting a bunch of students and posting.
 * Gets should be ignored and successful actions should redirect back to the submission and feedback screen
 */
trait AdminSelectionAction {

	self : BaseController =>

	def RedirectBack(assignment: Assignment) = Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))

	// shouldn't ever be called as a GET - if it is, just redirect back to the submission list
	@RequestMapping
	def get(@PathVariable assignment: Assignment) = RedirectBack(assignment)
}
