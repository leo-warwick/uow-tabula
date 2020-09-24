package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}
import uk.ac.warwick.tabula.data.convert.ConvertibleConverter

import scala.collection.immutable

sealed abstract class TabulaAssessmentSubtype(val code: String) extends EnumEntry
object TabulaAssessmentSubtype extends Enum[TabulaAssessmentSubtype] {

  case object Assignment extends TabulaAssessmentSubtype("A")
  case object Exam extends TabulaAssessmentSubtype("E")
  case object Other extends TabulaAssessmentSubtype("O")

  override val values: immutable.IndexedSeq[TabulaAssessmentSubtype] = findValues
}

// full list of types taken from here - https://repo.elab.warwick.ac.uk/projects/MAPP/repos/app/browse/app/sits/AssessmentHelper.scala#92-156
// TODO - fetch the list via an API call to module approval rather than maintaining it twice - https://moduleapproval.warwick.ac.uk/api/v1/assessment_types
sealed abstract class AssessmentType(val astCode: String, val name: String, val subtype: TabulaAssessmentSubtype) extends EnumEntry with Convertible[String] {
  val code:String = subtype.code
  override val value: String = astCode

  // old school getters so spring form options tags work
  def getAstCode: String = astCode
  def getName: String = name
}

sealed abstract class AssignmentType(astCode: String, name: String) extends AssessmentType(astCode, name, TabulaAssessmentSubtype.Assignment)
sealed abstract class ExamType(astCode: String, name: String, val onlineEquivalent: Option[OnlineExamType] = None) extends AssessmentType(astCode, name, TabulaAssessmentSubtype.Exam)
sealed abstract class OnlineExamType(astCode: String, name: String) extends AssessmentType(astCode, name, TabulaAssessmentSubtype.Exam)

object AssessmentType extends Enum[AssessmentType] {

  implicit val factory: String => AssessmentType = { astCode: String => values.find(_.astCode == astCode).getOrElse(Other) }

  case object Other extends AssessmentType("O", "Other", TabulaAssessmentSubtype.Other)

  case object OnlineSummerExam extends OnlineExamType("OE", "Online Examination (Summer)")
  case object OnlineJanuaryExam extends OnlineExamType("OEJ", "Online Examination (January)")
  case object OnlineMarchExam extends OnlineExamType("OEM", "Online Examination (March)")
  case object OnlineAprilExam extends OnlineExamType("OEA", "Online Examination (April)")
  case object OnlineMayExam extends OnlineExamType("OEY", "Online Examination (May)")
  case object OnlineSeptemberExam extends OnlineExamType("OES", "Online Examination (September)")
  case object OnlineDecemberExam extends OnlineExamType("OED", "Online Examination (December)")

  case object SummerExam extends ExamType("E", "Exam (summer)", Some(OnlineSummerExam))
  case object JanuaryExam extends ExamType("EJ", "Exam (January)", Some(OnlineJanuaryExam))
  case object FebruaryExam extends ExamType("EF", "Exam (February)", Some(OnlineMarchExam)) // NOTE not in Module Approval 2020-01-22
  case object MarchExam extends ExamType("EM", "Exam (March)", Some(OnlineMarchExam))
  case object AprilExam extends ExamType("EA", "Exam (April)", Some(OnlineAprilExam))
  case object MayExam extends ExamType("EY", "Exam (May)", Some(OnlineMayExam))
  case object JulyExam extends ExamType("EL", "Exam (July)", Some(OnlineSeptemberExam)) // NOTE not in Module Approval 2020-01-22
  case object Week39Exam extends ExamType("WK39", "Exam (June / July)", Some(OnlineSeptemberExam)) // https://warwick.slack.com/archives/CMARBSEE9/p1583487098028300
  case object SeptemberExam extends ExamType("ES", "Exam (September)", Some(OnlineSeptemberExam))
  case object DecemberExam extends ExamType("ED", "Exam (December)", Some(OnlineDecemberExam))

  case object LocalExamination extends ExamType("LX", "Locally-timetabled examination")
  case object TakeHome extends ExamType("HE", "Take-home examiniation")
  case object Viva extends ExamType("VV", "Viva Voce Examination")
  case object Interview extends ExamType("INT", "Interview")
  case object SpokenAssessment extends ExamType("SP", "Spoken Assessment")

  case object Assignment extends AssignmentType("A", "Assignment") // NOTE not in Module Approval 2020-01-22
  case object AssignmentBestSubsetGroup extends AssignmentType("AN", "Assignment") // NOTE not in Module Approval 2020-01-22 - don't rely on this name being what you think it is
  case object AssignmentBestSubsetIndividual extends AssignmentType("AV", "Assignment") // NOTE not in Module Approval 2020-01-22 - don't rely on this name being what you think it is

  case object Abstract extends AssignmentType("AB", "Abstract")
  case object Curation extends AssignmentType("C", "Practicum - Curation")
  case object CommonplaceBook extends AssignmentType("CB", "Commonplace book")
  case object CriticalReview extends AssignmentType("CR", "Critical review")
  case object ComputerBasedInClassTest extends AssignmentType("CTC", "Computer-based - In-class test")
  case object ComputerBasedTakeHomeTest extends AssignmentType("CTH", "Computer-based - Take home test")
  case object Dissertation extends AssignmentType("D", "Dissertation")
  case object DesignAssignment extends AssignmentType("DA", "Design assignment")
  case object DissertationPlan extends AssignmentType("DP", "Dissertation plan")
  case object DissertationProgressReport extends AssignmentType("DPR", "Dissertation progress report")
  case object Essay extends AssignmentType("WRI", "Essay")
  case object EssayDraft extends AssignmentType("WRID", "Essay draft")
  case object EssayPlan extends AssignmentType("WRIP", "Essay plan")
  case object FilmProduction extends AssignmentType("FI", "Film production")
  case object Laboratory extends AssignmentType("LAB", "Practicum - Laboratory")
  case object LiteratureReview extends AssignmentType("LR", "Literature review")
  case object ObjectiveStructuredClinicalAssessment extends AssignmentType("OSCA", "Practicum - Objective structured clinical assessment (OSCA)")
  case object PolicyBrief extends AssignmentType("PB", "Policy brief")
  case object Performance extends AssignmentType("PERF", "Practicum - Performance")
  case object GroupProjectMarkedCollectively extends AssignmentType("PJG", "Project - Group - Marked collectively")
  case object GroupProjectMarkedIndividually extends AssignmentType("PJGI", "Project - Group - Marked individually")
  case object GroupProjectCombinedIndividualAndCollectiveMark extends AssignmentType("PJGIC", "Project - Group - Combined individual and collective mark")
  case object IndividualProject extends AssignmentType("PJI", "Project - Individual")
  case object WorkBasedLearningOrPlacement extends AssignmentType("PL", "Practicum - Work based learning or placement")
  case object GroupPosterPresentationMarkedCollectively extends AssignmentType("POG", "Poster presentation - Group - Marked collectively")
  case object GroupPosterPresentationMarkedIndividually extends AssignmentType("POGI", "Poster presentation - Group - Marked individually")
  case object GroupPosterPresentationCombinedIndividualAndCollectiveMark extends AssignmentType("POGIC", "Poster presentation - Group - Combined individual and collective mark")
  case object IndividualPosterPresentation extends AssignmentType("POI", "Poster presentation - Individual")
  case object PortfolioAssignment extends AssignmentType("PORT", "Portfolio assignment")
  case object ProblemSet extends AssignmentType("PR", "Problem set")
  case object GroupPresentationMarkedCollectively extends AssignmentType("PRG", "Presentation - Group - Marked collectively")
  case object GroupPresentationCombinedIndividualAndCollectiveMark extends AssignmentType("PRGIC", "Presentation - Group - Combined individual and collective mark")
  case object GroupPresentationMarkedIndividually extends AssignmentType("PRGI", "Presentation - Group - Marked individually")
  case object IndividualPresentation extends AssignmentType("PRI", "Presentation - Individual")
  case object OnlinePublication extends AssignmentType("PUB", "Online publication")
  case object ReflectivePiece extends AssignmentType("REF", "Reflective piece")
  case object WrittenReport extends AssignmentType("REP", "Written report")
  case object StudentDevisedAssessment extends AssignmentType("SDA", "Student devised assessment")
  case object SystemsModellingAndSystemIdentificationAssignment extends AssignmentType("SM", "Systems modelling and system identification assignment")
  case object MootTrial extends AssignmentType("TR", "Practicum - Moot trial")
  case object Workshop extends AssignmentType("W", "Practicum - Workshop")
  case object Worksheet extends AssignmentType("WS", "Worksheet")
  case object InClassTest extends AssignmentType("T", "In-class test") // NOTE not in Module Approval 2020-01-22
  case object InClassTestMultipleChoice extends AssignmentType("TMC", "In-class test - Multiple choice")
  case object InClassTestShortAnswer extends AssignmentType("TSA", "In-class test - Short answer")
  case object InClassTestOther extends AssignmentType("TO", "In-class test - Other")
  case object ContributionInLearningActivities extends AssignmentType("LA", "Contribution in learning activities (face-to-face or digital)")

  override val values: immutable.IndexedSeq[AssessmentType] = findValues
}

class AssessmentTypeUserType extends ConvertibleStringUserType[AssessmentType]
class AssessmentTypeConverter extends ConvertibleConverter[String, AssessmentType]
