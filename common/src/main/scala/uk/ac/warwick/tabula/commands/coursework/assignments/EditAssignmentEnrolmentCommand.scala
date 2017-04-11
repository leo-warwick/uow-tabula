package uk.ac.warwick.tabula.commands.coursework.assignments


import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.commands.Description
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.AcademicYear


/**
 * This is a stub class, which isn't applied, but exposes the student membership (enrolment) for an assignment
 * via the ModifyAssignmentCommand to rebuild views within an existing form
 */
class EditAssignmentEnrolmentCommand(module: Module = null, academicYear: AcademicYear) extends ModifyAssignmentCommand(module) with Unaudited {

	PermissionCheckAny(
		Seq(CheckablePermission(Permissions.Assignment.Create, module),
			CheckablePermission(Permissions.Assignment.Update, module))
	)

	// not required
	def assignment: Assignment = null

	// not required
	def contextSpecificValidation(errors: Errors) {}

	// not required
	override def applyInternal(): Assignment = throw new UnsupportedOperationException
}
