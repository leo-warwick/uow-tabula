package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.commands.SelfValidating
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.commands.sysadmin.ListStudentRelationshipTypesCommand
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.commands.sysadmin.AddStudentRelationshipTypeCommand
import org.springframework.validation.Errors
import javax.validation.Valid

import uk.ac.warwick.tabula.commands.sysadmin.EditStudentRelationshipTypeCommand
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.commands.sysadmin.DeleteStudentRelationshipTypeCommand
import org.springframework.web.bind.{WebDataBinder, annotation}
import uk.ac.warwick.tabula.data.model.StudentRelationshipSource
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor

trait StudentRelationshipTypeController extends BaseSysadminController {
	validatesSelf[SelfValidating]

	override final def binding[A](binder: WebDataBinder, cmd: A) {
		binder.registerCustomEditor(classOf[StudentRelationshipSource], new AbstractPropertyEditor[StudentRelationshipSource] {
			override def fromString(code: String): StudentRelationshipSource = StudentRelationshipSource.fromCode(code)
			override def toString(source: StudentRelationshipSource): String = source.dbValue
		})
	}
}

@Controller
@RequestMapping(value = Array("/sysadmin/relationships"))
class ListStudentRelationshipTypesController extends StudentRelationshipTypeController {
	@ModelAttribute("listStudentRelationshipTypesCommand")
		def listStudentRelationshipTypesCommand = ListStudentRelationshipTypesCommand()

	@annotation.RequestMapping
	def list(@ModelAttribute("listStudentRelationshipTypesCommand") cmd: Appliable[Seq[StudentRelationshipType]]) =
		Mav("sysadmin/relationships/list",
			"relationshipTypes" -> cmd.apply()
		)
}

@Controller
@RequestMapping(value = Array("/sysadmin/relationships/add"))
class AddStudentRelationshipTypeController extends StudentRelationshipTypeController {
	@ModelAttribute("addStudentRelationshipTypeCommand")
		def addStudentRelationshipTypeCommand() = AddStudentRelationshipTypeCommand()

	@annotation.RequestMapping(method=Array(GET, HEAD))
	def form(): Mav = Mav("sysadmin/relationships/add").crumbs(
		SysadminBreadcrumbs.Relationships.Home
	)

	@annotation.RequestMapping(method=Array(POST))
	def add(@Valid @ModelAttribute("addStudentRelationshipTypeCommand") cmd: Appliable[StudentRelationshipType], errors: Errors): Mav =
		if (errors.hasErrors){
			form()
		} else {
			cmd.apply()
			Redirect("/sysadmin/relationships")
		}
}

@Controller
@RequestMapping(value = Array("/sysadmin/relationships/{relationshipType}/edit"))
class EditStudentRelationshipTypeController extends StudentRelationshipTypeController {
	@ModelAttribute("editStudentRelationshipTypeCommand")
		def editStudentRelationshipTypeCommand(@PathVariable relationshipType: StudentRelationshipType) =
			EditStudentRelationshipTypeCommand(relationshipType)

	@annotation.RequestMapping(method=Array(GET, HEAD))
	def form(): Mav = Mav("sysadmin/relationships/edit").crumbs(
		SysadminBreadcrumbs.Relationships.Home
	)

	@annotation.RequestMapping(method=Array(POST))
	def edit(@Valid @ModelAttribute("editStudentRelationshipTypeCommand") cmd: Appliable[StudentRelationshipType], errors: Errors): Mav =
		if (errors.hasErrors){
			form()
		} else {
			cmd.apply()
			Redirect("/sysadmin/relationships")
		}
}

@Controller
@RequestMapping(value = Array("/sysadmin/relationships/{relationshipType}/delete"))
class DeleteStudentRelationshipTypeController extends StudentRelationshipTypeController {
	@ModelAttribute("deleteStudentRelationshipTypeCommand")
		def deleteStudentRelationshipTypeCommand(@PathVariable relationshipType: StudentRelationshipType) =
			DeleteStudentRelationshipTypeCommand(relationshipType)

	@annotation.RequestMapping(method=Array(GET, HEAD))
	def form(): Mav = Mav("sysadmin/relationships/delete").crumbs(
		SysadminBreadcrumbs.Relationships.Home
	)

	@annotation.RequestMapping(method=Array(POST))
	def delete(@Valid @ModelAttribute("deleteStudentRelationshipTypeCommand") cmd: Appliable[StudentRelationshipType], errors: Errors): Mav =
		if (errors.hasErrors){
			form()
		} else {
			cmd.apply()
			Redirect("/sysadmin/relationships")
		}
}