package uk.ac.warwick.tabula.dev.web.commands

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.{MemberStudentRelationship, StudentMember, StudentRelationship}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, RelationshipService}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class RelationshipFixtureCommand extends CommandInternal[MemberStudentRelationship] {
	this: TransactionalComponent with SessionComponent=>

	val memberDao: MemberDao = Wire[MemberDao]
	val relationshipDao: RelationshipDao = Wire[RelationshipDao]
	val relationshipService: RelationshipService = Wire[RelationshipService]
	var agent:String = _
	var studentUniId:String = _
	var relationshipType:String = "tutor"

	protected def applyInternal(): MemberStudentRelationship =
		transactional() {
			val relType = relationshipService.getStudentRelationshipTypeByUrlPart(relationshipType).get
			val student = memberDao.getByUniversityId(studentUniId).get match {
				case x: StudentMember => x
				case _ => throw new RuntimeException(s"$studentUniId could not be resolved to a student member")
			}
			val existing = relationshipDao.getCurrentRelationshipsByAgent(relType, agent).find (_.studentId == studentUniId)

			val modifications = existing match {
				case Some(existingRel) =>
					existingRel.endDate = null // make sure it hasn't expired
					existingRel
				case None =>
					val relationship = StudentRelationship(memberDao.getByUniversityId(agent).get, relType, student, DateTime.now)
					relationship
			}
			session.saveOrUpdate(modifications)
			modifications
		}
}

object RelationshipFixtureCommand{
	def apply(): RelationshipFixtureCommand with ComposableCommand[MemberStudentRelationship] with AutowiringModuleAndDepartmentServiceComponent with Daoisms with AutowiringTransactionalComponent with Unaudited with PubliclyVisiblePermissions ={
		new RelationshipFixtureCommand
			with ComposableCommand[MemberStudentRelationship]
			with AutowiringModuleAndDepartmentServiceComponent
			with Daoisms
			with AutowiringTransactionalComponent
			with Unaudited
			with PubliclyVisiblePermissions
	}
}
