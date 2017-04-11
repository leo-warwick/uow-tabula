package uk.ac.warwick.tabula.commands.cm2.markingworkflows

import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils.rejectIfEmptyOrWhitespace
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.markingworkflow._
import uk.ac.warwick.tabula.helpers.StringUtils
import uk.ac.warwick.tabula.services.{UserLookupComponent, _}
import uk.ac.warwick.tabula.validators.UsercodeListValidator
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object EditMarkingWorkflowCommand {
	def apply(department:Department, academicYear: AcademicYear, workflow: CM2MarkingWorkflow) =
		new EditMarkingWorkflowCommandInternal(department, academicYear, workflow)
			with ComposableCommand[CM2MarkingWorkflow]
			with EditMarkingWorkflowValidation
			with MarkingWorkflowDepartmentPermissions
			with ModifyMarkingWorkflowDescription
			with EditMarkingWorkflowState
			with AutoWiringCM2MarkingWorkflowServiceComponent
			with AutowiringUserLookupComponent
}


class EditMarkingWorkflowCommandInternal(
	val department: Department,
	val academicYear: AcademicYear,
	val workflow: CM2MarkingWorkflow) extends CommandInternal[CM2MarkingWorkflow] {

	self: EditMarkingWorkflowState with CM2MarkingWorkflowServiceComponent with UserLookupComponent =>

	name = workflow.name
	extractMarkers match { case (a, b) =>
		markersA = JArrayList(a)
		markersB = JArrayList(b)
	}

	def applyInternal(): CM2MarkingWorkflow = {
		workflow.name = name
		workflow.replaceMarkers(markersAUsers, markersBUsers)
		cm2MarkingWorkflowService.save(workflow)
		workflow
	}
}

trait EditMarkingWorkflowValidation extends ModifyMarkingWorkflowValidation with StringUtils {

	self: EditMarkingWorkflowState with UserLookupComponent =>

	override def validate(errors: Errors) {
		genericValidate(errors, workflow.workflowType)

		if (department.cm2MarkingWorkflows.exists(w => w.id != workflow.id && w.academicYear == academicYear && w.name == name)) {
			errors.rejectValue("name", "name.duplicate.markingWorkflow", Array(name), null)
		}

		lazy val (existingMarkerAs, existingMarkerBs) = extractMarkers match { case (a, b) => (a.toSet, b.toSet) }
		lazy val removedMarkerAs: Set[Usercode] = existingMarkerAs -- markersA.asScala.toSet
		lazy val removedMarkerBs: Set[Usercode] = existingMarkerBs -- markersB.asScala.toSet

		if(!workflow.canDeleteMarkers) {
			val errorCode =
				if (workflow.studentsChooseMarkers) "markingWorkflow.studentsChoose.cannotRemoveMarkers"
				else "markingWorkflow.markers.cannotRemoveMarkers"

			if(removedMarkerAs.nonEmpty) {
				errors.rejectValue("markersA", errorCode)

			}
			if(removedMarkerBs.nonEmpty) {
				errors.rejectValue("markersB", errorCode)
			}
		}

	}
}

trait EditMarkingWorkflowState extends ModifyMarkingWorkflowState {
	this: UserLookupComponent =>
	def  workflow: CM2MarkingWorkflow

	protected def extractMarkers: (Seq[Usercode], Seq[Usercode]) = {
		workflow.markersByRole.values.toList match {
			case a :: b => (a.map(_.getUserId), b.headOption.getOrElse(Nil).map(_.getUserId))
			case _ => throw new IllegalArgumentException(s"workflow ${workflow.id} has no markers")
		}
	}
}

trait ModifyMarkingWorkflowValidation extends SelfValidating {

	self: ModifyMarkingWorkflowState with UserLookupComponent =>

	def hasDuplicates(usercodes: JList[String]): Boolean =
		usercodes.asScala.distinct.size != usercodes.asScala.size

	// validation shared between add and edit
	def genericValidate(errors: Errors, workflowType: MarkingWorkflowType): Unit = {
		rejectIfEmptyOrWhitespace(errors, "name", "NotEmpty")

		val markerAValidator = new UsercodeListValidator(markersA, "markersA"){
			override def alreadyHasCode: Boolean = hasDuplicates(markersA)
		}
		markerAValidator.userLookup = this.userLookup // makes testing easier
		markerAValidator.validate(errors)

		// validate only when two groups of marker are used
		if(workflowType.roleNames.length > 1){
			val markerBValidator = new UsercodeListValidator(markersB, "markersB"){
				override def alreadyHasCode: Boolean = hasDuplicates(markersB)
			}
			markerBValidator.userLookup = this.userLookup  // makes testing easier
			markerBValidator.validate(errors)
		}
	}
}


trait ModifyMarkingWorkflowDescription extends Describable[CM2MarkingWorkflow] {
	self: ModifyMarkingWorkflowState =>

	def describe(d: Description) {
		d.department(department)
	}
}


trait ModifyMarkingWorkflowState {

	this: UserLookupComponent =>

	type Usercode = String

	def department: Department
	def academicYear: AcademicYear

	// bindable
	var name: String = _
	// all the current workflows have at most 2 sets of markers
	var markersA: JList[Usercode] = _
	var markersB: JList[Usercode] = _

	def markersAUsers: Seq[User] = userLookup.getUsersByUserIds(markersA.asScala).values.toSeq
	def markersBUsers: Seq[User] = userLookup.getUsersByUserIds(markersB.asScala).values.toSeq
}