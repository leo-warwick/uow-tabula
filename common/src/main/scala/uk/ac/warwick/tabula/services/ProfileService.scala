package uk.ac.warwick.tabula.services

import java.util.UUID

import org.hibernate.criterion.Restrictions._
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.FiltersStudents
import uk.ac.warwick.tabula.data.ScalaRestriction.inIfNotEmptyMultipleProperties
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.elasticsearch.ProfileQueryService
import uk.ac.warwick.userlookup.User

/**
  * Service providing access to members and profiles.
  */
trait ProfileService {
  def save(member: Member): Unit

  def regenerateTimetableHash(member: Member): Unit

  def getMemberByUniversityId(universityId: String, disableFilter: Boolean = false, eagerLoad: Boolean = false): Option[Member]

  def getMemberByUniversityIdStaleOrFresh(universityId: String): Option[Member]

  def getAllMembersWithUniversityIds(universityIds: Seq[String]): Seq[Member]

  def getAllMembersWithUniversityIdsStaleOrFresh(universityIds: Seq[String]): Seq[Member]

  def getAllMembersWithUserId(userId: String, disableFilter: Boolean = false, eagerLoad: Boolean = false, activeOnly: Boolean = true): Seq[Member]

  def getAllMembersWithUserIds(userIds: Seq[String], disableFilter: Boolean = false, eagerLoad: Boolean = false, activeOnly: Boolean = true): Seq[Member]

  def getMemberByUser(user: User, disableFilter: Boolean = false, eagerLoad: Boolean = false, activeOnly:Boolean = true): Option[Member]

  def getStudentBySprCode(sprCode: String): Option[StudentMember]

  def getMemberByTimetableHash(timetableHash: String): Option[Member]

  def findMembersByQuery(query: String, departments: Seq[Department], userTypes: Set[MemberUserType], searchAllDepts: Boolean, activeOnly: Boolean): Seq[Member]

  def findMembersByDepartment(department: Department, includeTouched: Boolean, userTypes: Set[MemberUserType]): Seq[Member]

  def listMembersUpdatedSince(startDate: DateTime, max: Int): Seq[Member]

  def countStudentsByDepartment(department: Department): Int

  def getStudentsByRoute(route: Route): Seq[StudentMember]

  def getStudentsByRoute(route: Route, academicYear: AcademicYear): Seq[StudentMember]

  def getStudentCourseDetailsByScjCode(scjCode: String): Option[StudentCourseDetails]

  def getStudentCourseDetailsBySprCode(sprCode: String): Seq[StudentCourseDetails]

  def countStudentsByRestrictions(department: Department, academicYear: AcademicYear, restrictions: Seq[ScalaRestriction]): Int

  def countStudentsByRestrictionsInAffiliatedDepartments(department: Department, restrictions: Seq[ScalaRestriction]): Int

  def findStudentsByRestrictions(
    department: Department,
    academicYear: AcademicYear,
    restrictions: Seq[ScalaRestriction],
    orders: Seq[ScalaOrder] = Seq(),
    maxResults: Int = 50,
    startResult: Int = 0
  ): (Int, Seq[StudentMember])

  def findStudentsByRestrictionsInAffiliatedDepartments(
    department: Department,
    restrictions: Seq[ScalaRestriction],
    orders: Seq[ScalaOrder] = Seq(),
    maxResults: Int = 50,
    startResult: Int = 0
  ): (Int, Seq[StudentMember])

  def findAllStudentsByRestrictions(department: Department, restrictions: Seq[ScalaRestriction], orders: Seq[ScalaOrder] = Seq()): Seq[StudentMember]

  def findAllUniversityIdsByRestrictions(restrictions: Seq[ScalaRestriction], orders: Seq[ScalaOrder] = Seq()): Seq[String]

  def findAllMembersByRestrictions(
    restrictions: Seq[ScalaRestriction],
    orders: Seq[ScalaOrder],
    maxResults: Int = 50,
    startResult: Int = 0
  ): Seq[Member]

  def findAllMembersByRestrictionsInAffiliatedDepartments(
    department: Department,
    restrictions: Seq[ScalaRestriction],
    orders: Seq[ScalaOrder],
    maxResults: Int = 50,
    startResult: Int = 0
  ): Seq[Member]

  def countAllMembersByRestrictions(restrictions: Seq[ScalaRestriction]): Int

  def countAllMembersByRestrictionsInAffiliatedDepartments(department: Department, restrictions: Seq[ScalaRestriction]): Int

  def findAllUserIdsByRestrictions(restrictions: Seq[ScalaRestriction]): Seq[String]

  def findAllUniversityIdsByRestrictionsInAffiliatedDepartments(department: Department, restrictions: Seq[ScalaRestriction], orders: Seq[ScalaOrder] = Seq()): Seq[String]

  def findAllUniversityIdsByRestrictionsInTouchedDepartmentsOrModules(department: Department, modules: Set[Module], year: AcademicYear, restrictions: Seq[ScalaRestriction], orders: Seq[ScalaOrder] = Seq()): Seq[String]

  def findAllUserIdsByRestrictionsInAffiliatedDepartments(department: Department, restrictions: Seq[ScalaRestriction]): Seq[String]

  def findAllStudentDataByRestrictionsInAffiliatedDepartments(department: Department, restrictions: Seq[ScalaRestriction], academicYear: AcademicYear): Seq[AttendanceMonitoringStudentData]

  def getSCDsByAgentRelationshipAndRestrictions(
    relationshipType: StudentRelationshipType,
    agent: Member,
    restrictions: Seq[ScalaRestriction]
  ): Seq[StudentCourseDetails]

  def findAllUniversityIdsByRestrictions(department: Department, restrictions: Seq[ScalaRestriction]): Seq[String]

  def findStaffMembersWithAssistant(user: User): Seq[StaffMember]

  def allModesOfAttendance(department: Department): Seq[ModeOfAttendance]

  def allSprStatuses(department: Department): Seq[SitsStatus]

  def getDisability(code: String): Option[Disability]

  def findUsercodesInHomeDepartment(department: Department): Seq[String]

  def findStaffUsercodesInHomeDepartment(department: Department): Seq[String]

  @deprecated("TeachingStaff attribute is not reliable", since = "2018.2.2") def findTeachingStaffUsercodesInHomeDepartment(department: Department): Seq[String]

  @deprecated("TeachingStaff attribute is not reliable", since = "2018.2.2") def findAdminStaffUsercodesInHomeDepartment(department: Department): Seq[String]

  def findUndergraduatesUsercodesInHomeDepartment(department: Department): Seq[String]

  def findTaughtPostgraduatesUsercodesInHomeDepartment(department: Department): Seq[String]

  def findResearchPostgraduatesUsercodesInHomeDepartment(department: Department): Seq[String]

  def findUndergraduatesUsercodesInHomeDepartmentByLevel(department: Department, levelCode: String): Seq[String]

  def findFinalistUndergraduateUsercodesInHomeDepartment(department: Department): Seq[String]

  def findUndergraduateUsercodes(): Seq[String]

  def findUndergraduatesUsercodesByLevel(level: String): Seq[String]

  def findFinalistUndergraduateUsercodes(): Seq[String]
}

abstract class AbstractProfileService extends ProfileService with Logging {

  self: MemberDaoComponent
    with StudentCourseDetailsDaoComponent
    with StaffAssistantsHelpers =>

  @Autowired var profileQueryService: ProfileQueryService = _

  def getMemberByUniversityId(universityId: String, disableFilter: Boolean = false, eagerLoad: Boolean = false): Option[Member] = transactional(readOnly = true) {
    memberDao.getByUniversityId(universityId, disableFilter, eagerLoad)
  }

  def getAllMembersByUniversityIds(universityIds: Seq[String], disableFilter: Boolean = false, eagerLoad: Boolean = false): Seq[Member] = transactional(readOnly = true) {
    memberDao.getByUniversityIds(universityIds, disableFilter, eagerLoad)
  }

  def getMemberByUniversityIdStaleOrFresh(universityId: String): Option[Member] = transactional(readOnly = true) {
    memberDao.getByUniversityIdStaleOrFresh(universityId)
  }

  def getAllMembersWithUniversityIds(universityIds: Seq[String]): Seq[Member] = transactional(readOnly = true) {
    memberDao.getAllWithUniversityIds(universityIds)
  }

  def getAllMembersWithUniversityIdsStaleOrFresh(universityIds: Seq[String]): Seq[Member] = transactional(readOnly = true) {
    memberDao.getAllWithUniversityIdsStaleOrFresh(universityIds)
  }

  def getAllMembersWithUserId(userId: String, disableFilter: Boolean = false, eagerLoad: Boolean = false, activeOnly: Boolean = true): Seq[Member] = transactional(readOnly = true) {
    memberDao.getAllByUserId(userId, disableFilter, eagerLoad, activeOnly)
  }

  def getAllMembersWithUserIds(userIds: Seq[String], disableFilter: Boolean = false, eagerLoad: Boolean = false, activeOnly: Boolean = true): Seq[Member] = transactional(readOnly = true) {
    memberDao.getAllByUserIds(userIds, disableFilter, eagerLoad, activeOnly)
  }

  def getMemberByUser(user: User, disableFilter: Boolean = false, eagerLoad: Boolean = false, activeOnly: Boolean = true): Option[Member] = {
    val allMembers = getAllMembersWithUserId(user.getUserId, disableFilter, eagerLoad, activeOnly)
    val usercodeMatch =
      allMembers.find(_.universityId == user.getWarwickId)
        .orElse(allMembers.headOption) // TAB-1716

    if (usercodeMatch.isDefined || !user.getWarwickId.hasText) {
      usercodeMatch
    } else {
      // TAB-2014 look for a universityId match, but only return it if the email address matches
      getMemberByUniversityId(user.getWarwickId, disableFilter, eagerLoad)
        .filter(_.email.safeTrim.safeLowercase == user.getEmail.safeTrim.safeLowercase)
    }
  }

  def getStudentBySprCode(sprCode: String): Option[StudentMember] = transactional(readOnly = true) {
    studentCourseDetailsDao.getStudentBySprCode(sprCode)
  }

  def getMemberByTimetableHash(timetableHash: String): Option[Member] = {
    memberDao.getMemberByTimetableHash(timetableHash)
  }

  def regenerateTimetableHash(member: Member): Unit = memberDao.setTimetableHash(member, UUID.randomUUID.toString)

  def findMembersByQuery(query: String, departments: Seq[Department], userTypes: Set[MemberUserType], searchAllDepts: Boolean, activeOnly: Boolean): Seq[Member] = transactional(readOnly = true) {
    profileQueryService.find(query, departments, userTypes, searchAllDepts, activeOnly)
  }

  def findMembersByDepartment(department: Department, includeTouched: Boolean, userTypes: Set[MemberUserType]): Seq[Member] = transactional(readOnly = true) {
    profileQueryService.find(department, includeTouched, userTypes)
  }

  def listMembersUpdatedSince(startDate: DateTime, max: Int): Seq[Member] = transactional(readOnly = true) {
    memberDao.listUpdatedSince(startDate, max)
  }

  def save(member: Member): Unit = memberDao.saveOrUpdate(member)

  def countStudentsByDepartment(department: Department): Int = transactional(readOnly = true) {
    memberDao.getStudentsByDepartment(department.rootDepartment).count(s => department.filterRule.matches(s, Option(department), None))
  }

  def getStudentsByRoute(route: Route): Seq[StudentMember] = transactional(readOnly = true) {
    studentCourseDetailsDao.getByRoute(route)
      .filter { s => s.statusOnRoute != null && !SitsStatus.isPermanentlyWithdrawnStatusOnRoute(s.statusOnRoute) }
      .filter(s => s.mostSignificant == JBoolean(Option(true)))
      .map(_.student)
  }

  def getStudentsByRoute(route: Route, academicYear: AcademicYear): Seq[StudentMember] = transactional(readOnly = true) {
    studentCourseDetailsDao.getByRoute(route)
      .filter { s => s.statusOnRoute != null && !SitsStatus.isPermanentlyWithdrawnStatusOnRoute(s.statusOnRoute) }
      .filter(s => s.mostSignificant == JBoolean(Option(true)))
      .filter(_.freshStudentCourseYearDetails.exists(s => s.academicYear == academicYear))
      .map(_.student)
  }

  def getStudentCourseDetailsByScjCode(scjCode: String): Option[StudentCourseDetails] =
    studentCourseDetailsDao.getByScjCode(scjCode)

  def getStudentCourseDetailsBySprCode(sprCode: String): Seq[StudentCourseDetails] =
    studentCourseDetailsDao.getBySprCode(sprCode)

  private def studentDepartmentFilterMatches(department: Department)(member: StudentMember) = department.filterRule.matches(member, Option(department), None)

  /**
    * this returns a tuple of the startResult (offset into query) actually returned, with the resultset itself
    */
  def findStudentsByRestrictions(
    department: Department,
    academicYear: AcademicYear,
    restrictions: Seq[ScalaRestriction],
    orders: Seq[ScalaOrder] = Seq(),
    maxResults: Int = 50,
    startResult: Int = 0
  ): (Int, Seq[StudentMember]) = transactional(readOnly = true) {
    val allRestrictions = ScalaRestriction.is(
      "studentCourseYearDetails.enrolmentDepartment", department.rootDepartment,
      FiltersStudents.AliasPaths("studentCourseYearDetails"): _*
    ) ++ ScalaRestriction.is(
      "studentCourseYearDetails.academicYear", academicYear,
      FiltersStudents.AliasPaths("studentCourseYearDetails"): _*
    ) ++ department.filterRule.restriction(FiltersStudents.AliasPaths, Some(department)) ++ restrictions

    val offsetStudents = memberDao.findStudentsByRestrictions(allRestrictions, orders, maxResults, startResult)

    if (offsetStudents.nonEmpty) {
      (startResult, offsetStudents)
    } else {
      // meh, have to hit DAO twice if no results for this offset, but at least this should be a rare occurrence
      val unoffsetStudents = memberDao.findStudentsByRestrictions(allRestrictions, orders, maxResults, 0)
      if (unoffsetStudents.isEmpty) {
        (0, Seq())
      } else {
        (0, unoffsetStudents)
      }
    }
  }

  /**
    * this returns a tuple of the startResult (offset into query) actually returned, with the resultset itself
    */
  def findStudentsByRestrictionsInAffiliatedDepartments(
    department: Department,
    restrictions: Seq[ScalaRestriction],
    orders: Seq[ScalaOrder] = Seq(),
    maxResults: Int = 50,
    startResult: Int = 0
  ): (Int, Seq[StudentMember]) = transactional(readOnly = true) {
    val allRestrictions = affiliatedDepartmentsRestriction(department, restrictions) ++
      department.filterRule.restriction(FiltersStudents.AliasPaths, Some(department))

    val offsetStudents = memberDao.findStudentsByRestrictions(allRestrictions, orders, maxResults, startResult)

    if (offsetStudents.nonEmpty) {
      (startResult, offsetStudents)
    } else {
      // meh, have to hit DAO twice if no results for this offset, but at least this should be a rare occurrence
      val unoffsetStudents = memberDao.findStudentsByRestrictions(allRestrictions, orders, maxResults, 0)
      if (unoffsetStudents.isEmpty) {
        (0, Seq())
      } else {
        (0, unoffsetStudents)
      }
    }
  }

  def findAllStudentsByRestrictions(
    department: Department,
    restrictions: Seq[ScalaRestriction],
    orders: Seq[ScalaOrder] = Seq()
  ): Seq[StudentMember] = transactional(readOnly = true) {
    if (!department.isImportDepartment) {
      val allRestrictions = ScalaRestriction.is(
        "studentCourseYearDetails.enrolmentDepartment", department.rootDepartment,
        FiltersStudents.AliasPaths("studentCourseYearDetails"): _*
      ) ++ restrictions

      memberDao.findStudentsByRestrictions(allRestrictions, orders, Int.MaxValue, 0)
        .filter(studentDepartmentFilterMatches(department))
    } else {
      val allRestrictions = ScalaRestriction.is(
        "studentCourseYearDetails.enrolmentDepartment", department,
        FiltersStudents.AliasPaths("studentCourseYearDetails"): _*
      ) ++ restrictions

      memberDao.findStudentsByRestrictions(allRestrictions, orders, Int.MaxValue, 0)
    }
  }

  def getSCDsByAgentRelationshipAndRestrictions(
    relationshipType: StudentRelationshipType,
    agent: Member,
    restrictions: Seq[ScalaRestriction]
  ): Seq[StudentCourseDetails] = transactional(readOnly = true) {
    memberDao.getSCDsByAgentRelationshipAndRestrictions(relationshipType, agent.id, restrictions)
  }

  private def affiliatedDepartmentsRestriction(department: Department, restrictions: Seq[ScalaRestriction]) = {
    val queryDepartment = department.rootDepartment

    val departmentRestriction = Aliasable.addAliases(
      new ScalaRestriction(
        org.hibernate.criterion.Restrictions.or(
          HibernateHelpers.is("studentCourseYearDetails.enrolmentDepartment", queryDepartment),
          HibernateHelpers.is("route.adminDepartment", queryDepartment),
          HibernateHelpers.is("homeDepartment", queryDepartment),
          HibernateHelpers.is("department.parent", queryDepartment),
          org.hibernate.criterion.Restrictions.and(
            HibernateHelpers.is("route.teachingDepartmentsActive", true),
            HibernateHelpers.is("teachingInfo.department", queryDepartment)
          )
        )
      ),
      Seq(
        FiltersStudents.AliasPaths("studentCourseYearDetails"),
        FiltersStudents.AliasPaths("route"),
        FiltersStudents.AliasPaths("teachingInfo"),
        FiltersStudents.AliasPaths("department")
      ).flatten: _*
    )

    Seq(departmentRestriction) ++ restrictions
  }

  def findAllUniversityIdsByRestrictions(department: Department, restrictions: Seq[ScalaRestriction]): Seq[String] = transactional(readOnly = true) {
    memberDao.findUniversityIdsByRestrictions(
      ScalaRestriction.is(
        "studentCourseYearDetails.enrolmentDepartment", department.rootDepartment,
        FiltersStudents.AliasPaths("studentCourseYearDetails"): _*
      ) ++ restrictions
    )
  }

  def findAllUniversityIdsByRestrictions(
    restrictions: Seq[ScalaRestriction],
    orders: Seq[ScalaOrder] = Seq()
  ): Seq[String] = transactional(readOnly = true) {
    memberDao.findUniversityIdsByRestrictions(restrictions, orders)
  }

  def findAllMembersByRestrictions(
    restrictions: Seq[ScalaRestriction],
    orders: Seq[ScalaOrder],
    maxResults: Int,
    startResult: Int
  ): Seq[Member] = transactional(readOnly = true) {
    memberDao.findAllMembersByRestrictions(restrictions, orders, maxResults, startResult)
  }

  def findAllMembersByRestrictionsInAffiliatedDepartments(
    department: Department,
    restrictions: Seq[ScalaRestriction],
    orders: Seq[ScalaOrder],
    maxResults: Int,
    startResult: Int
  ): Seq[Member] = transactional(readOnly = true) {

    val allRestrictions = affiliatedDepartmentsRestriction(department, restrictions) ++
      department.filterRule.restriction(FiltersStudents.AliasPaths, Some(department))

    memberDao.findAllMembersByRestrictions(allRestrictions, orders, maxResults, startResult)
  }

  def countAllMembersByRestrictions(restrictions: Seq[ScalaRestriction]): Int =
    memberDao.countAllMembersByRestrictions(restrictions)

  def countAllMembersByRestrictionsInAffiliatedDepartments(department:Department, restrictions: Seq[ScalaRestriction]): Int = {
    val allRestrictions = affiliatedDepartmentsRestriction(department, restrictions) ++
      department.filterRule.restriction(FiltersStudents.AliasPaths, Some(department))
    memberDao.countAllMembersByRestrictions(allRestrictions)
  }

  def findAllUserIdsByRestrictions(
    restrictions: Seq[ScalaRestriction]
  ): Seq[String] = transactional(readOnly = true) {
    memberDao.findAllUsercodesByRestrictions(restrictions)
  }

  def findAllUniversityIdsByRestrictionsInAffiliatedDepartments(
    department: Department,
    restrictions: Seq[ScalaRestriction],
    orders: Seq[ScalaOrder] = Seq()
  ): Seq[String] = transactional(readOnly = true) {

    val allRestrictions = affiliatedDepartmentsRestriction(department, restrictions) ++
      department.filterRule.restriction(FiltersStudents.AliasPaths, Some(department))

    memberDao.findUniversityIdsByRestrictions(allRestrictions, orders)
  }

  def findAllUniversityIdsByRestrictionsInTouchedDepartmentsOrModules(
    department: Department,
    modules: Set[Module],
    year: AcademicYear,
    restrictions: Seq[ScalaRestriction],
    orders: Seq[ScalaOrder] = Seq()
  ): Seq[String] = transactional(readOnly = true) {

    val deptRestrictions = affiliatedDepartmentsRestriction(department, restrictions) ++
      department.filterRule.restriction(FiltersStudents.AliasPaths, Some(department))

    val moduleRestrictions = inIfNotEmptyMultipleProperties(
      Seq("moduleRegistration.module", "moduleRegistration.academicYear", "moduleRegistration.deleted"),
      Seq(modules.toSeq, Seq(year), Seq(false)),
      FiltersStudents.AliasPaths("moduleRegistration"): _*
    ).toSeq ++ restrictions

    val allRestrictions = if(modules.nonEmpty) {
      ScalaRestriction.custom(
        disjunction(conjunction(deptRestrictions.map(_.underlying): _*), conjunction(moduleRestrictions.map(_.underlying): _*)),
        deptRestrictions.flatMap(_.aliases.toSeq) ++ moduleRestrictions.flatMap(_.aliases.toSeq) : _*
      ).toSeq
    } else {
      deptRestrictions
    }

    memberDao.findUniversityIdsByRestrictions(allRestrictions, orders)
  }


  override def findAllUserIdsByRestrictionsInAffiliatedDepartments(
    department: Department,
    restrictions: Seq[ScalaRestriction]
  ): Seq[String] = {
    val allRestrictions = affiliatedDepartmentsRestriction(department, restrictions) ++
      department.filterRule.restriction(FiltersStudents.AliasPaths, Some(department))
    memberDao.findAllUsercodesByRestrictions(allRestrictions)
  }

  def findAllStudentDataByRestrictionsInAffiliatedDepartments(department: Department, restrictions: Seq[ScalaRestriction], academicYear: AcademicYear): Seq[AttendanceMonitoringStudentData] = {
    val allRestrictions = affiliatedDepartmentsRestriction(department, restrictions) ++
      department.filterRule.restriction(FiltersStudents.AliasPaths, Some(department))

    memberDao.findAllStudentDataByRestrictions(allRestrictions, academicYear: AcademicYear)
  }

  def countStudentsByRestrictions(department: Department, academicYear: AcademicYear, restrictions: Seq[ScalaRestriction]): Int = transactional(readOnly = true) {
    val allRestrictions = ScalaRestriction.is(
      "studentCourseYearDetails.enrolmentDepartment", department.rootDepartment,
      FiltersStudents.AliasPaths("studentCourseYearDetails"): _*
    ) ++ ScalaRestriction.is(
      "studentCourseYearDetails.academicYear", academicYear,
      FiltersStudents.AliasPaths("studentCourseYearDetails"): _*
    ) ++ department.filterRule.restriction(FiltersStudents.AliasPaths, Some(department)) ++ restrictions

    memberDao.countStudentsByRestrictions(allRestrictions)
  }

  def countStudentsByRestrictionsInAffiliatedDepartments(department: Department, restrictions: Seq[ScalaRestriction]): Int = transactional(readOnly = true) {
    val allRestrictions = affiliatedDepartmentsRestriction(department, restrictions) ++
      department.filterRule.restriction(FiltersStudents.AliasPaths, Some(department))

    memberDao.countStudentsByRestrictions(allRestrictions)
  }

  def findStaffMembersWithAssistant(user: User): Seq[StaffMember] = staffAssistantsHelper.findBy(user)

  def allModesOfAttendance(department: Department): Seq[ModeOfAttendance] = transactional(readOnly = true) {
    memberDao.getAllModesOfAttendance(department).filter(_ != null)
  }

  def allSprStatuses(department: Department): Seq[SitsStatus] = transactional(readOnly = true) {
    memberDao.getAllSprStatuses(department).filter(_ != null)
  }

  def getDisability(code: String): Option[Disability] = transactional(readOnly = true) {
    // lookup disability iff a non-null code is passed, otherwise fallback to None - I <3 scala options and flatMap
    Option(code).flatMap(memberDao.getDisability)
  }

  override def findUsercodesInHomeDepartment(department: Department): Seq[String] = transactional(readOnly = true) {
    memberDao.findAllUsercodesByRestrictions(Seq(
      ScalaRestriction.is("homeDepartment", department.rootDepartment).get,
      ScalaRestriction.inIfNotEmpty("userType", Seq(MemberUserType.Staff, MemberUserType.Student, MemberUserType.Emeritus)).get
    ))
  }

  override def findStaffUsercodesInHomeDepartment(department: Department): Seq[String] = transactional(readOnly = true) {
    memberDao.findAllUsercodesByRestrictions(Seq(
      ScalaRestriction.is("homeDepartment", department.rootDepartment).get
    ), staffOnly = true)
  }

  override def findTeachingStaffUsercodesInHomeDepartment(department: Department): Seq[String] = transactional(readOnly = true) {
    memberDao.findAllUsercodesByRestrictions(Seq(
      ScalaRestriction.is("homeDepartment", department.rootDepartment).get,
      ScalaRestriction.is("teachingStaff", true).get
    ), staffOnly = true)
  }

  override def findAdminStaffUsercodesInHomeDepartment(department: Department): Seq[String] = transactional(readOnly = true) {
    memberDao.findAllUsercodesByRestrictions(Seq(
      ScalaRestriction.is("homeDepartment", department.rootDepartment).get,
      ScalaRestriction.is("teachingStaff", false).get
    ), staffOnly = true)
  }

  override def findUndergraduatesUsercodesInHomeDepartment(department: Department): Seq[String] = transactional(readOnly = true) {
    memberDao.findAllUsercodesByRestrictions(Seq(
      ScalaRestriction.is("homeDepartment", department.rootDepartment).get,
      new ScalaRestriction(HibernateHelpers.like("groupName", "Undergraduate%"))
    ), studentOnly = true)
  }

  override def findTaughtPostgraduatesUsercodesInHomeDepartment(department: Department): Seq[String] = transactional(readOnly = true) {
    memberDao.findAllUsercodesByRestrictions(Seq(
      ScalaRestriction.is("homeDepartment", department.rootDepartment).get,
      new ScalaRestriction(HibernateHelpers.like("groupName", "Postgraduate (taught)%"))
    ), studentOnly = true)
  }

  override def findResearchPostgraduatesUsercodesInHomeDepartment(department: Department): Seq[String] = transactional(readOnly = true) {
    memberDao.findAllUsercodesByRestrictions(Seq(
      ScalaRestriction.is("homeDepartment", department.rootDepartment).get,
      new ScalaRestriction(HibernateHelpers.like("groupName", "Postgraduate (research)%"))
    ), studentOnly = true)
  }

  override def findUndergraduateUsercodes(): Seq[String] = transactional(readOnly = true) {
    memberDao.findAllUsercodesByRestrictions(Seq(
      new ScalaRestriction(HibernateHelpers.like("groupName", "Undergraduate%"))
    ), studentOnly = true)
  }

  override def findUndergraduatesUsercodesByLevel(levelCode: String): Seq[String] = transactional(readOnly = true) {
    memberDao.findUndergraduateUsercodesByLevel(levelCode)
  }

  override def findFinalistUndergraduateUsercodes(): Seq[String] = transactional(readOnly = true) {
    memberDao.findFinalistUndergraduateUsercodes()
  }

  override def findUndergraduatesUsercodesInHomeDepartmentByLevel(department: Department, levelCode: String): Seq[String] = transactional(readOnly = true) {
    memberDao.findUndergraduateUsercodesByHomeDepartmentAndLevel(department, levelCode)
  }

  override def findFinalistUndergraduateUsercodesInHomeDepartment(department: Department): Seq[String] = transactional(readOnly = true) {
    memberDao.findFinalistUndergraduateUsercodesByHomeDepartment(department)
  }
}

trait StaffAssistantsHelpers {
  val staffAssistantsHelper: UserGroupMembershipHelperMethods[StaffMember]
}

trait StaffAssistantsHelpersImpl extends StaffAssistantsHelpers {
  lazy val staffAssistantsHelper = new UserGroupMembershipHelper[StaffMember]("_assistantsGroup")
}

trait ProfileServiceComponent {
  def profileService: ProfileService
}

trait AutowiringProfileServiceComponent extends ProfileServiceComponent {
  var profileService: ProfileService = Wire[ProfileService]
}

@Service("profileService")
class ProfileServiceImpl
  extends AbstractProfileService
    with AutowiringMemberDaoComponent
    with AutowiringStudentCourseDetailsDaoComponent
    with StaffAssistantsHelpersImpl
