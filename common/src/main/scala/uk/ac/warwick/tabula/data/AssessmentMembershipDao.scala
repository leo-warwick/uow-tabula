package uk.ac.warwick.tabula.data

import javax.persistence.EntityManager
import org.hibernate.FetchMode
import org.hibernate.`type`.StandardBasicTypes
import org.hibernate.criterion.Order._
import org.hibernate.criterion.Restrictions._
import org.hibernate.criterion.{Order, Projections, Restrictions}
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.ManualMembershipInfo
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

trait AssessmentMembershipDaoComponent {
  val membershipDao: AssessmentMembershipDao
}

trait AutowiringAssessmentMembershipDaoComponent extends AssessmentMembershipDaoComponent {
  val membershipDao: AssessmentMembershipDao = Wire[AssessmentMembershipDao]
}

/**
  * TODO Rename all of this to be less Assignment-centric
  */
trait AssessmentMembershipDao {
  def find(assignment: AssessmentComponent): Option[AssessmentComponent]

  def find(group: UpstreamAssessmentGroup, eagerLoad: Boolean): Option[UpstreamAssessmentGroup]

  def find(group: AssessmentGroup): Option[AssessmentGroup]

  def save(group: AssessmentGroup): Unit

  def save(assignment: AssessmentComponent): AssessmentComponent

  def save(group: UpstreamAssessmentGroup): Unit

  def save(member: UpstreamAssessmentGroupMember): Unit

  def delete(group: AssessmentGroup): Unit

  def delete(assessmentComponent: AssessmentComponent): Unit

  def delete(upstreamAssessmentGroup: UpstreamAssessmentGroup): Unit

  def getAssessmentGroup(id: String): Option[AssessmentGroup]

  def getUpstreamAssessmentGroup(id: String): Option[UpstreamAssessmentGroup]

  def getAssessmentComponent(id: String): Option[AssessmentComponent]

  def getAssessmentComponent(group: UpstreamAssessmentGroup): Option[AssessmentComponent]

  def getUpstreamAssessmentGroups(module: Module, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup]

  def getUpstreamAssessmentGroups(student: StudentMember, academicYear: AcademicYear, resitOnly: Boolean): Seq[UpstreamAssessmentGroup]

  /**
    * Get all AssessmentComponents that appear to belong to this module.
    *
    * Typically used to provide possible candidates to link to an app assignment,
    * in conjunction with #getUpstreamAssessmentGroups.
    */
  def getAssessmentComponents(module: Module, inUseOnly: Boolean): Seq[AssessmentComponent]

  def getAssessmentComponents(department: Department, includeSubDepartments: Boolean, inUseOnly: Boolean): Seq[AssessmentComponent]

  def getAssessmentComponents(department: Department, includeSubDepartments: Boolean, assessmentType: Option[AssessmentType], withExamPapersOnly: Boolean, inUseOnly: Boolean): Seq[AssessmentComponent]

  def getAssessmentComponents(moduleCode: String, inUseOnly: Boolean): Seq[AssessmentComponent]

  def getAssessmentComponents(moduleCodes: Set[String], inUseOnly: Boolean): Seq[AssessmentComponent]

  def getAssessmentComponents(department: Department, ids: Seq[String]): Seq[AssessmentComponent]

  def getAssessmentComponentsByPaperCode(department: Department, paperCodes: Seq[String]): Map[String, Seq[AssessmentComponent]]

  def getAllAssessmentComponents(academicYears: Seq[AcademicYear]): Seq[AssessmentComponent]

  def getAssignmentsForAssessmentGroups(keys: Seq[UpstreamAssessmentGroupKey]): Seq[Assignment]

  /**
    * Get all assessment groups that can serve this assignment this year.
    * Should return as many groups as there are distinct OCCURRENCE values for a given
    * assessment group code, which most of the time is just 1.
    */
  def getUpstreamAssessmentGroups(component: AssessmentComponent, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup]

  def getCurrentUpstreamAssessmentGroupMembers(component: AssessmentComponent, academicYear: AcademicYear): Seq[UpstreamAssessmentGroupMember]

  def getCurrentUpstreamAssessmentGroupMembers(components: Seq[AssessmentComponent], academicYear: AcademicYear): Seq[UpstreamAssessmentGroupMember]

  def getCurrentUpstreamAssessmentGroupMembers(uagid: String): Seq[UpstreamAssessmentGroupMember]

  def getUpstreamAssessmentGroups(registration: ModuleRegistration, allAssessmentGroups: Boolean, eagerLoad: Boolean): Seq[UpstreamAssessmentGroup]

  def getUpstreamAssessmentGroups(academicYears: Seq[AcademicYear]): Seq[UpstreamAssessmentGroup]

  def getUpstreamAssessmentGroups(academicYear: AcademicYear, moduleCode: String): Seq[UpstreamAssessmentGroup]

  def getUpstreamAssessmentGroupsNotIn(ids: Seq[String], academicYears: Seq[AcademicYear]): Seq[String]

  def getUpstreamAssessmentGroupInfo(groups: Seq[AssessmentGroup], academicYear: AcademicYear): Seq[UpstreamAssessmentGroupInfo]

  def getUpstreamAssessmentGroupInfoForComponents(components: Seq[AssessmentComponent], academicYear: AcademicYear): Seq[UpstreamAssessmentGroupInfo]

  def emptyMembers(groupsToEmpty: Seq[String]): Int

  def countPublishedFeedback(assignment: Assignment): Int

  def countFullFeedback(assignment: Assignment): Int

  /**
    * Get SITS enrolled assignments/small group sets *only* - doesn't include any assignments where someone
    * has modified the members group. Also doesn't take into account assignments where the
    * user has been manually excluded. AssignmentMembershipService.getEnrolledAssignments
    * takes this into account.
    */
  def getSITSEnrolledAssignments(user: User, academicYear: Option[AcademicYear]): Seq[Assignment]

  def getSITSEnrolledSmallGroupSets(user: User): Seq[SmallGroupSet]

  def save(gb: GradeBoundary): Unit

  def deleteGradeBoundaries(marksCode: String): Unit

  def getGradeBoundaries(marksCode: String): Seq[GradeBoundary]

  def getGradeBoundaries(marksCode: String, process: GradeBoundaryProcess, attempt: Int): Seq[GradeBoundary]

  def getPassMark(marksCode: String, process: GradeBoundaryProcess, attempt: Int): Option[Int]

  def departmentsManualMembership(department: Department, academicYear: AcademicYear): ManualMembershipInfo

  def departmentsWithManualAssessmentsOrGroups(academicYear: AcademicYear): Seq[DepartmentWithManualUsers]

  def allScheduledExams(examProfileCodes: Seq[String]): Seq[AssessmentComponentExamSchedule]

  def findScheduledExamBySlotSequence(examProfileCode: String, slotId: String, sequence: String, locationSequence: String): Option[AssessmentComponentExamSchedule]

  def findScheduledExams(component: AssessmentComponent, academicYear: Option[AcademicYear]): Seq[AssessmentComponentExamSchedule]

  def save(schedule: AssessmentComponentExamSchedule): Unit

  def delete(schedule: AssessmentComponentExamSchedule): Unit

  def allVariableAssessmentWeightingRules: Seq[VariableAssessmentWeightingRule]

  def getVariableAssessmentWeightingRules(moduleCodeWithCats: String, assessmentGroup: String): Seq[VariableAssessmentWeightingRule]

  def save(rule: VariableAssessmentWeightingRule): Unit

  def delete(rule: VariableAssessmentWeightingRule): Unit
}

@Repository
class AssessmentMembershipDaoImpl extends AssessmentMembershipDao with Daoisms with Logging {

  def getSITSEnrolledAssignments(user: User, academicYear: Option[AcademicYear]): Seq[Assignment] = {
    val query =
      session.newQuery[Assignment](
        s"""select distinct a
        from Assignment a
          join a.assessmentGroups ag
          join ag.assessmentComponent ac
          join UpstreamAssessmentGroup uag
            on ac.assessmentGroup = uag.assessmentGroup and
               ac.moduleCode = uag.moduleCode and
               ac.sequence = uag.sequence and
               uag.occurrence = ag.occurrence and
               uag.academicYear = a.academicYear
          join uag.members uagms
          join StudentMember sm
            on sm.universityId = uagms.universityId
          join sm.studentCourseDetails scd
          join scd.studentCourseYearDetails scyd with scyd.academicYear = a.academicYear
        where
          scd.student.universityId = :universityId and
          scd.statusOnCourse.code not like 'P%' and
          ${if (academicYear.nonEmpty) "a.academicYear = :academicYear and" else ""}
          ((a.resitAssessment = true and uagms.assessmentType = 'Reassessment') or a.resitAssessment = false) and
          a.deleted = false and a._hiddenFromStudents = false""")
        .setString("universityId", user.getWarwickId)

    academicYear.foreach { year => query.setParameter("academicYear", year) }

    query.seq
  }

  def getSITSEnrolledSmallGroupSets(user: User): Seq[SmallGroupSet] =
    session.newQuery[SmallGroupSet](
      """select sgs
			from SmallGroupSet sgs
        join sgs.assessmentGroups ag
        join ag.assessmentComponent ac
        join UpstreamAssessmentGroup uag
          on ac.assessmentGroup = uag.assessmentGroup and
             ac.moduleCode = uag.moduleCode and
             ac.sequence = uag.sequence and
             uag.occurrence = ag.occurrence and
             uag.academicYear = sgs.academicYear
        join uag.members uagms
			where
        uagms.universityId = :universityId and
				sgs.deleted = false and sgs.archived = false""")
      .setString("universityId", user.getWarwickId)
      .distinct.seq

  /**
    * Tries to find an identical AssessmentComponent in the database, based on the
    * fact that moduleCode and sequence uniquely identify the assignment.
    */
  def find(assignment: AssessmentComponent): Option[AssessmentComponent] =
    session.newCriteria[AssessmentComponent]
      .add(is("moduleCode", assignment.moduleCode))
      .add(is("sequence", assignment.sequence))
      .uniqueResult

  def find(group: UpstreamAssessmentGroup, eagerLoad: Boolean): Option[UpstreamAssessmentGroup] = {
    val criteria = session.newCriteria[UpstreamAssessmentGroup]
      .add(is("assessmentGroup", group.assessmentGroup))
      .add(is("academicYear", group.academicYear))
      .add(is("moduleCode", group.moduleCode))
      .add(is("occurrence", group.occurrence))
      .add(is("sequence", group.sequence))

    if (eagerLoad) {
      criteria.setFetchMode("members", FetchMode.JOIN).distinct
    }

    criteria.uniqueResult
  }

  def find(group: AssessmentGroup): Option[AssessmentGroup] = {
    if (group.assignment == null && group.smallGroupSet == null && group.exam == null) None
    else {
      val criteria = session.newCriteria[AssessmentGroup]
        .add(is("assessmentComponent", group.assessmentComponent))
        .add(is("occurrence", group.occurrence))

      if (group.assignment != null) {
        criteria.add(is("assignment", group.assignment))
      } else {
        criteria.add(is("smallGroupSet", group.smallGroupSet))
      }

      criteria.uniqueResult
    }
  }

  def save(group: AssessmentGroup): Unit = session.saveOrUpdate(group)

  def save(assignment: AssessmentComponent): AssessmentComponent =
    find(assignment)
      .map { existing =>
        if (existing.needsUpdatingFrom(assignment)) {
          existing.copyFrom(assignment)
          session.update(existing)
        }
        existing
      }
      .getOrElse {
        session.save(assignment)
        assignment
      }

  def save(group: UpstreamAssessmentGroup): Unit =
    find(group, eagerLoad = false) match {
      case Some(existing) if existing.deadline == group.deadline => // Do nothing
      case Some(outdated) =>
        outdated.deadline = group.deadline
        session.update(outdated)

      case _ => session.save(group)
    }

  def save(member: UpstreamAssessmentGroupMember): Unit = session.saveOrUpdate(member)


  def getAssessmentGroup(id: String): Option[AssessmentGroup] = getById[AssessmentGroup](id)

  def getUpstreamAssessmentGroup(id: String): Option[UpstreamAssessmentGroup] = getById[UpstreamAssessmentGroup](id)

  def delete(group: AssessmentGroup): Unit = {
    if (group.assignment != null) group.assignment.assessmentGroups.remove(group)
    if (group.smallGroupSet != null) group.smallGroupSet.assessmentGroups.remove(group)
    session.delete(group)
    session.flush()
  }

  def delete(assessmentComponent: AssessmentComponent): Unit = {
    session.delete(assessmentComponent)
    session.flush()
  }

  def delete(upstreamAssessmentGroup: UpstreamAssessmentGroup): Unit = {
    session.delete(upstreamAssessmentGroup)
    session.flush()
  }

  def getAssessmentComponent(id: String): Option[AssessmentComponent] = getById[AssessmentComponent](id)

  def getAssessmentComponent(group: UpstreamAssessmentGroup): Option[AssessmentComponent] = {
    session.newCriteria[AssessmentComponent]
      .add(is("moduleCode", group.moduleCode))
      .add(is("assessmentGroup", group.assessmentGroup))
      .add(is("sequence", group.sequence))
      .uniqueResult
  }

  def getUpstreamAssessmentGroups(module: Module, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup] = {
    session.newCriteria[UpstreamAssessmentGroup]
      .add(Restrictions.like("moduleCode", module.code.toUpperCase + "-%"))
      .add(is("academicYear", academicYear))
      .addOrder(Order.asc("assessmentGroup"))
      .addOrder(Order.asc("sequence"))
      .seq
  }

  def getUpstreamAssessmentGroups(student: StudentMember, academicYear: AcademicYear, resitOnly: Boolean): Seq[UpstreamAssessmentGroup] = {
    val criteria = session.newCriteria[UpstreamAssessmentGroup]
      .createAlias("members", "member")
      .add(is("member.universityId", student.universityId))

    if (resitOnly) {
      criteria.add(is("member.assessmentType", UpstreamAssessmentGroupMemberAssessmentType.Reassessment))
    }

    criteria.add(is("academicYear", academicYear))
      .addOrder(Order.asc("moduleCode"))
      .addOrder(Order.asc("assessmentGroup"))
      .addOrder(Order.asc("sequence"))
      .seq
  }


  /** Just gets components of type Assignment for this module, not all components. */
  def getAssessmentComponents(module: Module, inUseOnly: Boolean): Seq[AssessmentComponent] = {
    val c = session.newCriteria[AssessmentComponent]
      .add(Restrictions.like("moduleCode", module.code.toUpperCase + "-%"))
      .addOrder(Order.asc("sequence"))

    if (inUseOnly) {
      c.add(is("inUse", true))
    }

    c.seq
  }

  // TAB-2676 Include modules in sub-departments optionally
  private def modules(d: Department): Seq[Module] = d.modules.asScala.toSeq

  private def modulesIncludingSubDepartments(d: Department): Seq[Module] =
    modules(d) ++ d.children.asScala.flatMap(modulesIncludingSubDepartments)

  def getAssessmentComponents(department: Department, includeSubDepartments: Boolean, inUseOnly: Boolean): Seq[AssessmentComponent] = {
    val deptModules =
      if (includeSubDepartments) modulesIncludingSubDepartments(department)
      else modules(department)

    if (deptModules.isEmpty) Nil
    else {
      val components = safeInSeq(() => {
        val c = session.newCriteria[AssessmentComponent]

        if (inUseOnly) {
          c.add(is("inUse", true))
        }

        c
          .addOrder(asc("moduleCode"))
          .addOrder(asc("sequence"))
      }, "module", deptModules)
      components.sortBy { c =>
        (c.moduleCode, c.sequence)
      }
    }
  }

  def getAssessmentComponents(department: Department, includeSubDepartments: Boolean, assessmentType: Option[AssessmentType], withExamPapersOnly: Boolean, inUseOnly: Boolean): Seq[AssessmentComponent] = {
    val deptModules =
      if (includeSubDepartments) modulesIncludingSubDepartments(department)
      else modules(department)

    if (deptModules.isEmpty) Nil
    else {
      val c = session.newCriteria[AssessmentComponent]
        .addOrder(asc("moduleCode"))
        .addOrder(asc("sequence"))
      if (inUseOnly) {
        c.add(is("inUse", true))
      }
      if (withExamPapersOnly) {
        c.add(isNotNull("_examPaperCode"))
      }
      if (assessmentType.isDefined) {
        c.add(isNotNull("assessmentType"))
        c.add(is("assessmentType", assessmentType.get))
      }

      val components = safeInSeq(() => {
        c
      }, "module", deptModules)
      components.sortBy { c =>
        (c.moduleCode, c.sequence)
      }
    }
  }

  def getAssessmentComponents(moduleCode: String, inUseOnly: Boolean): Seq[AssessmentComponent] = {
    val c = session.newCriteria[AssessmentComponent]
      .add(is("moduleCode", moduleCode))
      .addOrder(Order.asc("sequence"))
    if (inUseOnly) {
      c.add(is("inUse", true))
    }
    c.seq
  }

  def getAssessmentComponents(moduleCodes: Set[String], inUseOnly: Boolean): Seq[AssessmentComponent] = {
    val c = session.newCriteria[AssessmentComponent]
      .add(safeIn("moduleCode", moduleCodes.toSeq))
      .addOrder(Order.asc("sequence"))
    if (inUseOnly) {
      c.add(is("inUse", true))
    }
    c.seq
  }

  def getAssessmentComponents(department: Department, ids: Seq[String]): Seq[AssessmentComponent] = {
    val modules = modulesIncludingSubDepartments(department)
    session.newCriteria[AssessmentComponent]
      .add(safeIn("id", ids))
      .add(safeIn("module", modules))
      .seq
  }

  def getAssessmentComponentsByPaperCode(department: Department, paperCodes: Seq[String]): Map[String, Seq[AssessmentComponent]] = {
    val modules = modulesIncludingSubDepartments(department)
    session.newCriteria[AssessmentComponent]
      .add(safeIn("_examPaperCode", paperCodes))
      .add(safeIn("module", modules))
      .seq
      .groupBy(_.examPaperCode.getOrElse("none"))
  }

  def getAllAssessmentComponents(academicYears: Seq[AcademicYear]): Seq[AssessmentComponent] =
    session.newQuery[AssessmentComponent](
      """
        select distinct ac
        from AssessmentComponent ac
          join UpstreamAssessmentGroup uag
            on ac.assessmentGroup = uag.assessmentGroup and
               ac.moduleCode = uag.moduleCode and
               ac.sequence = uag.sequence
        where
          uag.academicYear in (:academicYears)""")
      .setParameterList("academicYears", academicYears)
      .seq

  def getAssignmentsForAssessmentGroups(keys: Seq[UpstreamAssessmentGroupKey]): Seq[Assignment] = {
    val em: EntityManager = session
    em.createNativeQuery(s"""
      select distinct a.* from assignment a
        join assessmentgroup ag on a.id = ag.assignment_id
        join assessmentcomponent ac on ac.id = ag.upstream_id
      where array[ac.modulecode, cast(a.academicyear as varchar), ac.sequence, ag.occurrence] in (:keys)
    """, classOf[Assignment])
      .setParameter("keys", keys.asJava)
      .getResultList.asScala.toSeq.asInstanceOf[Seq[Assignment]]
  }

  def countPublishedFeedback(assignment: Assignment): Int = {
    session.createSQLQuery("""select count(*) from feedback where assignment_id = :assignmentId and released = true""")
      .setString("assignmentId", assignment.id)
      .uniqueResult
      .asInstanceOf[Number].intValue
  }

  def countFullFeedback(assignment: Assignment): Int = { //join f.attachments a
    session.newQuery[Number](
      """select count(*) from Feedback f
			where f.assignment = :assignment
			and not (actualMark is null and actualGrade is null and f.attachments is empty)""")
      .setEntity("assignment", assignment)
      .uniqueResult
      .get.intValue
  }

  def getUpstreamAssessmentGroups(component: AssessmentComponent, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup] = {
    session.newCriteria[UpstreamAssessmentGroup]
      .add(is("academicYear", academicYear))
      .add(is("moduleCode", component.moduleCode))
      .add(is("assessmentGroup", component.assessmentGroup))
      .add(is("sequence", component.sequence))
      .seq
  }

  def getCurrentUpstreamAssessmentGroupMembers(component: AssessmentComponent, academicYear: AcademicYear): Seq[UpstreamAssessmentGroupMember] = {
    session.createSQLQuery(
      s"""
			select distinct uagm.* from UpstreamAssessmentGroupMember uagm
				join UpstreamAssessmentGroup uag on uagm.group_id = uag.id and uag.academicYear = :academicYear and uag.moduleCode = :moduleCode and uag.assessmentGroup = :assessmentGroup and uag.sequence = :sequence
				join StudentCourseDetails scd on scd.universityId = uagm.universityId
				join StudentCourseYearDetails scyd on scyd.scjCode = scd.scjCode and  scyd.academicyear = uag.academicYear and scd.scjStatusCode not like  'P%'
			""")
      .addEntity(classOf[UpstreamAssessmentGroupMember])
      .setInteger("academicYear", academicYear.startYear)
      .setString("moduleCode", component.moduleCode)
      .setString("assessmentGroup", component.assessmentGroup)
      .setString("sequence", component.sequence)
      .list.asScala.toSeq.asInstanceOf[Seq[UpstreamAssessmentGroupMember]]
  }

  def getCurrentUpstreamAssessmentGroupMembers(components: Seq[AssessmentComponent], academicYear: AcademicYear): Seq[UpstreamAssessmentGroupMember] = {
    val em: EntityManager = session
    em.createNativeQuery(s"""
      select distinct uagm.* from UpstreamAssessmentGroupMember uagm
				join UpstreamAssessmentGroup uag on uagm.group_id = uag.id and uag.academicYear = :academicYear
				join StudentCourseDetails scd on scd.universityId = uagm.universityId
				join StudentCourseYearDetails scyd on scyd.scjCode = scd.scjCode and  scyd.academicyear = uag.academicYear and scd.scjStatusCode not like  'P%'
      where array[uag.moduleCode, uag.assessmentGroup, uag.sequence] in (:compositeKeys)
    """, classOf[UpstreamAssessmentGroupMember])
      .setParameter("academicYear", academicYear.startYear)
      .setParameter("compositeKeys", components.map(AssessmentComponentKey.apply).asJava)
      .getResultList.asScala.toSeq.asInstanceOf[Seq[UpstreamAssessmentGroupMember]]
  }

  def getCurrentUpstreamAssessmentGroupMembers(uagid: String): Seq[UpstreamAssessmentGroupMember] = {
    session.createSQLQuery(
      s"""
			select distinct uagm.* from UpstreamAssessmentGroupMember uagm
				join UpstreamAssessmentGroup uag on uagm.group_id = uag.id and uag.id = :uagid
				join StudentCourseDetails scd on scd.universityId = uagm.universityId
				join StudentCourseYearDetails scyd on scyd.scjCode = scd.scjCode and  scyd.academicyear = uag.academicYear and scd.scjStatusCode not like  'P%'
			""")
      .addEntity(classOf[UpstreamAssessmentGroupMember])
      .setString("uagid", uagid)
      .list.asScala.toSeq.asInstanceOf[Seq[UpstreamAssessmentGroupMember]]
  }

  def getUpstreamAssessmentGroups(registration: ModuleRegistration, allAssessmentGroups: Boolean, eagerLoad: Boolean): Seq[UpstreamAssessmentGroup] = {
    val criteria =
      session.newCriteria[UpstreamAssessmentGroup]
        .add(is("academicYear", registration.academicYear))
        .add(is("moduleCode", registration.sitsModuleCode))
        .add(is("occurrence", registration.occurrence))

    if (!allAssessmentGroups) {
      criteria.add(is("assessmentGroup", registration.assessmentGroup))
    }

    if (eagerLoad) {
      criteria.setFetchMode("members", FetchMode.JOIN).distinct
    }

    criteria.seq
  }

  def getUpstreamAssessmentGroups(academicYears: Seq[AcademicYear]): Seq[UpstreamAssessmentGroup] = {
    val criteria = session.newCriteria[UpstreamAssessmentGroup]
      .add(safeIn("academicYear", academicYears))
    criteria.seq
  }

  def getUpstreamAssessmentGroups(academicYear: AcademicYear, moduleCode: String): Seq[UpstreamAssessmentGroup] = {
    session.newCriteria[UpstreamAssessmentGroup]
      .add(is("academicYear", academicYear))
      .add(like("moduleCode", moduleCode.toUpperCase + "%"))
      .seq
  }

  def getUpstreamAssessmentGroupsNotIn(ids: Seq[String], academicYears: Seq[AcademicYear]): Seq[String] = {
    val c = session.newCriteria[UpstreamAssessmentGroup]
      .add(safeIn("academicYear", academicYears))

    if (ids.nonEmpty) {
      // TODO Is there a way to do not-in with multiple queries?
      c.add(not(safeIn("id", ids)))
    }

    c.project[String](Projections.id()).seq
  }

  // Get only current members
  private def currentMembersByGroup(upstreamGroups: Seq[UpstreamAssessmentGroup]): Map[UpstreamAssessmentGroup, Seq[UpstreamAssessmentGroupMember]] =
    if (upstreamGroups.isEmpty) Map.empty
    else session.newQuery[UpstreamAssessmentGroupMember](
      """
        select distinct uagm
        from UpstreamAssessmentGroupMember uagm
          join uagm.upstreamAssessmentGroup uag

          join StudentCourseDetails scd on
            scd.student.universityId = uagm.universityId

          join StudentCourseYearDetails scyd on
            scyd.studentCourseDetails = scd and
            scyd.academicYear = uag.academicYear and
            scd.statusOnCourse.code not like 'P%'
        where
          uag in (:upstreamGroups)""")
      .setParameterList("upstreamGroups", upstreamGroups)
      .seq
      .groupBy(_.upstreamAssessmentGroup)

  def getUpstreamAssessmentGroupInfo(groups: Seq[AssessmentGroup], academicYear: AcademicYear): Seq[UpstreamAssessmentGroupInfo] = {
    // Get the UpstreamAssessmentGroups first as some of them will be empty
    val upstreamGroups: Seq[UpstreamAssessmentGroup] =
      if (groups.isEmpty) Nil
      else session.newQuery[UpstreamAssessmentGroup](
        """
        select uag
        from UpstreamAssessmentGroup uag
          join AssessmentComponent ac
            on ac.assessmentGroup = uag.assessmentGroup and
               ac.moduleCode = uag.moduleCode and
               ac.sequence = uag.sequence

          join AssessmentGroup ag
            on ag.assessmentComponent = ac and
               uag.occurrence = ag.occurrence
        where
          uag.academicYear = :academicYear and
          ag in (:groups)""")
        .setParameter("academicYear", academicYear)
        .setParameterList("groups", groups)
        .seq

    val currentMembers: Map[UpstreamAssessmentGroup, Seq[UpstreamAssessmentGroupMember]] = currentMembersByGroup(upstreamGroups)

    upstreamGroups.map { group =>
      UpstreamAssessmentGroupInfo(group, currentMembers.getOrElse(group, Nil))
    }
  }

  def getUpstreamAssessmentGroupInfoForComponents(components: Seq[AssessmentComponent], academicYear: AcademicYear): Seq[UpstreamAssessmentGroupInfo] = {
    // Get the UpstreamAssessmentGroups first as some of them will be empty
    val upstreamGroups: Seq[UpstreamAssessmentGroup] =
      if (components.isEmpty) Nil
      else session.newQuery[UpstreamAssessmentGroup](
        """
        select distinct uag
        from UpstreamAssessmentGroup uag
          join fetch AssessmentComponent ac
            on ac.assessmentGroup = uag.assessmentGroup and
               ac.moduleCode = uag.moduleCode and
               ac.sequence = uag.sequence
          join fetch uag.members
        where
          uag.academicYear = :academicYear and
          ac in (:components)""")
        .setParameter("academicYear", academicYear)
        .setParameterList("components", components)
        .seq

    val currentMembers: Map[UpstreamAssessmentGroup, Seq[UpstreamAssessmentGroupMember]] = currentMembersByGroup(upstreamGroups)

    upstreamGroups.map { group =>
      UpstreamAssessmentGroupInfo(group, currentMembers.getOrElse(group, Nil))
    }
  }

  def emptyMembers(groupsToEmpty: Seq[String]): Int = {
    var count = 0
    val partitionedIds = groupsToEmpty.grouped(Daoisms.MaxInClauseCount)
    partitionedIds.map(batch => {
      val numDeleted = transactional() {
        session.createSQLQuery("delete from UpstreamAssessmentGroupMember where group_id in (:batch)")
          .setParameterList("batch", batch.asJava)
          .executeUpdate
      }
      count += Daoisms.MaxInClauseCount
      logger.info(s"Emptied $count groups")
      numDeleted
    }).sum

  }

  def save(gb: GradeBoundary): Unit = {
    session.save(gb)
  }

  def deleteGradeBoundaries(marksCode: String): Unit =
    session.newUpdateQuery("delete GradeBoundary gb where gb.marksCode = :marksCode")
      .setParameter("marksCode", marksCode)
      .run()

  def getGradeBoundaries(marksCode: String): Seq[GradeBoundary] =
    session.newCriteria[GradeBoundary]
      .add(is("marksCode", marksCode))
      .seq

  def getGradeBoundaries(marksCode: String, process: GradeBoundaryProcess, attempt: Int): Seq[GradeBoundary] =
    session.newCriteria[GradeBoundary]
      .add(is("marksCode", marksCode))
      .add(is("process", process))
      .add(is("attempt", attempt))
      .seq

  def getPassMark(marksCode: String, process: GradeBoundaryProcess, attempt: Int): Option[Int] =
    session.newCriteria[GradeBoundary]
      .add(is("marksCode", marksCode))
      .add(is("process", process))
      .add(is("attempt", attempt))
      .add(is("_result", ModuleResult.Pass))
      .add(in("signalStatus", GradeBoundarySignalStatus.values.filter(_.isDefaultCandidate): _*))
      .project[Option[Int]](Projections.min("minimumMark"))
      .uniqueResult.flatten

  def departmentsManualMembership(department: Department, academicYear: AcademicYear): ManualMembershipInfo = {
    val assignments = session.createSQLQuery(
      s"""
			select a.* from Assignment a
				join Module m on a.module_id = m.id
				join Department d on m.department_id = d.id and a.academicyear = :academicYear and d.code = :departmentCode
				where a.membersgroup_id in (select distinct(i.group_id) from usergroupinclude i join member m on i.usercode = m.userid where i.group_id = a.membersgroup_id)
			""")
      .addEntity(classOf[Assignment])
      .setInteger("academicYear", academicYear.startYear)
      .setString("departmentCode", department.code)
      .list.asScala.toSeq.asInstanceOf[Seq[Assignment]]

    val smallGroupSets = session.createSQLQuery(
      s"""
			select s.* from smallgroupset s
				join module m on s.module_id = m.id
				join department d on m.department_id = d.id and s.academicyear = :academicYear and d.code = :departmentCode
				where s.membersgroup_id in (select distinct(i.group_id) from usergroupinclude i join member m on i.usercode = m.userid where i.group_id = s.membersgroup_id)
			""")
      .addEntity(classOf[SmallGroupSet])
      .setInteger("academicYear", academicYear.startYear)
      .setString("departmentCode", department.code)
      .list.asScala.toSeq.asInstanceOf[Seq[SmallGroupSet]]

    ManualMembershipInfo(department, assignments, smallGroupSets)
  }


  def departmentsWithManualAssessmentsOrGroups(academicYear: AcademicYear): Seq[DepartmentWithManualUsers] = {
    // join the usergroupinclude table with member to weed out manually added ext-users
    // join with upstreamassessmentgroup to ignore assignments/group sets that cannot be linked to an upstream assessment component because none exists
    val results = session.createSQLQuery(
      """
			select distinct(d.id) as id, count(distinct(a.id)) as assignments, count(distinct(s.id)) as smallGroupSets from department d
				join module m on m.department_id = d.id
				left join assignment a on a.module_id = m.id and a.academicyear = :academicYear and a.membersgroup_id in
					(select distinct(i.group_id) from usergroupinclude i join member m on i.usercode = m.userid where i.group_id = a.membersgroup_id)
				left join smallgroupset s on s.module_id = m.id and s.academicyear = :academicYear and s.membersgroup_id in
					(select distinct(i.group_id) from usergroupinclude i join member m on i.usercode = m.userid where i.group_id = s.membersgroup_id)
				join assessmentcomponent c on c.module_id = m.id
				join upstreamassessmentgroup uag
					on (uag.academicyear = a.academicyear or uag.academicyear = s.academicyear)
						 and uag.modulecode = c.modulecode
						 and uag.assessmentgroup = c.assessmentgroup
						 and uag.sequence = c.sequence
				where not (s.id is null and a.id is null)
				group by d.id, d.parent_id
		""")
      .addScalar("id", StandardBasicTypes.STRING)
      .addScalar("assignments", StandardBasicTypes.INTEGER)
      .addScalar("smallGroupSets", StandardBasicTypes.INTEGER)
      .setInteger("academicYear", academicYear.startYear)
      .list.asScala.toSeq.asInstanceOf[Seq[Array[Object]]]

    results.map(columns => DepartmentWithManualUsers(
      columns(0).asInstanceOf[String],
      columns(1).asInstanceOf[Int],
      columns(2).asInstanceOf[Int]
    ))
  }

  override def allScheduledExams(examProfileCodes: Seq[String]): Seq[AssessmentComponentExamSchedule] =
    if (examProfileCodes.isEmpty) Seq.empty
    else session.newCriteria[AssessmentComponentExamSchedule]
      .add(safeIn("examProfileCode", examProfileCodes))
      .seq

  override def findScheduledExamBySlotSequence(examProfileCode: String, slotId: String, sequence: String, locationSequence: String): Option[AssessmentComponentExamSchedule] =
    session.newCriteria[AssessmentComponentExamSchedule]
      .add(is("examProfileCode", examProfileCode))
      .add(is("slotId", slotId))
      .add(is("sequence", sequence))
      .add(is("locationSequence", locationSequence))
      .seq.headOption

  override def findScheduledExams(component: AssessmentComponent, academicYear: Option[AcademicYear]): Seq[AssessmentComponentExamSchedule] = {
    val c =
      session.newCriteria[AssessmentComponentExamSchedule]
        .add(is("moduleCode", component.moduleCode))
        .add(is("assessmentComponentSequence", component.sequence))
        .addOrder(asc("startTime"))

    academicYear.foreach(year => c.add(is("academicYear", year)))

    c.seq
  }

  override def save(schedule: AssessmentComponentExamSchedule): Unit =
    session.saveOrUpdate(schedule)

  override def delete(schedule: AssessmentComponentExamSchedule): Unit =
    session.delete(schedule)

  override def allVariableAssessmentWeightingRules: Seq[VariableAssessmentWeightingRule] =
    session.newCriteria[VariableAssessmentWeightingRule]
      .addOrder(asc("moduleCode"))
      .addOrder(asc("assessmentGroup"))
      .addOrder(asc("ruleSequence"))
      .seq

  override def getVariableAssessmentWeightingRules(moduleCodeWithCats: String, assessmentGroup: String): Seq[VariableAssessmentWeightingRule] =
    session.newCriteria[VariableAssessmentWeightingRule]
      .add(is("moduleCode", moduleCodeWithCats))
      .add(is("assessmentGroup", assessmentGroup))
      .addOrder(asc("ruleSequence"))
      .seq

  override def save(rule: VariableAssessmentWeightingRule): Unit = session.saveOrUpdate(rule)

  override def delete(rule: VariableAssessmentWeightingRule): Unit = session.delete(rule)
}
