package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.apache.commons.lang3.builder.{EqualsBuilder, HashCodeBuilder}
import org.hibernate.annotations.{Any => _, _}
import org.joda.time.{DateTime, Duration}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntityYear
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.helpers.RequestLevelCache
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{LevelService, ModuleAndDepartmentService, UserLookupService}
import uk.ac.warwick.tabula.system.permissions.Restricted
import uk.ac.warwick.tabula.{AcademicYear, ToString}
import uk.ac.warwick.userlookup.User

import scala.beans.BeanProperty

object StudentCourseYearDetails {
  type YearOfStudy = Int

  final val FreshCourseYearDetailsOnlyFilter = "freshStudentCourseYearDetailsOnly"

  object Overcatting {
    final val Modules = "modules"
    final val ChosenBy = "chosenBy"
    final val ChosenDate = "chosenDate"
    final val MarkOverrides = "markOverrides"
  }

  //ensure we have  a single module code from module registration records. Some students have same module codes for different years but the latest year is the valid one with board marks
  def extractValidModuleRegistrations(mrRecords: Seq[ModuleRegistration]): Seq[ModuleRegistration] =
    mrRecords.groupBy(_.module.code).values.flatMap { modRegs =>
      modRegs.filterNot(mr => mr.agreedGrade.orElse(mr.actualGrade).contains(GradeBoundary.WithdrawnGrade))
        .maxByOption(_.academicYear.startYear)
    }.toSeq

  // makes an ExamGridEntityYear that is really multiple study years that contribute to a single level or block (groups related StudentCourseYearDetails together)
  def toExamGridEntityYearGrouped(yearOfStudy: YearOfStudy, scyds: StudentCourseYearDetails*): ExamGridEntityYear =
    RequestLevelCache.cachedBy("StudentCourseYearDetails.toExamGridEntityYearGrouped", s"$yearOfStudy-${scyds.map(_.id).sorted.mkString("-")}") {
      if (scyds.map(_.studyLevel).distinct.size > 1) throw new IllegalArgumentException(s"Cannot group StudentCourseYearDetails from different levels ${scyds.map(_.studyLevel).distinct.mkString(", ")}")
      val moduleRegistrations = extractValidModuleRegistrations(scyds.flatMap(_.moduleRegistrations))
      val route = {
        val allRoutes = scyds.sorted.flatMap(scyd => Option(scyd.route)) // ignore any nulls
        allRoutes.lastOption.getOrElse(scyds.head.studentCourseDetails.currentRoute)
      }
      val overcattingModules = scyds.map(_.overcattingModules).fold(Option(Seq()))((m1, m2) => Option((m1 ++ m2).flatten.toList.distinct).filter(_.nonEmpty))

      ExamGridEntityYear(
        moduleRegistrations = moduleRegistrations,
        cats = moduleRegistrations.map(mr => mr.safeCats.getOrElse(BigDecimal(0))).sum,
        route = route,
        baseAcademicYear = scyds.min.academicYear,
        overcattingModules = overcattingModules,
        markOverrides = None,
        studentCourseYearDetails = scyds.sorted.lastOption,
        agreedMark = scyds.toSeq.flatMap(scyd => Option(scyd.agreedMark)).lastOption.map(BigDecimal(_)),
        yearAbroad = scyds.exists(_.yearAbroad),
        level = scyds.head.level,
        yearOfStudy = yearOfStudy,
      )
    }

}

@FilterDefs(Array(
  new FilterDef(name = StudentCourseYearDetails.FreshCourseYearDetailsOnlyFilter, defaultCondition = "missingFromImportSince is null")
))
@Filters(Array(
  new Filter(name = StudentCourseYearDetails.FreshCourseYearDetailsOnlyFilter)
))
@javax.persistence.Entity
class StudentCourseYearDetails extends StudentCourseYearProperties
  with GeneratedId with ToString with HibernateVersioned with PermissionsTarget with CanBeStale
  with Ordered[StudentCourseYearDetails] with PostLoadBehaviour {

  def this(studentCourseDetails: StudentCourseDetails, sceSequenceNumber: JInteger, year: AcademicYear) = {
    this()
    this.studentCourseDetails = studentCourseDetails
    this.sceSequenceNumber = sceSequenceNumber
    this.academicYear = year
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "scjCode", referencedColumnName = "scjCode")
  var studentCourseDetails: StudentCourseDetails = _

  def toStringProps: Seq[(String, Any)] = Seq(
    "studentCourseDetails" -> studentCourseDetails,
    "sceSequenceNumber" -> sceSequenceNumber,
    "academicYear" -> academicYear
  )

  def permissionsParents: LazyList[PermissionsTarget] =
    LazyList(Option(studentCourseDetails), Option(enrolmentDepartment)).flatten

  /**
    * This is used to calculate StudentCourseDetails.latestStudentCourseYearDetails
    */
  def compare(that: StudentCourseYearDetails): Int = {
    if (this.studentCourseDetails.scjCode != that.studentCourseDetails.scjCode)
      this.studentCourseDetails.compare(that.studentCourseDetails)
    else if (this.academicYear != that.academicYear)
      this.academicYear.compare(that.academicYear)
    else
      this.sceSequenceNumber - that.sceSequenceNumber
  }

  def equals(that: StudentCourseYearDetails): Boolean = {
    (this.studentCourseDetails.scjCode == that.studentCourseDetails.scjCode) && (this.sceSequenceNumber == that.sceSequenceNumber)
  }

  // There can be more than one StudentCourseYearDetails per year if there are multiple sequence numbers,
  // so moduleRegistrations are not attached directly - instead, get them from StudentCourseDetails,
  // filtering by year:
  def moduleRegistrations: Seq[ModuleRegistration] = studentCourseDetails.moduleRegistrations.filter(mr => mr.academicYear == this.academicYear && !mr.deleted)

  // similarly for accredited prior learning
  def accreditedPriorLearning: Seq[AccreditedPriorLearning] = {
    studentCourseDetails.accreditedPriorLearning.filter(_.academicYear == this.academicYear)
  }

  def registeredModules: Seq[Module] = moduleRegistrations.map(mr => mr.module)

  def hasModuleRegistrations: Boolean = moduleRegistrations.nonEmpty

  def hasModuleRegistrationWithNonStandardOccurrence: Boolean = moduleRegistrations.exists(_.occurrence != "A")

  def hasAccreditedPriorLearning: Boolean = accreditedPriorLearning.nonEmpty

  def isLatest: Boolean = this.equals(studentCourseDetails.latestStudentCourseYearDetails)

  def relationships(relationshipType: StudentRelationshipType): Seq[StudentRelationship] = {
    try {
      val academicYearStartDate = academicYear.firstDay
      val academicYearEndDate = academicYear.lastDay
      val twoMonthsInDays = 2 * 30

      studentCourseDetails.allRelationshipsOfType(relationshipType)
        .filter(r => r.endDate == null || r.startDate.isBefore(r.endDate))
        .filter(relationship => {
          // For the most recent YoS, only show current relationships
          if (studentCourseDetails.freshStudentCourseYearDetails.nonEmpty && studentCourseDetails.freshStudentCourseYearDetails.max == this) {
            relationship.isCurrent
          } else {
            // Otherwise return the relationship if it lasted for at least 2 months in this academic year
            val relationshipEndDate = if (relationship.endDate == null) academicYearEndDate.toDateTimeAtStartOfDay else relationship.endDate
            if (relationshipEndDate.isBefore(academicYearStartDate.toDateTimeAtStartOfDay) || relationship.startDate.isAfter(academicYearEndDate.toDateTimeAtStartOfDay)) {
              false
            } else {
              val inYearStartDate =
                if (relationship.startDate.isBefore(academicYearStartDate.toDateTimeAtStartOfDay))
                  academicYearStartDate.toDateTimeAtStartOfDay
                else
                  relationship.startDate
              val inYearEndDate =
                if (relationshipEndDate.isAfter(academicYearEndDate.toDateTimeAtStartOfDay))
                  academicYearEndDate.toDateTimeAtStartOfDay
                else
                  relationshipEndDate
              new Duration(inYearStartDate, inYearEndDate).getStandardDays > twoMonthsInDays
            }
          }
        })
    } catch {
      case _: IllegalStateException => Seq()
    }
  }

  def isFinalYear: Boolean = yearOfStudy == studentCourseDetails.courseYearLength

  @Type(`type` = "uk.ac.warwick.tabula.data.model.JsonMapUserType")
  protected var overcatting: Map[String, Any] = Map()

  protected def ensureOvercatting(): Unit = {
    if (overcatting == null) overcatting = Map()
  }

  @transient
  var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

  def overcattingModules: Option[Seq[Module]] = (Option(overcatting).flatMap(_.get(StudentCourseYearDetails.Overcatting.Modules)) match {
    case Some(value: Seq[_]) => Some(value.asInstanceOf[Seq[String]])
    case _ => None
  }).map(moduleAndDepartmentService.getModulesByCodes)

  def overcattingModules_=(modules: Seq[Module]): Unit = overcatting += (StudentCourseYearDetails.Overcatting.Modules -> modules.map(_.code))

  @transient
  var userLookup: UserLookupService = Wire[UserLookupService]("userLookup")

  def overcattingChosenBy: Option[User] = (Option(overcatting).flatMap(_.get(StudentCourseYearDetails.Overcatting.ChosenBy)) match {
    case Some(value: String) => Some(value)
    case _ => None
  }).map(userId => userLookup.getUserByUserId(userId))

  def overcattingChosenBy_=(chosenBy: User): Unit = overcatting += (StudentCourseYearDetails.Overcatting.ChosenBy -> chosenBy.getUserId)

  def overcattingChosenDate: Option[DateTime] = Option(overcatting).flatMap(_.get(StudentCourseYearDetails.Overcatting.ChosenDate)) match {
    case Some(value: DateTime) => Some(value)
    case _ => None
  }

  def overcattingChosenDate_=(chosenDate: DateTime): Unit = overcatting += (StudentCourseYearDetails.Overcatting.ChosenDate -> chosenDate)

  final var agreedMarkUploadedDate: DateTime = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  final var agreedMarkUploadedBy: User = _

  def totalCats: BigDecimal = moduleRegistrations.map(mr => mr.safeCats.getOrElse(BigDecimal(0))).sum

  def toExamGridEntityYear: ExamGridEntityYear =
    RequestLevelCache.cachedBy("StudentCourseYearDetails.toExamGridEntityYear", id) {
      ExamGridEntityYear(
        moduleRegistrations = moduleRegistrations, //ones that are not deleted
        cats = totalCats,
        route = route match {
          case _: Route => route
          case _ => studentCourseDetails.currentRoute
        },
        baseAcademicYear = academicYear,
        overcattingModules = overcattingModules,
        markOverrides = None,
        studentCourseYearDetails = Some(this),
        agreedMark = Option(agreedMark).map(BigDecimal(_)),
        yearAbroad = yearAbroad,
        level = level,
        yearOfStudy = this.yearOfStudy,
      )
    }

  // Logic picked up from cognos - TAB-6397
  // Don't worry about block occurrence if the MoA ends with "E" for "Erasmus"
  def yearAbroad: Boolean = modeOfAttendance != null && modeOfAttendance.yearAbroad && (modeOfAttendance.code.endsWith("E") || blockOccurrence == null || blockOccurrence != "I") // doesn't apply to intercalated years

  override def postLoad(): Unit = {
    ensureOvercatting()
  }
}

trait BasicStudentCourseYearProperties {
  var sceSequenceNumber: JInteger = _

  /**
    * Sequence in SITS is stored as a 2-digit zero-padded number
    */
  def sceSequenceNumberSitsFormat = f"${sceSequenceNumber.toInt}%02d"

  // TODO - rename to studyBlock - TAB-5980
  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  var yearOfStudy: JInteger = _

  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  var studyLevel: String = _

  @transient
  var levelService: LevelService = Wire.auto[LevelService]

  def level: Option[Level] =
    RequestLevelCache.cachedBy("StudentCourseYearDetails.level", studyLevel) {
      levelService.levelFromCode(studyLevel)
    }

  @Column(name = "cas_used")
  @Restricted(Array("Profiles.Read.Tier4VisaRequirement"))
  var casUsed: JBoolean = _

  @Column(name = "tier4visa")
  @Restricted(Array("Profiles.Read.Tier4VisaRequirement"))
  var tier4Visa: JBoolean = _

  var agreedMark: JBigDecimal = _

}

trait StudentCourseYearProperties extends BasicStudentCourseYearProperties {
  var lastUpdatedDate: DateTime = DateTime.now
  var missingFromImportSince: DateTime = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.ModuleRegistrationStatusUserType")
  var moduleRegistrationStatus: ModuleRegistrationStatus = _ // cam_ssn.ssn_mrgs

  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  var academicYear: AcademicYear = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "routeCode", referencedColumnName = "code")
  @Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
  var route: Route = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrolmentStatusCode", referencedColumnName = "code")
  @Restricted(Array("Profiles.Read.StudentCourseDetails.Status"))
  var enrolmentStatus: SitsStatus = _

  // this is the department from the SCE table in SITS (Student Course Enrolment). It is likely to be the
  // same as the department on the Route table, and on the StudentCourseDetails, but in some cases, e.g. where routes
  // change ownership in different years, this might contain a different department. This indicates the
  // department responsible for administration for the student for this year.
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrolment_department_id")
  var enrolmentDepartment: Department = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "modeOfAttendanceCode", referencedColumnName = "code")
  @Restricted(Array("Profiles.Read.StudentCourseDetails.Status"))
  var modeOfAttendance: ModeOfAttendance = _

  var blockOccurrence: String = _

  /*
   * A boolean flag set at import-time (see ImportStudentCourseYearCommand) that indicates whether we believe
   * the student was enrolled in this year or not. For every year except the student's final year, this is effectively
   * the same as checking that the enrolmentStatus is not permanently withdrawn (doesn't start with P). Because
   * Student Administrative Services mark SCE records as permanently withdrawn when a student graduates, we also set this for true
   * if the student is permanently withdrawn but their SCJ reason for transfer code starts with S (i.e. they have successfully
   * completed their course).
   */
  var enrolledOrCompleted: Boolean = _
}

class StudentCourseYearKey {
  @BeanProperty
  var scjCode: String = _

  @BeanProperty
  var sceSequenceNumber: JInteger = _

  def this(scjCode: String, sceSequenceNumber: JInteger) = {
    this()
    this.scjCode = scjCode
    this.sceSequenceNumber = sceSequenceNumber
  }

  override final def equals(other: scala.Any): Boolean = other match {
    case that: StudentCourseYearKey =>
      new EqualsBuilder()
        .append(scjCode, that.scjCode)
        .append(sceSequenceNumber, that.sceSequenceNumber)
        .build()
    case _ => false
  }

  override final def hashCode: Int =
    new HashCodeBuilder()
      .append(scjCode)
      .append(sceSequenceNumber)
      .build()
}

