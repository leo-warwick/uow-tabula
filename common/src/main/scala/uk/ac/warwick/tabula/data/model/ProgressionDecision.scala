package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}
import freemarker.core.TemplateHTMLOutputModel
import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.EnumTwoWayConverter
import uk.ac.warwick.tabula.{AcademicYear, Features, ItemNotFoundException, ToString}

import scala.jdk.CollectionConverters._

/**
 * A progression decision for a student (linked by SPR code).
 */
@Entity
@Proxy
@Access(AccessType.FIELD)
class ProgressionDecision extends GeneratedId with ToString {

  @Column(name = "spr_code", nullable = false)
  var sprCode: String = _

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "StudentCourseDetails_ProgressionDecision",
    joinColumns = Array(new JoinColumn(name = "progression_decision_id", insertable = false, updatable = false)),
    inverseJoinColumns = Array(new JoinColumn(name = "scjcode", insertable = false, updatable = false))
  )
  @JoinColumn(name = "scjcode", insertable = false, updatable = false)
  @BatchSize(size = 200)
  var _allStudentCourseDetails: JSet[StudentCourseDetails] = JHashSet()

  def studentCourseDetails: Option[StudentCourseDetails] =
    _allStudentCourseDetails.asScala.find(_.mostSignificant)
      .orElse(_allStudentCourseDetails.asScala.maxByOption(_.scjCode))

  @Column(nullable = false)
  var sequence: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(name = "academic_year", nullable = false)
  var academicYear: AcademicYear = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.ProgressionDecisionOutcomeUserType")
  var outcome: Option[ProgressionDecisionOutcome] = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.ProgressionDecisionProcessStatusUserType")
  @Column(nullable = false)
  var status: ProgressionDecisionProcessStatus = _


  /**
   * These are *NOT* visible to students
   */
  @Column(name = "notes")
  private var _notes: String = _
  def notes: Option[String] = _notes.maybeText
  def notes_=(notes: Option[String]): Unit = _notes = notes.orNull

  @Column(name = "minutes")
  private var _minutes: String = _
  def minutes: Option[String] = _minutes.maybeText
  def minutes_=(minutes: Option[String]): Unit = _minutes = minutes.orNull

  @Column(name = "resit_period", nullable = false)
  var resitPeriod: Boolean = _

  @transient var features: Features = Wire[Features]
  def isVisibleToStudent: Boolean = ((features.ignoreResultRelease || MarkState.resultsReleasedToStudents(academicYear, studentCourseDetails, MarkState.DecisionReleaseTime))
    && status == ProgressionDecisionProcessStatus.Complete)

  override def toStringProps: Seq[(String, Any)] = Seq(
    "sprCode" -> sprCode,
    "sequence" -> sequence,
    "academicYear" -> academicYear,
    "outcome" -> outcome,
    "minutes" -> minutes,
    "resitPeriod" -> resitPeriod
  )
}

sealed abstract class ProgressionDecisionOutcome(val pitCodes: Set[String], val description: String, val message: TemplateHTMLOutputModel) extends EnumEntry {
  def hasAward: Boolean = false // can't work with types easily in freemarker so implement this crufty def instead
}

sealed abstract class ProgressionDecisionWithAward(pitCodes: Set[String], description: String, message: TemplateHTMLOutputModel)
  extends ProgressionDecisionOutcome(pitCodes, description, message) {
    override def hasAward: Boolean = true
  }

object ProgressionDecisionOutcome extends Enum[ProgressionDecisionOutcome] {
  // Common suffixes:
  // -S decision in September
  // -D student is a debtor
  // We don't care about the distinction in Tabula

  case object Held extends ProgressionDecisionOutcome(Set("H"), "Held", message = FormattedHtml("Your progression decision is not yet available"))
  case object UndergraduateAwardHonours extends ProgressionDecisionWithAward(Set("UA1", "UA1-D", "UA1-S"), "Award honours degree", message = FormattedHtml("You've passed."))
  case object UndergraduateAwardPass extends ProgressionDecisionWithAward(Set("UA2", "UA2-D"), "Award pass degree", message = FormattedHtml("You've passed."))
  case object UndergraduateAwardDiploma extends ProgressionDecisionWithAward(Set("UA3", "UA3-D"), "Award diploma", message = FormattedHtml("Your results indicate that you have been awarded a Diploma of Higher Education. You will receive an email containing further details."))
  case object UndergraduateAwardCertificate extends ProgressionDecisionWithAward(Set("UA4", "UA4-D"), "Award certificate", message = FormattedHtml("Your results indicate that you have been awarded a Certificate of Higher Education. You will receive an email containing further details."))
  case object UndergraduateProceedHonours extends ProgressionDecisionOutcome(Set("UP1", "UP1-S"), "Proceed", message = FormattedHtml("Congratulations, you've passed!"))
  case object UndergraduateProceedPass extends ProgressionDecisionOutcome(Set("UP2", "UP2-S"), "Proceed to pass degree", message = FormattedHtml("Congratulations, you've passed!"))
  case object UndergraduateProceedLevel1 extends ProgressionDecisionOutcome(Set("UP3"), "Proceed to foundation degree", message = FormattedHtml("Congratulations, you've passed!"))
  case object UndergraduateProceedOptionalResit extends ProgressionDecisionOutcome(Set("UPH1"), "Proceed - resit optional", message = FormattedHtml("Congratulations, you've passed! You will however be able to resit (an) assessment(s) if you wish. You will receive an email containing further details."))
  case object UndergraduateProceedOptionalFurtherFirstSit extends ProgressionDecisionOutcome(Set("UPK1"), "Proceed - further first sit optional", message = FormattedHtml("Congratulations, you've passed! You will however be able to resit (an) assessment(s) if you wish. You will receive an email containing further details."))
  case object UndergraduateFinalistAcademicFail extends ProgressionDecisionOutcome(Set("UF1", "UF1-D"), "Academic fail", message = FormattedHtml("Your results indicate that you are not eligible for the award of a degree. You will receive an email containing further details. Personal support is available to you as always, either through your Personal Tutor or through Wellbeing Services."))
  case object UndergraduateNonFinalistWithdraw extends ProgressionDecisionOutcome(Set("UF2", "UF2-S"), "Withdraw", message = FormattedHtml("Your results indicate that you are required to withdraw from your course. You will receive an email containing further details. Personal support is available to you as always, either through your Personal Tutor or through Wellbeing Services."))
  case object UndergraduateResitInSeptember extends ProgressionDecisionOutcome(Set("UR1"), "Resit required", message = FormattedHtml("Your results indicate that further assessment will be required in order to pass this year. You will receive an email containing further details, which will set out what you need to do next. There is nothing you need to do until you have received that email. However, personal support is available to you as always, either through your Personal Tutor or through Wellbeing Services."))
  case object UndergraduateWithdrawOrResit extends ProgressionDecisionOutcome(Set("UR2", "UR2-S"), "Withdraw or permit resit", message = FormattedHtml("Your results indicate that further assessment will be required in order to pass this year. You will receive an email containing further details, which will set out what you need to do next. There is nothing you need to do until you have received that email. However, personal support is available to you as always, either through your Personal Tutor or through Wellbeing Services."))
  case object UndergraduateResitWithoutResidence extends ProgressionDecisionOutcome(Set("UR3", "UR3-S"), "Resit without residence", message = FormattedHtml("Your results indicate that further assessment will be required in order to pass this year. You will receive an email containing further details, which will set out what you need to do next. There is nothing you need to do until you have received that email. However, personal support is available to you as always, either through your Personal Tutor or through Wellbeing Services."))
  case object UndergraduateResitWithResidence extends ProgressionDecisionOutcome(Set("UR4", "UR4-S"), "Resit with residence", message = FormattedHtml("Your results indicate that further assessment will be required in order to pass this year. You will receive an email containing further details, which will set out what you need to do next. There is nothing you need to do until you have received that email. However, personal support is available to you as always, either through your Personal Tutor or through Wellbeing Services."))
  case object UndergraduateFirstSitInSeptember extends ProgressionDecisionOutcome(Set("US1"), "Further first sit required", message = FormattedHtml("Your results indicate that further assessment will be required in order to pass this year. You will receive an email containing further details, which will set out what you need to do next. There is nothing you need to do until you have received that email. However, personal support is available to you as always, either through your Personal Tutor or through Wellbeing Services."))
  case object UndergraduateFirstSitWithoutResidence extends ProgressionDecisionOutcome(Set("US2", "US2-S"), "Further first sit without residence", message = FormattedHtml("Your results indicate that further assessment will be required in order to pass this year. You will receive an email containing further details, which will set out what you need to do next. There is nothing you need to do until you have received that email. However, personal support is available to you as always, either through your Personal Tutor or through Wellbeing Services."))
  case object UndergraduateFirstSitWithResidence extends ProgressionDecisionOutcome(Set("US3", "US3-S"), "Further first sit with residence", message = FormattedHtml("Your results indicate that further assessment will be required in order to pass this year. You will receive an email containing further details, which will set out what you need to do next. There is nothing you need to do until you have received that email. However, personal support is available to you as always, either through your Personal Tutor or through Wellbeing Services."))
  case object UndergraduateDeferToSeptember extends ProgressionDecisionOutcome(Set("UD1"), "Defer to September", message = FormattedHtml("You deferred your assessments until September."))
  case object RequiredToRestart extends ProgressionDecisionOutcome(Set("RS"), "Required to restart", message = FormattedHtml("Your results indicate that you are not eligible to proceed to the next year of your degree. The Board of Examiners have decided that you should restart your 1st year. You will receive an email containing further details which will set our what you need to do next. Support is available to you as always either through your Personal Tutor or through Wellbeing Services."))

  def forPitCode(pitCode: String): ProgressionDecisionOutcome =
    values.find(_.pitCodes.contains(pitCode)).getOrElse(throw new NoSuchElementException(s"$pitCode is not a recognised PIT code"))

  override def values: IndexedSeq[ProgressionDecisionOutcome] = findValues
}

class ProgressionDecisionOutcomeUserType extends OptionEnumUserType(ProgressionDecisionOutcome)

sealed abstract class ActualProgressionDecision(
  val pitCode: String,
  val description: String,
  val finalistDescription: Option[String],
  val notesHelp: Option[String] = None
) extends EnumEntry {
  def validForFinalists: Boolean = finalistDescription.isDefined
  def notesRequired: Boolean = notesHelp.isDefined
}

object ActualProgressionDecision extends Enum[ActualProgressionDecision] {
  case object Proceed extends ActualProgressionDecision("D1", "Proceed to next year of Honours Degree", None)

  case object First extends ActualProgressionDecision (
    pitCode = "D1-01",
    description ="Proceed to next year of Honours – First so far",
    finalistDescription = Some("Award – First")
  )
  case object UpperSecond extends ActualProgressionDecision (
    pitCode = "D1-21",
    description = "Proceed to next year of Honours - Upper Second so far",
    Some("Award – Upper Second")
  )
  case object LowerSecond extends ActualProgressionDecision (
    pitCode = "D1-22",
    description = "Proceed to next year of Honours - Lower Second so far",
    Some("Award – Lower Second")
  )
  case object Third extends ActualProgressionDecision (
    pitCode = "D1-03",
    description = "Proceed to next year of Honours – Third so far",
    Some("Award - Third")
  )
  case object Pass extends ActualProgressionDecision (
    pitCode = "D3",
    description = "Proceed to Pass",
    Some("Pass Degree")
  )
  case object Resit extends ActualProgressionDecision (
    pitCode = "D4",
    description = "Resit / Further First Attempt",
    Some("Resit / Further First Attempt"),
    notesHelp = Some("For each resit please include: the module code, list the assessments that are to be resat, when the resits will take place and if the resits are further first attempts or resits.")
  )
  case object Withdraw extends ActualProgressionDecision (
    pitCode = "D5",
    description = "Fail: Required to Withdraw",
    Some("Fail: Required to Withdraw"),
    notesHelp = Some("Please state why the student is required to withdraw (failed laboratory tests or failed resit examanations).")
  )

  override def values: IndexedSeq[ActualProgressionDecision] = findValues
}

class ActualProgressionDecisionUserType extends EnumUserType(ActualProgressionDecision)
class ActualProgressionDecisionConverter extends EnumTwoWayConverter(ActualProgressionDecision)

sealed abstract class ProgressionDecisionProcessStatus (val code: Int) extends EnumEntry

object ProgressionDecisionProcessStatus extends Enum[ProgressionDecisionProcessStatus] {
  case object Incomplete extends ProgressionDecisionProcessStatus (0)
  case object Held extends ProgressionDecisionProcessStatus (1)
  case object Complete extends ProgressionDecisionProcessStatus (2)
  case object Agreed extends ProgressionDecisionProcessStatus (3)

  override def values: IndexedSeq[ProgressionDecisionProcessStatus] = findValues

  def forCode(code: Int): ProgressionDecisionProcessStatus = values.find(_.code == code)
    .getOrElse(throw new ItemNotFoundException(s"$code is not a valid ProgressionDecisionProcessStatus"))
}
class ProgressionDecisionProcessStatusUserType extends EnumUserType(ProgressionDecisionProcessStatus)


