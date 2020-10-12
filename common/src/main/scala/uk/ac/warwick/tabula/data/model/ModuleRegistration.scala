package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.apache.commons.lang3.builder.CompareToBuilder
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.{DateTime, DateTimeConstants, LocalDate}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.RequestLevelCache
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.services.marks.AssessmentComponentMarksService
import uk.ac.warwick.tabula.system.permissions.Restricted
import uk.ac.warwick.tabula.{AcademicYear, SprCode}

import scala.jdk.CollectionConverters._
import scala.util.Try

object ModuleRegistration {
  final val GraduationBenchmarkCutoff: LocalDate = new LocalDate(2020, DateTimeConstants.MARCH, 13)

  // a list of all the markscheme codes that we consider to be pass/fail modules
  final val PassFailMarkSchemeCodes = Seq("PF")

  def filterToLatestAttempt(allMembers: Seq[UpstreamAssessmentGroupMember]): Seq[UpstreamAssessmentGroupMember] =
    if (allMembers.forall(!_.isReassessment)) allMembers
    else {
      // Filter down to just the latest resit sequence
      // Find the assessment group to filter by (this is for students who take multiple reassessments)
      val assessmentGroup =
        allMembers.maxByOption(_.resitSequence).map(_.upstreamAssessmentGroup.assessmentGroup)

      // Group by assessment component so we only get the latest by resit sequence
      val uagms =
        allMembers
          .filter(uagm => assessmentGroup.contains(uagm.upstreamAssessmentGroup.assessmentGroup))
          .groupBy(uagm => (uagm.upstreamAssessmentGroup.moduleCode, uagm.upstreamAssessmentGroup.sequence))
          .values.map(_.maxBy(_.resitSequence))
          .toSeq

      var rawWeighting: Int = 0

      uagms.sortBy(_.resitSequence).reverse.takeWhile { uagm =>
        if (rawWeighting == 100 || rawWeighting == 1000 || rawWeighting == 10000) false // that'll do, pig
        else {
          rawWeighting +=
            uagm.resitAssessmentWeighting
              .orElse(uagm.upstreamAssessmentGroup.assessmentComponent.flatMap(ac => Option(ac.rawWeighting).map(_.toInt)))
              .getOrElse(0)

          true
        }
      }.reverse // Put them back in resit sequence order
    }
}

/*
 * sprCode, fullModuleCode, cat score, academicYear and occurrence are a notional key for this table but giving it a generated ID to be
 * consistent with the other tables in Tabula which all have a key that's a single field.  In the db, there should be
 * a unique constraint on the combination of those three.
 */
@Entity
@Proxy
@Access(AccessType.FIELD)
class ModuleRegistration extends GeneratedId with PermissionsTarget with CanBeDeleted with Ordered[ModuleRegistration] with Serializable {

  def this(sprCode: String, module: Module, cats: JBigDecimal, sitsModuleCode: String, academicYear: AcademicYear, occurrence: String, marksCode: String) = {
    this()
    this.sprCode = sprCode
    this.module = module
    this.academicYear = academicYear
    this.cats = cats
    this.sitsModuleCode = sitsModuleCode
    this.occurrence = occurrence
    this.marksCode = marksCode.maybeText.orNull
  }

  @transient var membershipService: AssessmentMembershipService = Wire[AssessmentMembershipService]
  @transient var assessmentComponentMarksService: AssessmentComponentMarksService = Wire[AssessmentComponentMarksService]

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "moduleCode", referencedColumnName = "code")
  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var module: Module = _

  /**
   * This is the CATS value of the module registration, it does not *necessarily* match the CATS value
   * of the module or what's in the fullModuleCode.
   */
  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var cats: JBigDecimal = _

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  def safeCats: Option[BigDecimal] = Option(cats).map(BigDecimal(_))

  /**
   * Uppercase module code, with CATS. Doesn't necessarily match the cats property (which is the credits registered)
   */
  var sitsModuleCode: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var academicYear: AcademicYear = _

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "StudentCourseDetails_ModuleRegistration",
    joinColumns = Array(new JoinColumn(name = "module_registration_id", insertable = false, updatable = false)),
    inverseJoinColumns = Array(new JoinColumn(name = "scjcode", insertable = false, updatable = false))
  )
  @JoinColumn(name = "scjcode", insertable = false, updatable = false)
  @BatchSize(size = 200)
  var _allStudentCourseDetails: JSet[StudentCourseDetails] = JHashSet()

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  def studentCourseDetails: StudentCourseDetails =
    _allStudentCourseDetails.asScala.find(_.mostSignificant)
      .orElse(_allStudentCourseDetails.asScala.maxByOption(_.scjCode))
      .orNull

  // This isn't really a ManyToMany but we don't have bytecode instrumentation so this allows us to make it lazy at both ends
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "ModuleRegistration_RecordedModuleRegistration",
    joinColumns = Array(new JoinColumn(name = "module_registration_id", insertable = false, updatable = false)),
    inverseJoinColumns = Array(new JoinColumn(name = "recorded_module_registration_id", insertable = false, updatable = false))
  )
  @JoinColumn(name = "id", insertable = false, updatable = false)
  @BatchSize(size = 200)
  private val _recordedModuleRegistration: JSet[RecordedModuleRegistration] = JHashSet()
  def recordedModuleRegistration: Option[RecordedModuleRegistration] = _recordedModuleRegistration.asScala.headOption

  var sprCode: String = _

  // get the integer part of the SPR code so we can sort registrations to the same module by it
  def sprSequence: Int = sprCode.split("/").lastOption.flatMap(s => Try(s.toInt).toOption).getOrElse(Int.MinValue)

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var assessmentGroup: String = _

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var occurrence: String = _

  @Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var actualMark: Option[Int] = None

  @Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  var actualGrade: Option[String] = None

  @Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var agreedMark: Option[Int] = None

  @Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  var agreedGrade: Option[String] = None

  def firstDefinedMark: Option[Int] = agreedMark.orElse(actualMark)

  def firstDefinedGrade: Option[String] = agreedGrade.orElse(actualGrade)

  def hasAgreedMarkOrGrade: Boolean = agreedMark.isDefined || agreedGrade.isDefined

  @Type(`type` = "uk.ac.warwick.tabula.data.model.ModuleSelectionStatusUserType")
  @Column(name = "selectionstatuscode")
  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var selectionStatus: ModuleSelectionStatus = _ // core, option or optional core

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var lastUpdatedDate: DateTime = DateTime.now

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var marksCode: String = _

  def process: GradeBoundaryProcess = if(currentResitAttempt.nonEmpty) GradeBoundaryProcess.Reassessment else GradeBoundaryProcess.StudentAssessment

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  def passFail: Boolean = marksCode.maybeText.exists(ModuleRegistration.PassFailMarkSchemeCodes.contains)

  @Type(`type` = "uk.ac.warwick.tabula.data.model.ModuleResultUserType")
  @Column(name = "moduleresult")
  var moduleResult: ModuleResult = _

  @Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
  var endDate: LocalDate = _

  def passedCats: Option[Boolean] = moduleResult match {
    case _: ModuleResult.Pass.type if hasAgreedMarkOrGrade => Some(true)
    case _: ModuleResult.Fail.type if hasAgreedMarkOrGrade => Some(false)
    case _ => None
  }

  def upstreamAssessmentGroups: Seq[UpstreamAssessmentGroup] =
    RequestLevelCache.cachedBy("ModuleRegistration.upstreamAssessmentGroups", s"$academicYear-$sitsModuleCode-$occurrence") {
      membershipService.getUpstreamAssessmentGroups(this, allAssessmentGroups = true, eagerLoad = false)
    }

  def upstreamAssessmentGroupMembersAllAttempts(extractMarks: Seq[UpstreamAssessmentGroupMember] => Seq[(AssessmentType, String, Option[Int])]): Seq[Seq[(UpstreamAssessmentGroupMember, Option[BigDecimal])]] =
    if (studentCourseDetails == null) Seq.empty
    else {
      val uagms = RequestLevelCache.cachedBy("ModuleRegistration.upstreamAssessmentGroupMembers", s"$academicYear-$sitsModuleCode-$occurrence") {
        membershipService.getUpstreamAssessmentGroups(this, allAssessmentGroups = true, eagerLoad = true).flatMap(_.members.asScala)
      }.filter(member => member.universityId == studentCourseDetails.student.universityId)

      val firstAttempt = uagms.filterNot(_.isReassessment)
      val firstAttemptMarks = extractMarks(firstAttempt)

      val firstAttemptWithWeightings: Seq[(UpstreamAssessmentGroupMember, Option[BigDecimal])] = firstAttempt.map { uagm =>
        val weighting =
          if (firstAttemptMarks.exists { case (_, _, mark) => mark.nonEmpty })
            uagm.upstreamAssessmentGroup.assessmentComponent.flatMap { ac =>
              ac.weightingFor(firstAttemptMarks).getOrElse(ac.scaledWeighting)
            }
          else uagm.upstreamAssessmentGroup.assessmentComponent.flatMap(_.scaledWeighting)

        (uagm, weighting)
      }

      // No resits, exit early
      if (uagms.forall(!_.isReassessment)) Seq(firstAttemptWithWeightings)
      else {
        val reassessments = uagms.filter(_.isReassessment).sortBy(_.resitSequence)

        def calculateReassessmentWeightings(components: Seq[UpstreamAssessmentGroupMember]): Seq[(UpstreamAssessmentGroupMember, Option[BigDecimal])] = {
          val hasResitWeightings: Boolean = components.exists(_.resitAssessmentWeighting.nonEmpty)

          val totalRawWeighting: Int =
            if (hasResitWeightings)
              components.map { uagm =>
                uagm.resitAssessmentWeighting
                  .orElse(uagm.upstreamAssessmentGroup.assessmentComponent.flatMap(ac => Option(ac.rawWeighting).map(_.toInt)))
                  .getOrElse(0)
              }.sum
            else 100

          def scaleWeighting(raw: Int): BigDecimal =
            if (raw == 0 || totalRawWeighting == 0) BigDecimal(0) // 0 will always scale to 0 and a total of 0 will always lead to a weighting of 0
            else if (totalRawWeighting == 100) BigDecimal(raw)
            else {
              val bd = BigDecimal(raw * 100) / BigDecimal(totalRawWeighting)
              bd.setScale(1, BigDecimal.RoundingMode.HALF_UP)
              bd
            }

          lazy val marks = extractMarks(components)

          components.map { uagm =>
            val weighting: Option[BigDecimal] =
              if (hasResitWeightings) {
                uagm.resitAssessmentWeighting
                  .orElse(uagm.upstreamAssessmentGroup.assessmentComponent.flatMap(ac => Option(ac.rawWeighting).map(_.toInt)))
                  .map(scaleWeighting)
              } else if (marks.exists { case (_, _, mark) => mark.nonEmpty }) {
                uagm.upstreamAssessmentGroup.assessmentComponent
                  .flatMap(ac => ac.weightingFor(marks).getOrElse(ac.scaledWeighting))
              } else {
                uagm.upstreamAssessmentGroup.assessmentComponent
                  .flatMap(_.scaledWeighting)
              }

            (uagm, weighting)
          }
        }

        var sittings: Seq[Seq[(UpstreamAssessmentGroupMember, Option[BigDecimal])]] = Seq.empty
        var thisSitting: Seq[UpstreamAssessmentGroupMember] = Seq.empty
        reassessments.foreach { reassessment =>
          // If the attempt or assessment group has changed or the sequence has already been seen, add thisSitting to sittings
          if (thisSitting.exists(uagm => uagm.currentResitAttempt != reassessment.currentResitAttempt || uagm.upstreamAssessmentGroup.assessmentGroup != reassessment.upstreamAssessmentGroup.assessmentGroup || uagm.upstreamAssessmentGroup.sequence == reassessment.upstreamAssessmentGroup.sequence)) {
            sittings ++= Seq(calculateReassessmentWeightings(ModuleRegistration.filterToLatestAttempt(firstAttempt ++ thisSitting)))
            thisSitting = Seq.empty
          }

          thisSitting ++= Seq(reassessment)
        }
        sittings ++= Seq(calculateReassessmentWeightings(ModuleRegistration.filterToLatestAttempt(firstAttempt ++ thisSitting)))

        Seq(firstAttemptWithWeightings) ++ sittings
      }
    }

  def upstreamAssessmentGroupMembers: Seq[UpstreamAssessmentGroupMember] = {
    def extractMarks(components: Seq[UpstreamAssessmentGroupMember]): Seq[(AssessmentType, String, Option[Int])] = components.flatMap { uagm =>
      uagm.upstreamAssessmentGroup.assessmentComponent.map { ac =>
        (ac.assessmentType, ac.sequence, uagm.agreedMark) // Agreed marks only
      }
    }

    upstreamAssessmentGroupMembersAllAttempts(extractMarks).last.map(_._1)
  }

  def recordedAssessmentComponentStudents: Seq[RecordedAssessmentComponentStudent] = {
    val uagms = upstreamAssessmentGroupMembers

    RequestLevelCache.cachedBy("ModuleRegistration.recordedAssessmentComponentStudents", s"$academicYear-$sitsModuleCode-$occurrence") {
      upstreamAssessmentGroups.flatMap(assessmentComponentMarksService.getAllRecordedStudents)
    }.filter(r => uagms.exists(r.matchesIdentity))
  }

  def currentResitAttempt: Option[Int] = upstreamAssessmentGroupMembers.flatMap(_.currentResitAttempt).maxOption

  def currentUpstreamAssessmentGroupMembers: Seq[UpstreamAssessmentGroupMember] = {
    val withdrawnCourse = Option(studentCourseDetails.statusOnCourse).exists(_.code.startsWith("P"))
    upstreamAssessmentGroupMembers.filterNot(_ => withdrawnCourse)
  }

  def componentsForBenchmark: Seq[UpstreamAssessmentGroupMember] = {
    upstreamAssessmentGroupMembers
      .filter(_.deadline.exists(d => d.isBefore(ModuleRegistration.GraduationBenchmarkCutoff) || d.isEqual(ModuleRegistration.GraduationBenchmarkCutoff)))
      .filter(_.firstDefinedMark.isDefined)
  }

  def componentsIgnoredForBenchmark: Seq[UpstreamAssessmentGroupMember] = {
    upstreamAssessmentGroupMembers.diff(componentsForBenchmark)
  }

  def componentMarks(includeActualMarks: Boolean): Seq[(AssessmentType, String, Option[Int])] =
    upstreamAssessmentGroupMembers.flatMap { uagm =>
      uagm.upstreamAssessmentGroup.assessmentComponent.map { ac =>
        (ac.assessmentType, ac.sequence, if (includeActualMarks) uagm.firstDefinedMark else uagm.agreedMark)
      }
    }

  override def toString: String = s"$sprCode-$sitsModuleCode-$academicYear"

  //allowing module manager to see MR records - TAB-6062(module grids)
  def permissionsParents: LazyList[PermissionsTarget] = LazyList(Option(studentCourseDetails), Option(module)).flatten

  override def compare(that: ModuleRegistration): Int =
    new CompareToBuilder()
      .append(sprCode, that.sprCode)
      .append(sitsModuleCode, that.sitsModuleCode)
      .append(module, that.module)
      .append(cats, that.cats)
      .append(academicYear, that.academicYear)
      .append(occurrence, that.occurrence)
      .build()

  def moduleList(route: Route): Option[UpstreamModuleList] = route.upstreamModuleLists.asScala
    .filter(_.academicYear == academicYear)
    .find(_.matches(sitsModuleCode))
}

/**
  * Holds data about an individual student's registration on a single module.
  */
case class UpstreamAssessmentRegistration(
  year: String,
  sprCode: String,
  seatNumber: String,
  occurrence: String,
  sequence: String,
  moduleCode: String,
  assessmentGroup: String,
  actualMark: String,
  actualGrade: String,
  agreedMark: String,
  agreedGrade: String,
  currentResitAttempt: String,
  resitSequence: String,
  resitAssessmentName: Option[String],
  resitAssessmentType: Option[AssessmentType],
  resitAssessmentWeighting: Option[Int],
) {

  def universityId: String = SprCode.getUniversityId(sprCode)

  // Assessment group membership doesn't vary by sequence for original assessment but does for re-assessment
  def differentGroup(other: UpstreamAssessmentRegistration, assessmentType: UpstreamAssessmentGroupMemberAssessmentType): Boolean =
    year != other.year ||
    (occurrence != other.occurrence && assessmentGroup != AssessmentComponent.NoneAssessmentGroup) ||
    moduleCode != other.moduleCode ||
    assessmentGroup != other.assessmentGroup ||
    (assessmentType == UpstreamAssessmentGroupMemberAssessmentType.Reassessment && sequence != other.sequence)

  /**
    * Returns UpstreamAssessmentGroups matching the group attributes.
    */
  def toUpstreamAssessmentGroups(sequences: Seq[String]): Seq[UpstreamAssessmentGroup] = {
    sequences.map(sequence => {
      val g = new UpstreamAssessmentGroup
      g.academicYear = AcademicYear.parse(year)
      g.moduleCode = moduleCode
      g.assessmentGroup = assessmentGroup
      g.sequence = sequence
      // for the NONE group, override occurrence to also be NONE, because we create a single UpstreamAssessmentGroup
      // for each module with group=NONE and occurrence=NONE, and all unallocated students go in there together.
      g.occurrence =
        if (assessmentGroup == AssessmentComponent.NoneAssessmentGroup)
          AssessmentComponent.NoneAssessmentGroup
        else
          occurrence
      g
    })
  }

  /**
    * Returns an UpstreamAssessmentGroup matching the group attributes, including sequence.
    */
  def toExactUpstreamAssessmentGroup: UpstreamAssessmentGroup = {
    val g = new UpstreamAssessmentGroup
    g.academicYear = AcademicYear.parse(year)
    g.moduleCode = moduleCode
    g.assessmentGroup = assessmentGroup
    g.sequence = sequence
    // for the NONE group, override occurrence to also be NONE, because we create a single UpstreamAssessmentGroup
    // for each module with group=NONE and occurrence=NONE, and all unallocated students go in there together.
    g.occurrence =
      if (assessmentGroup == AssessmentComponent.NoneAssessmentGroup)
        AssessmentComponent.NoneAssessmentGroup
      else
        occurrence
    g
  }
}
