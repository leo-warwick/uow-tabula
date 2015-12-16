package uk.ac.warwick.tabula.data.commands

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.{Daoisms, SessionComponent}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.StudentSignUp
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupAllocationMethod, SmallGroupFormat, SmallGroupSet}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringTermServiceComponent, ModuleAndDepartmentServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class SmallGroupSetFixtureCommand extends CommandInternal[SmallGroupSet] with Logging {

	this: ModuleAndDepartmentServiceComponent with TermServiceComponent with SessionComponent =>

	var moduleCode:String = _
	var groupSetName:String = _
	var formatName:String = _
	var openForSignups:Boolean = true
	var allocationMethodName:String = _
	var groupCount:Int = _
	var maxGroupSize:Int = 0
	var allowSelfGroupSwitching:Boolean = true
	var releasedToStudents:Boolean = true

	protected def applyInternal() = {
		transactional() {
			val module = moduleAndDepartmentService.getModuleByCode(moduleCode).getOrElse(throw new RuntimeException)
			val groupSet = new SmallGroupSet()
			groupSet.name = groupSetName
			groupSet.format = SmallGroupFormat.fromCode(formatName)
			groupSet.module = module
			groupSet.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
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
	def apply() = {
		new SmallGroupSetFixtureCommand()
			with ComposableCommand[SmallGroupSet]
			with AutowiringModuleAndDepartmentServiceComponent
			with Daoisms
			with Unaudited
			with PubliclyVisiblePermissions
			with AutowiringTermServiceComponent
	}
}
