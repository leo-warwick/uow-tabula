package uk.ac.warwick.tabula.data.model

import javax.persistence.{CascadeType, Entity, _}
import org.hibernate.annotations.{Proxy, _}
import org.joda.time.{DateTime, Duration, LocalDate}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.convert.ConvertibleConverter
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{LevelService, RelationshipService}
import uk.ac.warwick.tabula.system.permissions.Restricted
import uk.ac.warwick.tabula.{AcademicYear, ToString}

import scala.collection.mutable
import scala.jdk.CollectionConverters._

object StudentCourseDetails {
  final val FreshCourseDetailsOnlyFilter = "freshStudentCourseDetailsOnly"
  final val GracePeriodInMonths = 3
}

@FilterDefs(Array(
  new FilterDef(name = StudentCourseDetails.FreshCourseDetailsOnlyFilter, defaultCondition = "missingFromImportSince is null")
))
@Filters(Array(
  new Filter(name = StudentCourseDetails.FreshCourseDetailsOnlyFilter)
))
@Entity
@Proxy
class StudentCourseDetails
  extends StudentCourseProperties
    with ToString
    with HibernateVersioned
    with CanBeStale
    with PermissionsTarget
    with Ordered[StudentCourseDetails]
    with Serializable {

  @transient
  var relationshipService: RelationshipService = Wire.auto[RelationshipService]

  @transient
  var levelService: LevelService = Wire.auto[LevelService]

  def this(student: StudentMember, scjCode: String) = {
    this()
    this.student = student
    this.scjCode = scjCode
  }

  @Id var scjCode: String = _

  def id: String = scjCode

  def urlSafeId: String = scjCode.replace("/", "_")

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "universityId", referencedColumnName = "universityId")
  var student: StudentMember = _

  // made this private as can't think of any instances in the app where you wouldn't prefer freshStudentCourseDetails
  @OneToMany(mappedBy = "studentCourseDetails", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  @BatchSize(size = 200)
  private val studentCourseYearDetails: JSet[StudentCourseYearDetails] = JHashSet()

  def freshStudentCourseYearDetails: Seq[StudentCourseYearDetails] = studentCourseYearDetails.asScala.filter(scyd => !scyd.stale).toSeq.sorted

  def freshStudentCourseYearDetailsForYear(academicYear: AcademicYear): Option[StudentCourseYearDetails] = {
    freshStudentCourseYearDetails.filter(_.academicYear == academicYear).lastOption
  }

  def freshOrStaleStudentCourseYearDetails: mutable.Set[StudentCourseYearDetails] = studentCourseYearDetails.asScala

  def freshOrStaleStudentCourseYearDetailsForYear(academicYear: AcademicYear): Option[StudentCourseYearDetails] = {
    studentCourseYearDetails.asScala.filter(_.academicYear == academicYear).lastOption
  }

  @ManyToMany(fetch = FetchType.LAZY, mappedBy = "_allStudentCourseDetails")
  @BatchSize(size = 200)
  val _moduleRegistrations: JSet[ModuleRegistration] = JHashSet()

  def moduleRegistrations: Seq[ModuleRegistration] = _moduleRegistrations.asScala.toSeq.sortBy { reg => reg.module.code }

  @ManyToMany(fetch = FetchType.LAZY, mappedBy = "_allStudentCourseDetails")
  @BatchSize(size = 200)
  val _progressionDecisions: JSet[ProgressionDecision] = JHashSet()

  def progressionDecisions: Seq[ProgressionDecision] = _progressionDecisions.asScala.toSeq.sortBy(_.sequence)

  def progressionDecisionsByYear(year: AcademicYear): Seq[ProgressionDecision] = progressionDecisions.filter(_.academicYear == year)

  def registeredModulesByYear(year: Option[AcademicYear]): Seq[Module] = moduleRegistrationsByYear(year).map(_.module)

  def moduleRegistrationsByYear(year: Option[AcademicYear]): Seq[ModuleRegistration] =
    moduleRegistrations.collect {
      case modReg if year.isEmpty => modReg
      case modReg if modReg.academicYear == year.orNull => modReg
    }

  @OneToMany(mappedBy = "studentCourseDetails", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @BatchSize(size = 200)
  private val _accreditedPriorLearning: JSet[AccreditedPriorLearning] = JHashSet()

  def accreditedPriorLearning: Seq[AccreditedPriorLearning] = _accreditedPriorLearning.asScala.toSeq

  def accreditedPriorLearningByYear(year: Option[AcademicYear]): Seq[AccreditedPriorLearning] =
    accreditedPriorLearning.collect {
      case apl if year.isEmpty => apl
      case apl if apl.academicYear == year.orNull => apl
    }

  def toStringProps = Seq(
    "scjCode" -> scjCode,
    "sprCode" -> sprCode)

  def permissionsParents: LazyList[PermissionsTarget] = {
    val latestYearDetails = Option(latestStudentCourseYearDetails)
    val enrolmentDepartment = latestYearDetails.flatMap { scyd => Option(scyd.enrolmentDepartment) }

    LazyList(Option(student), Option(currentRoute), Option(department), enrolmentDepartment).flatten #:::
      latestYearDetails.to(LazyList).flatMap { scyd =>
        // Only include module registrations for the latest year
        // FIXME TAB-2971 See StudentMember.permissionsParents
        moduleRegistrationsByYear(Some(scyd.academicYear)).map(_.module)
      }
  }

  def hasCurrentEnrolment: Boolean = {
    Option(latestStudentCourseYearDetails).exists(scyd => scyd.enrolmentStatus != null && !scyd.enrolmentStatus.code.startsWith("P"))
  }

  def permanentlyWithdrawn: Boolean = {
    statusOnRoute != null && SitsStatus.isPermanentlyWithdrawnStatusOnRoute(statusOnRoute)
  }

  @OneToOne(fetch = FetchType.LAZY) // don't cascade, cascaded separately
  @JoinColumn(name = "latestYearDetails")
  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  var latestStudentCourseYearDetails: StudentCourseYearDetails = _

  def courseType: Option[CourseType] = {
    if (course == null) None
    else Some(CourseType.fromCourseCode(course.code))
  }

  def specificCourseType: Option[SpecificCourseType] = {
    if (course == null) None
    else Some(SpecificCourseType.fromCourseCode(course.code))
  }

  @OneToMany(mappedBy = "studentCourseDetails", fetch = FetchType.LAZY, cascade = Array(CascadeType.PERSIST))
  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  @BatchSize(size = 200)
  var allRelationships: JSet[StudentRelationship] = JHashSet()

  @ElementCollection
  @CollectionTable(name = "STUDENTCOURSEDETAILSNOTE", joinColumns = Array(new JoinColumn(name = "scjCode")))
  @Column(name = "NOTE")
  var notes: JSet[String] = JHashSet()

  def allRelationshipsOfType(relationshipType: StudentRelationshipType): Seq[StudentRelationship] = {
    allRelationships.asScala
      .toSeq
      .filter(_.relationshipType == relationshipType)
      .sortBy(relationship => (relationship.agentLastName, relationship.agentName))
      .sortBy(relationship => Option(relationship.percentage))(Ordering[Option[JBigDecimal]].reverse)
  }

  // We can't restrict this because it's not a getter. Restrict in
  // view code if necessary (or implement for all methods in  ScalaBeansWrapper)
  def relationships(relationshipType: StudentRelationshipType): Seq[StudentRelationship] =
    relationshipService.findCurrentRelationships(relationshipType, this)

  def hasRelationship(relationshipType: StudentRelationshipType): Boolean = relationships(relationshipType).nonEmpty

  def compare(that: StudentCourseDetails): Int = {
    this.scjCode.compare(that.scjCode)
  }

  def equals(that: StudentCourseDetails): Boolean = this.scjCode == that.scjCode

  def attachStudentCourseYearDetails(yearDetailsToAdd: StudentCourseYearDetails): Unit = {
    studentCourseYearDetails.remove(yearDetailsToAdd)
    studentCourseYearDetails.add(yearDetailsToAdd)

    latestStudentCourseYearDetails = freshStudentCourseYearDetails.max
  }

  // use these methods ONLY for tests, as they don't automagically update latestStudentCourseYearDetails
  def addStudentCourseYearDetails(scyd: StudentCourseYearDetails): Boolean = studentCourseYearDetails.add(scyd)

  def removeStudentCourseYearDetails(scyd: StudentCourseYearDetails): Boolean = studentCourseYearDetails.remove(scyd)

  def beginYear: JInteger = beginDate match {
    case begin: LocalDate => new Integer(beginDate.year().getAsText)
    case null => new Integer(0)
  }

  def endYear: JInteger = endDate match {
    case null =>
      expectedEndDate match {
        case expectedEnd: LocalDate => new Integer(expectedEndDate.year().getAsText)
        case null => new Integer(0)
      }
    case end: LocalDate => new Integer(end.year().getAsText)
  }

  def isStudentRelationshipTypeForDisplay(relationshipType: StudentRelationshipType): Boolean = {
    if (department == null) false
    else {
      // first see if any of the sub-departments that the student is in are set to display this relationship type
      val relationshipDisplayedForSubDepts = department.subDepartmentsContaining(student).flatMap(_.studentRelationshipDisplayed.get(relationshipType.id)).exists(_.toBoolean)

      // if either a sub-dept is set to display this relationship type or the parent department is, then display it:
      relationshipDisplayedForSubDepts || department.getStudentRelationshipDisplayed(relationshipType)
    }
  }

  def hasAccreditedPriorLearning: Boolean = accreditedPriorLearning.nonEmpty

  def isEnded: Boolean = endDate != null && endDate.isBefore(DateTime.now.toLocalDate)

  // we won't automatically expire a relationship if it ended recently
  def hasEndedRecently: Boolean = endDate != null &&
    endDate.toDateTimeAtStartOfDay.plusMonths(StudentCourseDetails.GracePeriodInMonths).isAfterNow

  def level: Option[Level] = levelService.levelFromCode(levelCode)
}

// properties for a student on a course which come direct from SITS - those that need to be
// transformed in some way are in StudentCourseProperties
trait BasicStudentCourseProperties {
  // There can be multiple StudentCourseDetails rows for a single SPR code, even though a route is a sub-category of a course;
  // this is just an artefact of the weird way SITS works.  If a student changes route within a course, they end up with a new
  // course join (SCJ) row in SITS.  Equally perversely, they keep the same sprcode and SPR row even though this should be the
  // student's record for their route (SPR = student programme route) - the route code is just edited.  Hence this is not unique.
  var sprCode: String = _

  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  var levelCode: String = _

  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  var beginDate: LocalDate = _

  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  var endDate: LocalDate = _

  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  var expectedEndDate: LocalDate = _

  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  var courseYearLength: JInteger = _

  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  var mostSignificant: JBoolean = _

  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  var reasonForTransferCode: String = _

  @Restricted(Array("Profiles.Read.StudentCourseDetails.SpecialExamArrangements"))
  @Column(nullable = true, name = "special_exam_arrangements")
  var specialExamArrangements: JBoolean = _

  @Restricted(Array("Profiles.Read.StudentCourseDetails.SpecialExamArrangements"))
  @Type(`type` = "uk.ac.warwick.tabula.data.model.LocationUserType")
  @Column(nullable = true, name = "special_exam_arrangements_location")
  var specialExamArrangementsLocation: Location = _

  @Restricted(Array("Profiles.Read.StudentCourseDetails.SpecialExamArrangements"))
  @Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDurationAsString")
  @Column(nullable = true, name = "special_exam_arrangements_extra_time")
  var specialExamArrangementsExtraTime: Duration = _
}

trait StudentCourseProperties extends BasicStudentCourseProperties {
  var lastUpdatedDate: DateTime = DateTime.now
  var missingFromImportSince: DateTime = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "courseCode", referencedColumnName = "code")
  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  var course: Course = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "routeCode", referencedColumnName = "code")
  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  var currentRoute: Route = _

  // this is the department from the SPR table in SITS (Student Programme Route).  It is likely to be the
  // same as the department on the Route table, but in some cases, e.g. where routes change ownership in
  // different years, the SPR code might contain a different department.
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  var department: Department = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "awardCode", referencedColumnName = "code")
  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  var award: Award = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sprStatusCode")
  @Restricted(Array("Profiles.Read.StudentCourseDetails.Status"))
  var statusOnRoute: SitsStatus = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "scjStatusCode")
  @Restricted(Array("Profiles.Read.StudentCourseDetails.Status"))
  var statusOnCourse: SitsStatus = _

  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  var sprStartAcademicYear: AcademicYear = _
}

sealed abstract class CourseType(val code: String, val level: String, val description: String, val courseCodeChars: Set[Char]) extends Convertible[String] with Product with Serializable {
  def value: String = code
}

object CourseType {
  implicit val factory: String => CourseType = { code: String => CourseType(code) }

  case object PGR extends CourseType("PG(R)", "Postgraduate", "Postgraduate (Research)", Set('R'))

  case object PGT extends CourseType("PG(T)", "Postgraduate", "Postgraduate (Taught)", Set('T', 'E'))

  case object UG extends CourseType("UG", "Undergraduate", "Undergraduate", Set('U', 'D'))

  case object Foundation extends CourseType("F", "Foundation", "Foundation course", Set('F')) // pre-UG
  case object PreSessional extends CourseType("PS", "Pre-sessional", "Pre-sessional course", Set('N')) // pre-PG

  // these are used in narrowing departmental filters via Department.filterRule
  val all = Seq(UG, PGT, PGR, Foundation, PreSessional)
  val ugCourseTypes = Seq(UG, Foundation)
  val pgCourseTypes = Seq(PGT, PGR, PreSessional)

  def apply(code: String): CourseType = code match {
    case UG.code => UG
    case PGT.code => PGT
    case PGR.code => PGR
    case Foundation.code => Foundation
    case PreSessional.code => PreSessional
    case other => throw new IllegalArgumentException("Unexpected course code: %s".format(other))
  }

  def fromCourseCode(cc: String): CourseType = {
    if (cc == null || cc.isEmpty) {
      null
    } else {
      cc.charAt(0) match {
        case c if UG.courseCodeChars.contains(c) => UG
        case c if PGT.courseCodeChars.contains(c) => PGT
        case c if PGR.courseCodeChars.contains(c) => PGR
        case c if Foundation.courseCodeChars.contains(c) => Foundation
        case c if PreSessional.courseCodeChars.contains(c) => PreSessional
        case other => throw new IllegalArgumentException("Unexpected first character of course code: %s".format(other))
      }
    }
  }
}

// converter for spring
class CourseTypeConverter extends ConvertibleConverter[String, CourseType]

sealed abstract class SpecificCourseType(val code: Char) extends Convertible[String] with Product with Serializable {
  def value: String = code.toString
}

object SpecificCourseType {

  implicit val factory: String => SpecificCourseType = { code: String => SpecificCourseType.fromCourseCode(code) }

  case object PGR extends SpecificCourseType('R')
  case object PGT extends SpecificCourseType('T')
  case object PGTE extends SpecificCourseType('E')
  case object UG extends SpecificCourseType('U')
  case object UGD extends SpecificCourseType('D')
  case object Foundation extends SpecificCourseType('F')
  case object PreSessional extends SpecificCourseType('N')

  def fromCourseCode(cc: String): SpecificCourseType = {
    if (cc == null || cc.isEmpty) {
      null
    } else {
      cc.charAt(0) match {
        case UG.code => UG
        case UGD.code => UGD
        case PGT.code => PGT
        case PGTE.code => PGTE
        case PGR.code => PGR
        case Foundation.code => Foundation
        case PreSessional.code => PreSessional
        case other => throw new IllegalArgumentException("Unexpected first character of course code: %s".format(other))
      }
    }
  }
}

// converter for spring
class SpecificCourseTypeConverter extends ConvertibleConverter[String, SpecificCourseType]
