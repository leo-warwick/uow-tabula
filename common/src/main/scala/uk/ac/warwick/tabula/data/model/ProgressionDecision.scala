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
  case object UndergraduateAwardPass extends ProgressionDecisionWithAward(Set("UA2", "UA2-D"), "Award pass degree", message = FormattedHtml("Congratulations, you've passed!"))
  case object UndergraduateAwardDiploma extends ProgressionDecisionWithAward(Set("UA3", "UA3-D"), "Award diploma", message = FormattedHtml("Your results indicate that you have been awarded a Diploma of Higher Education. You will receive an email containing further details shortly."))
  case object UndergraduateAwardCertificate extends ProgressionDecisionWithAward(Set("UA4", "UA4-D"), "Award certificate", message = FormattedHtml("Your results indicate that you have been awarded a Certificate of Higher Education. You will receive an email containing further details shortly."))
  case object UndergraduateProceedHonours extends ProgressionDecisionOutcome(Set("UP1", "UP1-S"), "Proceed", message = FormattedHtml("Congratulations, you've passed!"))
  case object UndergraduateProceedPass extends ProgressionDecisionOutcome(Set("UP2", "UP2-S"), "Proceed to pass degree", message = FormattedHtml("Congratulations, you've passed!"))
  case object UndergraduateProceedLevel1 extends ProgressionDecisionOutcome(Set("UP3"), "Proceed to foundation degree", message = FormattedHtml("Congratulations, you've passed!"))
  case object UndergraduateProceedOptionalResit extends ProgressionDecisionOutcome(Set("UPH1", "UPH1-S"), "Proceed - resit optional", message = FormattedHtml("Congratulations, you've passed! You will however be able to resit (an) assessment(s) if you wish. You will receive an email containing further details."))
  case object UndergraduateProceedOptionalFurtherFirstSit extends ProgressionDecisionOutcome(Set("UPK1", "UPK1-S"), "Proceed - further first sit optional", message = FormattedHtml("Congratulations, you've passed! You will however be able to resit (an) assessment(s) if you wish. You will receive an email containing further details."))
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
  case object RequiredToRestart extends ProgressionDecisionOutcome(Set("RS", "RS-S"), "Required to restart", message = FormattedHtml("Your results indicate that you are not eligible to proceed to the next year of your degree. The Board of Examiners have decided that you should restart your 1st year. You will receive an email containing further details which will set our what you need to do next. Support is available to you as always either through your Personal Tutor or through Wellbeing Services."))
  case object Rejoin extends ProgressionDecisionOutcome(Set("RJ", "RJ-S"), "Rejoin", message = FormattedHtml("")) // TODO
  case object CourseTransfer extends ProgressionDecisionOutcome(Set("CTF", "CTP", "CTF-S", "CTP-S"), "Course transfer", message = FormattedHtml("")) // TODO
  case object DeferToSummer extends ProgressionDecisionOutcome(Set("UFD1-S"), "Defer to Summer", message = FormattedHtml("You deferred your assessments until the summer."))

  def forPitCode(pitCode: String): ProgressionDecisionOutcome =
    values.find(_.pitCodes.contains(pitCode)).getOrElse(throw new NoSuchElementException(s"$pitCode is not a recognised PIT code"))

  override def values: IndexedSeq[ProgressionDecisionOutcome] = findValues
}

class ProgressionDecisionOutcomeUserType extends OptionEnumUserType(ProgressionDecisionOutcome)

sealed abstract class ProgressionDecisionLevel extends EnumEntry
object ProgressionDecisionLevel extends Enum[ProgressionDecisionLevel] {
  case object FirstYear extends ProgressionDecisionLevel
  case object Intermediate extends ProgressionDecisionLevel
  case object Finalist extends ProgressionDecisionLevel
  override def values: IndexedSeq[ProgressionDecisionLevel] = findValues
}

sealed abstract class ProgressionDecisionBoard(val description: String) extends EnumEntry
object ProgressionDecisionBoard extends Enum[ProgressionDecisionBoard] {
  case object Summer extends ProgressionDecisionBoard(description = "Summer exam period")
  case object September extends ProgressionDecisionBoard(description = "Summer vacation exam period")
  override def values: IndexedSeq[ProgressionDecisionBoard] = findValues
}

sealed abstract class ActualProgressionDecision (
  val pitCode: String,
  val description: String,
  val applicableLevels: Seq[ProgressionDecisionLevel],
  val applicableBoards: Seq[ProgressionDecisionBoard],
  val notesRequired: Boolean = false,
  val notesHelp: Option[String] = None,
  val minutesRequired: Boolean = false,
  val minutesHelp: Option[String] = None
) extends EnumEntry

object ActualProgressionDecision extends Enum[ActualProgressionDecision] {

  import uk.ac.warwick.tabula.data.model.ProgressionDecisionLevel._
  import uk.ac.warwick.tabula.data.model.ProgressionDecisionBoard._

  case object CourseTransferFinalist extends ActualProgressionDecision (
    pitCode = "CTF",
    description = "Course transfer - finalist",
    applicableLevels = Seq(Intermediate, Finalist),
    applicableBoards = Seq(Summer),
    notesRequired = true,
    notesHelp = None, // TODO
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object CourseTransferFinalistSeptember extends ActualProgressionDecision (
    pitCode = "CTF-S",
    description = "Course transfer - finalist",
    applicableLevels = Seq(Intermediate, Finalist),
    applicableBoards = Seq(September),
    notesRequired = true,
    notesHelp = None, // TODO
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object CourseTransferProceed extends ActualProgressionDecision (
    pitCode = "CTP",
    description = "Course transfer - proceed",
    applicableLevels = Seq(FirstYear, Intermediate),
    applicableBoards = Seq(Summer),
    notesRequired = true,
    notesHelp = None, // TODO
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object CourseTransferProceedSeptember extends ActualProgressionDecision (
    pitCode = "CTP-S",
    description = "Course transfer - proceed",
    applicableLevels = Seq(FirstYear, Intermediate),
    applicableBoards = Seq(September),
    notesRequired = true,
    notesHelp = None, // TODO
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object First extends ActualProgressionDecision ("D1-01", "Honours degree - 1st Class", Seq(Finalist), Seq(Summer, September))
  case object UpperSecond extends ActualProgressionDecision ("D1-21", "Honours degree -  2:1", Seq(Finalist), Seq(Summer, September))
  case object LowerSecond extends ActualProgressionDecision ("D1-22", "Honours degree -  2:2", Seq(Finalist), Seq(Summer, September))
  case object Third extends ActualProgressionDecision ("D1-03", "Honours degree -  3rd class", Seq(Finalist), Seq(Summer, September))

  case object Rejoin extends ActualProgressionDecision (
    pitCode = "RJ",
    description = "Re-join",
    applicableLevels = Seq(FirstYear),
    applicableBoards = Seq(Summer),
    notesRequired = true,
    notesHelp = None, // TODO
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object RejoinSeptember extends ActualProgressionDecision (
    pitCode = "RJ-S",
    description = "Re-join",
    applicableLevels = Seq(FirstYear),
    applicableBoards = Seq(September),
    notesRequired = true,
    notesHelp = None, // TODO
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object Restart extends ActualProgressionDecision (
    pitCode = "RS",
    description = "Restart",
    applicableLevels = Seq(FirstYear),
    applicableBoards = Seq(Summer),
    notesRequired = true,
    notesHelp = None, // TODO
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object RestartSeptember extends ActualProgressionDecision (
    pitCode = "RS-S",
    description = "Restart",
    applicableLevels = Seq(FirstYear),
    applicableBoards = Seq(September),
    notesRequired = true,
    notesHelp = None, // TODO
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object UGAwardPassDegree extends ActualProgressionDecision ("UA2", "UG award pass degree", Seq(Finalist), Seq(Summer, September))
  case object UGAwardDiploma extends ActualProgressionDecision ("UA3", "UG award diploma", Seq(Finalist), Seq(Summer, September))
  case object UGAwardCertificate extends ActualProgressionDecision ("UA4", "UG award certificate", Seq(Finalist), Seq(Summer, September))
  case object DeferToSeptember extends ActualProgressionDecision ("UD1", "UG award certificate", Seq(FirstYear, Intermediate, Finalist), Seq(Summer))

  case object UGFinalistFail extends ActualProgressionDecision (
    pitCode = "UF1",
    description = "UG finalist fail",
    applicableLevels = Seq(Finalist),
    applicableBoards = Seq(Summer),
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object UGNonFinalistWithdraw extends ActualProgressionDecision (
    pitCode = "UF2",
    description = "UG non-finalist required to withdraw",
    applicableLevels = Seq(FirstYear, Intermediate),
    applicableBoards = Seq(Summer),
    minutesRequired = true,
    minutesHelp = Some("Please state why the student is required to withdraw (failed laboratory tests or failed resit examinations).")
  )

  case object UGNonFinalistWithdrawSeptember extends ActualProgressionDecision (
    pitCode = "UF2-S",
    description = "UG non-finalist required to withdraw",
    applicableLevels = Seq(FirstYear, Intermediate),
    applicableBoards = Seq(September),
    minutesRequired = true,
    minutesHelp = Some("Please state why the student is required to withdraw (failed laboratory tests or failed resit examinations).")
  )

  case object DeferToSummer extends ActualProgressionDecision (
    pitCode = "UFD1-S",
    description = "Defer to Summer",
    applicableLevels = ProgressionDecisionLevel.values,
    applicableBoards = Seq(September),
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object ProceedToHonours extends ActualProgressionDecision ("UP1", "UG proceed to honours", Seq(FirstYear, Intermediate), Seq(Summer))
  case object ProceedToHonoursSeptember extends ActualProgressionDecision ("UP1-S", "UG proceed to honours", Seq(FirstYear, Intermediate), Seq(September))
  case object ProceedToPass extends ActualProgressionDecision ("UP2", "UG proceed to pass", Seq(FirstYear, Intermediate), Seq(Summer))
  case object ProceedToPassSeptember extends ActualProgressionDecision ("UP2-S", "UG proceed to pass", Seq(FirstYear, Intermediate), Seq(September))
  case object ProceedToLevelOne extends ActualProgressionDecision ("UP3", "UG proceed level 1", Seq(FirstYear, Intermediate), Seq(Summer))

  case object ProceedResitOptional extends ActualProgressionDecision (
    pitCode = "UPH1",
    description = "UG proceed - resit optional",
    applicableLevels = Seq(FirstYear, Intermediate),
    applicableBoards = Seq(Summer),
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object ProceedResitOptionalSeptember extends ActualProgressionDecision (
    pitCode = "UPH1-S",
    description = "UG proceed - resit optional",
    applicableLevels = Seq(FirstYear, Intermediate),
    applicableBoards = Seq(September),
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object ProceedFFAOptional extends ActualProgressionDecision (
    pitCode = "UPK1",
    description = "UG proceed - further first attempt optional",
    applicableLevels = Seq(FirstYear, Intermediate),
    applicableBoards = Seq(Summer),
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object ProceedFFAOptionalSeptember extends ActualProgressionDecision (
    pitCode = "UPK1-S",
    description = "UG proceed - further first attempt optional",
    applicableLevels = Seq(FirstYear, Intermediate),
    applicableBoards = Seq(September),
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object ResitInSeptember extends ActualProgressionDecision (
    pitCode = "UR1",
    description = "UG resit in September",
    applicableLevels = ProgressionDecisionLevel.values,
    applicableBoards = Seq(Summer),
    minutesRequired = true,
    minutesHelp = Some("For each resit please include: the module code, list the assessments that are to be resat, when the resits will take place and if the resits are further first attempts or resits.")
  )

  case object RecommendWithdrawalPermitResitInSeptember extends ActualProgressionDecision (
    pitCode = "UR2",
    description = "UG recommend withdrawal/permit resit in September",
    applicableLevels = ProgressionDecisionLevel.values,
    applicableBoards = Seq(Summer),
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object RecommendWithdrawalPermitFinalResitInJune extends ActualProgressionDecision (
    pitCode = "UR2-S",
    description = "UG recommend withdrawal/permit resit in September",
    applicableLevels = ProgressionDecisionLevel.values,
    applicableBoards = Seq(September),
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object ResitYearWithoutResidence extends ActualProgressionDecision (
    pitCode = "UR3",
    description = "UG resit next year without residence",
    applicableLevels = ProgressionDecisionLevel.values,
    applicableBoards = Seq(Summer),
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object ResitYearWithoutResidenceSeptember extends ActualProgressionDecision (
    pitCode = "UR3-S",
    description = "UG resit next year without residence",
    applicableLevels = ProgressionDecisionLevel.values,
    applicableBoards = Seq(September),
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object ResitYearWithResidence extends ActualProgressionDecision (
    pitCode = "UR4",
    description = "UG resit next year with residence",
    applicableLevels = ProgressionDecisionLevel.values,
    applicableBoards = Seq(Summer),
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object ResitYearWithResidenceSeptember extends ActualProgressionDecision (
    pitCode = "UR4-S",
    description = "UG resit next year with residence",
    applicableLevels = ProgressionDecisionLevel.values,
    applicableBoards = Seq(September),
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object FirstAttemptInSeptember extends ActualProgressionDecision (
    pitCode = "US1",
    description = "UG Sit as first attempt in September",
    applicableLevels = ProgressionDecisionLevel.values,
    applicableBoards = Seq(Summer),
    minutesRequired = true,
    minutesHelp = None
  )

  case object FirstAttemptWithoutResidence extends ActualProgressionDecision (
    pitCode = "US2",
    description = "UG sit as first attempt without residence",
    applicableLevels = ProgressionDecisionLevel.values,
    applicableBoards = Seq(Summer),
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object FirstAttemptWithoutResidenceSeptember extends ActualProgressionDecision (
    pitCode = "US2-S",
    description = "UG sit as first attempt without residence",
    applicableLevels = ProgressionDecisionLevel.values,
    applicableBoards = Seq(September),
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object FirstAttemptWithResidence extends ActualProgressionDecision (
    pitCode = "US3",
    description = "UG sit as first attempt with residence",
    applicableLevels = ProgressionDecisionLevel.values,
    applicableBoards = Seq(Summer),
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  case object FirstAttemptWithResidenceSeptember extends ActualProgressionDecision (
    pitCode = "US3-S",
    description = "UG sit as first attempt with residence",
    applicableLevels = ProgressionDecisionLevel.values,
    applicableBoards = Seq(September),
    minutesRequired = true,
    minutesHelp = None // TODO
  )

  override def values: IndexedSeq[ActualProgressionDecision] = findValues

  def bySessionAndLevel: Map[(ProgressionDecisionBoard, ProgressionDecisionLevel), Seq[ActualProgressionDecision]] = ( for (
    t <- ProgressionDecisionBoard.values;
    l <- ProgressionDecisionLevel.values
  ) yield { (t, l) -> values.filter(v => v.applicableBoards.contains(t) && v.applicableLevels.contains(l)) } ).toMap
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


