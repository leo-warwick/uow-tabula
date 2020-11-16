package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.{Proxy, Type}
import org.joda.time.Duration
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.PreSaveBehaviour
import uk.ac.warwick.tabula.helpers.{Logging, RequestLevelCache}
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.{AcademicYear, ToString}

import scala.jdk.CollectionConverters._
import scala.util.{Success, Try}

/**
  * Represents an upstream assessment component as found in the central
  * University systems. An component is timeless - it doesn't
  * relate to a specific instance of an assignment/exam or even a particular year.
  *
  */
@Entity
@Proxy
class AssessmentComponent extends GeneratedId with PreSaveBehaviour with Serializable with ToString with Logging {

  @transient var membershipService: AssessmentMembershipService = Wire.auto[AssessmentMembershipService]

  /**
    * Uppercase module code, with CATS. e.g. IN304-15
    */
  var moduleCode: String = _

  /**
    * An OPTIONAL link to a Tabula representation of a Module.
    */
  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "module_id")
  var module: Module = _

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "assessmentComponent")
  var links: JList[AssessmentGroup] = JArrayList()

  /**
    * Assessment group the assignment is in. Is mostly a meaningless
    * character but will map to a corresponding student module registratation
    * to the same group.
    */
  var assessmentGroup: String = _

  /**
    * Identifier for the assignment, unique within a given moduleCode.
    */
  var sequence: String = _

  /**
    * Name as defined upstream.
    */
  var name: String = _

  @Column(name = "IN_USE")
  var inUse: Boolean = _

  /**
    * The type of component. Typical values are A for assignment,
    * E for summer exam. Other values exist.
    */
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AssessmentTypeUserType")
  @Column(nullable = false)
  var assessmentType: AssessmentType = _

  var marksCode: String = _

  /**
   * The raw weighting of the assessment component. Beware! This may need scaling (e.g. where weightings are 125/125/750 because SITS doesn't support decimals)
   * or may be affected by VariableAssessmentWeightingRules.
   */
  @Column(name = "weighting")
  var rawWeighting: JInteger = _

  private def scaleWeighting(raw: Int, total: Int): BigDecimal =
    if (raw == 0 || total == 0) BigDecimal(0) // 0 will always scale to 0 and a total of 0 will always lead to a weighting of 0
    else if (total == 100) BigDecimal(raw)
    else {
      val bd = BigDecimal(raw * 100) / BigDecimal(total)
      bd.setScale(1, BigDecimal.RoundingMode.HALF_UP)
      bd
    }

  @transient lazy val scaledWeighting: Option[BigDecimal] = Option(rawWeighting).map(_.toInt).map { weighting =>
    // If VAW applies, just use the raw weighting
    if (variableAssessmentWeightingRules.nonEmpty) BigDecimal(weighting)
    else {
      val totalWeight =
        allComponentsForAssessmentGroup
          .flatMap(ac => Option(ac.rawWeighting).map(_.toInt))
          .sum

      scaleWeighting(weighting, totalWeight)
    }
  }

  @transient lazy val variableAssessmentWeightingRules: Seq[VariableAssessmentWeightingRule] =
    membershipService.getVariableAssessmentWeightingRules(moduleCode, assessmentGroup)

  private def assessmentComponentsForModule: Seq[AssessmentComponent] =
    RequestLevelCache.cachedBy("AssessmentMembershipService.getAssessmentComponents", moduleCode) {
      membershipService.getAssessmentComponents(moduleCode, inUseOnly = false)
    }

  @transient lazy val allComponentsForAssessmentGroup: Seq[AssessmentComponent] = assessmentComponentsForModule.filter(_.assessmentGroup == assessmentGroup)

  /**
   * Calculate the weighting for the student that the UpstreamAssessmentGroupMembers represent, taking into account any
   * variable assessment weighting rules.
   *
   * @param marks a sequence of tuples of assessment type, assessment sequence (so we can identify this assessment) and mark,
   *              if available. This should only include marks that should be considered, which may be only agreed marks, or
   *              agreed marks falling back to actual marks.
   *
   * @return the scaled (out of 100%) weighting of this assessment component, using variable assessment weightings if available
   *         or falling back to [[scaledWeighting]]
   */
  def weightingFor(marks: Seq[(AssessmentType, String, Option[Int])]): Try[Option[BigDecimal]] =
    if (variableAssessmentWeightingRules.isEmpty) Success(scaledWeighting)
    else Try {
      // Get matching assessment components for this assessment _type_
      val componentsForType: Map[String, AssessmentComponent] =
        allComponentsForAssessmentGroup.filter(_.assessmentType == assessmentType)
          .map(ac => ac.sequence -> ac)
          .toMap

      val rulesForType = variableAssessmentWeightingRules.filter(_.assessmentType == assessmentType)

      // Tuple of sequence and mark, ordered by highest mark
      val marksForType: Seq[(String, Option[Int])] =
        marks.filter { case (t, _, _) => t == assessmentType }
          .map { case (_, sequence, mark) => (sequence, mark) }
          .sortBy { case (seq, mark) =>
            // Order by mark (in reverse order, so highest mark first) then by sequence, alphabetically
            (
              -mark.getOrElse(-1),
              seq
            )
          }

      // Data validation check. We must have:
      // - The same number of Variable Assessment Weighting Rules as there are components
      // - No extra unexpected marks have been passed
      // - Marks (or a record with no mark) for every assessment sequence
      require(componentsForType.size == rulesForType.size, s"${toString()}: There are ${componentsForType.size} assessment components for $assessmentType (${componentsForType.keys.mkString(",")}) but ${rulesForType.size} VAW rules")
      require(componentsForType.size == marksForType.size, s"${toString()}: There are ${componentsForType.size} assessment components for $assessmentType (${componentsForType.keys.mkString(",")}) but ${marksForType.size} marks passed (${marksForType.map(_._1).mkString(",")})")

      val missingMarkSequences = componentsForType.keys.filterNot(sequence => marksForType.exists { case (s, _) => s == sequence })
      require(missingMarkSequences.isEmpty, s"No mark was provided for assessment sequence(s) ${missingMarkSequences.mkString(", ")}")

      val rawWeightings: Map[AssessmentComponent, Int] =
        // The order is by highest mark
        marksForType.zipWithIndex.map { case ((seq, _), index) =>
          val rule = rulesForType(index)
          val component = componentsForType(seq)

          component -> rule.rawWeighting
        }.toMap

      rawWeightings.get(this).map { rawWeighting =>
        // Need to scale this lot too because VAW has raw weightings too!
        val totalWeight = variableAssessmentWeightingRules.map(_.rawWeighting).sum
        scaleWeighting(rawWeighting, totalWeight)
      }
    }

  @Column(name = "exam_paper_code")
  private var _examPaperCode: String = _
  def examPaperCode: Option[String] = Option(_examPaperCode)
  def examPaperCode_=(code: Option[String]): Unit = _examPaperCode = code.orNull

  @Column(name = "exam_paper_title")
  private var _examPaperTitle: String = _
  def examPaperTitle: Option[String] = Option(_examPaperTitle)
  def examPaperTitle_=(title: Option[String]): Unit = _examPaperTitle = title.orNull

  @Column(name = "exam_paper_section")
  private var _examPaperSection: String = _
  def examPaperSection: Option[String] = Option(_examPaperSection)
  def examPaperSection_=(section: Option[String]): Unit = _examPaperSection = section.orNull

  @Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDurationAsString")
  @Column(name = "exam_paper_duration")
  private var _examPaperDuration: Duration = _
  def examPaperDuration: Option[Duration] = Option(_examPaperDuration)
  def examPaperDuration_=(duration: Option[Duration]): Unit = _examPaperDuration = duration.orNull

  @Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDurationAsString")
  @Column(name = "exam_paper_reading_time")
  private var _examPaperReadingTime: Duration = _
  def examPaperReadingTime: Option[Duration] = Option(_examPaperReadingTime)
  def examPaperReadingTime_=(readingTime: Option[Duration]): Unit = _examPaperReadingTime = readingTime.orNull

  @Type(`type` = "uk.ac.warwick.tabula.data.model.ExaminationTypeUserType")
  @Column(name = "exam_paper_type")
  private var _examPaperType: ExaminationType = _
  def examPaperType: Option[ExaminationType] = Option(_examPaperType)
  def examPaperType_=(examPaperType: Option[ExaminationType]): Unit = _examPaperType = examPaperType.orNull

  // If the parent module is to be reassessed by a different set of components then this property identifies the assessment group containing the new components
  // If this is defined then the parent module has Reassessment model - Model B
  // populated from MAB_UDF8
  @Column(name = "reassessment_group")
  private var _reassessmentGroup: String = _
  def reassessmentGroup: Option[String] = Option(_reassessmentGroup)
  def reassessmentGroup_=(code: Option[String]): Unit = _reassessmentGroup = code.orNull

  def reassessmentComponents: Seq[AssessmentComponent] = Option(_reassessmentGroup).map { rg =>
    assessmentComponentsForModule.filter(_.assessmentGroup == rg)
  }.getOrElse(Nil)

  // If this is defined then this assessment represents a change in assessment method for a resit - this property is the sequence of the assessment component that _this_ replaces
  // populated from MAB_UDF3
  @Column(name = "assessment_replaced")
  private var _assessmentReplaced: String = _
  def assessmentReplaced: Option[String] = Option(_assessmentReplaced)
  def assessmentReplaced_=(code: Option[String]): Unit = _assessmentReplaced = code.orNull

  // if this component is to be reassessed by a different method this returns the new component
  def replacedBy: Option[AssessmentComponent] = assessmentComponentsForModule.find(_.assessmentReplaced.contains(sequence))

  /**
   * Whether this assessment component represents the final chronological assessment for this assessment group. This
   * will (should) be set to true for exactly one AssessmentComponent for a combination of [[moduleCode]] and [[assessmentGroup]].
   */
  @Column(name = "final_chronological_assessment")
  var finalChronologicalAssessment: Boolean = _

  /**
    * Returns moduleCode without CATS. e.g. in304
    */
  def moduleCodeBasic: String = Module.stripCats(moduleCode).getOrElse(throw new IllegalArgumentException(s"$moduleCode did not fit expected module code pattern"))

  /**
    * Returns the CATS as a string if it's present, e.g. 50
    */
  def cats: Option[String] = Module.extractCats(moduleCode)

  def sameKey(other: AssessmentComponent): Boolean = {
    this.sequence == other.sequence && this.moduleCode == other.moduleCode
  }

  def needsUpdatingFrom(other: AssessmentComponent): Boolean =
    this.name != other.name ||
    this.module != other.module ||
    this.assessmentGroup != other.assessmentGroup ||
    this.assessmentType != other.assessmentType ||
    this.inUse != other.inUse ||
    this.marksCode != other.marksCode ||
    this.rawWeighting != other.rawWeighting ||
    this.examPaperCode != other.examPaperCode ||
    this.examPaperTitle != other.examPaperTitle ||
    this.examPaperSection != other.examPaperSection ||
    this.examPaperDuration != other.examPaperDuration ||
    this.examPaperReadingTime != other.examPaperReadingTime ||
    this.examPaperType != other.examPaperType ||
    this.finalChronologicalAssessment != other.finalChronologicalAssessment ||
    this.reassessmentGroup != other.reassessmentGroup ||
    this.assessmentReplaced != other.assessmentReplaced

  override def preSave(newRecord: Boolean): Unit = {
    ensureNotNull("name", name)
    ensureNotNull("moduleCode", moduleCode)
  }

  private def ensureNotNull(name: String, value: Any): Unit = {
    if (value == null) throw new IllegalStateException("null " + name + " not allowed")
  }

  def copyFrom(other: AssessmentComponent): Unit = {
    moduleCode = other.moduleCode
    assessmentGroup = other.assessmentGroup
    sequence = other.sequence
    inUse = other.inUse
    module = other.module
    name = other.name
    assessmentType = other.assessmentType
    marksCode = other.marksCode
    rawWeighting = other.rawWeighting
    examPaperCode = other.examPaperCode
    examPaperTitle = other.examPaperTitle
    examPaperSection = other.examPaperSection
    examPaperDuration = other.examPaperDuration
    examPaperReadingTime = other.examPaperReadingTime
    examPaperType = other.examPaperType
    finalChronologicalAssessment = other.finalChronologicalAssessment
    reassessmentGroup = other.reassessmentGroup
    assessmentReplaced = other.assessmentReplaced
  }

  override def toStringProps: Seq[(String, Any)] = Seq(
    "moduleCode" -> moduleCode,
    "assessmentGroup" -> assessmentGroup,
    "sequence" -> sequence,
    "inUse" -> inUse,
    "module" -> module,
    "name" -> name,
    "assessmentType" -> assessmentType,
    "marksCode" -> marksCode,
    "rawWeighting" -> rawWeighting,
    "examPaperCode" -> examPaperCode,
    "examPaperTitle" -> examPaperTitle,
    "examPaperSection" -> examPaperSection,
    "examPaperDuration" -> examPaperDuration,
    "examPaperReadingTime" -> examPaperReadingTime,
    "examPaperType" -> examPaperType,
    "finalChronologicalAssessment" -> finalChronologicalAssessment,
    "reassessmentGroup" -> reassessmentGroup,
    "assessmentReplaced" -> assessmentReplaced
  )

  def upstreamAssessmentGroups(year: AcademicYear): Seq[UpstreamAssessmentGroup] = membershipService.getUpstreamAssessmentGroups(this, year)

  def linkedAssignments: Seq[Assignment] = links.asScala.toSeq.collect { case l if l.assignment != null => l.assignment }

  def scheduledExams(academicYear: Option[AcademicYear]): Seq[AssessmentComponentExamSchedule] = membershipService.findScheduledExams(this, academicYear)
}

object AssessmentComponent {
  /** The value we store as the assessment group when no group has been chosen for this student
    * (in ADS it is null)
    *
    * We also use this value for a few other fields, like Occurrence, so the variable
    * name is a bit too specific.
    */
  val NoneAssessmentGroup = "NONE"
}
