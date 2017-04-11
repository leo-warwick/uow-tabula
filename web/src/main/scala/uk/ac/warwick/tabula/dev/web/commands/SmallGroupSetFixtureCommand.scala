package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent, AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.data.{Daoisms, SessionComponent}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupAllocationMethod, SmallGroupFormat, SmallGroupSet}
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.StudentSignUp
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime

class SmallGroupSetFixtureCommand extends CommandInternal[SmallGroupSet] with Logging {

	this: ModuleAndDepartmentServiceComponent with SessionComponent with TermServiceComponent =>

	var moduleCode: String = _
	var groupSetName: String = _
	var formatName: String = _
	var openForSignups: Boolean = true
	var allocationMethodName: String = _
	var groupCount: Int = _
	var maxGroupSize: Int = 0
	var allowSelfGroupSwitching: Boolean = true
	var releasedToStudents: Boolean = true
	var academicYear: AcademicYear = _

	protected def applyInternal(): SmallGroupSet = {
		transactional() {
			val module = moduleAndDepartmentService.getModuleByCode(moduleCode).getOrElse(throw new RuntimeException)
			val groupSet = new SmallGroupSet()
			groupSet.name = groupSetName
			groupSet.format = SmallGroupFormat.fromCode(formatName)
			groupSet.module = module
			groupSet.academicYear = academicYear
			groupSet.allocationMethod = SmallGroupAllocationMethod.fromDatabase(allocationMethodName)
			if (groupSet.allocationMethod == StudentSignUp ){
				groupSet.allowSelfGroupSwitching = allowSelfGroupSwitching
			}
			groupSet.openForSignups = openForSignups
			groupSet.releasedToStudents = releasedToStudents
			groupSet.groups = JArrayList()
			groupSet.members = UserGroup.ofUsercodes
			for (i <- 1 to groupCount) {
				val group = new SmallGroup
				group.name = s"Group $i"
				if (maxGroupSize > 0){
					group.maxGroupSize = maxGroupSize
				}
				group.students = UserGroup.ofUsercodes // have to user userCodes, because you can't look up an ext-user
				                                       // (e.g. tabula-functest-student1) by uni ID.
				groupSet.groups.add(group)

			}
			session.save(groupSet)
			groupSet
		}
	}
}
object SmallGroupSetFixtureCommand{
	def apply(): SmallGroupSetFixtureCommand with ComposableCommand[SmallGroupSet] with AutowiringModuleAndDepartmentServiceComponent with Daoisms with Unaudited with PubliclyVisiblePermissions with AutowiringTermServiceComponent = {
		new SmallGroupSetFixtureCommand()
			with ComposableCommand[SmallGroupSet]
			with AutowiringModuleAndDepartmentServiceComponent
			with Daoisms
			with Unaudited
			with PubliclyVisiblePermissions
			with AutowiringTermServiceComponent
	}
}
