package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType, StudentMember}
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles._
import uk.ac.warwick.tabula.services.{RelationshipServiceComponent, AutowiringRelationshipServiceComponent}
import uk.ac.warwick.tabula.commands.TaskBenchmarking

@Component
class StudentRelationshipAgentRoleProvider extends RoleProvider
	with AutowiringRelationshipServiceComponent
	with CustomRolesForAdminDepartments
	with TaskBenchmarking {


	def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role] = benchmarkTask("Get roles for StudentRelationshipAgentRoleProvider "){
		scope match {
			case student: StudentMember =>
				relationshipService
					// lists all of the tutors current relationships to this student (expired relationships and withdrawn students ignored)
					.getCurrentRelationships(student, user.apparentUser.getWarwickId)
					.toStream
					.filterNot(_.explicitlyTerminated)
					// gather all of the distinct relationship types
					.map { rel => rel.relationshipType }
					.distinct
					.map { relationshipType =>
					val custom = customRoles(student, relationshipType, StudentRelationshipAgentRoleDefinition(relationshipType))
					custom.headOption.getOrElse { StudentRelationshipAgent(student, relationshipType) }
				}

			// We don't need to check for the StudentRelationshipAgent role on any other scopes
			case _ => Stream.empty
		}
	}

	def rolesProvided = Set(classOf[StudentRelationshipAgent])

}

@Component
class HistoricStudentRelationshipAgentRoleProvider extends RoleProvider
	with AutowiringRelationshipServiceComponent
	with CustomRolesForAdminDepartments {

	override def getRolesFor(user: CurrentUser, scope: PermissionsTarget) : Stream[Role] = scope match {
		case student: StudentMember =>
			relationshipService
				.getAllPastAndPresentRelationships(student)
				.toStream
				.filterNot(_.explicitlyTerminated)
				.filter(_.agent == user.apparentUser.getWarwickId)
				.map(_.relationshipType).distinct
				.map(relType => {
				// for each type of relationship return the first department override we fgetRolesForind or the inbuild role
				val custom = customRoles(student, relType, HistoricStudentRelationshipAgentRoleDefinition(relType))
				custom.headOption.getOrElse(HistoricStudentRelationshipAgent(student, relType))
			})

		// Member is the only valid scope for this Role
		case _ => Stream.empty
	}

	override def rolesProvided = Set(classOf[HistoricStudentRelationshipAgent])

}

trait CustomRolesForAdminDepartments {

	this : RoleProvider with RelationshipServiceComponent =>

	// departments to check for custom roles
	private def studentsAdminDepartments(student: StudentMember): Seq[Department] = {
		student.mostSignificantCourseDetails.map(scd => {
			Option(scd.latestStudentCourseYearDetails.enrolmentDepartment).flatMap(_.subDepartmentsContaining(student).lastOption).toSeq ++
			Option(scd.currentRoute).flatMap(r => Option(r.adminDepartment)).flatMap(_.subDepartmentsContaining(student).lastOption).toSeq
		}).getOrElse(Nil).distinct.sortBy(_.code)
	}

	// returns department overrides for the specified definition for all of the students admin departments
	def customRoles(student: StudentMember, relType: StudentRelationshipType, definition: RoleDefinition): Seq[Role] =
		for {
			department <- studentsAdminDepartments(student)
			customRole <- customRoleFor(department)(definition, student)
		} yield customRole

}