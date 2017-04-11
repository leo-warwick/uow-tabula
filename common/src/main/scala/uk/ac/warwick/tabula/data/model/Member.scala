package uk.ac.warwick.tabula.data.model

import javax.persistence.CascadeType._
import javax.persistence.{CascadeType, Entity, _}

import org.apache.commons.lang3.builder.{EqualsBuilder, HashCodeBuilder}
import org.hibernate.annotations.{AccessType => _, Any => _, ForeignKey => _, _}
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringCheckpointTotal
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.model.permissions.MemberGrantedRole
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{Restricted, RestrictionProvider}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, ToString}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.mutable

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
@Access(AccessType.FIELD)
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
	name="userType",
	discriminatorType=DiscriminatorType.STRING
)
abstract class Member
	extends MemberProperties
		with HasSettings
		with PostLoadBehaviour
		with ToString
		with HibernateVersioned
		with PermissionsTarget
		with Logging
		with Serializable {

	@transient
	var profileService: ProfileService with StaffAssistantsHelpers = Wire[ProfileService with StaffAssistantsHelpers]

	@transient
	var relationshipService: RelationshipService = Wire[RelationshipService]

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
	def officialName: String = title + " " + Option(fullFirstName).getOrElse(firstName) + " " + lastName

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
	def affiliatedDepartments: Stream[Department] = homeDepartment match {
		case null => Stream()
		case _ => Stream(homeDepartment)
	}

	/**
		* Get all departments that this member touches.
		* (Overridden by StudentMember).
		*/
	def touchedDepartments: Stream[Department] = {
		val topLevelDepts = affiliatedDepartments
		topLevelDepts flatMap(_.subDepartmentsContaining(this))
	}

	def permissionsParents: Stream[PermissionsTarget] = touchedDepartments
	override def humanReadableId: String = fullName.getOrElse(toString())
	override final def urlCategory = "member"

	@OneToMany(mappedBy="scope", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	@ForeignKey(name="none")
	@BatchSize(size=200)
	var grantedRoles:JList[MemberGrantedRole] = JArrayList()

	override def postLoad {
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

		u.setLoginDisabled(!(inUseFlag.hasText && (inUseFlag == "Active" || inUseFlag.startsWith("Inactive - Starts "))))
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

	def toStringProps = Seq(
		"universityId" -> universityId
	)

	def isStaff: Boolean = userType == MemberUserType.Staff
	def isStudent: Boolean = userType == MemberUserType.Student
	def isRelationshipAgent(relationshipType: StudentRelationshipType): Boolean = {
		relationshipService.listCurrentStudentRelationshipsWithMember(relationshipType, this).nonEmpty
	}

	// Overridden in StudentMember
	def hasRelationship(relationshipType: StudentRelationshipType) = false

	def isFresh: Boolean = missingFromImportSince == null

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
@DiscriminatorValue("S")
class StudentMember extends Member with StudentProperties {
	this.userType = MemberUserType.Student

	// made this private as can't think of any instances in the app where you wouldn't prefer freshStudentCourseDetails
	@OneToMany(mappedBy = "student", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	@BatchSize(size=200)
	private val studentCourseDetails: JSet[StudentCourseDetails] = JHashSet()

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	def freshStudentCourseDetails: Seq[StudentCourseDetails] = {
		studentCourseDetails.asScala.filter(scd => scd.isFresh).toSeq.sorted
	}

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	def freshOrStaleStudentCourseDetails: mutable.Set[StudentCourseDetails] = studentCourseDetails.asScala

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	def freshOrStaleStudentCourseYearDetails(year: AcademicYear): mutable.Set[StudentCourseYearDetails] =
		freshOrStaleStudentCourseDetails
			.flatMap(_.freshOrStaleStudentCourseYearDetails)
			.filter(_.academicYear == year)


	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	def freshStudentCourseYearDetailsForYear(year: AcademicYear): Option[StudentCourseYearDetails] =
		freshOrStaleStudentCourseYearDetails(year).filter(scyd => scyd.isFresh).lastOption

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
	@BatchSize(size=200)
	var attendanceCheckpointTotals: JSet[AttendanceMonitoringCheckpointTotal] = JHashSet()

	@OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@Restricted(Array("MemberNotes.Read"))
	@BatchSize(size=200)
	var memberNotes: JSet[MemberNote] = JHashSet()

	def this(id: String) = {
		this()
		this.universityId = id
	}

	/**
		* Get all departments that this student is affiliated with at a departmental level.
		* This includes their home department, and the department running their course.
		*/
	override def affiliatedDepartments: Stream[Department] = {
		val sprDepartments = freshStudentCourseDetails.flatMap(scd => Option(scd.department)).toStream
		val sceDepartments = freshStudentCourseDetails.flatMap(_.freshStudentCourseYearDetails).flatMap(scyd => Option(scyd.enrolmentDepartment)).toStream
		val routeDepartments = freshStudentCourseDetails.flatMap(scd => Option(scd.currentRoute)).flatMap(route => route.teachingDepartments).toStream

		(Option(homeDepartment).toStream #:::
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
	override def touchedDepartments: Stream[Department] = {
		def moduleDepts = registeredModulesByYear(None).map(_.adminDepartment).toStream

		val topLevelDepts = (affiliatedDepartments #::: moduleDepts).distinct
		topLevelDepts flatMap(_.subDepartmentsContaining(this))
	}

	@transient var smallGroupService: SmallGroupService = Wire[SmallGroupService]

	/**
		* Get all small groups that this student is signed up for
		*/
	def registeredSmallGroups: Stream[SmallGroup] = smallGroupService.findSmallGroupsByStudent(asSsoUser).toStream

	override def permissionsParents: Stream[PermissionsTarget] = {
		// TAB-3007 We shouldn't swim downstream to the StudentCourseDetails or StudentCourseYearDetails for things here,
		// rely on them swimming up.

		// TAB-3598 - Add study departments and routes for any current course
		val currentCourses = freshStudentCourseDetails.filterNot(_.isEnded)
		val studyDepartments = currentCourses.flatMap { scd => Option(scd.department) }
		val currentCourseRoutes: Stream[PermissionsTarget] = currentCourses.map { _.currentRoute }.toStream

		// Cache the def result
		val mostSignificantCourse = mostSignificantCourseDetails

		// Allow swimming down only for *current* information
		val latestStudentCourseYearDetails = mostSignificantCourse.flatMap { scd => Option(scd.latestStudentCourseYearDetails) }
		val enrolmentDepartment = latestStudentCourseYearDetails.flatMap { scyd => Option(scyd.enrolmentDepartment) }

		val departments: Stream[PermissionsTarget] =
			(Stream(Option(homeDepartment), enrolmentDepartment).flatten ++ studyDepartments.toStream).distinct.flatMap(_.subDepartmentsContaining(this))

		/*
		 * FIXME TAB-2971 The small groups and modules from registrations here shouldn't be in permissionsParents, because
		 * the SmallGroup doesn't wholly contain the student and neither does the Module. As things stand, you can add a student
		 * to a small group and thereby elevate your own permissions on that student, which is wrong. We're really using this
		 * as a mechanism to show more information to markers, module managers and small group tutors, which could be achieved
		 * via a separate role provider that searches for registered small groups (or module registrations) when given the scope
		 * of a StudentMember.
		 */
		val smallGroups: Stream[PermissionsTarget] = registeredSmallGroups
		val modules: Stream[PermissionsTarget] = mostSignificantCourse.toStream.flatMap { scd =>
			latestStudentCourseYearDetails.toStream.flatMap { scyd =>
				// Only include module registrations for the latest year of the most significant course
				scd.moduleRegistrationsByYear(Some(scyd.academicYear)).map { _.module }
			}
		}

		departments #::: modules #::: smallGroups #::: currentCourseRoutes
	}

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	def mostSignificantCourseDetails: Option[StudentCourseDetails] = Option(mostSignificantCourse).filter(course => course.isFresh)

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	def mostSignificantCourseDetailsForYear(academicYear: AcademicYear): Option[StudentCourseDetails] =
		freshStudentCourseDetails
			.filter { _.freshStudentCourseYearDetails.exists { _.academicYear == academicYear } }
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
					.count(!_.code.startsWith("P")) == 0
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

	def attachStudentCourseDetails(detailsToAdd: StudentCourseDetails) {
		studentCourseDetails.remove(detailsToAdd)
		studentCourseDetails.add(detailsToAdd)
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

	def toExamGridEntity(baseSCYD: StudentCourseYearDetails): ExamGridEntity = {
		val allSCYDs: Seq[StudentCourseYearDetails] = freshOrStaleStudentCourseDetails.toSeq.sorted
			.flatMap(_.freshOrStaleStudentCourseYearDetails.toSeq.sorted)
			.takeWhile(_ != baseSCYD) ++ Seq(baseSCYD)
		ExamGridEntity(
			firstName = Option(firstName).getOrElse("[Unknown]"),
			lastName = Option(lastName).getOrElse("[Unknown]"),
			universityId = universityId,
			lastImportDate = Option(lastImportDate),
			years = (1 to baseSCYD.yearOfStudy).map(year =>
				year -> allSCYDs.reverse.find(_.yearOfStudy == year).get.toExamGridEntityYear
			).toMap
		)
	}
}

@Entity
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
	def assistants_=(group: UserGroup) { _assistantsGroup = group }
}

@Entity
@DiscriminatorValue("A")
class EmeritusMember extends Member with StaffProperties {
	this.userType = MemberUserType.Emeritus

	def this(id: String) = {
		this()
		this.universityId = id
	}
}

@Entity
@DiscriminatorValue("P")
class ApplicantMember extends Member with RestrictedPhoneNumber {
	this.userType = MemberUserType.Applicant

	def this(id: String) = {
		this()
		this.universityId = id
	}
}

@Entity
@DiscriminatorValue("O")
class OtherMember extends Member with RestrictedPhoneNumber {
	this.userType = MemberUserType.Other

	def this(id: String) = {
		this()
		this.universityId = id
	}
}

class RuntimeMember(user: CurrentUser) extends Member(user) with RestrictedPhoneNumber {
	override def permissionsParents: Stream[Nothing] = Stream.empty
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
	var fullFirstName: String = _

	@Type(`type` = "uk.ac.warwick.tabula.data.model.MemberUserTypeUserType")
	@Column(insertable = false, updatable = false)
	var userType: MemberUserType = _

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

	@Restricted(Array("Profiles.Read.MobileNumber"))
	var mobileNumber: String = _

	@Restricted(Array("Profiles.Read.TimetablePrivateFeed"))
	@Column(name = "timetable_hash")
	var timetableHash: String = _

	var deceased: Boolean = _

	def phoneNumberPermissions: Seq[Permission]

}

trait StudentProperties extends RestrictedPhoneNumber {
	@OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
	@JoinColumn(name="HOME_ADDRESS_ID")
	@Restricted(Array("Profiles.Read.HomeAndTermTimeAddresses"))
	var homeAddress: Address = _

	@OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
	@JoinColumn(name="TERMTIME_ADDRESS_ID")
	@Restricted(Array("Profiles.Read.HomeAndTermTimeAddresses"))
	var termtimeAddress: Address = _

	@OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = Array(ALL))
	@Restricted(Array("Profiles.Read.NextOfKin"))
	@BatchSize(size=200)
	var nextOfKins:JList[NextOfKin] = JArrayList()

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "disability")
	@Restricted(Array("Profiles.Read.Disability"))
	private var _disability: Disability = _
	def disability_=(d: Disability): Unit = { _disability = d }

	@Restricted(Array("Profiles.Read.Disability"))
	def disability: Option[Disability] = Option(_disability)

	@Column(name="tier4_visa_requirement")
	@Restricted(Array("Profiles.Read.Tier4VisaRequirement"))
	var tier4VisaRequirement: JBoolean = _
}

trait RestrictedPhoneNumber {
	def phoneNumberPermissions = Seq(Permissions.Profiles.Read.TelephoneNumber)
}

trait StaffProperties {
	// Anyone can view staff phone number
	def phoneNumberPermissions = Nil
}
