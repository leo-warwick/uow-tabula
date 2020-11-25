package uk.ac.warwick.tabula.data.model

import freemarker.core.TemplateHTMLOutputModel
import javax.persistence.CascadeType._
import javax.persistence._
import org.apache.commons.lang3.builder.{EqualsBuilder, HashCodeBuilder}
import org.hibernate.annotations.{BatchSize, Filter, FilterDef, FilterDefs, Filters, Proxy, Type}
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, ExamGridEntityYear}
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringCheckpointTotal
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{Logging, RequestLevelCache}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{Restricted, RestrictionProvider}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, ToString}
import uk.ac.warwick.userlookup.User

import scala.collection.mutable
import scala.jdk.CollectionConverters._

object Member {
  final val StudentsOnlyFilter = "studentsOnly"
  final val ActiveOnlyFilter = "activeOnly"
  final val FreshOnlyFilter = "freshMemberOnly"
}

/**
  * Notes about the notDeleted filter:
  * filters don't run on session.get() but getById will check for you.
  * queries will only include it if it's the entity after "from" and not
  * some other secondary entity joined on. It's usually possible to flip the
  * query around to make this work.
  */
@FilterDefs(Array(
  new FilterDef(name = Member.StudentsOnlyFilter, defaultCondition = "usertype = 'S'"),
  new FilterDef(name = Member.ActiveOnlyFilter, defaultCondition = "(inuseflag = 'Active' or inuseflag like 'Inactive - Starts %')"),
  new FilterDef(name = Member.FreshOnlyFilter, defaultCondition = "missingFromImportSince is null")
))
@Filters(Array(
  new Filter(name = Member.StudentsOnlyFilter),
  new Filter(name = Member.ActiveOnlyFilter),
  new Filter(name = Member.FreshOnlyFilter)
))
@Entity
@Proxy
@Access(AccessType.FIELD)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
  name = "userType",
  discriminatorType = DiscriminatorType.STRING
)
abstract class Member
  extends MemberProperties
    with HasSettings
    with PostLoadBehaviour
    with ToString
    with HibernateVersioned
    with PermissionsTarget
    with CanBeStale
    with Logging
    with Serializable {

  @transient
  var profileService: ProfileService with StaffAssistantsHelpers = Wire[ProfileService with StaffAssistantsHelpers]

  @transient
  var relationshipService: RelationshipService = Wire[RelationshipService]

  @transient
  var courseAndRouteService: CourseAndRouteService = Wire[CourseAndRouteService]

  def this(user: CurrentUser) = {
    this()

    this.userId = user.apparentId
    this.firstName = user.firstName
    this.lastName = user.lastName
    this.universityId = user.universityId
    this.email = user.email
    this.userType =
      if (user.isStudent) MemberUserType.Student
      else if (user.isStaff) MemberUserType.Staff
      else MemberUserType.Other
  }

  def this(id: String) = {
    this()
    this.universityId = id
  }

  var lastUpdatedDate: DateTime = DateTime.now

  var missingFromImportSince: DateTime = _

  var lastImportDate: DateTime = _

  def fullName: Option[String] = {
    (Option(firstName) ++ Option(lastName)).toList match {
      case Nil => None
      case names => Some(names.mkString(" "))
    }
  }

  def routeName: String = ""

  def description: String = {
    val userTypeString =
      if (userType == MemberUserType.Staff && Option(jobTitle).isDefined) jobTitle
      else Option(groupName).getOrElse("")

    val deptName = Option(homeDepartment).fold("")(", " + _.name)
    userTypeString + routeName + deptName
  }

  /**
    * Get all departments that this member is affiliated to.
    * (Overriden by StudentMember).
    */
  def affiliatedDepartments: LazyList[Department] = homeDepartment match {
    case null => LazyList.empty
    case _ => LazyList(homeDepartment)
  }

  /**
    * Get all departments that this member touches.
    * (Overridden by StudentMember).
    */
  def touchedDepartments: LazyList[Department] = {
    val topLevelDepts = affiliatedDepartments
    topLevelDepts flatMap (_.subDepartmentsContaining(this))
  }

  def permissionsParents: LazyList[PermissionsTarget] = touchedDepartments

  override def humanReadableId: String = fullName.getOrElse(toString())

  override final def urlCategory = "member"

  override def postLoad(): Unit = {
    ensureSettings
  }

  def asSsoUser: User = {
    val u = new User
    u.setUserId(userId)
    u.setWarwickId(universityId)
    u.setFirstName(firstName)
    u.setLastName(lastName)
    u.setFullName(fullName match {
      case None => "[Unknown user]"
      case Some(name) => name
    })
    u.setEmail(email)
    Option(homeDepartment) foreach { dept =>
      u.setDepartment(dept.name)
      u.setDepartmentCode(dept.code.toUpperCase)
    }

    u.setLoginDisabled(!active)
    userType match {
      case MemberUserType.Staff =>
        u.setStaff(true)
        u.setUserType("Staff")
      case MemberUserType.Emeritus =>
        u.setStaff(true)
        u.setUserType("Staff")
      case MemberUserType.Student =>
        u.setStudent(true)
        u.setUserType("Student")
      case MemberUserType.Applicant =>
        u.setUserType("Applicant")
      case _ => u.setUserType("External")
    }

    u.setExtraProperties(JHashMap(
      "urn:websignon:usertype" -> u.getUserType,
      "urn:websignon:timestamp" -> DateTime.now.toString,
      "urn:websignon:usersource" -> "Tabula"
    ))

    u.setVerified(true)
    u.setFoundUser(true)
    u
  }

  def toStringProps: Seq[(String, Any)] = Seq(
    "universityId" -> universityId
  )

  def isStaff: Boolean = userType == MemberUserType.Staff

  def isStudent: Boolean = userType == MemberUserType.Student

  def isApplicant: Boolean = userType == MemberUserType.Applicant

  def isRelationshipAgent(relationshipType: StudentRelationshipType): Boolean = {
    relationshipService.listCurrentStudentRelationshipsWithMember(relationshipType, this).nonEmpty
  }

  def active: Boolean = activeNow || activeLater

  def activeNow: Boolean = inUseFlag == "Active"

  def activeLater: Boolean = inUseFlag.hasText && inUseFlag.startsWith("Inactive - Starts")

  // Overridden in StudentMember
  def hasRelationship(relationshipType: StudentRelationshipType) = false

  override final def equals(other: Any): Boolean = other match {
    case that: Member =>
      new EqualsBuilder()
        .append(universityId, that.universityId)
        .build()
    case _ => false
  }

  override final def hashCode: Int =
    new HashCodeBuilder()
      .append(universityId)
      .build()
}

@Entity
@Proxy
@DiscriminatorValue("S")
class StudentMember extends Member with StudentProperties {
  this.userType = MemberUserType.Student

  // made this private as can't think of any instances in the app where you wouldn't prefer freshStudentCourseDetails
  @OneToMany(mappedBy = "student", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  @BatchSize(size = 200)
  private val studentCourseDetails: JSet[StudentCourseDetails] = JHashSet()

  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  def freshStudentCourseDetails: Seq[StudentCourseDetails] = {
    studentCourseDetails.asScala.filter(scd => !scd.stale).toSeq.sorted
  }

  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  def freshOrStaleStudentCourseDetails: mutable.Set[StudentCourseDetails] = studentCourseDetails.asScala

  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  def freshOrStaleStudentCourseYearDetails(year: AcademicYear): mutable.Set[StudentCourseYearDetails] =
    freshOrStaleStudentCourseDetails
      .flatMap(_.freshOrStaleStudentCourseYearDetails)
      .filter(_.academicYear == year)

  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  def allFreshStudentCourseYearDetailsForYear(year: AcademicYear): Seq[StudentCourseYearDetails] =
    freshOrStaleStudentCourseYearDetails(year).toSeq.filter(scyd => !scyd.stale)

  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  def freshStudentCourseYearDetailsForYear(year: AcademicYear): Option[StudentCourseYearDetails] =
    allFreshStudentCourseYearDetailsForYear(year).lastOption

  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  def freshOrStaleStudentCourseYearDetailsForYear(year: AcademicYear): Option[StudentCourseYearDetails] =
    freshOrStaleStudentCourseYearDetails(year).lastOption

  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  def freshOrStaleStudentCourseYearDetailsFrom(year: AcademicYear): mutable.Set[StudentCourseYearDetails] =
    freshOrStaleStudentCourseDetails
      .flatMap(_.freshOrStaleStudentCourseYearDetails)
      .filter(_.academicYear >= year)

  @OneToOne(fetch = FetchType.LAZY) // don't cascade, cascaded separately
  @JoinColumn(name = "mostSignificantCourse")
  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  var mostSignificantCourse: StudentCourseDetails = _

  @Restricted(Array("Profiles.Read.Tier4VisaRequirement"))
  def casUsed: Option[Boolean] = {
    mostSignificantCourseDetails.flatMap(scd => scd.latestStudentCourseYearDetails.casUsed match {
      case null => None
      case casUsed => Some(casUsed)
    })
  }

  @Restricted(Array("Profiles.Read.Tier4VisaRequirement"))
  def hasTier4Visa: Option[Boolean] = {
    mostSignificantCourseDetails.flatMap(scd => scd.latestStudentCourseYearDetails.tier4Visa match {
      case null => None
      case hasTier4Visa => Some(hasTier4Visa)
    })
  }

  // Both of the below unused directly but needed as an association for filtering

  @OneToMany(mappedBy = "student", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @Restricted(Array("MonitoringPoints.View"))
  @BatchSize(size = 200)
  var attendanceCheckpointTotals: JSet[AttendanceMonitoringCheckpointTotal] = JHashSet()

  @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @Restricted(Array("MemberNotes.Read"))
  @BatchSize(size = 200)
  var memberNotes: JSet[MemberNote] = JHashSet()

  def this(id: String) = {
    this()
    this.universityId = id
  }

  /**
    * Get all departments that this student is affiliated with at a departmental level.
    * This includes their home department, and the department running their course.
    */
  override def affiliatedDepartments: LazyList[Department] = {
    val sprDepartments: LazyList[Department] = freshStudentCourseDetails.flatMap(scd => Option(scd.department)).to(LazyList)
    val sceDepartments: LazyList[Department] = freshStudentCourseDetails.flatMap(_.freshStudentCourseYearDetails).flatMap(scyd => Option(scyd.enrolmentDepartment)).to(LazyList)
    val routeDepartments: LazyList[Department] = freshStudentCourseDetails.flatMap(scd => Option(scd.currentRoute)).flatMap(route => route.teachingDepartments).to(LazyList)
    val homeDepartments: LazyList[Department] = Option(homeDepartment).to(LazyList)

    (homeDepartments #:::
      sprDepartments #:::
      sceDepartments #:::
      routeDepartments
    ).distinct
  }

  /**
    * Get all departments that this student touches. This includes their home department,
    * the department running their course and any departments that they are taking modules in.
    *
    * For each department, enumerate any sub-departments that the member matches
    */
  override def touchedDepartments: LazyList[Department] = {
    def moduleDepts = registeredModulesByYear(None).map(_.adminDepartment).to(LazyList)

    val topLevelDepts = (affiliatedDepartments #::: moduleDepts).distinct
    topLevelDepts flatMap (_.subDepartmentsContaining(this))
  }

  override def permissionsParents: LazyList[PermissionsTarget] = {
    // TAB-3007 We shouldn't swim downstream to the StudentCourseDetails or StudentCourseYearDetails for things here,
    // rely on them swimming up.

    // TAB-3598 - Add study departments and routes for any current course
    val currentCourses = freshStudentCourseDetails.filterNot(_.isEnded)
    val studyDepartments = currentCourses.flatMap { scd => Option(scd.department) }
    val currentCourseRoutes: LazyList[PermissionsTarget] = currentCourses.flatMap { c => Option(c.currentRoute) }.to(LazyList)

    // Cache the def result
    val mostSignificantCourse = mostSignificantCourseDetails

    // Allow swimming down only for *current* information
    val latestStudentCourseYearDetails = mostSignificantCourse.flatMap { scd => Option(scd.latestStudentCourseYearDetails) }
    val enrolmentDepartment = latestStudentCourseYearDetails.flatMap { scyd => Option(scyd.enrolmentDepartment) }

    val departments: LazyList[PermissionsTarget] =
      (LazyList(Option(homeDepartment), enrolmentDepartment).flatten #::: studyDepartments.to(LazyList)).distinct.flatMap(_.subDepartmentsContaining(this))

    /*
     * FIXME TAB-2971 The modules from registrations here shouldn't be in permissionsParents, because the Module doesn't wholly
     * contain the student. As things stand, when a student registers for an option in another department that department gets
     * a whole heap of permissions on that student, which is wrong. We're really using this as a mechanism to show more information
     * to markers and module managers, which could be achieved via a separate role provider that searches for module registrations when
     * given the scope of a StudentMember.
     */
    val modules: LazyList[PermissionsTarget] = mostSignificantCourse.to(LazyList).flatMap { scd =>
      latestStudentCourseYearDetails.to(LazyList).flatMap { scyd =>
        // Only include module registrations for the latest year of the most significant course
        scd.moduleRegistrationsByYear(Some(scyd.academicYear)).map(_.module)
      }
    }

    departments #::: modules #::: currentCourseRoutes
  }

  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  def mostSignificantCourseDetails: Option[StudentCourseDetails] = Option(mostSignificantCourse).filter(course => !course.stale)

  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  def mostSignificantCourseDetailsForYear(academicYear: AcademicYear): Option[StudentCourseDetails] =
    freshStudentCourseDetails
      .filter(_.freshStudentCourseYearDetails.exists(_.academicYear == academicYear))
      .lastOption // because of the way that StudentCourseDetails are sorted, this is the "last" course details created

  def defaultYearDetails: Option[StudentCourseYearDetails] = mostSignificantCourseDetails.map(_.latestStudentCourseYearDetails)

  def hasCurrentEnrolment: Boolean = freshStudentCourseDetails.exists(_.hasCurrentEnrolment)

  // perm withdrawn if the member is inactive or
  // there are no fresh studentCourseDetails with a non-null route status that's not P
  def permanentlyWithdrawn: Boolean = {
    inUseFlag.matches("Inactive.*") ||
      (freshStudentCourseDetails.count(_.statusOnRoute == null) == 0 && // if they have a course details with null status they may be active
        freshStudentCourseDetails
          .filter(_.statusOnRoute != null)
          .map(_.statusOnRoute)
          .filter(_.code != null)
          .count(statusOnRoute => !statusOnRoute.code.startsWith("P") && !statusOnRoute.code.startsWith("D")) == 0
        )
  }

  override def hasRelationship(relationshipType: StudentRelationshipType): Boolean =
    studentCourseDetails.asScala.exists(_.hasRelationship(relationshipType))

  override def routeName: String = mostSignificantCourseDetails match {
    case Some(details) =>
      if (details != null && details.currentRoute != null) ", " + details.currentRoute.name
      else ""
    case _ => ""
  }

  def attachStudentCourseDetails(detailsToAdd: StudentCourseDetails): Unit = {
    studentCourseDetails.remove(detailsToAdd)
    studentCourseDetails.add(detailsToAdd)
  }

  def attachAllStudentCourseDetails(allDetailsToAdd: Seq[StudentCourseDetails]): Unit = {
    for (scd <- allDetailsToAdd) {
      studentCourseDetails.remove(scd)
      studentCourseDetails.add(scd)
    }
  }

  /**
    * Get all modules this this student is registered on, including historically.
    * TODO consider caching based on getLastUpdatedDate
    */
  def registeredModulesByYear(year: Option[AcademicYear]): Set[Module] =
    freshStudentCourseDetails.toSet[StudentCourseDetails].flatMap(_.registeredModulesByYear(year))

  def moduleRegistrationsByYear(year: Option[AcademicYear]): Set[ModuleRegistration] =
    freshStudentCourseDetails.toSet[StudentCourseDetails].flatMap(_.moduleRegistrationsByYear(year))

  def isPGR: Boolean = groupName == "Postgraduate (research) FT" || groupName == "Postgraduate (research) PT"

  def isPGT: Boolean = groupName == "Postgraduate (taught) FT" || groupName == "Postgraduate (taught) PT"

  def isUG: Boolean = groupName == "Undergraduate - full-time" || groupName == "Undergraduate - part-time"

  def toExamGridEntity(baseSCYD: StudentCourseYearDetails, basedOnLevel: Boolean = false, mitCircs: Seq[MitigatingCircumstancesSubmission] = Nil): ExamGridEntity =
    RequestLevelCache.cachedBy("StudentMember.toExamGridEntity", s"$universityId-${baseSCYD.id}-$basedOnLevel") {
      /** Level grids can be used for UG or non UG ( PG/F etc). If someone is on a UG course there is a possibility some scyd entry will be missing sometimes at the SITS end. We need to ensure that level entry is still there in the map otherwise
        * it will blow up further. Example - level grid 3 for UG but missing level 2 scyd. Map needs constructing with all three level entries. For PG level grids, there is always just one level that grid generates so you have one entry in the map.
        */
      val entitiesPerYear: Map[Int, ExamGridEntityYear] = ProgressionService.getEntityPerYear(baseSCYD, basedOnLevel, baseSCYD.yearOfStudy)

      val years: Map[Int, Option[ExamGridEntityYear]] = if (basedOnLevel) {
        // groups by level preserving the order in which they appear for this student
        // add index to the scyd list and group by level
        val ugLevel = baseSCYD.studentCourseDetails.courseType.contains(CourseType.UG)
        val relevantYears = if (ugLevel) baseSCYD.studyLevel.toInt else 1

        (1 to relevantYears).map { yr =>
          yr -> entitiesPerYear.get(yr).filter(_ != null)
        }.toMap
      } else {
        (1 to baseSCYD.yearOfStudy).map(year =>
          year -> entitiesPerYear.get(year).filter(_ != null)
        ).toMap
      }

      ExamGridEntity(
        firstName = Option(firstName).getOrElse("[Unknown]"),
        lastName = Option(lastName).getOrElse("[Unknown]"),
        universityId = universityId,
        lastImportDate = Option(lastImportDate),
        years = years,
        yearWeightings = courseAndRouteService.findAllCourseYearWeightings(Seq(baseSCYD.studentCourseDetails.course), baseSCYD.studentCourseDetails.sprStartAcademicYear), // year weightings based on the GRID course that we are generating
        mitigatingCircumstances = mitCircs
      )
    }
}

@Entity
@Proxy
@DiscriminatorValue("N")
class StaffMember extends Member with StaffProperties {
  this.userType = MemberUserType.Staff

  def this(id: String) = {
    this()
    this.universityId = id
  }

  @OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
  @JoinColumn(name = "assistantsgroup_id")
  private var _assistantsGroup: UserGroup = UserGroup.ofUsercodes

  def assistants: UnspecifiedTypeUserGroup = new UserGroupCacheManager(_assistantsGroup, profileService.staffAssistantsHelper)

  def assistants_=(group: UserGroup): Unit = {
    _assistantsGroup = group
  }
}

@Entity
@Proxy
@DiscriminatorValue("A")
class EmeritusMember extends Member with StaffProperties {
  this.userType = MemberUserType.Emeritus

  def this(id: String) = {
    this()
    this.universityId = id
  }
}

@Entity
@Proxy
@DiscriminatorValue("P")
class ApplicantMember extends Member with ApplicantProperties with RestrictedPhoneNumber {
  this.userType = MemberUserType.Applicant

  def this(id: String) = {
    this()
    this.universityId = id
  }
}

@Entity
@Proxy
@DiscriminatorValue("O")
class OtherMember extends Member with RestrictedPhoneNumber {
  this.userType = MemberUserType.Other

  def this(id: String) = {
    this()
    this.universityId = id
  }
}

class RuntimeMember(user: CurrentUser) extends Member(user) with RestrictedPhoneNumber {
  override def permissionsParents: LazyList[Nothing] = LazyList.empty
}

trait MemberProperties extends StringId {

  @Id var universityId: String = _

  def id: String = universityId

  @Restricted(Array("Profiles.Read.Usercode"))
  var userId: String = _

  var firstName: String = _
  var lastName: String = _
  var email: String = _

  @Restricted(Array("Profiles.Read.PrivateDetails"))
  var homeEmail: String = _

  var title: String = _

  @deprecated("firstName should be used", since = "2018.10.3")
  var fullFirstName: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.MemberUserTypeUserType")
  @Column(insertable = false, updatable = false)
  var userType: MemberUserType = _

  @deprecated("No systems should care about or display Gender", since = "2018.10.1")
  @Restricted(Array("Profiles.Read.Gender"))
  @Type(`type` = "uk.ac.warwick.tabula.data.model.GenderUserType")
  var gender: Gender = _

  var inUseFlag: String = _

  var inactivationDate: LocalDate = _

  var groupName: String = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "home_department_id")
  var homeDepartment: Department = _

  @Restricted(Array("Profiles.Read.PrivateDetails"))
  var dateOfBirth: LocalDate = _

  var jobTitle: String = _

  @RestrictionProvider("phoneNumberPermissions")
  var phoneNumber: String = _

  @Restricted(Array("Profiles.Read.PrivateDetails"))
  var nationality: String = _

  @Restricted(Array("Profiles.Read.PrivateDetails"))
  var secondNationality: String = _

  @Restricted(Array("Profiles.Read.MobileNumber"))
  var mobileNumber: String = _

  @Restricted(Array("Profiles.Read.TimetablePrivateFeed"))
  @Column(name = "timetable_hash")
  var timetableHash: String = _

  /**
    * The members current confirmed correspondence address in SITS. Can be entered by the user themselves so data quality is often poor.
    * Could be the same as StudentProperties.termtimeAddress
    */
  @OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
  @JoinColumn(name = "HOME_ADDRESS_ID")
  @Restricted(Array("Profiles.Read.HomeAndTermTimeAddresses"))
  var currentAddress: Address = _

  var deceased: Boolean = _

  def phoneNumberPermissions: Seq[Seq[Permission]]

}

trait ApplicantProperties {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "disability")
  @Restricted(Array("Profiles.Read.Disability"))
  private var _disability: Disability = _

  def disability_=(d: Disability): Unit = {
    _disability = d
  }

  @Restricted(Array("Profiles.Read.Disability"))
  def disability: Option[Disability] = Option(_disability)

  @Column(name = "disability_funding")
  @Type(`type` = "uk.ac.warwick.tabula.data.model.DisabilityFundingStatusUserType")
  private var _disabilityFundingStatus: DisabilityFundingStatus = _

  def disabilityFundingStatus_=(d: DisabilityFundingStatus): Unit = {
    _disabilityFundingStatus = d
  }

  @Restricted(Array("Profiles.Read.Disability"))
  def disabilityFundingStatus: Option[DisabilityFundingStatus] = Option(_disabilityFundingStatus)


}

trait StudentProperties extends ApplicantProperties with RestrictedPhoneNumber {

  /**
    * The student's hall of residence. Unlike address this is data entered into SITS by an Accommodation process so it should be in a reliable format.
    * We could rename this to hallOfResidence or similar but we would need to make sure that the Tabula API remains stable or any changes are managed properly
    */
  @OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
  @JoinColumn(name = "TERMTIME_ADDRESS_ID")
  @Restricted(Array("Profiles.Read.HomeAndTermTimeAddresses"))
  var termtimeAddress: Address = _

  @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = Array(ALL))
  @Restricted(Array("Profiles.Read.NextOfKin"))
  @BatchSize(size = 200)
  var nextOfKins: JList[NextOfKin] = JArrayList()

  @Column(name = "tier4_visa_requirement")
  @Restricted(Array("Profiles.Read.Tier4VisaRequirement"))
  var tier4VisaRequirement: JBoolean = _

  @Column(name = "reasonable_adjustments")
  @Type(`type` = "uk.ac.warwick.tabula.data.model.ReasonableAdjustmentsUserType")
  @Restricted(Array("Profiles.Read.ReasonableAdjustments"))
  var reasonableAdjustments: Set[ReasonableAdjustment] = _

  @Column(name = "reasonable_adjustments_notes")
  @Restricted(Array("Profiles.Read.ReasonableAdjustmentsNotes"))
  var reasonableAdjustmentsNotes: String = _

  def formattedReasonableAdjustmentsNotes: TemplateHTMLOutputModel = FormattedHtml(reasonableAdjustmentsNotes)
}

trait RestrictedPhoneNumber {
  def phoneNumberPermissions: Seq[Seq[Permission]] = Seq(Seq(Permissions.Profiles.Read.TelephoneNumber))
}

trait StaffProperties {
  // Anyone can view staff phone number
  def phoneNumberPermissions: Nil.type = Nil

  @deprecated("TeachingStaff attribute is not reliable", since = "2018.2.2") var teachingStaff: JBoolean = _
}
