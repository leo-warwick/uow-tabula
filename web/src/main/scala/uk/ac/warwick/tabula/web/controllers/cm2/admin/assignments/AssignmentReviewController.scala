package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.SharedAssignmentPropertiesForm
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.{CourseworkBreadcrumbs, CourseworkController}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/new/{assignment}/review"))
class AssignmentReviewController extends CourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment): ViewViewableCommand[Assignment] = {
		new ViewViewableCommand(Permissions.Assignment.Read, mandatory(assignment))
	}


	@RequestMapping(method = Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[Assignment]): Mav = {
		val assignment = cmd.apply()
		val sharedPropertiesForm = new SharedAssignmentPropertiesForm
		sharedPropertiesForm.copySharedFrom(assignment)
		Mav(s"$urlPrefix/admin/assignments/assignment_review_details",
			"module" -> assignment.module,
			"assignment" -> assignment,
			"membershipInfo" -> assignment.membershipInfo,
			"sharedPropertiesForm" -> sharedPropertiesForm
		).crumbs(CourseworkBreadcrumbs.Assignment.AssignmentManagement())
	}

}

