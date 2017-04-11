package uk.ac.warwick.tabula.data.model.permissions

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{Fixtures, TestBase}
import uk.ac.warwick.tabula.roles.AssignmentSubmitterRoleDefinition
import uk.ac.warwick.tabula.permissions.Permissions

class GrantedRoleTest extends TestBase {

	val dept: Department = Fixtures.department("in")
	val module: Module = Fixtures.module("in101")
	module.adminDepartment = dept

	val assignment: Assignment = Fixtures.assignment("assignment")
	val member: StaffMember = Fixtures.staff()

	val feedback: AssignmentFeedback = Fixtures.assignmentFeedback()

	val roleDefinition = AssignmentSubmitterRoleDefinition

	@Test def initDepartment {
		GrantedRole.canDefineFor(dept) should be (true)
		val gr = GrantedRole(dept, roleDefinition)
		gr.scope should be (dept)
		gr.roleDefinition should be (roleDefinition)
	}

	@Test def initModule {
		GrantedRole.canDefineFor(module) should be (true)
		val gr = GrantedRole(module, roleDefinition)
		gr.scope should be (module)
		gr.roleDefinition should be (roleDefinition)
	}

	@Test def initAssignment {
		GrantedRole.canDefineFor(assignment) should be (true)
		val gr = GrantedRole(assignment, roleDefinition)
		gr.scope should be (assignment)
		gr.roleDefinition should be (roleDefinition)
	}

	@Test def initMember {
		GrantedRole.canDefineFor(member) should be (true)
		val gr = GrantedRole(member, roleDefinition)
		gr.scope should be (member)
		gr.roleDefinition should be (roleDefinition)
	}

	@Test(expected = classOf[IllegalArgumentException]) def initInvalid {
		GrantedRole.canDefineFor(feedback) should be (false)
		GrantedRole(feedback, roleDefinition)
	}

	@Test def build {
		val gr = GrantedRole(module, roleDefinition)
		val role = gr.build()
		role.getName should be ("AssignmentSubmitterRoleDefinition")
		role.explicitPermissionsAsList.contains((Permissions.Submission.Create, Some(module))) should be (true)
	}

	@Test def buildWithOverride {
		val gr = GrantedRole(module, roleDefinition)

		val custom = new CustomRoleDefinition
		custom.department = dept
		custom.baseRoleDefinition = roleDefinition
		custom.replacesBaseDefinition = true
		custom.name = "Custom definition"

		val noSubmit = new RoleOverride
		noSubmit.overrideType = false
		noSubmit.permission = Permissions.Submission.Create
		noSubmit.customRoleDefinition = custom

		custom.overrides.add(noSubmit)

		dept.customRoleDefinitions.add(custom)

		val role = gr.build()
		role.getName should be ("Custom definition")
		role.explicitPermissionsAsList.contains((Permissions.Submission.Create, Some(module))) should be (false)
	}

}