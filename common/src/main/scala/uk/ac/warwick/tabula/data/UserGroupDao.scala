package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository

import uk.ac.warwick.tabula.data.model.{Department, UserGroup}
import uk.ac.warwick.spring.Wire

trait UserGroupDaoComponent {
	val userGroupDao: UserGroupDao
}

trait AutowiringUserGroupDaoComponent extends UserGroupDaoComponent {
	val userGroupDao: UserGroupDao = Wire[UserGroupDao]
}

trait UserGroupDao {
	def saveOrUpdate(userGroup: UserGroup)
}

@Repository
class UserGroupDaoImpl extends UserGroupDao with Daoisms {

	def saveOrUpdate(userGroup: UserGroup): Unit = session.saveOrUpdate(userGroup)

}
