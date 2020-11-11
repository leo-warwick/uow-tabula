package uk.ac.warwick.tabula.services

import org.hibernate.criterion.Restrictions
import org.hibernate.sql.JoinType
import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{FiltersStudents, StudentAssociationData, StudentAssociationEntityData, TaskBenchmarking}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.ScalaRestriction.is

import scala.jdk.CollectionConverters._
import scala.collection.immutable.TreeMap

trait RelationshipServiceComponent {
  def relationshipService: RelationshipService
}

trait AutowiringRelationshipServiceComponent extends RelationshipServiceComponent {
  var relationshipService: RelationshipService = Wire[RelationshipService]
}

case class SortableAgentIdentifier(agentId: String, lastName: Option[String]) {
  val sortkey: String = lastName.getOrElse("") + agentId

  override def toString: String = sortkey
}

object SortableAgentIdentifier {
  def apply(r: StudentRelationship): SortableAgentIdentifier = SortableAgentIdentifier(r.agent, r.agentMember.map(_.lastName))

  val KeyOrdering: Ordering[SortableAgentIdentifier] = Ordering.by { a: SortableAgentIdentifier => a.sortkey }
}

/**
  * Service providing access to members and profiles.
  */
trait RelationshipService {
  def allStudentRelationshipTypes: Seq[StudentRelationshipType]

  def saveOrUpdate(relationshipType: StudentRelationshipType): Unit

  def delete(relationshipType: StudentRelationshipType): Unit

  def getStudentRelationshipTypeById(id: String): Option[StudentRelationshipType]

  def getStudentRelationshipTypeByUrlPart(urlPart: String): Option[StudentRelationshipType]

  def getStudentRelationshipTypesWithRdxType: Seq[StudentRelationshipType]

  def getStudentRelationshipById(id: String): Option[StudentRelationship]

  def saveOrUpdate(relationship: StudentRelationship): Unit

  def findCurrentRelationships(relationshipType: StudentRelationshipType, scd: StudentCourseDetails): Seq[StudentRelationship]

  def findCurrentRelationships(scd: StudentCourseDetails): Seq[StudentRelationship]

  def findFutureRelationships(relationshipType: StudentRelationshipType, scd: StudentCourseDetails): Seq[StudentRelationship]

  def findCurrentRelationships(relationshipType: StudentRelationshipType, student: StudentMember): Seq[StudentRelationship]

  def getCurrentRelationship(relationshipType: StudentRelationshipType, studentCourse: StudentCourseDetails, agent: Member): Option[StudentRelationship]

  def getCurrentRelationships(student: StudentMember, agentId: String): Seq[StudentRelationship]

  def getRelationships(relationshipType: StudentRelationshipType, studentCourseDetails: StudentCourseDetails): Seq[StudentRelationship]

  def getRelationships(relationshipType: StudentRelationshipType, student: StudentMember): Seq[StudentRelationship]

  def saveStudentRelationship(
    relationshipType: StudentRelationshipType,
    studentCourseDetails: StudentCourseDetails,
    agent: Either[Member, String],
    startDate: DateTime,
    replaces: Seq[StudentRelationship] = Seq()
  ): StudentRelationship

  def replaceStudentRelationships(
    relationshipType: StudentRelationshipType,
    studentCourseDetails: StudentCourseDetails,
    agent: Member,
    scheduledDate: DateTime
  ): StudentRelationship

  def replaceStudentRelationshipsWithPercentages(
    relationshipType: StudentRelationshipType,
    studentCourseDetails: StudentCourseDetails,
    agentsWithPercentages: Seq[(Member, JBigDecimal)]
  ): Seq[StudentRelationship]

  def listCurrentStudentRelationshipsByDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship]

  def listCurrentStudentRelationshipsByStudentOrStaffDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship]

  def listCurrentStudentRelationshipsWithMember(agent: Member): Seq[StudentRelationship]

  def listCurrentStudentRelationshipTypesWithMember(agent: Member): Seq[StudentRelationshipType]

  def listCurrentStudentRelationshipsWithMember(relationshipType: StudentRelationshipType, agent: Member): Seq[StudentRelationship]

  def listCurrentStudentRelationshipsWithMemberInDepartment(relationshipType: StudentRelationshipType, agent: Member, department: Department): Seq[StudentRelationship]

  def listAllStudentRelationshipsWithUniversityId(agentId: String): Seq[StudentRelationship]

  def listStudentsWithoutCurrentRelationship(relationshipType: StudentRelationshipType, department: Department): Seq[Member]

  def listScheduledRelationshipChanges(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship]

  def countStudentsByRelationship(relationshipType: StudentRelationshipType): Int

  def getAllCurrentRelationships(student: StudentMember): Seq[StudentRelationship]

  def getAllFutureRelationships(student: StudentMember): Seq[StudentRelationship]

  def getAllPastAndPresentRelationships(student: StudentMember): Seq[StudentRelationship]

  def getAllPastAndPresentRelationships(relationshipType: StudentRelationshipType, scd: StudentCourseDetails): Seq[StudentRelationship]

  def endStudentRelationships(relationships: Seq[StudentRelationship], endDate: DateTime): Unit

  def removeFutureStudentRelationships(relationships: Seq[StudentRelationship]): Unit

  def getStudentAssociationDataWithoutRelationship(department: Department, relationshipType: StudentRelationshipType, restrictions: Seq[ScalaRestriction] = Seq()): Seq[StudentAssociationData]

  def getStudentAssociationEntityData(department: Department, relationshipType: StudentRelationshipType, additionalEntityIds: Seq[String]): Seq[StudentAssociationEntityData]

  def listCurrentRelationshipsWithAgent(relationshipType: StudentRelationshipType, agentId: String): Seq[StudentRelationship]

  def listAgentRelationshipsByDepartment(relationshipType: StudentRelationshipType, department: Department): TreeMap[SortableAgentIdentifier, Seq[StudentRelationship]]

  def listCurrentRelationshipsGlobally(relationshipType: StudentRelationshipType): Seq[Array[Object]]
}

abstract class AbstractRelationshipService extends RelationshipService with Logging with TaskBenchmarking {

  self: RelationshipDaoComponent =>

  def saveOrUpdate(relationship: StudentRelationship): Unit = relationshipDao.saveOrUpdate(relationship)

  def allStudentRelationshipTypes: Seq[StudentRelationshipType] = relationshipDao.allStudentRelationshipTypes

  def getStudentRelationshipTypeById(id: String): Option[StudentRelationshipType] = relationshipDao.getStudentRelationshipTypeById(id)

  def getStudentRelationshipTypeByUrlPart(urlPart: String): Option[StudentRelationshipType] = relationshipDao.getStudentRelationshipTypeByUrlPart(urlPart)

  def saveOrUpdate(relationshipType: StudentRelationshipType): Unit = relationshipDao.saveOrUpdate(relationshipType)

  def delete(relationshipType: StudentRelationshipType): Unit = relationshipDao.delete(relationshipType)

  def findCurrentRelationships(relationshipType: StudentRelationshipType, scd: StudentCourseDetails): Seq[StudentRelationship] = transactional(readOnly = true) {
    relationshipDao.getCurrentRelationships(relationshipType, scd)
  }

  def findCurrentRelationships(scd: StudentCourseDetails): Seq[StudentRelationship] = transactional() {
    relationshipDao.getCurrentRelationships(scd)
  }

  def findFutureRelationships(relationshipType: StudentRelationshipType, scd: StudentCourseDetails): Seq[StudentRelationship] = transactional(readOnly = true) {
    relationshipDao.getFutureRelationships(relationshipType, scd)
  }

  def findCurrentRelationships(relationshipType: StudentRelationshipType, student: StudentMember): Seq[StudentRelationship] = transactional(readOnly = true) {
    relationshipDao.getCurrentRelationships(relationshipType, student)
  }

  def getCurrentRelationship(relationshipType: StudentRelationshipType, studentCourse: StudentCourseDetails, agent: Member): Option[StudentRelationship] = transactional(readOnly = true) {
    relationshipDao.getCurrentRelationship(relationshipType, studentCourse, agent)
  }

  def getCurrentRelationships(student: StudentMember, agentId: String): Seq[StudentRelationship] = transactional(readOnly = true) {
    relationshipDao.getCurrentRelationships(student, agentId).filter(relationshipNotPermanentlyWithdrawn)
  }

  def getAllCurrentRelationships(student: StudentMember): Seq[StudentRelationship] = transactional(readOnly = true) {
    relationshipDao.getAllCurrentRelationships(student)
  }

  def getAllFutureRelationships(student: StudentMember): Seq[StudentRelationship] = transactional(readOnly = true) {
    relationshipDao.getAllFutureRelationships(student)
  }

  def getAllPastAndPresentRelationships(student: StudentMember): Seq[StudentRelationship] = transactional(readOnly = true) {
    relationshipDao.getAllPastAndPresentRelationships(student)
  }

  def getAllPastAndPresentRelationships(relationshipType: StudentRelationshipType, scd: StudentCourseDetails): Seq[StudentRelationship] = transactional(readOnly = true) {
    relationshipDao.getAllPastAndPresentRelationships(relationshipType, scd)
  }

  def getRelationships(relationshipType: StudentRelationshipType, studentCourseDetails: StudentCourseDetails): Seq[StudentRelationship] = transactional(readOnly = true) {
    relationshipDao.getRelationshipsByCourseDetails(relationshipType, studentCourseDetails)
  }

  def getRelationships(relationshipType: StudentRelationshipType, student: StudentMember): Seq[StudentRelationship] = transactional(readOnly = true) {
    relationshipDao.getRelationshipsByTarget(relationshipType, student)
  }

  def saveStudentRelationship(
    relationshipType: StudentRelationshipType,
    studentCourseDetails: StudentCourseDetails,
    agent: Either[Member, String],
    startDate: DateTime,
    replaces: Seq[StudentRelationship] = Seq()
  ): StudentRelationship = transactional() {
    /** For each SCD-Agent pair:
      * if there is current relationship that ends after the specified startDate, do nothing (return that relationship)
      * else if there is a future relationship, update the startDate of the existing future relationship
      * else create a new relationship with the specified startDate
      */
    val currentRelationships = findCurrentRelationships(relationshipType, studentCourseDetails)
    val futureRelationships = findFutureRelationships(relationshipType, studentCourseDetails)

    val agentId = agent.fold(a => a.universityId, a => a)

    val relationship = {
      val existingCurrentRelationship = currentRelationships.find(_.agent.contains(agentId))
      val existingFutureRelationship = futureRelationships.find(_.agent.contains(agentId))
      if (existingCurrentRelationship.isDefined && (existingCurrentRelationship.get.endDate == null || existingCurrentRelationship.get.endDate.isAfter(startDate))) {
        // Return the existing relationship
        existingCurrentRelationship.get.replacesRelationships.addAll(replaces.asJava)
        replaces.foreach(_.replacedBy = existingCurrentRelationship.get)
        existingCurrentRelationship.get
      } else if (existingFutureRelationship.isDefined) {
        // Update the scheduled date
        existingFutureRelationship.get.startDate = startDate
        existingFutureRelationship.get.replacesRelationships.addAll(replaces.asJava)
        replaces.foreach(_.replacedBy = existingFutureRelationship.get)
        existingFutureRelationship.get
      } else {
        // Create the new one
        val newRelationship = agent match {
          case Left(agentMember) => StudentRelationship(agentMember, relationshipType, studentCourseDetails, startDate)
          case Right(external) => ExternalStudentRelationship(external, relationshipType, studentCourseDetails, startDate)
        }
        newRelationship.replacesRelationships.addAll(replaces.asJava)
        replaces.foreach(_.replacedBy = newRelationship)
        newRelationship
      }

    }

    (Seq(relationship) ++ replaces).foreach(relationshipDao.saveOrUpdate)

    relationship
  }

  def saveStudentRelationshipsWithPercentages(
    relationshipType: StudentRelationshipType,
    studentCourseDetails: StudentCourseDetails,
    agents: Seq[(Member, JBigDecimal)]
  ): Seq[StudentRelationship] = transactional() {
    val currentRelationships = findCurrentRelationships(relationshipType, studentCourseDetails)
    val existingRelationships = currentRelationships.filter { rel => rel.agentMember.exists { agent => agents.map(_._1).contains(agent)
    }
    }
    val agentsToCreate = agents.filterNot { case (agent, _) => currentRelationships.exists(_.agentMember.contains(agent)) }

    agentsToCreate.map { case (agent, percentage) =>
      // create the new one
      val newRelationship = StudentRelationship(agent, relationshipType, studentCourseDetails, DateTime.now)
      newRelationship.percentage = percentage
      relationshipDao.saveOrUpdate(newRelationship)
      newRelationship
    } ++ existingRelationships
  }

  // end any existing relationships of the same type for this student, then save the new one
  def replaceStudentRelationships(
    relationshipType: StudentRelationshipType,
    studentCourseDetails: StudentCourseDetails,
    agent: Member,
    scheduledDate: DateTime
  ): StudentRelationship = transactional() {
    val currentRelationships = findCurrentRelationships(relationshipType, studentCourseDetails)
    val (existingRelationships, relationshipsToEnd) = currentRelationships.partition { rel => rel.agentMember.contains(agent) }

    // Don't need to do anything with existingRelationships, but need to handle the others

    // End all relationships for agents not passed in
    endStudentRelationships(relationshipsToEnd, scheduledDate)

    // Save new relationship if agent that didn't already exist
    if (existingRelationships.isEmpty) {
      saveStudentRelationship(relationshipType, studentCourseDetails, Left(agent), scheduledDate)
    } else {
      existingRelationships.head
    }
  }

  def endStudentRelationships(relationships: Seq[StudentRelationship], endDate: DateTime): Unit = {
    relationships.foreach {
      rel => {
        rel.endDate = endDate
        saveOrUpdate(rel)
      }
    }
  }

  def removeFutureStudentRelationships(relationships: Seq[StudentRelationship]): Unit = {
    relationships.filter(r => r.startDate != null && r.startDate.isAfterNow).foreach { relationship =>
      relationship.replacesRelationships.asScala.foreach { rel =>
        rel.replacedBy = null
        rel.endDate = null
      }
      relationship.replacesRelationships.clear()
      relationshipDao.delete(relationship)
    }
  }

  def replaceStudentRelationshipsWithPercentages(
    relationshipType: StudentRelationshipType,
    studentCourseDetails: StudentCourseDetails,
    agents: Seq[(Member, JBigDecimal)]
  ): Seq[StudentRelationship] = transactional() {
    val currentRelationships = findCurrentRelationships(relationshipType, studentCourseDetails)
    val (existingRelationships, relationshipsToEnd) = currentRelationships.partition {
      rel => rel.agentMember.exists { agent => agents.map(_._1).contains(agent)
      }
    }

    val agentsToAdd = agents.filterNot { case (agent, percentage) => existingRelationships.exists(_.agentMember.contains(agent)) }

    // Find existing relationships with the wrong percentage
    existingRelationships.foreach { rel =>
      val percentage = agents.find { case (agent, _) => rel.agentMember.contains(agent) }.get._2
      if (rel.percentage != percentage) {
        rel.percentage = percentage
        relationshipDao.saveOrUpdate(rel)
      }
    }

    // Don't need to do anything with existingRelationships, but need to handle the others

    // End all relationships for agents not passed in
    relationshipsToEnd.foreach(_.endDate = DateTime.now)

    // Save new relationships for agents that don't already exist
    saveStudentRelationshipsWithPercentages(relationshipType, studentCourseDetails, agentsToAdd)
  }

  def relationshipDepartmentFilterMatches(department: Department)(rel: StudentRelationship): Boolean =
    rel.studentMember.exists(studentDepartmentFilterMatches(department))

  def relationshipNotPermanentlyWithdrawn(rel: StudentRelationship): Boolean = {
    Option(rel.studentCourseDetails).exists(
      scd => !scd.permanentlyWithdrawn && !scd.stale)
  }

  def studentDepartmentFilterMatches(department: Department)(member: StudentMember): Boolean = department.filterRule.matches(member, Option(department), None)

  def studentNotPermanentlyWithdrawn(member: StudentMember): Boolean = !member.permanentlyWithdrawn

  def studentDepartmentMatchesAndExpectedToHaveRelationship(relationshipType: StudentRelationshipType, department: Department)(member: StudentMember): Boolean = {
    department.filterRule.matches(member, Option(department), None) &&
      member.freshStudentCourseDetails
        .filter(scd => Option(scd.currentRoute).exists(route => route.adminDepartment == department || route.adminDepartment == department.rootDepartment)) // there needs to be an SCD for the right department ...
        .filter(_.mostSignificant)
        .filter(!_.permanentlyWithdrawn) // that's not permanently withdrawn ...
        .exists(relationshipType.isExpected) // and has a course of the type that is expected to have this kind of relationship
  }

  def listCurrentStudentRelationshipsByDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship] = transactional(readOnly = true) {
    benchmarkTask("listStudentRelationshipsByDepartment") {
      relationshipDao.getCurrentRelationshipsByDepartment(relationshipType, department.rootDepartment)
        .filter(relationshipDepartmentFilterMatches(department))
        .filter(relationshipNotPermanentlyWithdrawn)
    }
  }

  def listCurrentStudentRelationshipsByStudentOrStaffDepartment(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship] = transactional(readOnly = true) {
    benchmarkTask("listStudentRelationshipsByStudentOrStaffDepartment") {
      relationshipDao.getCurrentRelationshipsByStudentOrStaffDepartment(relationshipType, department.rootDepartment)
        .filter(relationshipDepartmentFilterMatches(department))
        .filter(relationshipNotPermanentlyWithdrawn)
    }
  }

  def listCurrentStudentRelationshipsWithMember(agent: Member): Seq[MemberStudentRelationship] = transactional(readOnly = true) {
    relationshipDao.getCurrentRelationshipsForAgent(agent.universityId)
      .filter(relationshipNotPermanentlyWithdrawn)
  }

  def listCurrentStudentRelationshipTypesWithMember(agent: Member): Seq[StudentRelationshipType] = transactional(readOnly = true) {
    relationshipDao.getCurrentRelationshipTypesByAgent(agent.universityId)
  }

  def listCurrentStudentRelationshipsWithMember(relationshipType: StudentRelationshipType, agent: Member): Seq[MemberStudentRelationship] = transactional(readOnly = true) {
    relationshipDao.getCurrentRelationshipsByAgent(relationshipType, agent.universityId)
      .filter(relationshipNotPermanentlyWithdrawn)
  }

  def listCurrentStudentRelationshipsWithMemberInDepartment(relationshipType: StudentRelationshipType, agent: Member, department: Department): Seq[MemberStudentRelationship] = transactional(readOnly = true) {
    relationshipDao.getCurrentRelationshipsByAgent(relationshipType, agent.universityId)
      .filter(relationshipNotPermanentlyWithdrawn)
      .filter(r => r.studentCourseDetails.department == department.rootDepartment)
  }

  def listAllStudentRelationshipsWithUniversityId(agentId: String): Seq[MemberStudentRelationship] = transactional(readOnly = true) {
    relationshipDao.getCurrentRelationshipsForAgent(agentId)
      .filter(relationshipNotPermanentlyWithdrawn)
  }

  def listStudentsWithoutCurrentRelationship(relationshipType: StudentRelationshipType, department: Department): Seq[StudentMember] = transactional(readOnly = true) {
    benchmarkTask("listStudentsWithoutCurrentRelationship") {
      relationshipDao.getStudentsWithoutCurrentRelationshipByDepartment(relationshipType, department.rootDepartment)
        .filter(studentDepartmentMatchesAndExpectedToHaveRelationship(relationshipType, department))
    }
  }

  def listScheduledRelationshipChanges(relationshipType: StudentRelationshipType, department: Department): Seq[StudentRelationship] = transactional(readOnly = true) {
    benchmarkTask("listScheduledRelationshipChanges") {
      relationshipDao.getScheduledRelationshipChangesByDepartment(relationshipType, department.rootDepartment)
        .filter(rel =>
          department.filterRule.matches(rel.studentCourseDetails.student, Option(department), Some(rel.studentCourseDetails)) &&
            Option(rel.studentCourseDetails.currentRoute).exists(route => route.adminDepartment == department || route.adminDepartment == department.rootDepartment)
        )
    }
  }

  def countStudentsByRelationship(relationshipType: StudentRelationshipType): Int = transactional(readOnly = true) {
    relationshipDao.countStudentsByRelationship(relationshipType).intValue
  }

  def getStudentRelationshipTypesWithRdxType: Seq[StudentRelationshipType] = {
    allStudentRelationshipTypes.filter(_.defaultRdxType != null)
  }

  def getStudentRelationshipById(id: String): Option[StudentRelationship] = relationshipDao.getStudentRelationshipById(id)

  /**
    * Students enrolled in the department and matching the department filter
    */
  private def departmentRestrictions(department: Department): Iterable[ScalaRestriction] = {
    ScalaRestriction.is(
      "mostSignificantCourse.department", department.rootDepartment,
      FiltersStudents.AliasPaths("mostSignificantCourse"): _*
    ) ++ department.filterRule.restriction(FiltersStudents.AliasPaths)
  }

  private val notPermanentlyWithdrawnRestriction = ScalaRestriction.custom(
    Restrictions.not(Restrictions.like("mostSignificantCourse.statusOnRoute.code", "P%")),
    FiltersStudents.AliasPaths("statusOnRoute"): _*
  )

  private val notDeceasedRestriction = is("deceased", false)

  def getStudentAssociationDataWithoutRelationship(department: Department, relationshipType: StudentRelationshipType, restrictions: Seq[ScalaRestriction] = Seq()): Seq[StudentAssociationData] = transactional(readOnly = true) {
    benchmarkTask("getStudentAssociationDataWithoutRelationship") {
      val allRestrictions = departmentRestrictions(department) ++ notPermanentlyWithdrawnRestriction ++ notDeceasedRestriction ++
        // For this relationship type and not expired, but null
        ScalaRestriction.custom(
          Restrictions.isNull("relationshipsOfType.id"),
          "mostSignificantCourse" -> AliasAndJoinType("mostSignificantCourse"),
          "mostSignificantCourse.allRelationships" ->
            AliasAndJoinType("relationshipsOfType", JoinType.LEFT_OUTER_JOIN, Some(Restrictions.and(
              Restrictions.eq("relationshipType", relationshipType),
              Restrictions.or(
                Restrictions.isNull("endDate"),
                Restrictions.gt("endDate", DateTime.now)
              )
            )))
        ) ++
        // Plus whatever was passed in
        restrictions

      relationshipDao.getStudentAssociationData(allRestrictions)
        // Only return students who are expected to have this type of relationship
        .filter(student => relationshipType.displayIfEmpty(student.courseType, department))
    }
  }

  def getStudentAssociationEntityData(department: Department, relationshipType: StudentRelationshipType, additionalEntityIds: Seq[String]): Seq[StudentAssociationEntityData] = transactional(readOnly = true) {
    benchmarkTask("getStudentAssociationEntityData") {
      val studentData = relationshipDao.getStudentAssociationData(departmentRestrictions(department) ++ notPermanentlyWithdrawnRestriction)
      relationshipDao.getStudentAssociationEntityData(department, relationshipType, studentData, additionalEntityIds)
    }
  }

  def listCurrentRelationshipsWithAgent(relationshipType: StudentRelationshipType, agentId: String): Seq[StudentRelationship] = {
    benchmarkTask("listCurrentRelationshipsWithAgent") {
      relationshipDao.listCurrentRelationshipsWithAgent(relationshipType, agentId)
    }
  }

  def listAgentRelationshipsByDepartment(relationshipType: StudentRelationshipType, department: Department): TreeMap[SortableAgentIdentifier, Seq[StudentRelationship]] = {
    // all students with a department X or a relationship agent in department X
    val unsortedRelationships = listCurrentStudentRelationshipsByStudentOrStaffDepartment(relationshipType, department)

    // remove the dups
    val unsortedAgentRelationships = unsortedRelationships
      // TAB-2750 treat relationships between the same agent and student COURSE as identical
      .groupBy { rel => (rel.agent, rel.studentCourseDetails) }
      .map { case (_, rels) => rels.maxBy { rel => rel.startDate.getMillis } }
      .toSeq

    // group into map by agent lastname, or id if the lastname is unavailable
    val groupedAgentRelationships = unsortedAgentRelationships.groupBy(r => SortableAgentIdentifier(r))

    // alpha sort by constructing a TreeMap
    TreeMap(groupedAgentRelationships.toSeq: _*)(SortableAgentIdentifier.KeyOrdering)
  }

  def listCurrentRelationshipsGlobally(relationshipType: StudentRelationshipType): Seq[Array[Object]] = {
    relationshipDao.getAllCurrentRelationshipsOfType(relationshipType)
  }
}

@Service("relationshipService")
class RelationshipServiceImpl
  extends AbstractRelationshipService
    with AutowiringRelationshipDaoComponent
