package uk.ac.warwick.tabula.commands.sysadmin

import org.hibernate.validator.constraints._
import javax.validation.constraints._
import uk.ac.warwick.tabula.data.model.StudentRelationshipSource
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.services.RelationshipServiceComponent
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.Describable
import uk.ac.warwick.tabula.commands.Description

trait HasExistingStudentRelationshipType {
	val relationshipType: StudentRelationshipType
}

abstract class ModifyStudentRelationshipTypeCommandInternal extends CommandInternal[StudentRelationshipType]
	with StudentRelationshipTypeProperties with SelfValidating {
	this: RelationshipServiceComponent =>

	def copyFrom(tpe: StudentRelationshipType) {
		id = tpe.id
		urlPart = tpe.urlPart
		agentRole = tpe.agentRole
		studentRole = tpe.studentRole
		description = tpe.description
		defaultSource = tpe.defaultSource
		defaultDisplay = tpe.defaultDisplay
		expectedUG = tpe.expectedUG
		expectedPGT = tpe.expectedPGT
		expectedPGR = tpe.expectedPGR
		sortOrder = tpe.sortOrder
	}

	def copyTo(tpe: StudentRelationshipType) {
		tpe.id = id
		tpe.urlPart = urlPart
		tpe.agentRole = agentRole
		tpe.studentRole = studentRole
		tpe.description = description
		tpe.defaultSource = defaultSource
		tpe.defaultDisplay = defaultDisplay
		tpe.expectedUG = expectedUG
		tpe.expectedPGT = expectedPGT
		tpe.expectedPGR = expectedPGR
		tpe.sortOrder = sortOrder
	}

	def validate(errors: Errors) {
		// Ensure that we don't dupe url part
		relationshipService.getStudentRelationshipTypeByUrlPart(urlPart).filter { _.id != id }.foreach { dupe =>
			errors.rejectValue("urlPart", "relationshipType.urlPart.duplicate")
		}
	}
}

trait ModifyStudentRelationshipTypeCommandDescription extends Describable[StudentRelationshipType] {
	this: StudentRelationshipTypeProperties =>
	// describe the thing that's happening.
	override def describe(d: Description): Unit =
		d.properties(
			"id" -> id,
			"urlPart" -> urlPart,
			"description" -> description
		)
}

trait StudentRelationshipTypeProperties {

	@Length(min=1, max=20)
	@Pattern(regexp="[A-Za-z0-9_\\-]+")
	var id: String = _

	@Length(min=1, max=50)
	@Pattern(regexp="[A-Za-z0-9_\\-]+")
	var urlPart: String = _

	@Length(min=1, max=50)
	var agentRole: String = _

	@Length(min=1, max=50)
	var studentRole: String = _

	@Length(min=1, max=50)
	var description: String = _

	@NotNull
	var defaultSource: StudentRelationshipSource = _

	var defaultDisplay: Boolean = true
	var expectedUG: Boolean = false
	var expectedPGT: Boolean = false
	var expectedPGR: Boolean = false

	@Min(0)
	@NotNull
	var sortOrder: Int = 20

}