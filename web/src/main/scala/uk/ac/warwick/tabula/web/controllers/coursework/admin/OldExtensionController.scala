package uk.ac.warwick.tabula.web.controllers.coursework.admin

import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model.{Assignment, Department, Module, StudentMember}
import uk.ac.warwick.tabula.commands.coursework.assignments.extensions._
import uk.ac.warwick.tabula.web.Mav
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService, UserLookupService}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, JsonHelper}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.helpers.DateBuilder
import javax.validation.Valid

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

abstract class OldExtensionController extends OldCourseworkController {
	var json: ObjectMapper = Wire[ObjectMapper]
	var userLookup: UserLookupService = Wire[UserLookupService]
	var relationshipService: RelationshipService = Wire[RelationshipService]
	var profileService: ProfileService = Wire[ProfileService]

	class ExtensionMap(extension: Extension) {
		def asMap: Map[String, String] = {

			def convertDateToString(date: Option[DateTime]) = date.map(DateBuilder.format).getOrElse("")

			def convertDateToMillis(date: Option[DateTime]) = date.map(_.getMillis.toString).orNull

			Map(
				"id" -> extension.universityId.getOrElse(""),
				"usercode" -> extension.usercode,
				"status" -> extension.state.description,
				"requestedExpiryDate" -> convertDateToString(extension.requestedExpiryDate),
				"expiryDate" -> convertDateToString(extension.expiryDate),
				"expiryDateMillis" -> convertDateToMillis(extension.expiryDate),
				"extensionDuration" -> extension.duration.toString,
				"requestedExtraExtensionDuration" -> extension.requestedExtraDuration.toString,
				"reviewerComments" -> extension.reviewerComments
			)
		}
	}
	import scala.language.implicitConversions
	implicit def asMap(e: Extension): ExtensionMap = new ExtensionMap(e)
}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/extensions"))
class OldListExtensionsForAssignmentController extends OldExtensionController {

	// Add the common breadcrumbs to the model
	def crumbed(mav: Mav, module: Module): Mav
	= mav.crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module))

	@ModelAttribute
	def listCommand(
		@PathVariable module:Module,
		@PathVariable assignment:Assignment
		) = new ListExtensionsForAssignmentCommand(module, assignment, user)

	@RequestMapping(method=Array(HEAD,GET))
	def listExtensions(cmd: ListExtensionsForAssignmentCommand, @RequestParam(value="usercode", required=false) usercode: String): Mav = {
		val extensionGraphs = cmd.apply()

		val model = Mav(s"$urlPrefix/admin/assignments/extensions/summary",
			"detailUrl" -> Routes.admin.assignment.extension.detail(cmd.assignment),
			"module" -> cmd.module,
			"extensionToOpen" -> usercode,
			"assignment" -> cmd.assignment,
			"extensionGraphs" -> extensionGraphs,
			"maxDaysToDisplayAsProgressBar" -> Extension.MaxDaysToDisplayAsProgressBar
		)

		crumbed(model, cmd.module)
	}
}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/department/{department}/manage/extensions"))
class OldListAllExtensionsController extends OldExtensionController {

	// Add the common breadcrumbs to the model
	def crumbed(mav: Mav, department: Department): Mav
	= mav.crumbs(Breadcrumbs.Department(department))

	@ModelAttribute("command")
	def listCommand(@PathVariable department:Department, @RequestParam(value="academicYear", required=false) academicYear: AcademicYear) =
		new ListAllExtensionsCommand(department, Option(academicYear).getOrElse(AcademicYear.guessSITSAcademicYearByDate(new DateTime)))

	@ModelAttribute("academicYears")
	def academicYearChoices: JList[AcademicYear] =
		AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(2, 2).asJava

	@RequestMapping(method=Array(HEAD,GET))
	def listExtensions(@ModelAttribute("command") cmd: ListAllExtensionsCommand, @RequestParam(value="usercode", required=false) usercode: String): Mav = {
		val extensionGraphs = cmd.apply()

		val model = Mav(s"$urlPrefix/admin/assignments/extensions/departmentSummary",
			"extensionToOpen" -> usercode,
			"extensionGraphs" -> extensionGraphs,
			"maxDaysToDisplayAsProgressBar" -> Extension.MaxDaysToDisplayAsProgressBar
		)

		crumbed(model, cmd.department)
	}
}

/**
 * FIXME TAB-2426 Is the second mapping here necessary? Is it referred to anywhere? If not, it should be removed.
 * The first mapping is the one that should be used.
 */
@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/extensions/detail/{student}"))
class OldEditExtensionController extends OldExtensionController {

	@ModelAttribute("modifyExtensionCommand")
	def editCommand(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable student: User,
		user: CurrentUser,
		@RequestParam(defaultValue = "") action: String
	) = {
		EditExtensionCommand(module, assignment, student, user, action)
	}

	validatesSelf[SelfValidating]

	// view an extension (or request)
	@RequestMapping(method=Array(GET))
	def editExtension(
		@ModelAttribute("modifyExtensionCommand") cmd: Appliable[Extension] with ModifyExtensionCommandState,
		errors: Errors
	): Mav = {

		val studentMember = cmd.extension.universityId.flatMap(uid => profileService.getMemberByUniversityId(uid))
		val studentContext = studentMember match {
			case Some(s: StudentMember) =>
				val relationships = relationshipService.allStudentRelationshipTypes.map { relationshipType =>
					(relationshipType.description, relationshipService.findCurrentRelationships(relationshipType, s))
				}.toMap.filter({case (relationshipType,relations) => relations.nonEmpty})

				Map(
					"relationships" -> relationships,
					"course" -> s.mostSignificantCourseDetails
				)
			case _ => Map.empty
		}

		val model = Mav(s"$urlPrefix/admin/assignments/extensions/detail",
			"command" -> cmd,
			"module" -> cmd.module,
			"assignment" -> cmd.assignment,
			"usercode" -> cmd.student.getUserId,
			"studentIdentifier" -> Option(cmd.student.getWarwickId).getOrElse(cmd.student.getUserId),
			"student" -> studentMember,
			"studentContext" -> studentContext,
			"userFullName" -> cmd.student.getFullName,
			"updateAction" -> cmd.UpdateApprovalAction,
			"approvalAction" -> cmd.ApprovalAction,
			"rejectionAction" -> cmd.RejectionAction,
			"revocationAction" -> cmd.RevocationAction
		).noLayout()
		model
	}

	@RequestMapping(method=Array(POST))
	@ResponseBody
	def persistExtension(
		@Valid @ModelAttribute("modifyExtensionCommand") cmd: Appliable[Extension] with ModifyExtensionCommandState,
		result: BindingResult,
		errors: Errors
	): Mav = {
		if (errors.hasErrors) {
			editExtension(cmd, errors)
		} else {
			val extensionJson = JsonHelper.toJson(cmd.apply().asMap)
			Mav("ajax_success", "data" -> extensionJson).noLayout()
		}
	}
}


@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/extensions/revoke/{student}"))
class OldDeleteExtensionController extends OldExtensionController {

	@ModelAttribute("deleteExtensionCommand")
	def deleteCommand(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable student: User,
		user: CurrentUser
	) = {
		DeleteExtensionCommand(module, assignment, student, user)
	}

	// delete a manually created extension item - this revokes the extension
	@RequestMapping(method=Array(POST))
	def deleteExtension(@ModelAttribute("deleteExtensionCommand") cmd: Appliable[Extension] with ModifyExtensionCommandState): Mav = {
		val extensionJson = JsonHelper.toJson(cmd.apply().asMap)
		Mav("ajax_success", "data" -> extensionJson).noLayout()
	}
}
