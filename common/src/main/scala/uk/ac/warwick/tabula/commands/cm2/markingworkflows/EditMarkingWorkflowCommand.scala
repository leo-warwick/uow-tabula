package uk.ac.warwick.tabula.commands.cm2.markingworkflows

import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils.rejectIfEmptyOrWhitespace
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.markingworkflow._
import uk.ac.warwick.tabula.helpers.StringUtils
import uk.ac.warwick.tabula.services.{UserLookupComponent, _}
import uk.ac.warwick.tabula.validators.UsercodeListValidator
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.collection.JavaConverters._

object EditMarkingWorkflowCommand {
	def apply(department:Department, academicYear: AcademicYear, workflow: CM2MarkingWorkflow) =
		new EditMarkingWorkflowCommandInternal(department, academicYear, workflow)
			with ComposableCommand[Option[CM2MarkingWorkflow]]
			with EditMarkingWorkflowValidation
			with MarkingWorkflowDepartmentPermissions
			with EditMarkingWorkflowDescription
			with EditMarkingWorkflowState
			with AutowiringCM2MarkingWorkflowServiceComponent
			with AutowiringUserLookupComponent
}


class EditMarkingWorkflowCommandInternal(
	val department: Department,
	val academicYear: AcademicYear,
	w: CM2MarkingWorkflow) extends CommandInternal[Option[CM2MarkingWorkflow]] {

	self: EditMarkingWorkflowState with CM2MarkingWorkflowServiceComponent with UserLookupComponent =>

	val workflow = Some(w)

	workflowName = w.name

	extractMarkers match { case (a, b) =>
		markersA = JArrayList(a)
		markersB = JArrayList(b)
	}

	def applyInternal(): Option[CM2MarkingWorkflow] = workflow.map(w => {
		w.name = workflowName
		w.replaceMarkers(markersAUsers, markersBUsers)
		cm2MarkingWorkflowService.save(w)
		w
	})
}

trait EditMarkingWorkflowValidation extends ModifyMarkingWorkflowValidation with StringUtils {

	self: EditMarkingWorkflowState with UserLookupComponent =>

	override def validate(errors: Errors): Unit = workflow.foreach( w => {
		rejectIfEmptyOrWhitespace(errors, "workflowName", "NotEmpty")

		markerValidation(errors, w.workflowType)

		if (department.cm2MarkingWorkflows.exists(dw => dw.id != w.id && dw.academicYear == academicYear && dw.name == workflowName)) {
			errors.rejectValue("workflowName", "name.duplicate.markingWorkflow", Array(workflowName), null)
		}

		lazy val (existingMarkerAs, existingMarkerBs) = extractMarkers match { case (a, b) => (a.toSet, b.toSet) }
		lazy val removedMarkerAs: Set[Usercode] = existingMarkerAs -- markersA.asScala.toSet
		lazy val removedMarkerBs: Set[Usercode] = existingMarkerBs -- markersB.asScala.toSet

		if(!w.canDeleteMarkers) {
			val errorCode =
				if (w.studentsChooseMarkers) "markingWorkflow.studentsChoose.cannotRemoveMarkers"
				else "markingWorkflow.markers.cannotRemoveMarkers"

			if(removedMarkerAs.nonEmpty) {
				errors.rejectValue("markersA", errorCode)

			}
			if(removedMarkerBs.nonEmpty) {
				errors.rejectValue("markersB", errorCode)
			}
		}

	})
}

trait EditMarkingWorkflowState extends ModifyMarkingWorkflowState {
	this: UserLookupComponent =>
	def  workflow: Option[CM2MarkingWorkflow]

	protected def extractMarkers: (Seq[Usercode], Seq[Usercode]) = workflow.map(w => {
		w.markersByRole.values.toList match {
			case a :: b => (a.map(_.getUserId), b.headOption.getOrElse(Nil).map(_.getUserId))
			case _ => throw new IllegalArgumentException(s"workflow ${w.id} has no markers")
		}
	}).getOrElse((Nil, Nil))
}

trait ModifyMarkingWorkflowValidation extends SelfValidating {

	self: ModifyMarkingWorkflowState with UserLookupComponent =>

	def hasDuplicates(usercodes: JList[String]): Boolean = {
		val trimmed = usercodes.asScala.filter(_.hasText).map(_.trim)
		trimmed.distinct.size != trimmed.size
	}

	// validation of the markers
	def markerValidation(errors: Errors, workflowType: MarkingWorkflowType): Unit = {

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


trait EditMarkingWorkflowDescription extends Describable[Option[CM2MarkingWorkflow]] {
	self: ModifyMarkingWorkflowState =>

	override lazy val eventName: String = "EditMarkingWorkflow"

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
	var workflowName: String = _
	var workflowType: MarkingWorkflowType = _
	// all the current workflows have at most 2 sets of markers
	var markersA: JList[Usercode] = _
	var markersB: JList[Usercode] = _

	var sampler: ModerationSampler = _

	def markersAUsers: Seq[User] = userLookup.getUsersByUserIds(markersA.asScala).values.toSeq
	def markersBUsers: Seq[User] = userLookup.getUsersByUserIds(markersB.asScala).values.toSeq
}