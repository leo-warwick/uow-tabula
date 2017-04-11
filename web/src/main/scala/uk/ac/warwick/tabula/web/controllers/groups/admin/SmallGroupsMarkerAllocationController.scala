package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.model.{Assignment, Exam}
import uk.ac.warwick.tabula.commands.groups.admin.{SetAllocation, SmallGroupsMarkerAllocationCommand, SmallGroupsMarkerAllocationCommandInternal, SmallGroupsMarkerAllocationCommandPermissions}
import uk.ac.warwick.tabula.services.{AutowiringAssessmentMembershipServiceComponent, AutowiringSmallGroupServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

@Controller
@RequestMapping(value=Array("/groups/admin/marker-allocation/{assignment}"))
class SmallGroupsMarkerAllocationController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment): SmallGroupsMarkerAllocationCommandInternal with ComposableCommand[Seq[SetAllocation]] with SmallGroupsMarkerAllocationCommandPermissions with AutowiringSmallGroupServiceComponent with AutowiringAssessmentMembershipServiceComponent with Unaudited = {
		SmallGroupsMarkerAllocationCommand(assignment)
	}

	@RequestMapping(method=Array(GET))
	def getGroupMembership(
		@PathVariable assignment:Assignment,
		@ModelAttribute("command") cmd: Appliable[Seq[SetAllocation]]
	): Mav = {

		val allocations = cmd.apply()

		Mav("groups/admin/module/marker-allocation",
			"sets" -> allocations.map(_.set),
			"assessment" -> assignment,
			"allocations" -> allocations,
			"hasSecondMarker" -> assignment.markingWorkflow.hasSecondMarker,
			"firstMarkerRole" -> assignment.markingWorkflow.firstMarkerRoleName,
			"secondMarkerRole" -> assignment.markingWorkflow.secondMarkerRoleName.getOrElse("Second marker")
		).noLayout()
	}
}

@Controller
@RequestMapping(value=Array("/groups/admin/marker-allocation/exam/{exam}"))
class SmallGroupsExamMarkerAllocationController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable exam: Exam): SmallGroupsMarkerAllocationCommandInternal with ComposableCommand[Seq[SetAllocation]] with SmallGroupsMarkerAllocationCommandPermissions with AutowiringSmallGroupServiceComponent with AutowiringAssessmentMembershipServiceComponent with Unaudited = {
		SmallGroupsMarkerAllocationCommand(exam)
	}

	@RequestMapping(method=Array(GET))
	def getGroupMembership(
		@PathVariable exam: Exam,
		@ModelAttribute("command") cmd: Appliable[Seq[SetAllocation]]
	): Mav = {

		val allocations = cmd.apply()

		Mav("groups/admin/module/marker-allocation",
			"sets" -> allocations.map(_.set),
			"assessment" -> exam,
			"allocations" -> allocations,
			"hasSecondMarker" -> exam.markingWorkflow.hasSecondMarker,
			"firstMarkerRole" -> exam.markingWorkflow.firstMarkerRoleName,
			"secondMarkerRole" -> exam.markingWorkflow.secondMarkerRoleName.getOrElse("Second marker")
		).noLayout()
	}
}
