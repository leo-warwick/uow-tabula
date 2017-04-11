package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.Course
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, CourseAndRouteService, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class CourseCreationFixtureCommand extends CommandInternal[Course] {
	this: ModuleAndDepartmentServiceComponent with SessionComponent with TransactionalComponent =>

	var courseAndRouteService: CourseAndRouteService = Wire[CourseAndRouteService]
	var courseCode: String = _
	var courseName: String = _

	protected def applyInternal(): Course =
		transactional() {
			val c = courseAndRouteService.getCourseByCode(courseCode).getOrElse(new Course)
			c.code = courseCode
			c.name = courseName
			c.shortName = courseName
			c.title = courseName

			courseAndRouteService.saveOrUpdate(c)
			c
		}
}

object CourseCreationFixtureCommand{
	def apply(): CourseCreationFixtureCommand with ComposableCommand[Course] with AutowiringModuleAndDepartmentServiceComponent with Daoisms with AutowiringTransactionalComponent with Unaudited with PubliclyVisiblePermissions ={
		new CourseCreationFixtureCommand
			with ComposableCommand[Course]
			with AutowiringModuleAndDepartmentServiceComponent
			with Daoisms
			with AutowiringTransactionalComponent
			with Unaudited
			with PubliclyVisiblePermissions

	}
}
