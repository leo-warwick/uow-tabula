package uk.ac.warwick.tabula.api.web.controllers.groups

import javax.validation.Valid

import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.controllers.groups.SmallGroupSetController.EditSmallGroupSetCommand
import uk.ac.warwick.tabula.api.web.helpers.SmallGroupSetToJsonConverter
import uk.ac.warwick.tabula.commands.groups.admin._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating, ViewViewableCommand}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{ViewGroup, ViewSet}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

import scala.collection.JavaConverters._

object SmallGroupSetController {
	type EditSmallGroupSetCommand = ModifySmallGroupSetCommand.Command
}

abstract class SmallGroupSetController extends ModuleSmallGroupSetsController with GetSmallGroupSetApiFullOutput {
	validatesSelf[SelfValidating]

	def getSmallGroupSetMav(smallGroupSet: SmallGroupSet): Mav = {
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok"
		) ++ outputJson(smallGroupSet)))
	}

}

trait GetSmallGroupSetApiOutput {
	def outputJson(smallGroupSet: SmallGroupSet): Map[String, Any]
}

trait GetSmallGroupSetApiFullOutput extends GetSmallGroupSetApiOutput {
	self: ApiController with SmallGroupSetToJsonConverter =>

	def outputJson(smallGroupSet: SmallGroupSet) = Map(
		"academicYear" -> smallGroupSet.academicYear.toString,
		"groupSet" -> jsonSmallGroupSetObject(new ViewSet(smallGroupSet, ViewGroup.fromGroups(smallGroupSet.groups.asScala.sorted), GroupsViewModel.Tutor))
	)

}

@Controller
@RequestMapping(Array("/v1/module/{module}/groups/{smallGroupSet}"))
class GetSmallGroupSetControllerForApi extends SmallGroupSetController with GetSmallGroupSetApi

trait GetSmallGroupSetApi {
	self: SmallGroupSetController =>

	@ModelAttribute("getCommand")
	def getCommand(@PathVariable module: Module, @PathVariable smallGroupSet: SmallGroupSet): ViewViewableCommand[SmallGroupSet] = {
		mustBeLinked(mandatory(smallGroupSet), mandatory(module))
		new ViewViewableCommand(Permissions.SmallGroups.Read, smallGroupSet)
	}

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def getIt(@Valid @ModelAttribute("getCommand") command: Appliable[SmallGroupSet], errors: Errors, @PathVariable smallGroupSet: SmallGroupSet): Mav = {
		// Return the GET representation
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val result = command.apply()
			getSmallGroupSetMav(result)
		}
	}
}

@Controller
@RequestMapping(Array("/v1/module/{module}/groups/{smallGroupSet}"))
class EditSmallGroupSetControllerForApi extends SmallGroupSetController with EditSmallGroupSetApi

trait EditSmallGroupSetApi {
	self: SmallGroupSetController =>

	@ModelAttribute("editCommand")
	def editCommand(@PathVariable module: Module, @PathVariable smallGroupSet: SmallGroupSet): EditSmallGroupSetCommand =
		ModifySmallGroupSetCommand.edit(module, smallGroupSet)

	@RequestMapping(method = Array(PUT), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
	def edit(@RequestBody request: EditSmallGroupSetRequest, @ModelAttribute("editCommand") command: EditSmallGroupSetCommand, errors: Errors): Mav = {
		request.copyTo(command, errors)
		globalValidator.validate(command, errors)
		command.validate(errors)
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val result = command.apply()
			getSmallGroupSetMav(result)
		}
	}
}

class EditSmallGroupSetRequest extends SmallGroupSetPropertiesRequest[EditSmallGroupSetCommand] {

	// Set Default values to null
	studentsCanSeeTutorName = null
	studentsCanSeeOtherMembers = null
	collectAttendance = null
	allowSelfGroupSwitching = null
}

@Controller
@RequestMapping(Array("/v1/module/{module}/groups/{smallGroupSet}"))
class DeleteSmallGroupSetControllerForApi extends SmallGroupSetController with DeleteSmallGroupSetApi

trait DeleteSmallGroupSetApi {
	self: SmallGroupSetController =>

	@ModelAttribute("deleteCommand")
	def deleteCommand(@PathVariable module: Module, @PathVariable smallGroupSet: SmallGroupSet): DeleteSmallGroupSetCommand = {
		val command = new DeleteSmallGroupSetCommand(module, smallGroupSet)
		command.confirm = true
		command
	}

	@RequestMapping(method = Array(DELETE), produces = Array("application/json"))
	def delete(@Valid @ModelAttribute("deleteCommand") command: DeleteSmallGroupSetCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			command.apply()
			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok"
			)))
		}
	}
}