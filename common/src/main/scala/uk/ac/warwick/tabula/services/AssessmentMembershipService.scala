package uk.ac.warwick.tabula.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.AssessmentMembershipDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{FoundUser, Logging, RequestLevelCache}
import uk.ac.warwick.userlookup.{AnonymousUser, User}

import scala.jdk.CollectionConverters._

trait AssessmentMembershipService {
  def assignmentManualMembershipHelper: UserGroupMembershipHelperMethods[Assignment]

  def find(assignment: AssessmentComponent): Option[AssessmentComponent]

  def find(group: UpstreamAssessmentGroup, eagerLoad: Boolean): Option[UpstreamAssessmentGroup]

  def find(group: AssessmentGroup): Option[AssessmentGroup]

  def save(group: AssessmentGroup): Unit

  def delete(group: AssessmentGroup): Unit

  def delete(assessmentComponent: AssessmentComponent): Unit

  def delete(upstreamAssessmentGroup: UpstreamAssessmentGroup): Unit

  def getAssessmentGroup(id: String): Option[AssessmentGroup]

  def getAssessmentGroup(template: AssessmentGroup): Option[AssessmentGroup]

  def getUpstreamAssessmentGroup(template: UpstreamAssessmentGroup, eagerLoad: Boolean): Option[UpstreamAssessmentGroup]

  def getUpstreamAssessmentGroupInfo(template: UpstreamAssessmentGroup): Option[UpstreamAssessmentGroupInfo]

  def getUpstreamAssessmentGroupInfo(groups: Seq[AssessmentGroup], academicYear: AcademicYear): Seq[UpstreamAssessmentGroupInfo]

  def getUpstreamAssessmentGroupInfoForComponents(components: Seq[AssessmentComponent], academicYear: AcademicYear): Seq[UpstreamAssessmentGroupInfo]

  def getUpstreamAssessmentGroups(module: Module, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup]

  def getUpstreamAssessmentGroups(student: StudentMember, academicYear: AcademicYear, resitOnly: Boolean): Seq[UpstreamAssessmentGroup]

  def getUpstreamAssessmentGroup(id: String): Option[UpstreamAssessmentGroup]

  def getCurrentUpstreamAssessmentGroupMembers(uagid: String): Seq[UpstreamAssessmentGroupMember]

  def getAssessmentComponent(id: String): Option[AssessmentComponent]

  def getAssessmentComponent(group: UpstreamAssessmentGroup): Option[AssessmentComponent]

  /**
    * Get all AssessmentComponents that appear to belong to this module.
    *
    * Typically used to provide possible candidates to link to an app assignment,
    * in conjunction with #getUpstreamAssessmentGroups.
    */
  def getAssessmentComponents(module: Module, inUseOnly: Boolean = true): Seq[AssessmentComponent]

  def getAssessmentComponents(department: Department, includeSubDepartments: Boolean, inUseOnly: Boolean): Seq[AssessmentComponent]

  def getAssessmentComponents(department: Department, includeSubDepartments: Boolean, assessmentType: Option[AssessmentType], withExamPapersOnly: Boolean, inUseOnly: Boolean): Seq[AssessmentComponent]

  def getAssessmentComponents(moduleCode: String, inUseOnly: Boolean): Seq[AssessmentComponent]

  def getAssessmentComponents(moduleCodes: Set[String], inUseOnly: Boolean): Seq[AssessmentComponent]

  def getAssessmentComponents(department: Department, ids: Seq[String]): Seq[AssessmentComponent]

  def getAssessmentComponentsByPaperCode(department: Department, paperCodes: Seq[String]): Map[String, Seq[AssessmentComponent]]

  def getUpstreamAssessmentGroupMembers(components: Seq[AssessmentComponent], academicYear: AcademicYear): Map[AssessmentComponentKey, Seq[UpstreamAssessmentGroupInfo]]

  def getAllAssessmentComponents(academicYears: Seq[AcademicYear]): Seq[AssessmentComponent]

  def getAssignmentsForAssessmentGroups(keys: Seq[UpstreamAssessmentGroupKey]): Map[UpstreamAssessmentGroupKey, Seq[Assignment]]

  /**
    * Get all assessment groups that can serve this assignment this year.
    * Should return as many groups as there are distinct OCCURRENCE values for a given
    * assessment group code, which most of the time is just 1.
    */
  def getUpstreamAssessmentGroups(component: AssessmentComponent, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup]

  def getUpstreamAssessmentGroupInfo(component: AssessmentComponent, academicYear: AcademicYear): Seq[UpstreamAssessmentGroupInfo]

  def getUpstreamAssessmentGroups(registration: ModuleRegistration, allAssessmentGroups: Boolean, eagerLoad: Boolean): Seq[UpstreamAssessmentGroup]

  def getUpstreamAssessmentGroups(academicYears: Seq[AcademicYear]): Seq[UpstreamAssessmentGroup]

  def getUpstreamAssessmentGroups(academicYear: AcademicYear, moduleCode: String): Seq[UpstreamAssessmentGroup]

  def save(assignment: AssessmentComponent): AssessmentComponent

  def save(group: UpstreamAssessmentGroup): Unit

  def save(member: UpstreamAssessmentGroupMember): Unit

  // empty the usergroups for all assessmentgroups in the specified academic years. Skip any groups specified in ignore
  def emptyMembers(groupsToEmpty: Seq[String]): Int

  def replaceMembers(group: UpstreamAssessmentGroup, registrations: Seq[UpstreamAssessmentRegistration], assessmentType: UpstreamAssessmentGroupMemberAssessmentType): UpstreamAssessmentGroup

  def getUpstreamAssessmentGroupsNotIn(ids: Seq[String], academicYears: Seq[AcademicYear]): Seq[String]

  def getEnrolledAssignments(user: User, academicYear: Option[AcademicYear]): Seq[Assignment]

  /**
    * This will throw an exception if the others are usercode groups, use determineMembership instead in that situation
    */
  def countCurrentMembershipWithUniversityIdGroup(upstream: Seq[UpstreamAssessmentGroupInfo], others: Option[UnspecifiedTypeUserGroup]): Int

  def determineMembership(upstream: Seq[UpstreamAssessmentGroupInfo], others: Option[UnspecifiedTypeUserGroup], resitOnly: Boolean): AssessmentMembershipInfo

  def determineMembership(assignment: Assignment): AssessmentMembershipInfo

  def determineMembershipUsers(upstream: Seq[UpstreamAssessmentGroupInfo], others: Option[UnspecifiedTypeUserGroup], resitOnly: Boolean): Seq[User]

  def determineMembershipUsers(assignment: Assignment): Seq[User]

  def determineMembershipUsersIncludingPWD(assignment: Assignment): Seq[User]

  def determineMembershipUsersWithOrder(assignment: Assignment): Seq[(User, Option[Int])]

  def determineMembershipIds(upstream: Seq[UpstreamAssessmentGroupInfo], others: Option[UnspecifiedTypeUserGroup]): Seq[String]

  def isStudentCurrentMember(user: User, upstream: Seq[UpstreamAssessmentGroupInfo], others: Option[UnspecifiedTypeUserGroup], resitOnly: Boolean): Boolean

  def save(gb: GradeBoundary): Unit

  def deleteGradeBoundaries(marksCode: String): Unit

  def gradesForMark(component: AssessmentComponent, mark: Option[Int], resitAttempt: Option[Int]): Seq[GradeBoundary]

  def gradesForMark(moduleRegistration: ModuleRegistration, mark: Option[Int]): Seq[GradeBoundary]

  def passMark(moduleRegistration: ModuleRegistration, resitAttempt: Option[Int]): Option[Int]

  def markScheme(marksCode: String): Seq[GradeBoundary]

  def departmentsWithManualAssessmentsOrGroups(academicYear: AcademicYear): Seq[DepartmentWithManualUsers]

  def departmentsManualMembership(department: Department, academicYear: AcademicYear): ManualMembershipInfo

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

// all the small group sets and assignments in Tabula for a department with manually added students
case class ManualMembershipInfo(department: Department, assignments: Seq[Assignment], smallGroupSets: Seq[SmallGroupSet])


@Service(value = "assignmentMembershipService")
class AssessmentMembershipServiceImpl
  extends AssessmentMembershipService
    with AssessmentMembershipMethods
    with UserLookupComponent
    with ProfileServiceComponent
    with Logging {

  @Autowired var userLookup: UserLookupService = _
  @Autowired var dao: AssessmentMembershipDao = _
  @Autowired var profileService: ProfileService = _

  val assignmentManualMembershipHelper = new UserGroupMembershipHelper[Assignment]("_members")
  val examManualMembershipHelper = new UserGroupMembershipHelper[Exam]("_members")

  def getEnrolledAssignments(user: User, academicYear: Option[AcademicYear]): Seq[Assignment] = {
    val autoEnrolled =
      dao.getSITSEnrolledAssignments(user, academicYear)
        .filterNot(_.members.excludesUser(user))

    // TAB-1749 If we've been passed a non-primary usercode (e.g. WBS logins)
    // then also get registrations for the primary usercode
    val manuallyEnrolled = {
      val ssoUser = if (user.getWarwickId != null) userLookup.getUserByWarwickUniId(user.getWarwickId) else userLookup.getUserByUserId(user.getUserId)
      ssoUser match {
        case FoundUser(primaryUser) if primaryUser.getUserId != user.getUserId =>
          assignmentManualMembershipHelper.findBy(primaryUser) ++ assignmentManualMembershipHelper.findBy(user)

        case _ => assignmentManualMembershipHelper.findBy(user)
      }
    }

    (autoEnrolled ++ manuallyEnrolled.filter { a => academicYear.isEmpty || academicYear.contains(a.academicYear) })
      .filter(_.isVisibleToStudents).distinct
  }

  def emptyMembers(groupsToEmpty: Seq[String]): Int =
    dao.emptyMembers(groupsToEmpty: Seq[String])

  def replaceMembers(template: UpstreamAssessmentGroup, registrations: Seq[UpstreamAssessmentRegistration], assessmentType: UpstreamAssessmentGroupMemberAssessmentType): UpstreamAssessmentGroup = {
    if (debugEnabled) debugReplace(template, registrations.map(_.universityId))

    getUpstreamAssessmentGroup(template, eagerLoad = true).map { group =>
      group.replaceMembers(registrations.map(r => (r.universityId, r.resitSequence.maybeText)), assessmentType)
      group
    } getOrElse {
      logger.warn("No such assessment group found: " + template.toString)
      template
    }
  }

  private def debugReplace(template: UpstreamAssessmentGroup, universityIds: Seq[String]): Unit = {
    logger.debug("Setting %d members in group %s" format(universityIds.size, template.toString))
  }

  /**
    * Tries to find an identical AssessmentComponent in the database, based on the
    * fact that moduleCode and sequence uniquely identify the assignment.
    */
  def find(assignment: AssessmentComponent): Option[AssessmentComponent] = dao.find(assignment)

  def find(group: UpstreamAssessmentGroup, eagerLoad: Boolean): Option[UpstreamAssessmentGroup] = dao.find(group, eagerLoad)

  def find(group: AssessmentGroup): Option[AssessmentGroup] = dao.find(group)

  def save(group: AssessmentGroup): Unit = dao.save(group)

  def save(assignment: AssessmentComponent): AssessmentComponent = dao.save(assignment)

  def save(group: UpstreamAssessmentGroup): Unit = dao.save(group)

  def save(member: UpstreamAssessmentGroupMember): Unit = dao.save(member)

  def getAssessmentGroup(id: String): Option[AssessmentGroup] = dao.getAssessmentGroup(id)

  def getAssessmentGroup(template: AssessmentGroup): Option[AssessmentGroup] = find(template)

  def getUpstreamAssessmentGroups(module: Module, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup] = dao.getUpstreamAssessmentGroups(module, academicYear)

  def getUpstreamAssessmentGroups(student: StudentMember, academicYear: AcademicYear, resitOnly: Boolean): Seq[UpstreamAssessmentGroup] = dao.getUpstreamAssessmentGroups(student, academicYear, resitOnly)

  def getUpstreamAssessmentGroup(template: UpstreamAssessmentGroup, eagerLoad: Boolean): Option[UpstreamAssessmentGroup] = find(template, eagerLoad)

  def getUpstreamAssessmentGroupInfo(template: UpstreamAssessmentGroup): Option[UpstreamAssessmentGroupInfo] = {
    find(template, eagerLoad = false).map(grp => UpstreamAssessmentGroupInfo(grp, getCurrentUpstreamAssessmentGroupMembers(grp.id)))
  }

  def getUpstreamAssessmentGroupInfo(groups: Seq[AssessmentGroup], academicYear: AcademicYear): Seq[UpstreamAssessmentGroupInfo] =
    dao.getUpstreamAssessmentGroupInfo(groups, academicYear)

  def getUpstreamAssessmentGroupInfoForComponents(components: Seq[AssessmentComponent], academicYear: AcademicYear): Seq[UpstreamAssessmentGroupInfo] =
    dao.getUpstreamAssessmentGroupInfoForComponents(components, academicYear)

  def getUpstreamAssessmentGroup(id: String): Option[UpstreamAssessmentGroup] = dao.getUpstreamAssessmentGroup(id)

  def getCurrentUpstreamAssessmentGroupMembers(uagid: String): Seq[UpstreamAssessmentGroupMember] = dao.getCurrentUpstreamAssessmentGroupMembers(uagid)

  def delete(group: AssessmentGroup): Unit = {
    dao.delete(group)
  }

  def delete(assessmentComponent: AssessmentComponent): Unit = {
    dao.delete(assessmentComponent)
  }

  def delete(upstreamAssessmentGroup: UpstreamAssessmentGroup): Unit = {
    dao.delete(upstreamAssessmentGroup)
  }

  def getAssessmentComponent(id: String): Option[AssessmentComponent] = dao.getAssessmentComponent(id)

  def getAssessmentComponent(group: UpstreamAssessmentGroup): Option[AssessmentComponent] = dao.getAssessmentComponent(group)

  /**
    * Gets assessment components for this module.
    */
  def getAssessmentComponents(module: Module, inUseOnly: Boolean = true): Seq[AssessmentComponent] = dao.getAssessmentComponents(module, inUseOnly)

  /**
    * Gets assessment components by SITS module code
    */
  def getAssessmentComponents(moduleCode: String, inUseOnly: Boolean): Seq[AssessmentComponent] =
    dao.getAssessmentComponents(moduleCode, inUseOnly)

  def getAssessmentComponents(moduleCodes: Set[String], inUseOnly: Boolean): Seq[AssessmentComponent] =
    dao.getAssessmentComponents(moduleCodes, inUseOnly)

  def getAssessmentComponents(department: Department, ids: Seq[String]): Seq[AssessmentComponent] = dao.getAssessmentComponents(department, ids)

  def getAssessmentComponentsByPaperCode(department: Department, paperCodes: Seq[String]): Map[String, Seq[AssessmentComponent]] =
    dao.getAssessmentComponentsByPaperCode(department, paperCodes)

  def getUpstreamAssessmentGroupMembers(components: Seq[AssessmentComponent], academicYear: AcademicYear): Map[AssessmentComponentKey, Seq[UpstreamAssessmentGroupInfo]] =
    dao.getCurrentUpstreamAssessmentGroupMembers(components, academicYear)
      .groupBy(_.upstreamAssessmentGroup)
      .map { case (uag, currentMembers) => UpstreamAssessmentGroupInfo(uag, currentMembers) }
      .toSeq
      .groupBy(uagi => AssessmentComponentKey(uagi.upstreamAssessmentGroup))

  def getAllAssessmentComponents(academicYears: Seq[AcademicYear]): Seq[AssessmentComponent] =
    dao.getAllAssessmentComponents(academicYears)

  def getAssignmentsForAssessmentGroups(keys: Seq[UpstreamAssessmentGroupKey]): Map[UpstreamAssessmentGroupKey, Seq[Assignment]] = {
    val allAssignments = dao.getAssignmentsForAssessmentGroups(keys)
    keys.map(k => k -> {
      allAssignments.filter(a => {
        k.academicYear == a.academicYear && a.assessmentGroups.asScala.toSeq.exists(ag => {
          val occurrence = ag.occurrence
          val sequence = ag.assessmentComponent.sequence
          val moduleCode = ag.assessmentComponent.moduleCode
          occurrence == k.occurrence && sequence == k.sequence && moduleCode == k.moduleCode
        })
      })
    }).toMap
  }

  /**
    * Gets assessment components for this department.
    */
  def getAssessmentComponents(department: Department, includeSubDepartments: Boolean, inUseOnly: Boolean): Seq[AssessmentComponent] = dao.getAssessmentComponents(department, includeSubDepartments, inUseOnly)

  def getAssessmentComponents(department: Department, includeSubDepartments: Boolean, assessmentType: Option[AssessmentType], withExamPapersOnly: Boolean, inUseOnly: Boolean): Seq[AssessmentComponent] =
    dao.getAssessmentComponents(department, includeSubDepartments, assessmentType, withExamPapersOnly, inUseOnly)

  def countPublishedFeedback(assignment: Assignment): Int = dao.countPublishedFeedback(assignment)

  def countFullFeedback(assignment: Assignment): Int = dao.countFullFeedback(assignment)

  def getUpstreamAssessmentGroups(component: AssessmentComponent, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup] =
    dao.getUpstreamAssessmentGroups(component, academicYear)

  def getUpstreamAssessmentGroupInfo(component: AssessmentComponent, academicYear: AcademicYear): Seq[UpstreamAssessmentGroupInfo] = {
    val uagMembers = dao.getCurrentUpstreamAssessmentGroupMembers(component, academicYear)
    val alluags = dao.getUpstreamAssessmentGroups(component, academicYear)
    val uagMap = uagMembers.groupBy(_.upstreamAssessmentGroup)
    val additional = alluags.diff(uagMap.keys.toSeq).map(grp => UpstreamAssessmentGroupInfo(grp, Seq()))
    uagMap.map { case (uag, currentMembers) => UpstreamAssessmentGroupInfo(uag, currentMembers) }.toSeq ++ additional
  }

  def getUpstreamAssessmentGroups(registration: ModuleRegistration, allAssessmentGroups: Boolean, eagerLoad: Boolean): Seq[UpstreamAssessmentGroup] =
    dao.getUpstreamAssessmentGroups(registration, allAssessmentGroups, eagerLoad)

  def getUpstreamAssessmentGroups(academicYears: Seq[AcademicYear]): Seq[UpstreamAssessmentGroup] =
    dao.getUpstreamAssessmentGroups(academicYears)

  def getUpstreamAssessmentGroups(academicYear: AcademicYear, moduleCode: String): Seq[UpstreamAssessmentGroup] =
    dao.getUpstreamAssessmentGroups(academicYear, moduleCode)

  def getUpstreamAssessmentGroupsNotIn(ids: Seq[String], academicYears: Seq[AcademicYear]): Seq[String] =
    dao.getUpstreamAssessmentGroupsNotIn(ids, academicYears)

  def save(gb: GradeBoundary): Unit = {
    dao.save(gb)
  }

  def deleteGradeBoundaries(marksCode: String): Unit = {
    dao.deleteGradeBoundaries(marksCode)
  }

  private def gradesForMark(marksCode: String, mark: Option[Int], resitAttempt: Option[Int]): Seq[GradeBoundary] = {
    val assessmentType = if (resitAttempt.nonEmpty) GradeBoundaryProcess.Reassessment else GradeBoundaryProcess.StudentAssessment
    val attempt = resitAttempt.getOrElse(1)

    RequestLevelCache.cachedBy("AssessmentMembershipService.getGradeBoundaries", s"$marksCode-$assessmentType-$attempt") {
      dao.getGradeBoundaries(marksCode, assessmentType, attempt)
    }.filter(_.isValidForMark(mark)).sorted
  }

  def gradesForMark(component: AssessmentComponent, mark: Option[Int], resitAttempt: Option[Int]): Seq[GradeBoundary] =
    component.marksCode.maybeText.map { marksCode =>
      gradesForMark(marksCode, mark, resitAttempt)
    }.getOrElse(Seq.empty)

  def gradesForMark(moduleRegistration: ModuleRegistration, mark: Option[Int]): Seq[GradeBoundary] =
    moduleRegistration.marksCode.maybeText.map { marksCode =>
      gradesForMark(marksCode, mark, moduleRegistration.currentResitAttempt)
    }.getOrElse(Seq.empty)

  def markScheme(marksCode: String): Seq[GradeBoundary] = dao.getGradeBoundaries(marksCode)

  def passMark(moduleRegistration: ModuleRegistration, resitAttempt: Option[Int]): Option[Int] =
    dao.getPassMark(moduleRegistration.marksCode, if (resitAttempt.nonEmpty) GradeBoundaryProcess.Reassessment else GradeBoundaryProcess.StudentAssessment, resitAttempt.getOrElse(1))

  def departmentsWithManualAssessmentsOrGroups(academicYear: AcademicYear): Seq[DepartmentWithManualUsers] = dao.departmentsWithManualAssessmentsOrGroups(academicYear)

  def departmentsManualMembership(department: Department, academicYear: AcademicYear): ManualMembershipInfo =
    dao.departmentsManualMembership(department, academicYear)

  override def allScheduledExams(examProfileCodes: Seq[String]): Seq[AssessmentComponentExamSchedule] =
    dao.allScheduledExams(examProfileCodes)

  override def findScheduledExamBySlotSequence(examProfileCode: String, slotId: String, sequence: String, locationSequence: String): Option[AssessmentComponentExamSchedule] =
    dao.findScheduledExamBySlotSequence(examProfileCode, slotId, sequence, locationSequence)

  override def findScheduledExams(component: AssessmentComponent, academicYear: Option[AcademicYear]): Seq[AssessmentComponentExamSchedule] =
    dao.findScheduledExams(component, academicYear)

  override def save(schedule: AssessmentComponentExamSchedule): Unit = dao.save(schedule)

  override def delete(schedule: AssessmentComponentExamSchedule): Unit = dao.delete(schedule)

  override def allVariableAssessmentWeightingRules: Seq[VariableAssessmentWeightingRule] =
    dao.allVariableAssessmentWeightingRules

  override def getVariableAssessmentWeightingRules(moduleCodeWithCats: String, assessmentGroup: String): Seq[VariableAssessmentWeightingRule] = RequestLevelCache.cachedBy("AssessmentMembershipService.getVariableAssessmentWeightingRules", s"$moduleCodeWithCats-$assessmentGroup") {
    dao.getVariableAssessmentWeightingRules(moduleCodeWithCats, assessmentGroup)
  }

  override def save(rule: VariableAssessmentWeightingRule): Unit = dao.save(rule)

  override def delete(rule: VariableAssessmentWeightingRule): Unit = dao.delete(rule)
}

class AssessmentMembershipInfo(val items: Seq[MembershipItem]) {
  val sitsCount: Int = items.count(_.itemType == SitsType)
  val totalCount: Int = items.filterNot(_.itemType == ExcludeType).size
  val includeCount: Int = items.count(_.itemType == IncludeType)
  val excludeCount: Int = items.count(_.itemType == ExcludeType)
  val usedIncludeCount: Int = items.count(i => i.itemType == IncludeType && !i.extraneous)
  val usedExcludeCount: Int = items.count(i => i.itemType == ExcludeType && !i.extraneous)
  val usercodes: Set[String] = items.filterNot(_.itemType == ExcludeType).flatMap(_.userId).toSet
}

trait AssessmentMembershipMethods extends Logging {

  self: AssessmentMembershipService with UserLookupComponent with ProfileServiceComponent =>

  private def deceasedItemsFilter(items: Seq[MembershipItem]) = {
    val profiles = profileService.getAllMembersWithUniversityIds(items.flatMap(_.universityId))
    items.filter(item => item.universityId.isEmpty || !profiles.find(_.universityId == item.universityId.get).exists(_.deceased))
  }

  private def deceasedUniIdsFilter(universityIds: Seq[String]) = {
    val profiles = profileService.getAllMembersWithUniversityIds(universityIds)
    universityIds.filter(universityId => !profiles.find(_.universityId == universityId).exists(_.deceased))
  }

  private def generateAssessmentMembershipInfo(upstream: Seq[UpstreamAssessmentGroupInfo], others: Option[UnspecifiedTypeUserGroup], includePWD: Boolean = false, resitOnly: Boolean = false): AssessmentMembershipInfo = {
    val sitsUsers =
      userLookup.usersByWarwickUniIds(
        upstream.flatMap { uagInfo =>
          val members = if (resitOnly) uagInfo.resitMembers else if (includePWD) uagInfo.allMembers else uagInfo.currentMembers
          members.map(_.universityId).filter(_.hasText)
        }.distinct
      ).toSeq

    val includes = others.map(_.users.map(u => u.getUserId -> u).toSeq).getOrElse(Nil)
    val excludes = others.map(_.excludes.map(u => u.getUserId -> u).toSeq).getOrElse(Nil)

    // convert lists of Users to lists of MembershipItems that we can render neatly together.

    val includeItems = makeIncludeItems(includes, sitsUsers)
    val excludeItems = makeExcludeItems(excludes, sitsUsers)
    val sitsItems = makeSitsItems(includes, excludes, sitsUsers)

    val sorted = (includeItems ++ excludeItems ++ sitsItems)
      .sortBy(membershipItem => (membershipItem.user.getLastName, membershipItem.user.getFirstName))

    new AssessmentMembershipInfo(deceasedItemsFilter(sorted))
  }

  def determineMembership(upstream: Seq[UpstreamAssessmentGroupInfo], others: Option[UnspecifiedTypeUserGroup], resitOnly: Boolean): AssessmentMembershipInfo =
    generateAssessmentMembershipInfo(upstream, others, resitOnly = resitOnly)

  def determineMembership(assignment: Assignment): AssessmentMembershipInfo =
    determineMembership(assignment.upstreamAssessmentGroupInfos, Option(assignment.members), resitOnly = assignment.resitAssessment)

  /**
    * Returns just a list of User objects who are on this assessment group.
    */
  def determineMembershipUsers(upstream: Seq[UpstreamAssessmentGroupInfo], others: Option[UnspecifiedTypeUserGroup], resitOnly: Boolean): Seq[User] = {
    determineMembership(upstream, others, resitOnly).items.filter(notExclude).map(toUser).filter(notNull).filter(notAnonymous)
  }

  /**
    * Returns a simple list of User objects for students who are enrolled on this assessment. May be empty.
    */
  def determineMembershipUsers(assignment: Assignment): Seq[User] =
    determineMembershipUsers(assignment.upstreamAssessmentGroupInfos, Option(assignment.members), assignment.resitAssessment)

  def determineMembershipUsersIncludingPWD(a: Assignment): Seq[User] =
    generateAssessmentMembershipInfo(a.upstreamAssessmentGroupInfos, Option(a.members), includePWD = true, a.resitAssessment).items
      .filter(notExclude)
      .map(toUser)
      .filter(notNull)
      .filter(notAnonymous)


  def determineMembershipUsersWithOrder(assignment: Assignment): Seq[(User, Option[Int])] = {
    val sitsMembers = assignment.upstreamAssessmentGroupInfos.flatMap(_.currentMembers).distinct.sortBy(_.position)
    val sitsUniIds = sitsMembers.map(_.universityId)
    val includesUniIds = Option(assignment.members).map(_.users.map(_.getWarwickId).filterNot(sitsUniIds.contains).toSeq).getOrElse(Nil)
    sitsMembers.map(m => userLookup.getUserByWarwickUniId(m.universityId) -> m.position) ++
      includesUniIds.map(u => userLookup.getUserByWarwickUniId(u)).sortBy(u => (u.getLastName, u.getFirstName)).map(u => u -> None)
  }

  def determineMembershipIds(upstream: Seq[UpstreamAssessmentGroupInfo], others: Option[UnspecifiedTypeUserGroup]): Seq[String] = {
    others.foreach { g => assert(g.universityIds) }

    val sitsUsers = upstream.flatMap(_.currentMembers.map(_.universityId)).distinct

    val includes = others.map(_.knownType.members.toSeq).getOrElse(Nil)
    val excludes = others.map(_.knownType.excludedUserIds.toSeq).getOrElse(Nil)

    deceasedUniIdsFilter((sitsUsers ++ includes).distinct.diff(excludes.distinct))
  }

  def countCurrentMembershipWithUniversityIdGroup(upstream: Seq[UpstreamAssessmentGroupInfo], others: Option[UnspecifiedTypeUserGroup]): Int = {
    others match {
      case Some(group) if !group.universityIds =>
        logger.warn("Attempted to use countCurrentWithUniversityIdGroup() with a usercode-type UserGroup. Falling back to determineMembership()")
        determineMembershipUsers(upstream, others, resitOnly = false).size
      case _ =>
        val sitsUsers = upstream.flatMap(_.currentMembers.map(_.universityId))

        val includes = others.map(_.knownType.allIncludedIds.toSeq) getOrElse Nil
        val excludes = others.map(_.knownType.allExcludedIds.toSeq) getOrElse Nil

        deceasedUniIdsFilter((sitsUsers ++ includes).distinct.diff(excludes)).size
    }
  }

  def isStudentCurrentMember(user: User, upstream: Seq[UpstreamAssessmentGroupInfo], others: Option[UnspecifiedTypeUserGroup], resitOnly: Boolean): Boolean = {
    if (others.exists(_.excludesUser(user))) false
    else if (others.exists(_.includesUser(user))) true
    else determineMembership(upstream, others, resitOnly).items.filter(notExclude).exists(_.universityId.contains(user.getWarwickId))
  }

  private def sameUserIdAs(user: User) = (other: (String, User)) => {
    user.getUserId == other._2.getUserId
  }

  private def in(seq: Seq[(String, User)]) = (other: (String, User)) => {
    seq exists sameUserIdAs(other._2)
  }

  private def makeIncludeItems(includes: Seq[(String, User)], sitsUsers: Seq[(String, User)]) =
    includes map {
      case (id, user) =>
        val extraneous = sitsUsers exists sameUserIdAs(user)
        MembershipItem(
          user = user,
          universityId = universityId(user, None),
          userId = userId(user, Some(id)),
          itemType = IncludeType,
          extraneous = extraneous)
    }

  private def makeExcludeItems(excludes: Seq[(String, User)], sitsUsers: Seq[(String, User)]) =
    excludes map {
      case (id, user) =>
        val extraneous = !(sitsUsers exists sameUserIdAs(user))
        MembershipItem(
          user = user,
          universityId = universityId(user, None),
          userId = userId(user, Some(id)),
          itemType = ExcludeType,
          extraneous = extraneous)
    }

  private def makeSitsItems(includes: Seq[(String, User)], excludes: Seq[(String, User)], sitsUsers: Seq[(String, User)]) =
    sitsUsers filterNot in(includes) filterNot in(excludes) map {
      case (id, user) =>
        MembershipItem(
          user = user,
          universityId = universityId(user, Some(id)),
          userId = userId(user, None),
          itemType = SitsType,
          extraneous = false)
    }

  private def universityId(user: User, fallback: Option[String]) = option(user).map(_.getWarwickId) orElse fallback

  private def userId(user: User, fallback: Option[String]) = option(user).map(_.getUserId) orElse fallback

  private def option(user: User): Option[User] = user match {
    case FoundUser(_) => Some(user)
    case _ => None
  }

  private def toUser(item: MembershipItem) = item.user

  private def notExclude(item: MembershipItem) = item.itemType != ExcludeType

  private def notNull[A](any: A) = {
    any != null
  }

  private def notAnonymous(user: User) = {
    !user.isInstanceOf[AnonymousUser]
  }
}

abstract class MembershipItemType(val value: String)

case object SitsType extends MembershipItemType("sits")

case object IncludeType extends MembershipItemType("include")

case object ExcludeType extends MembershipItemType("exclude")

/** Item in list of members for displaying in view.
  */
case class MembershipItem(
  user: User,
  universityId: Option[String],
  userId: Option[String],
  itemType: MembershipItemType, // sits, include or exclude
  /**
    * If include type, this item adds a user who's already in SITS.
    * If exclude type, this item excludes a user who isn't in the list anyway.
    */
  extraneous: Boolean) {

  def itemTypeString: String = itemType.value
}

trait AssessmentMembershipServiceComponent {
  def assessmentMembershipService: AssessmentMembershipService
}

trait AutowiringAssessmentMembershipServiceComponent extends AssessmentMembershipServiceComponent {
  var assessmentMembershipService: AssessmentMembershipService = Wire[AssessmentMembershipService]
}

trait GeneratesGradesFromMarks {
  def applyForMarks(marks: Map[String, Int]): Map[String, Seq[GradeBoundary]]
}

trait HasManualMembership {
  def members: UnspecifiedTypeUserGroup

  def module: Module
}
