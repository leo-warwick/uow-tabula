package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions._


class AddAssignmentCommand(module: Module = null) extends ModifyAssignmentCommand(module) {

	PermissionCheck(Permissions.Assignment.Create, module)

	def assignment: Assignment = null

	override def applyInternal(): Assignment = transactional() {
		val assignment = new Assignment(module)
		assignment.addDefaultFields()
		copyTo(assignment)
		service.save(assignment)
		assignment
	}

	override def describeResult(d: Description, assignment: Assignment): Unit = d.assignment(assignment)

	override def describe(d: Description): Unit = d.module(module).properties(
		"name" -> name,
		"openDate" -> openDate,
		"closeDate" -> closeDate)

	// can be overridden in concrete implementations to provide additional validation
	def contextSpecificValidation(errors: Errors) {}
}
