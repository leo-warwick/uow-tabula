package uk.ac.warwick.tabula.commands.coursework.assignments.extensions

import uk.ac.warwick.tabula.services.UserLookupService

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.AssessmentDao
import uk.ac.warwick.tabula.helpers.coursework.ExtensionGraph

class ListAllExtensionsCommand(val department: Department, val academicYear: AcademicYear)
	extends Command[Seq[ExtensionGraph]] with ReadOnly with Unaudited {

	// This permissions check limits this to anyone who has extension read permission over the whole department.
	// Since extension manager is only available as a departmental role ATOW, that's OK for now.
	// If extension manager were to become more fine-grained in future and used for people who don't have
	// Extension.Read permission, we would need to make this less strict.
	PermissionCheck(Permissions.Extension.Read, mandatory(department))

	var assignmentDao: AssessmentDao = Wire.auto[AssessmentDao]
	var userLookup: UserLookupService = Wire[UserLookupService]

	def applyInternal(): Seq[ExtensionGraph] = {

		// get all extensions for assignments in modules in the department for the current year
		assignmentDao.getAssignments(department, academicYear)
			.flatMap { _.extensions.asScala }
			.map { extension => ExtensionGraph(extension, userLookup.getUserByUserId(extension.usercode)) }
	}

}
