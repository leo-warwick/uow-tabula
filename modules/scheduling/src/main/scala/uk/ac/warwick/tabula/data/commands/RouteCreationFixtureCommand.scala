package uk.ac.warwick.tabula.data.commands

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.{DegreeType, Route}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class RouteCreationFixtureCommand extends CommandInternal[Route] {
	self: ModuleAndDepartmentServiceComponent with TransactionalComponent with SessionComponent =>

	var routeDao:RouteDao = Wire[RouteDao]
	var departmentCode: String = _
	var routeCode: String = _
	var routeName: String = _
  var degreeType:String = "UG"

	protected def applyInternal() = {
		transactional() {
			val existing = routeDao.getByCode(routeCode)
			for(route <- existing){
				// this may fail, if a department is deleted without deleting the routes associated with it, since
				// there is no constraint on route.department_id. If that happens, delete the route manually and fix
				// the code that's deleting the department to also clean up the routes.
				session.delete(route)
			}
		}

		transactional() {
			val dept = moduleAndDepartmentService.getDepartmentByCode(departmentCode).get
			val r = new Route
			r.adminDepartment = dept
			r.active = true
			r.code = routeCode
			r.degreeType = DegreeType.fromCode(degreeType)
			r.name = routeName
			routeDao.saveOrUpdate(r)
			r
		}
	}
}

object RouteCreationFixtureCommand{
	def apply()={
		new RouteCreationFixtureCommand
		with ComposableCommand[Route]
		with AutowiringModuleAndDepartmentServiceComponent
		with AutowiringTransactionalComponent
		with Daoisms
		with Unaudited
		with PubliclyVisiblePermissions

	}
}
