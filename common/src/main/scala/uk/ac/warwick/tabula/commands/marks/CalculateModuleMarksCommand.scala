package uk.ac.warwick.tabula.commands.marks

import freemarker.core.TemplateHTMLOutputModel
import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.eventusermodel.{ReadOnlySharedStringsTable, XSSFReader}
import org.apache.poi.xssf.usermodel.XSSFComment
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.CalculateModuleMarksCommand._
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.data.model.MarkState.UnconfirmedActual
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.marks.{ModuleRegistrationValidGradesForMarkRequest, ValidGradesForMark}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.coursework.docconversion.AbstractXslxSheetHandler
import uk.ac.warwick.tabula.services.marks._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.jdk.CollectionConverters._
import scala.util.Using
import scala.xml.InputSource

object CalculateModuleMarksCommand {

  sealed trait ModuleMarkCalculation {
    def isSuccessful: Boolean
    def isMultiple: Boolean
  }

  object ModuleMarkCalculation {

    case class Success(mark: Option[Int], grade: Option[String], result: Option[ModuleResult], comments: Option[String] = None) extends ModuleMarkCalculation {
      override val isSuccessful: Boolean = true
      override val isMultiple: Boolean = false
    }

    case class SuggestedModuleMarkCalculation(title: String, description: TemplateHTMLOutputModel, calculation: ModuleMarkCalculation)

    case class Multiple(reason: TemplateHTMLOutputModel, suggestions: Seq[SuggestedModuleMarkCalculation]) extends ModuleMarkCalculation {
      override val isSuccessful: Boolean = true
      override val isMultiple: Boolean = true
    }

    sealed trait Fail extends ModuleMarkCalculation {
      override val isSuccessful: Boolean = false
      override val isMultiple: Boolean = false
    }

    case class Failure(message: String) extends Fail
    case class FailureWithMark(mark: Int, message: String) extends Fail

    // Some reusable results (just trying to keep these together)
    object Success {
      def apply(mark: Option[Int], gradeBoundary: Option[GradeBoundary]): Success = Success(mark, gradeBoundary.map(_.grade), gradeBoundary.flatMap(_.result))

      def PassFail(gradeBoundary: GradeBoundary): Success = Success(None, Some(gradeBoundary.grade), gradeBoundary.result)
    }

    object MissingMarkAdjustment {
      val NoCalculationPossible: Success = Success(None, Some(GradeBoundary.ForceMajeureMissingComponentGrade), Some(ModuleResult.Pass), comments = Some("Missing mark adjustment - learning outcomes assessed, unable to calculate an overall module mark"))
      val AllComponentsMissing: Success = Success(None, Some(GradeBoundary.ForceMajeureMissingComponentGrade), Some(ModuleResult.Deferred), comments = Some("Missing mark adjustment - learning outcomes not assessed"))

      // Option 1) Use the remaining components to calculate a mark, with credit
      // Option 2) A force majeure pass, with credit
      // Option 3) No result, no credit
      def SomeComponentsMissing(calculation: ModuleMarkCalculation): Multiple = Multiple(
        reason = FormattedHtml(
          """
            |At least one component has been recorded as missed due to force majeure.
            |Which calculation to use depends on whether there have been sufficient other components completed to assess the learning outcomes.
            |[Guidance on the options in 2019/20 is available on the Teaching Continuity site](https://warwick.ac.uk/insite/coronavirus/staff/teaching/marksandexamboards/guidance/decisions-first-year/#credit).
            |""".stripMargin
        ),
        suggestions = Seq(
          SuggestedModuleMarkCalculation(
            title = "Option 1 (Overall Mark and Credit Awarded)",
            description = FormattedHtml("One or more assessments have been cancelled but the module learning outcomes have still been assessed. Sufficient assessment has taken place for student performance to be differentiated, so a module mark is calculated from a weighted average of the remaining components and pass/fail result is determined accordingly. Students will be awarded credit for the module where they pass."),
            calculation = calculation match {
              case s: ModuleMarkCalculation.Success => s.copy(comments = Some("Missing mark adjustment - learning outcomes assessed, weighted mark"))
              case o => o
            },
          ),
          SuggestedModuleMarkCalculation(
            title = "Option 2 (Pass/fail and Credit Awarded)",
            description = FormattedHtml("One or more assessments have been cancelled and the module learning outcomes have still been assessed. However, sufficient assessment has not taken place for student performance to be differentiated, so a module mark will not be calculated and only a pass/fail result will be determined. Students will be awarded credit for the module where they pass."),
            calculation = NoCalculationPossible,
          ),
          SuggestedModuleMarkCalculation(
            title = "Option 3 (No Result, No Credit)",
            description = FormattedHtml("All or most assessment has been cancelled and there is insufficient evidence available that the student has achieved the learning outcomes of the module, so no result can be recorded and no credit can be awarded."),
            calculation = AllComponentsMissing,
          ),
        ),
      )
    }

    object Failure {
      val NoModuleRegistration: Failure = Failure("No module registration found")
      val NoComponentMarks: Failure = Failure("There were no component marks")
      val NoMarkScheme: Failure = Failure("There is no mark scheme associated with the module registration")

      def MarksAndGradesMissingFor(sequences: Seq[String]): Failure = Failure(s"Marks and grades are missing for ${sequences.mkString(", ")}")

      object PassFail {
        val MismatchedMarkScheme: Failure = Failure("Not all components are pass/fail but module is")

        def GradesMissingFor(sequences: Seq[String]): Failure = Failure(s"Grades are missing for ${sequences.mkString(", ")}")

        val NoDefaultGrade: Failure = Failure("Unable to select a valid grade")

        def MismatchedGrades(grades: Seq[String]): Failure = Failure(s"Couldn't automatically select a result from component grades ${grades.mkString(", ")}")
      }

      def MarksMissingFor(sequences: Seq[String]): Failure = Failure(s"Marks are missing for ${sequences.mkString(", ")}")

      def WeightingsMissingFor(sequences: Seq[String]): Failure = Failure(s"Weightings are missing for ${sequences.mkString(", ")}")

      def NoGradeBoundary(grade: String): Failure = Failure(s"Unable to find grade boundary for $grade grade")

      def NoGradeBoundary(mark: Int, grade: String): FailureWithMark = FailureWithMark(mark, s"$grade is not a valid grade for a mark of $mark. Another grade must be selected")

      def MismatchedIndicatorGrades(grades: Seq[String], sequences: Seq[String]): Failure =
        Failure(s"For a mark to be calculated when an indicator grade has been used for a component, this same grade (${grades.mkString(", ")}) must be used for all components (${sequences.mkString(", ")}, excluding any with a grade of ${GradeBoundary.ForceMajeureMissingComponentGrade})")

      val General: Failure = Failure("Couldn't automatically calculate a module result")
    }

  }

  type SprCode = ModuleOccurrenceCommands.SprCode
  class StudentModuleMarksItem extends ModuleOccurrenceCommands.StudentModuleMarksItem {
    def this(sprCode: SprCode) = {
      this()
      this.sprCode = sprCode
    }

    var calculate: Boolean = true
  }

  type Result = Seq[RecordedModuleRegistration]
  type Command = Appliable[Result]
    with CalculateModuleMarksRequest
    with CalculateModuleMarksLoadModuleRegistrations
    with SelfValidating
    with BindListener
    with PopulateOnForm

  def apply(sitsModuleCode: String, module: Module, academicYear: AcademicYear, occurrence: String, currentUser: CurrentUser): Command =
    new CalculateModuleMarksCommandInternal(sitsModuleCode, module, academicYear, occurrence, currentUser)
      with CalculateModuleMarksLoadModuleRegistrations
      with CalculateModuleMarksRequest
      with CalculateModuleMarksValidation
      with ModuleOccurrenceUpdateMarksPermissions
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringResitServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringTransactionalComponent
      with AutowiringSecurityServiceComponent
      with ComposableCommand[Result] // late-init due to CalculateModuleMarksLoadModuleRegistrations being called from permissions
      with RecordedModuleRegistrationsDescription
      with ModuleOccurrenceValidGradesBindListener
      with CalculateModuleMarksSpreadsheetBindListener
      with CompositeCalculateModuleMarksBindListener
      with CalculateModuleMarksPopulateOnForm
      with CalculateModuleMarksAlgorithm
      with AutowiringProfileServiceComponent
      with ConfirmModuleMarkChangedCommandNotification
}

abstract class CalculateModuleMarksCommandInternal(val sitsModuleCode: String, val module: Module, val academicYear: AcademicYear, val occurrence: String, val currentUser: CurrentUser)
  extends CommandInternal[Result]
    with ModuleOccurrenceState with ClearRecordedModuleMarksState with RecordedModuleRegistrationNotificationDepartment {
  self: CalculateModuleMarksRequest
    with CalculateModuleMarksLoadModuleRegistrations
    with ModuleRegistrationMarksServiceComponent
    with TransactionalComponent
    with ProfileServiceComponent
    with RecordedModuleRegistrationsDescription =>

  override val mandatoryEventName: String = "CalculateModuleMarks"

  override def applyInternal(): Result = transactional() {
    students.asScala.values.filter(_.calculate).toSeq
      .map { item =>
        val moduleRegistration =
          moduleRegistrations.find(_.sprCode == item.sprCode)
            .get // We validate that this exists

        val recordedModuleRegistration: RecordedModuleRegistration =
          moduleRegistrationMarksService.getOrCreateRecordedModuleRegistration(moduleRegistration)

        val source =
          if (doesntMatchCalculation(item)) RecordedModuleMarkSource.RecordModuleMarks
          else RecordedModuleMarkSource.ComponentMarkCalculation

        recordedModuleRegistration.addMark(
          uploader = currentUser.apparentUser,
          mark = item.mark.maybeText.map(_.toInt),
          grade = item.grade.maybeText,
          result = item.result.maybeText.flatMap(c => Option(ModuleResult.fromCode(c))),
          source = source,
          markState = recordedModuleRegistration.latestState.getOrElse(UnconfirmedActual),
          comments = item.comments,
        )

        moduleRegistrationMarksService.saveOrUpdate(recordedModuleRegistration)

        recordedModuleRegistration
      }
  }
}

trait CalculateModuleMarksLoadModuleRegistrations extends ModuleOccurrenceLoadModuleRegistrations {
  self: ModuleOccurrenceState
    with CalculateModuleMarksAlgorithm
    with AssessmentMembershipServiceComponent
    with AssessmentComponentMarksServiceComponent
    with ModuleRegistrationServiceComponent
    with ModuleRegistrationMarksServiceComponent
    with ResitServiceComponent =>

  lazy val studentModuleMarkRecordsAndCalculations: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, (StudentMarkRecord, Option[BigDecimal])], Option[ModuleRegistration], ModuleMarkCalculation)] = {
    val noReg: Option[ModuleRegistration] = None
    studentModuleMarkRecords.map { student =>
      val (cm, mr, calculation) =
        moduleRegistrations.find(_.sprCode == student.sprCode).map { moduleRegistration =>
          val components: Map[AssessmentComponent, (StudentMarkRecord, Option[BigDecimal])] = componentMarks(moduleRegistration)
          val mr: Option[ModuleRegistration] = Some(moduleRegistration)

          (components, mr, calculate(moduleRegistration, components.toSeq))
        }.getOrElse((Map.empty[AssessmentComponent, (StudentMarkRecord, Option[BigDecimal])], noReg, ModuleMarkCalculation.Failure.NoModuleRegistration))

      (student, cm, mr, calculation)
    }
  }

  def doesntMatchCalculation(item: StudentModuleMarksItem): Boolean =
    studentModuleMarkRecordsAndCalculations.find(_._1.sprCode == item.sprCode).exists { case (currentModuleMarkRecord, _, _, calculation) =>
      def differsFrom(mark: Option[Int], grade: Option[String], result: Option[ModuleResult]): Boolean =
        item.mark.maybeText.exists(m => m != mark.map(_.toString).getOrElse("")) ||
        item.grade.maybeText.exists(g => g != grade.getOrElse("")) ||
        item.result.maybeText.exists(r => r != result.map(_.dbValue).getOrElse(""))

      def differsFromCalculation(calculation: ModuleMarkCalculation): Boolean = calculation match {
        // Allow an FM grade if there's any calculated FM grade, regardless of result or mark
        case ModuleMarkCalculation.Success(_, Some(GradeBoundary.ForceMajeureMissingComponentGrade), _, _) if item.grade.maybeText.contains(GradeBoundary.ForceMajeureMissingComponentGrade) => false
        case ModuleMarkCalculation.Success(mark, grade, result, _) => differsFrom(mark, grade, result)
        case ModuleMarkCalculation.Multiple(_, suggestions) => suggestions.map(_.calculation).forall(differsFromCalculation)
        case _ => false // If the calculation was a failure, allow it through
      }

      differsFrom(currentModuleMarkRecord.mark, currentModuleMarkRecord.grade, currentModuleMarkRecord.result) && differsFromCalculation(calculation)
    }
}

trait CalculateModuleMarksRequest extends ModuleOccurrenceMarksRequest[StudentModuleMarksItem] {
  override var students: JMap[SprCode, StudentModuleMarksItem] =
    LazyMaps.create { sprCode: SprCode => new StudentModuleMarksItem(sprCode) }
      .asJava

  // For uploading a spreadsheet
  var file: UploadedFile = new UploadedFile
}

trait CalculateModuleMarksSpreadsheetBindListener {
  self: CalculateModuleMarksRequest
    with TransactionalComponent =>

  final val MAX_MARKS_ROWS: Int = 5000
  final val VALID_FILE_TYPES: Seq[String] = Seq(".xlsx")

  def onBindSpreadsheet(result: BindingResult): Unit = {
    val fileNames = file.fileNames.map(_.toLowerCase)
    val invalidFiles = fileNames.filter(s => !VALID_FILE_TYPES.exists(s.endsWith))

    if (invalidFiles.nonEmpty) {
      if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString(""), VALID_FILE_TYPES.mkString(", ")), "")
      else result.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", "), VALID_FILE_TYPES.mkString(", ")), "")
    }

    if (!result.hasErrors) {
      transactional() {
        result.pushNestedPath("file")
        file.onBind(result)
        result.popNestedPath()

        file.attached.asScala.filter(_.hasData).foreach(file => {
          try {
            Using.resource(file.asByteSource.openStream()) { stream =>
              val pkg = OPCPackage.open(stream)
              val sst = new ReadOnlySharedStringsTable(pkg)
              val reader = new XSSFReader(pkg)
              val styles = reader.getStylesTable

              val items: JList[StudentModuleMarksItem] = JArrayList()
              val sheetHandler = new AbstractXslxSheetHandler(styles, sst, items) {
                override def newCurrentItem: StudentModuleMarksItem = new StudentModuleMarksItem()

                override def cell(cellReference: String, formattedValue: String, comment: XSSFComment): Unit = {
                  val col = new CellReference(cellReference).getCol
                  if (isFirstRow) {
                    columnMap(col) = formattedValue
                  } else if (columnMap.asJava.containsKey(col) && formattedValue.hasText) {
                    columnMap(col) match {
                      // Support the old format in the hope that SPR and SCJ codes match
                      case "SPR Code" | "SPR" | "SCJ Code" | "SCJ" =>
                        currentItem.sprCode = formattedValue
                      case "Mark" =>
                        currentItem.mark = formattedValue
                      case "Grade" =>
                        currentItem.grade = formattedValue
                      case "Result" =>
                        currentItem.result = formattedValue
                      case "Comments" =>
                        currentItem.comments = formattedValue
                      case _ => // ignore anything else
                    }
                  }
                }
              }

              val parser = sheetHandler.fetchSheetParser
              reader.getSheetsData.asScala.foreach { is =>
                Using.resource(is)(sheet => parser.parse(new InputSource(sheet)))
              }

              if (items.size() > MAX_MARKS_ROWS) {
                result.rejectValue("file", "file.tooManyRows", Array(MAX_MARKS_ROWS.toString), "")
                items.clear()
              } else {
                items.asScala.filter(_.sprCode.hasText).foreach { item =>
                  students.put(item.sprCode, item)
                }
              }
            }
          } catch {
            case _: InvalidFormatException => result.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", "), VALID_FILE_TYPES.mkString(", ")), "")
          }
        })
      }
    }
  }
}

trait CompositeCalculateModuleMarksBindListener extends BindListener {
  self: CalculateModuleMarksRequest
    with ModuleOccurrenceValidGradesBindListener
    with CalculateModuleMarksSpreadsheetBindListener
    with TransactionalComponent =>

  override def onBind(result: BindingResult): Unit = {
    onBindSpreadsheet(result)
    onBindValidGrades(result)
  }
}

trait CalculateModuleMarksPopulateOnForm extends PopulateOnForm {
  self: ModuleOccurrenceState
    with CalculateModuleMarksRequest
    with CalculateModuleMarksAlgorithm
    with CalculateModuleMarksLoadModuleRegistrations
    with ModuleRegistrationMarksServiceComponent
    with ModuleRegistrationServiceComponent
    with AssessmentMembershipServiceComponent
    with AssessmentComponentMarksServiceComponent =>

  override def populate(): Unit = {
    studentModuleMarkRecordsAndCalculations.foreach { case (student, components, mr, calculation) =>
      val s = new StudentModuleMarksItem(student.sprCode)

      def populateMarksItem(mark: Option[Int], grade: Option[String], result: Option[ModuleResult], comments: Option[String] = None): Unit = {
        mark.foreach(m => s.mark = m.toString)
        grade.foreach(s.grade = _)

        val request = new ValidModuleRegistrationGradesRequest
        request.mark = mark.map(_.toString).getOrElse("")
        request.existing = grade.orNull
        s.validGrades = ValidGradesForMark.getTuple(request, mr.get)(assessmentMembershipService = assessmentMembershipService)

        result.foreach(r => s.result = r.dbValue)
        comments.foreach(s.comments = _)
      }

      populateMarksItem(student.mark, student.grade, student.result)

      lazy val mostRecentChange =
        student.history
          .find(m => m.source == RecordedModuleMarkSource.ComponentMarkCalculation || m.source == RecordedModuleMarkSource.RecordModuleMarks)

      // We use the calculation _unless_:
      // - We had a RecordModuleMarks before the last ComponentMarkCalculation; or
      // - The most recent ComponentMarkCalculation had comments and there haven't been any component mark changes since
      lazy val shouldUseCalculation: Boolean = {

        if (student.mark.isEmpty && student.grade.isEmpty && student.result.isEmpty) true
        else if (mostRecentChange.isEmpty) true
        else if (mostRecentChange.exists(_.source == RecordedModuleMarkSource.RecordModuleMarks)) false
        else {
          val componentMarkRecords = components.map(_._2._1)
          componentMarkRecords.forall(_.history.find(_.source != RecordedAssessmentComponentStudentMarkSource.ModuleMarkConfirmation).forall(_.updatedDate.isBefore(mostRecentChange.get.updatedDate)))
        }
      }

      calculation match {
        case ModuleMarkCalculation.Success(mark, grade, result, comments) if shouldUseCalculation =>
          populateMarksItem(mark, grade, result, comments)
          // set the calculate checkbox to true if the calculation is suggesting a change
          s.calculate =
            !student.agreed && (
              (mark.nonEmpty && student.mark.isEmpty) ||
              (grade.nonEmpty && student.grade.isEmpty) ||
              (result.nonEmpty && student.result.isEmpty) ||
              doesntMatchCalculation(s)
            )

        case ModuleMarkCalculation.FailureWithMark(mark, _) if shouldUseCalculation =>
          populateMarksItem(Some(mark), None, None, None)
          s.calculate = !student.agreed && (student.mark.isEmpty || doesntMatchCalculation(s))
        case _ => s.calculate = false // do nothing
      }

      students.put(student.sprCode, s)
    }
  }
}

class ValidModuleRegistrationGradesRequest
  extends ModuleRegistrationValidGradesForMarkRequest

trait CalculateModuleMarksAlgorithm {
  self: AssessmentMembershipServiceComponent =>

  def calculate(moduleRegistration: ModuleRegistration, components: Seq[(AssessmentComponent, (StudentMarkRecord, Option[BigDecimal]))]): ModuleMarkCalculation = {
    require(components.isEmpty || components.forall { case (ac, (s, _)) =>
      ac.moduleCode == components.head._1.moduleCode &&
      s.universityId == components.head._2._1.universityId
    })

    if (components.isEmpty) ModuleMarkCalculation.Failure.NoComponentMarks
    else if (!moduleRegistration.marksCode.hasText) ModuleMarkCalculation.Failure.NoMarkScheme
    else {
      val componentsWithMissingMarkOrGrades = components.filter { case (_, (s, _)) => s.mark.isEmpty && s.grade.isEmpty }.map(_._1).sortBy(_.sequence)

      if (componentsWithMissingMarkOrGrades.nonEmpty) ModuleMarkCalculation.Failure.MarksAndGradesMissingFor(componentsWithMissingMarkOrGrades.map(_.sequence))
      else {
        val componentsForCalculation = components.filter { case (_, (s, _)) => !s.grade.contains(GradeBoundary.ForceMajeureMissingComponentGrade) }
        if (componentsForCalculation.isEmpty && components.forall(_._2._1.grade.contains(GradeBoundary.ForceMajeureMissingComponentGrade))) {
          // No components have been assessed, so it is not possible to grant credit.
          ModuleMarkCalculation.MissingMarkAdjustment.AllComponentsMissing
        } else {
          lazy val calculation = {
            def validGradesForMark(m: Option[Int]) =
              assessmentMembershipService.gradesForMark(moduleRegistration, m)

            // Is this a pass/fail module?
            if (moduleRegistration.passFail) {
              // All the components must also be passFail
              if (componentsForCalculation.exists { case (c, _) => !moduleRegistration.marksCode.maybeText.contains(c.marksCode) }) {
                ModuleMarkCalculation.Failure.PassFail.MismatchedMarkScheme
              } else {
                val validGrades = validGradesForMark(None)

                def passFailResult(grade: String): ModuleMarkCalculation =
                  validGrades.find(_.grade == grade) match {
                    case None => ModuleMarkCalculation.Failure.NoGradeBoundary(grade)
                    case Some(gradeBoundary) => ModuleMarkCalculation.Success.PassFail(gradeBoundary)
                  }

                // Each component must have a grade
                val componentsWithMissingGrades = componentsForCalculation.filter(_._2._1.grade.isEmpty).map(_._1).sortBy(_.sequence)

                if (componentsWithMissingGrades.nonEmpty) ModuleMarkCalculation.Failure.PassFail.GradesMissingFor(componentsWithMissingGrades.map(_.sequence))
                else if (componentsForCalculation.size == 1) {
                  // If there's a single component and it's valid, just copy it over
                  val gb: Option[GradeBoundary] = componentsForCalculation.head._2._1.grade match {
                    case Some(existing) => validGrades.find(_.grade == existing)
                    case _ => validGrades.find(_.isDefault)
                  }

                  gb match {
                    case None => ModuleMarkCalculation.Failure.PassFail.NoDefaultGrade
                    case Some(gradeBoundary) => ModuleMarkCalculation.Success.PassFail(gradeBoundary)
                  }
                } else if (componentsForCalculation.exists(_._2._1.grade.contains("F"))) {
                  // Any fail grades in lead to a fail grade out
                  passFailResult("F")
                } else if (componentsForCalculation.exists(_._2._1.grade.contains("R"))) {
                  // Any resit grades in to a resit grade out
                  passFailResult("R")
                } else if (componentsForCalculation.forall(_._2._1.grade.contains("P"))) {
                  // All passes in leads to a pass out
                  passFailResult("P")
                } else {
                  // ¯\_(ツ)_/¯
                  ModuleMarkCalculation.Failure.PassFail.MismatchedGrades(componentsForCalculation.flatMap(_._2._1.grade))
                }
              }
            } else {
              // Each component must have a mark
              val componentsWithMissingMarks = componentsForCalculation.filter(_._2._1.mark.isEmpty).map(_._1).sortBy(_.sequence)

              if (componentsWithMissingMarks.nonEmpty)
                ModuleMarkCalculation.Failure.MarksMissingFor(componentsWithMissingMarks.map(_.sequence))
              else {
                val componentsWithMissingWeighting = components.filter(_._1.scaledWeighting.isEmpty).map(_._1).sortBy(_.sequence)

                if (componentsWithMissingWeighting.nonEmpty) ModuleMarkCalculation.Failure.WeightingsMissingFor(componentsWithMissingWeighting.map(_.sequence))
                else {
                  // If there are any indicator grades, just fail out unless all the grades are the same
                  def isIndicatorGrade(ac: AssessmentComponent, s: StudentMarkRecord): Boolean = {
                    s.grade.exists { g =>
                      assessmentMembershipService.gradesForMark(ac, s.mark, s.upstreamAssessmentGroupMember.currentResitAttempt)
                        .find(_.grade == g)
                        .exists(!_.isDefault)
                    }
                  }

                  // We know that all weightings are defined and all marks are defined at this point
                  val calculatedMark = {
                    lazy val markCap: Option[Int] = {
                      val maxAttempt = componentsForCalculation.map { _._2._1.upstreamAssessmentGroupMember.currentResitAttempt.getOrElse(1) }.maxOption
                      assessmentMembershipService.passMark(moduleRegistration, maxAttempt)
                    }

                    val totalWeighting = componentsForCalculation.map(_._2._2.get).sum

                    val uncappedMark = componentsForCalculation.map { case (ac, (s, weighting)) =>
                      val mark = s.mark.get
                      if (totalWeighting == 0) BigDecimal(0)
                      else mark * (weighting.get / totalWeighting)
                    }.sum

                    // if any of the components being considered are a resit then cap the module mark
                    if (componentsForCalculation.exists { case (_, (s, _)) => s.isReassessment && !s.furtherFirstSit }) {
                      markCap.map(cap => if (uncappedMark > cap) BigDecimal(cap) else uncappedMark).getOrElse(uncappedMark)
                    } else {
                      uncappedMark
                    }
                  }.setScale(0, BigDecimal.RoundingMode.HALF_UP).toInt
                  val validGrades = validGradesForMark(Some(calculatedMark))

                  val componentsWithIndicatorGrades = componentsForCalculation
                    .filter { case (ac, (s, w)) => isIndicatorGrade(ac, s) && w.exists(_ > 0) }
                    .sortBy(_._1.sequence)

                  if (componentsForCalculation.nonEmpty && componentsWithIndicatorGrades.nonEmpty && componentsWithIndicatorGrades.forall(_._2._1.grade == componentsWithIndicatorGrades.head._2._1.grade)) {
                    val grade = if(componentsWithIndicatorGrades.nonEmpty) {
                      componentsWithIndicatorGrades.head._2._1.grade.get
                    }
                    else {
                      componentsWithIndicatorGrades.head._2._1.grade.get
                    }

                    validGrades.find(_.grade == grade) match {
                      case None => ModuleMarkCalculation.Failure.NoGradeBoundary(calculatedMark, grade)
                      case Some(gradeBoundary) => ModuleMarkCalculation.Success(Some(calculatedMark), Some(gradeBoundary))
                    }
                  } else if (componentsWithIndicatorGrades.nonEmpty) {
                    ModuleMarkCalculation.Failure.MismatchedIndicatorGrades(componentsWithIndicatorGrades.flatMap(_._2._1.grade), componentsWithIndicatorGrades.map(_._1.sequence))
                  } else if (componentsForCalculation.isEmpty) {
                    ModuleMarkCalculation.Failure.General
                  } else {
                    validGrades.find(_.isDefault) match {
                      case None => ModuleMarkCalculation.Success(Some(calculatedMark), None, None)
                      case Some(gradeBoundary) => ModuleMarkCalculation.Success(Some(calculatedMark), Some(gradeBoundary))
                    }
                  }
                }
              }
            }
          }

          if (componentsForCalculation.nonEmpty && components.exists(_._2._1.grade.contains(GradeBoundary.ForceMajeureMissingComponentGrade))) {
            // We have at least one FM component but not all, so we can't do a single calculation. We need to offer multiple options
            ModuleMarkCalculation.MissingMarkAdjustment.SomeComponentsMissing(calculation)
          } else calculation
        }
      }
    }
  }
}

trait CalculateModuleMarksValidation extends ModuleOccurrenceValidation with SelfValidating {
  self: ModuleOccurrenceState
    with ClearRecordedModuleMarksState
    with CalculateModuleMarksRequest
    with CalculateModuleMarksLoadModuleRegistrations
    with AssessmentMembershipServiceComponent
    with SecurityServiceComponent =>

  override def validate(errors: Errors): Unit = {
    val doGradeValidation = module.adminDepartment.assignmentGradeValidation
    students.asScala.filter { case (_, item) => item.calculate }.foreach { case (sprCode, item) =>
      errors.pushNestedPath(s"students[$sprCode]")

      validateMarkEntry(errors)(item, doGradeValidation)

      // TAB-8428 if the mark, grade or result differ from the current mark, grade or result AND they differ from the calculated
      // mark, grade and result, comment becomes mandatory
      if (!item.comments.hasText && doesntMatchCalculation(item)) {
        errors.rejectValue("comments", "moduleMarkCalculation.mismatch.noComment")
      }

      errors.popNestedPath()
    }
  }
}

trait ClearRecordedModuleMarksState {
  def currentUser: CurrentUser
}

object ClearRecordedModuleMarks {
  def shouldClear(moduleRegistration: ModuleRegistration)(moduleRegistrationMarksService: ModuleRegistrationMarksService): Option[StudentModuleMarkRecord] = {
    val existingRecordedModuleRegistration =
      moduleRegistrationMarksService.getRecordedModuleRegistration(moduleRegistration)

    val isNonEmpty =
      moduleRegistration.firstDefinedMark.nonEmpty ||
      moduleRegistration.firstDefinedGrade.nonEmpty ||
      Option(moduleRegistration.moduleResult).nonEmpty ||
      existingRecordedModuleRegistration.exists { recordedModuleRegistration =>
        recordedModuleRegistration.latestMark.nonEmpty ||
        recordedModuleRegistration.latestGrade.nonEmpty ||
        recordedModuleRegistration.latestResult.nonEmpty
      }

    if (isNonEmpty) Some(StudentModuleMarkRecord(moduleRegistration, existingRecordedModuleRegistration, requiresResit = false))
    else None
  }
}

/**
 * A mixin trait for component mark operations which mutate actual or agreed component marks, and therefore
 * mean the module mark/grade needs clearing for recalculation.
 */
trait ClearRecordedModuleMarks {
  self: ClearRecordedModuleMarksState
    with ModuleRegistrationMarksServiceComponent
    with ModuleRegistrationServiceComponent =>

  def clearRecordedModuleMarksFor(recordedAssessmentComponentStudent: RecordedAssessmentComponentStudent): Option[RecordedModuleRegistration] = {
    // There might be multiple module registrations here, for different SPR codes. Just blat them all
    // We can't restrict this by AssessmentGroup because it might be a resit mark by another mechanism
    moduleRegistrationService.getByModuleOccurrence(recordedAssessmentComponentStudent.moduleCode, recordedAssessmentComponentStudent.academicYear, recordedAssessmentComponentStudent.occurrence)
      .filter(mr => Option(mr.studentCourseDetails).exists(_.student.universityId == recordedAssessmentComponentStudent.universityId))
      .filter(ClearRecordedModuleMarks.shouldClear(_)(moduleRegistrationMarksService).nonEmpty)
      .map(moduleRegistrationMarksService.getOrCreateRecordedModuleRegistration)
      .map { recordedModuleRegistration =>
        recordedModuleRegistration.addMark(
          uploader = currentUser.apparentUser,
          mark = None,
          grade = None,
          result = None,
          source = RecordedModuleMarkSource.ComponentMarkChange,

          // This intentionally copies the state from the component mark, as it allows a resit mark
          // being recorded to reset an agreed module mark to be not-agreed.
          markState = recordedAssessmentComponentStudent.latestState.getOrElse(MarkState.UnconfirmedActual),
          comments = "Module mark calculation reset by component mark change",
        )

        moduleRegistrationMarksService.saveOrUpdate(recordedModuleRegistration)

        recordedModuleRegistration
      }
      .headOption
  }
}
