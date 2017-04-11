package uk.ac.warwick.tabula.data.model.permissions

import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.helpers.ReflectionsSetup

class GrantedPermissionPersistenceTest extends PersistenceTestBase with ReflectionsSetup {


	@Test def saveAndLoad {
		transactional { t =>
			val department = new Department
			department.code = "IN"
			department.fullName = "IT Services"

			session.save(department)
			session.flush()

			val permission = GrantedPermission(department, Permissions.Department.DownloadFeedbackReport, GrantedPermission.Allow)
			permission.users.knownType.addUserId("cuscav")

			session.save(permission)
			session.flush()
			session.clear()

			session.load(classOf[GrantedPermission[_]], permission.id) match {
				case permission: GrantedPermission[Department @unchecked] =>
					permission.scope.code should be ("IN")
				case _ => fail("What is this!")
			}
		}
	}

}