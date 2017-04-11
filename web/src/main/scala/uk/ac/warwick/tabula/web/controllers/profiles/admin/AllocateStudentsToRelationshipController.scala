package uk.ac.warwick.tabula.web.controllers.profiles.admin

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.profiles.relationships._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating, StudentAssociationResult}
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.web.views.{ExcelView, JSONView}

@Controller
@RequestMapping(value=Array("/profiles/department/{department}/{relationshipType}/allocate"))
class AllocateStudentsToRelationshipController extends ProfilesController {

	@ModelAttribute("activeDepartment")
	def activeDepartment(@PathVariable department: Department): Department = department

	@ModelAttribute("commandActions")
	def commandActions = FetchDepartmentRelationshipInformationCommand.Actions

	@ModelAttribute("allocationTypes")
	def allocationTypes = ExtractRelationshipsFromFileCommand.AllocationTypes

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable relationshipType: StudentRelationshipType) =
		FetchDepartmentRelationshipInformationCommand(mandatory(department), mandatory(relationshipType))

	@ModelAttribute("uploadCommand")
	def uploadCommand(@PathVariable department: Department, @PathVariable relationshipType: StudentRelationshipType) =
		ExtractRelationshipsFromFileCommand(mandatory(department), mandatory(relationshipType))

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[StudentAssociationResult], @PathVariable department: Department, @PathVariable relationshipType: StudentRelationshipType): Mav = {
		val results = cmd.apply()
		if (ajax) {
			Mav(new JSONView(
				Map("unallocated" -> results.unallocated.map(studentData => Map(
					"firstName" -> studentData.firstName,
					"lastName" -> studentData.lastName,
					"universityId" -> studentData.universityId
				)))
			)).noLayout()
		} else {
			Mav("profiles/relationships/allocate",
				"unallocated" -> results.unallocated,
				"allocated" -> results.allocated
			)
		}
	}

}

@Controller
@RequestMapping(value=Array("/profiles/department/{department}/{relationshipType}/allocate/template"))
class AllocateStudentsToRelationshipTemplateController extends ProfilesController {

	@ModelAttribute("templateCommand")
	def templateCommand(@PathVariable department: Department, @PathVariable relationshipType: StudentRelationshipType) =
		StudentRelationshipTemplateCommand(mandatory(department), mandatory(relationshipType))

	@RequestMapping
	def template(@ModelAttribute("templateCommand") cmd: Appliable[ExcelView]): ExcelView = cmd.apply()

}

@Controller
@RequestMapping(value=Array("/profiles/department/{department}/{relationshipType}/allocate/upload"))
class AllocateStudentsToRelationshipUploadController extends ProfilesController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable relationshipType: StudentRelationshipType) =
		ExtractRelationshipsFromFileCommand(mandatory(department), mandatory(relationshipType))

	@ModelAttribute("templateCommand")
	def templateCommand(@PathVariable department: Department, @PathVariable relationshipType: StudentRelationshipType) =
		StudentRelationshipTemplateCommand(mandatory(department), mandatory(relationshipType))

	@ModelAttribute("allocationTypes")
	def allocationTypes = ExtractRelationshipsFromFileCommand.AllocationTypes

	@RequestMapping(method = Array(POST))
	def home(
		@ModelAttribute("command") cmd: Appliable[Seq[ExtractRelationshipsFromFileCommandRow]],
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable relationshipType: StudentRelationshipType
	): Mav = {
		if (errors.hasErrors) {
			Mav("profiles/relationships/allocate_upload")
		} else {
			val result = cmd.apply()
			val validRows = result.filterNot(_.error.hasText).sortBy(_.studentId)
			val invalidRows = result.filter(_.error.hasText).sortBy(_.studentId)
			Mav("profiles/relationships/allocate_upload",
				"validRows" -> validRows,
				"invalidRows" -> invalidRows
			)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("templateWithChanges"))
	def template(@ModelAttribute("templateCommand") cmd: Appliable[ExcelView]): ExcelView = cmd.apply()

}

@Controller
@RequestMapping(value=Array("/profiles/department/{department}/{relationshipType}/allocate/preview"))
class AllocateStudentsToRelationshipPreviewController extends ProfilesController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable relationshipType: StudentRelationshipType) =
		AllocateStudentsToRelationshipCommand(mandatory(department), mandatory(relationshipType), user)

	@ModelAttribute("allocationTypes")
	def allocationTypes = ExtractRelationshipsFromFileCommand.AllocationTypes

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def form(@ModelAttribute("command") cmd: Appliable[AllocateStudentsToRelationshipCommand.Result], @PathVariable department: Department, @PathVariable relationshipType: StudentRelationshipType): Mav = {
		Mav("profiles/relationships/allocate_preview")
	}

	@RequestMapping(method = Array(POST), params = Array("confirm"))
	def submit(
		@ModelAttribute("command") cmd: Appliable[AllocateStudentsToRelationshipCommand.Result],
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable relationshipType: StudentRelationshipType
	): Mav = {
		if (errors.hasErrors) {
			form(cmd, department, relationshipType)
		} else {
			cmd.apply()
			Redirect(Routes.relationships(department, relationshipType))
		}
	}

}
