package uk.ac.warwick.tabula.web.controllers.cm2.admin.markingworkflows

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.web.controllers.cm2.admin.CourseworkDepartmentAndYearController

trait CM2MarkingWorkflowController extends CourseworkController with CourseworkDepartmentAndYearController {

	override val departmentPermission: Permission = Permissions.MarkingWorkflow.Manage

	def commonCrumbs(view: Mav, department: Department, academicYear: AcademicYear): Mav =
		view.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.cm2.admin.workflows(department, year)): _*)
}
