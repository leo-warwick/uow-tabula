package uk.ac.warwick.tabula.web.controllers.groups.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{InitBinder, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.commands.groups.admin._
import uk.ac.warwick.tabula.data.model.Module
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.{AutowiringAssessmentMembershipServiceComponent, AutowiringUserLookupComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

/**
* Controller to populate the user listing for editing, without persistence
*/
@Controller
@RequestMapping(value = Array("/groups/admin/module/{module}/groups/{smallGroupSet}/enrolment"))
class GroupsEnrolmentController extends GroupsController {

	validatesSelf[SelfValidating]
	type EditSmallGroupSetMembershipCommand = Appliable[SmallGroupSet] with ModifiesSmallGroupSetMembership

	@ModelAttribute("command") def formObject(@PathVariable module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): StubEditSmallGroupSetMembershipCommand with AutowiringUserLookupComponent with AutowiringAssessmentMembershipServiceComponent with ComposableCommand[SmallGroupSet] with ModifiesSmallGroupSetMembership with StubEditSmallGroupSetMembershipPermissions with Unaudited with ReadOnly with PopulateStateWithExistingData = {
		val cmd = EditSmallGroupSetMembershipCommand.stub(mandatory(module), mandatory(set))
		cmd.upstreamGroups.clear()
		cmd
	}

	@RequestMapping
	def showForm(@Valid @ModelAttribute("command") form: EditSmallGroupSetMembershipCommand, openDetails: Boolean = false): Mav = {
		form.afterBind()

		Mav("groups/admin/groups/enrolment",
			"department" -> form.module.adminDepartment,
			"module" -> form.module,
			"availableUpstreamGroups" -> form.availableUpstreamGroups,
			"linkedUpstreamAssessmentGroups" -> form.linkedUpstreamAssessmentGroups,
			"assessmentGroups" -> form.assessmentGroups,
			"openDetails" -> openDetails)
			.noLayout()
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}

}
