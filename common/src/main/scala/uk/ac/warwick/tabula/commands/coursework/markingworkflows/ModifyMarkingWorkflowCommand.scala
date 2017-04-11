package uk.ac.warwick.tabula.commands.coursework.markingworkflows

import scala.collection.JavaConverters._
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils._
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.model.{MarkingMethod, Department, MarkingWorkflow}
import uk.ac.warwick.tabula.data.model.MarkingMethod._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.validators.UsercodeListValidator
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.services.MarkingWorkflowServiceComponent

/** Abstract base command for either creating or editing a MarkingWorkflow */
abstract class ModifyMarkingWorkflowCommand(val department: Department)
	extends CommandInternal[MarkingWorkflow] with BindListener with MarkingWorkflowCommandState {
	self: MarkingWorkflowServiceComponent =>

	var name: String = _
	var firstMarkers: JList[String] = JArrayList()
	var secondMarkers: JList[String] = JArrayList()
	var markingMethod: MarkingMethod = _

	var firstMarkerRoleName: String = _
	var secondMarkerRoleName: String = _

	def onBind(result: BindingResult) {
	  firstMarkers = firstMarkers.asScala.filter(_.hasText).asJava
	  secondMarkers = secondMarkers.asScala.filter(_.hasText).asJava
	}

	def copyTo(scheme: MarkingWorkflow) {
		scheme.name = name
		scheme.firstMarkers.knownType.includedUserIds = firstMarkers.asScala
		scheme.secondMarkers.knownType.includedUserIds = secondMarkers.asScala
	}

	def addFirstMarkers(markers: Seq[String]) {
		val missingMarkers = markers.toSet -- firstMarkers.asScala.toSet
		firstMarkers.addAll(missingMarkers.asJava)
	}

	def addSecondMarkers(markers: Seq[String]) {
		val missingMarkers = markers.toSet -- secondMarkers.asScala.toSet
		secondMarkers.addAll(missingMarkers.asJava)
	}

	def copyFrom(scheme: MarkingWorkflow) {
		name = scheme.name
		firstMarkers.clear()
		firstMarkers.addAll(scheme.firstMarkers.knownType.includedUserIds.asJava)
		secondMarkers.clear()
		secondMarkers.addAll(scheme.secondMarkers.knownType.includedUserIds.asJava)
		markingMethod = scheme.markingMethod

		firstMarkerRoleName = scheme.firstMarkerRoleName
		secondMarkerRoleName = scheme.secondMarkerRoleName.getOrElse("Second marker")
	}

}

trait MarkingWorkflowCommandState {
	def department: Department
	def name: String
	def firstMarkers: JList[String]
	def secondMarkers: JList[String]
	def markingMethod: MarkingMethod

	def firstMarkerRoleName: String
	def secondMarkerRoleName: String
}

trait MarkingWorkflowCommandValidation extends SelfValidating {
	self: MarkingWorkflowCommandState =>

	// Subclasses can provide the "current" markingWorkflow if one applies, for validation.
	def currentMarkingWorkflow: Option[MarkingWorkflow]

	def contextSpecificValidation(errors: Errors)

	def validate(errors: Errors) {
		contextSpecificValidation(errors)

		rejectIfEmptyOrWhitespace(errors, "name", "NotEmpty")

		if (department.markingWorkflows.exists(sameName)) {
			errors.rejectValue("name", "name.duplicate.markingWorkflow", Array(this.name), null)
		}

		if (markingMethod == null)
			errors.rejectValue("markingMethod", "markingWorkflow.markingMethod.none")

		val firstMarkersValidator = new UsercodeListValidator(firstMarkers, "firstMarkers", universityIdRequired = true){
			override def alreadyHasCode: Boolean = hasDuplicates(firstMarkers.asScala)
		}
		firstMarkersValidator.validate(errors)

		// validate only when second markers are used
		if(Seq(SeenSecondMarking, SeenSecondMarkingLegacy, ModeratedMarking).contains(markingMethod)){
			val secondMarkersValidator = new UsercodeListValidator(secondMarkers, "secondMarkers", universityIdRequired = true){
				override def alreadyHasCode: Boolean = hasDuplicates(secondMarkers.asScala)
			}
			secondMarkersValidator.validate(errors)
		}
	}

	def hasDuplicates(strings: Seq[String]): Boolean = {
		strings.filter { _.hasText }.distinct.size != strings.count { _.hasText }
	}

	// If there's a current markingWorkflow, returns whether "other" is a different
	// scheme with the same name we're trying to use.
	// If there's no current markingWorkflow we just check if it's just the same name.
	def sameName(other: MarkingWorkflow): Boolean = currentMarkingWorkflow match {
		case Some(existing) =>
			other.id != existing.id && other.name == name
		case None =>
			other.name == name
	}
}