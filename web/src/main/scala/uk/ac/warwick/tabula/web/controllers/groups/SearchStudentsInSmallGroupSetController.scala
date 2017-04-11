package uk.ac.warwick.tabula.web.controllers.groups

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.SearchStudentsInSmallGroupSetCommand
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.{Member, Module}
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

import scala.collection.JavaConverters._

@RequestMapping(value = Array("/groups/module/{module}/groups/{smallGroupSet}/students/search.json"))
@Controller
class SearchStudentsInSmallGroupSetController extends GroupsController {

	type SearchStudentsInSmallGroupSetCommand = Appliable[Seq[Member]]

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): SearchStudentsInSmallGroupSetCommand =
		SearchStudentsInSmallGroupSetCommand(module, set)

	@RequestMapping def search(@Valid @ModelAttribute("command") cmd: SearchStudentsInSmallGroupSetCommand, errors: Errors): JSONView = {
		if (errors.hasErrors) new JSONErrorView(errors)
		else {
			val json: JList[Map[String, String]] = cmd.apply().map { member =>
				Map(
					"name" -> {member.fullName match {
						case None => "[Unknown user]"
						case Some(name) => name
					}},
					"id" -> member.universityId,
					"userId" -> member.userId,
					"description" -> member.description
				)
			}.asJava

			new JSONView(json)
		}
	}

}
