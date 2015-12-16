package uk.ac.warwick.tabula.data.commands

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.{MemberStudentRelationship, StudentMember, StudentRelationship}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, RelationshipService}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class RelationshipFixtureCommand extends CommandInternal[MemberStudentRelationship] {
	this: TransactionalComponent with SessionComponent =>

	val memberDao = Wire[MemberDao]
	val relationshipDao = Wire[RelationshipDao]
	val relationshipService = Wire[RelationshipService]
	var agent:String = _
	var studentUniId:String = _
	var relationshipType:String = "tutor"

	protected def applyInternal() =
		transactional() {
			val relType = relationshipService.getStudentRelationshipTypeByUrlPart(relationshipType).get
			val student = memberDao.getByUniversityId(studentUniId).get match {
				case x: StudentMember => x
				case _ => throw new RuntimeException(s"$studentUniId could not be resolved to a student member")
			}
			val existing = relationshipDao.getRelationshipsByAgent(relType, agent).find (_.studentId == studentUniId)

			val modifications = existing match {
				case Some(existingRel) =>{
					existingRel.endDate = null // make sure it hasn't expired
					existingRel
				}
				case None =>{
					val relationship = StudentRelationship(memberDao.getByUniversityId(agent).get, relType, student)
					relationship.startDate = DateTime.now()
					relationship
				}
			}
			session.saveOrUpdate(modifications)
			modifications
		}
}

object RelationshipFixtureCommand{
	def apply()={
		new RelationshipFixtureCommand
			with ComposableCommand[MemberStudentRelationship]
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringTransactionalComponent
			with Daoisms
			with Unaudited
			with PubliclyVisiblePermissions
	}
}
