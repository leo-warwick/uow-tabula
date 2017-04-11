package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.{Gender, StaffMember}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class StaffMemberFixtureCommand extends CommandInternal[StaffMember] with Logging {
	this: UserLookupComponent =>

	var userId: String = _
	var genderCode: String = _
	var deptCode:String=""

	var memberDao: MemberDao = Wire[MemberDao]
	var deptDao: DepartmentDao = Wire[DepartmentDao]

	def applyInternal(): StaffMember = {
		val userLookupUser = userLookup.getUserByUserId(userId)
		assert(userLookupUser != null)

		val existing = memberDao.getByUniversityId(userLookupUser.getWarwickId)
		val dept = if (deptCode!= "") deptDao.getByCode(deptCode) else None

		val newMember = new StaffMember
		newMember.universityId = userLookupUser.getWarwickId
		newMember.userId = userId
		newMember.gender = Gender.fromCode(genderCode)
		newMember.firstName = userLookupUser.getFirstName
		newMember.lastName = userLookupUser.getLastName
		newMember.inUseFlag = "Active"
		if (dept.isDefined)  newMember.homeDepartment = dept.get

		transactional() {

			existing foreach {
				memberDao.delete
			}

			memberDao.saveOrUpdate(newMember)
		}

		newMember
	}
}

object StaffMemberFixtureCommand {
	def apply(): StaffMemberFixtureCommand with ComposableCommand[StaffMember] with AutowiringUserLookupComponent with Unaudited with PubliclyVisiblePermissions = {
		new StaffMemberFixtureCommand with ComposableCommand[StaffMember]
			with AutowiringUserLookupComponent
			with Unaudited
			with PubliclyVisiblePermissions
	}
}
