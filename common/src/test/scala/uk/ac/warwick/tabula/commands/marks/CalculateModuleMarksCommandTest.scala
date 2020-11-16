package uk.ac.warwick.tabula.commands.marks

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.marks.CalculateModuleMarksCommand.ModuleMarkCalculation
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.data.model.MarkState.UnconfirmedActual
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, AssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class CalculateModuleMarksCommandTest extends TestBase with Mockito {

  private trait Fixture {
    val algorithm = new CalculateModuleMarksAlgorithm with AssessmentMembershipServiceComponent {
      override val assessmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]
    }

    val student = Fixtures.student()
    val studentCourseDetails = student.mostSignificantCourse

    val module = Fixtures.module("in101")
    val cats = BigDecimal(15)
    val academicYear = AcademicYear.starting(2019)
    val occurrence = "A"

    def markRecord(mark: Option[Int], grade: Option[String], resit: Boolean = false): StudentMarkRecord = {
      val uagm = new UpstreamAssessmentGroupMember
      if (resit) uagm.currentResitAttempt = Some(1)

      StudentMarkRecord(
        universityId = student.universityId,
        resitSequence = None,
        position = None,
        currentMember = true,
        isReassessment = resit,
        currentResitAttempt = if(resit) Some(2) else None,
        requiresResit = false,
        incrementsAttempt = false,
        existingResit = None,
        mark = mark,
        grade = grade,
        needsWritingToSits = false,
        outOfSync = false,
        sitsWriteError = None,
        markState = Some(UnconfirmedActual),
        agreed = false,
        recordedStudent = None,
        history = Seq.empty,
        upstreamAssessmentGroupMember = uagm,
      )
    }
  }

  @Test def calculateNoComponents(): Unit = new Fixture {
    val modReg = Fixtures.moduleRegistration(studentCourseDetails, module, JBigDecimal(Some(cats)), academicYear, occurrence)

    algorithm.calculate(modReg, Seq.empty) should be (ModuleMarkCalculation.Failure.NoComponentMarks)
  }

  @Test def calculateNoMarkScheme(): Unit = new Fixture {
    val modReg = Fixtures.moduleRegistration(studentCourseDetails, module, JBigDecimal(Some(cats)), academicYear, occurrence)
    val ac = Fixtures.assessmentComponent(module, 1)
    val smr = markRecord(Some(70), Some("1"))

    algorithm.calculate(modReg, Seq(ac -> (smr, None))) should be (ModuleMarkCalculation.Failure.NoMarkScheme)
  }

  private trait PassFailModuleFixture extends Fixture {
    val modReg = Fixtures.moduleRegistration(studentCourseDetails, module, JBigDecimal(Some(cats)), academicYear, occurrence, marksCode = "PF")
    modReg.membershipService = algorithm.assessmentMembershipService

    // This is just for getting currentResitAttempt so it's fine to just return empty
    modReg.membershipService.getUpstreamAssessmentGroups(modReg, allAssessmentGroups = true, eagerLoad = true) returns Seq.empty

    algorithm.assessmentMembershipService.gradesForMark(isEq(modReg), any[Option[Int]]) answers { args: Array[AnyRef] =>
      val process = if (modReg.currentResitAttempt.nonEmpty) GradeBoundaryProcess.Reassessment else GradeBoundaryProcess.StudentAssessment
      val attempt = modReg.currentResitAttempt.getOrElse(1)

      args(1).asInstanceOf[Option[Int]] match {
        case Some(_) => Seq.empty
        case _ => Seq(
          Fixtures.gradeBoundary("PF", process = process, attempt = attempt, rank = 1, grade = "P", result = Some(ModuleResult.Pass)),
          Fixtures.gradeBoundary("PF", process = process, attempt = attempt, rank = 2, grade = "F", result = Some(ModuleResult.Fail)),
          Fixtures.gradeBoundary("PF", process = process, attempt = attempt, rank = 3, grade = GradeBoundary.WithdrawnGrade, signalStatus = GradeBoundarySignalStatus.SpecificOutcome, result = Some(ModuleResult.Fail)),
          Fixtures.gradeBoundary("PF", process = process, attempt = attempt, rank = 4, grade = "R", signalStatus = GradeBoundarySignalStatus.SpecificOutcome, result = Some(ModuleResult.Fail), agreedStatus = GradeBoundaryAgreedStatus.Reassessment, incrementsAttempt = true),
          Fixtures.gradeBoundary("PF", process = process, attempt = attempt, rank = 5, grade = GradeBoundary.MitigatingCircumstancesGrade, signalStatus = GradeBoundarySignalStatus.SpecificOutcome, result = Some(ModuleResult.Deferred), agreedStatus = GradeBoundaryAgreedStatus.Held),
          Fixtures.gradeBoundary("PF", process = process, attempt = attempt, rank = 1000, GradeBoundary.ForceMajeureMissingComponentGrade, signalStatus = GradeBoundarySignalStatus.SpecificOutcome, result = None),
        )
      }
    }
  }

  @Test def calculateMissingMarksAndGrades(): Unit = new PassFailModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1)
    val smr1 = markRecord(Some(70), Some("1"))

    val ac2 = Fixtures.assessmentComponent(module, 2)
    val smr2 = markRecord(None, None)

    algorithm.calculate(modReg, Seq(ac1 -> (smr1, None), ac2 -> (smr2, None))) should be (ModuleMarkCalculation.Failure.MarksAndGradesMissingFor(Seq("A02")))
  }

  @Test def calculateMMA(): Unit = new PassFailModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1)
    val smr1 = markRecord(None, Some(GradeBoundary.ForceMajeureMissingComponentGrade))

    val ac2 = Fixtures.assessmentComponent(module, 2)
    val smr2 = markRecord(None, Some(GradeBoundary.ForceMajeureMissingComponentGrade))

    // No mark, FM grade, no result https://warwick.slack.com/archives/CTF8JH60L/p1590667231178600
    algorithm.calculate(modReg, Seq(ac1 -> (smr1, None), ac2 -> (smr2, None))) should be (ModuleMarkCalculation.MissingMarkAdjustment.AllComponentsMissing)
  }

  @Test def calculatePFMismatchedMarksCode(): Unit = new PassFailModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "PF")
    val smr1 = markRecord(None, Some("P"))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "WAR")
    val smr2 = markRecord(Some(70), Some("1"))

    algorithm.calculate(modReg, Seq(ac1 -> (smr1, None), ac2 -> (smr2, None))) should be (ModuleMarkCalculation.Failure.PassFail.MismatchedMarkScheme)
  }

  @Test def calculatePFMissingGrades(): Unit = new PassFailModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "PF")
    val smr1 = markRecord(None, Some("P"))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "PF")
    val smr2 = markRecord(Some(0), None) // Mark set just to check this branch, otherwise would get caught by calculateMissingMarksAndGrades()

    algorithm.calculate(modReg, Seq(ac1 -> (smr1, None), ac2 -> (smr2, None))) should be (ModuleMarkCalculation.Failure.PassFail.GradesMissingFor(Seq("A02")))
  }

  @Test def calculatePFSingleComponentMissingGradeBoundary(): Unit = new PassFailModuleFixture {
    val ac = Fixtures.assessmentComponent(module, 1, marksCode = "PF")
    val smr = markRecord(None, Some("??"))

    algorithm.calculate(modReg, Seq(ac -> (smr, None))) should be (ModuleMarkCalculation.Failure.PassFail.NoDefaultGrade)
  }

  @Test def calculatePFSingleComponent(): Unit = new PassFailModuleFixture {
    val ac = Fixtures.assessmentComponent(module, 1, marksCode = "PF")
    val smr = markRecord(None, Some("P"))

    algorithm.calculate(modReg, Seq(ac -> (smr, None))) should be (ModuleMarkCalculation.Success(None, Some("P"), Some(ModuleResult.Pass)))
  }

  @Test def calculatePFAnyFailed(): Unit = new PassFailModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "PF")
    val smr1 = markRecord(None, Some("P"))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "PF")
    val smr2 = markRecord(None, Some("F"))

    algorithm.calculate(modReg, Seq(ac1 -> (smr1, None), ac2 -> (smr2, None))) should be (ModuleMarkCalculation.Success(None, Some("F"), Some(ModuleResult.Fail)))
  }

  @Test def calculatePFAnyResit(): Unit = new PassFailModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "PF")
    val smr1 = markRecord(None, Some("P"))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "PF")
    val smr2 = markRecord(None, Some("R"))

    algorithm.calculate(modReg, Seq(ac1 -> (smr1, None), ac2 -> (smr2, None))) should be (ModuleMarkCalculation.Success(None, Some("R"), Some(ModuleResult.Fail)))
  }

  @Test def calculatePFAllPassed(): Unit = new PassFailModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "PF")
    val smr1 = markRecord(None, Some("P"))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "PF")
    val smr2 = markRecord(None, Some("P"))

    algorithm.calculate(modReg, Seq(ac1 -> (smr1, None), ac2 -> (smr2, None))) should be (ModuleMarkCalculation.Success(None, Some("P"), Some(ModuleResult.Pass)))
  }

  @Test def calculatePFMixedResults(): Unit = new PassFailModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "PF")
    val smr1 = markRecord(None, Some("P"))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "PF")
    val smr2 = markRecord(None, Some(GradeBoundary.WithdrawnGrade))

    algorithm.calculate(modReg, Seq(ac1 -> (smr1, None), ac2 -> (smr2, None))) should be (ModuleMarkCalculation.Failure.PassFail.MismatchedGrades(Seq("P", "W")))
  }

  @Test def calculatePFPartialMMA(): Unit = new PassFailModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "PF")
    val smr1 = markRecord(None, Some("P"))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "PF")
    val smr2 = markRecord(None, Some(GradeBoundary.ForceMajeureMissingComponentGrade))

    // We compare .toString because TemplateHTMLOutput isn't .equals() for the same HTML
    algorithm.calculate(modReg, Seq(ac1 -> (smr1, None), ac2 -> (smr2, None))).toString should be (ModuleMarkCalculation.MissingMarkAdjustment.SomeComponentsMissing(ModuleMarkCalculation.Success(None, Some("P"), Some(ModuleResult.Pass), Some("Missing mark adjustment - learning outcomes assessed, weighted mark"))).toString)
  }

  private trait UGModuleFixture extends Fixture {
    val modReg = Fixtures.moduleRegistration(studentCourseDetails, module, JBigDecimal(Some(cats)), academicYear, occurrence, marksCode = "WMR")
    modReg.membershipService = algorithm.assessmentMembershipService

    // This is just for getting currentResitAttempt so it's fine to just return empty
    modReg.membershipService.getUpstreamAssessmentGroups(modReg, allAssessmentGroups = true, eagerLoad = true) returns Seq.empty

    algorithm.assessmentMembershipService.gradesForMark(isEq(modReg), any[Option[Int]]) answers { args: Array[AnyRef] =>
      val process = if (modReg.currentResitAttempt.nonEmpty) GradeBoundaryProcess.Reassessment else GradeBoundaryProcess.StudentAssessment
      val attempt = modReg.currentResitAttempt.getOrElse(1)

      val gradeBoundaries = Seq(
        Fixtures.gradeBoundary("WMR", process = process, attempt = attempt, rank = 1, grade = "1", minimumMark = Some(70), maximumMark = Some(100), result = Some(ModuleResult.Pass)),
        Fixtures.gradeBoundary("WMR", process = process, attempt = attempt, rank = 2, grade = "21", minimumMark = Some(60), maximumMark = Some(69), result = Some(ModuleResult.Pass)),
        Fixtures.gradeBoundary("WMR", process = process, attempt = attempt, rank = 3, grade = "22", minimumMark = Some(50), maximumMark = Some(59), result = Some(ModuleResult.Pass)),
        Fixtures.gradeBoundary("WMR", process = process, attempt = attempt, rank = 4, grade = "3", minimumMark = Some(40), maximumMark = Some(49), result = Some(ModuleResult.Pass)),
        Fixtures.gradeBoundary("WMR", process = process, attempt = attempt, rank = 5, grade = "F", minimumMark = Some(0), maximumMark = Some(39), result = Some(ModuleResult.Fail)),
        Fixtures.gradeBoundary("WMR", process = process, attempt = attempt, rank = 10, grade = GradeBoundary.WithdrawnGrade, minimumMark = Some(0), maximumMark = Some(100), signalStatus = GradeBoundarySignalStatus.SpecificOutcome, result = Some(ModuleResult.Fail)),
        Fixtures.gradeBoundary("WMR", process = process, attempt = attempt, rank = 20, grade = "R", minimumMark = Some(0), maximumMark = Some(100), signalStatus = GradeBoundarySignalStatus.SpecificOutcome, result = Some(ModuleResult.Deferred), agreedStatus = GradeBoundaryAgreedStatus.Reassessment, incrementsAttempt = true),
        Fixtures.gradeBoundary("WMR", process = process, attempt = attempt, rank = 50, grade = GradeBoundary.MitigatingCircumstancesGrade, minimumMark = Some(0), maximumMark = Some(100), signalStatus = GradeBoundarySignalStatus.SpecificOutcome, result = Some(ModuleResult.Deferred), agreedStatus = GradeBoundaryAgreedStatus.Held),
        Fixtures.gradeBoundary("WMR", process = process, attempt = attempt, rank = 1000, grade = GradeBoundary.ForceMajeureMissingComponentGrade, signalStatus = GradeBoundarySignalStatus.SpecificOutcome),
      )

      gradeBoundaries.filter(_.isValidForMark(args(1).asInstanceOf[Option[Int]]))
    }

    algorithm.assessmentMembershipService.gradesForMark(any[AssessmentComponent], any[Option[Int]], any[Option[Int]]) answers { args: Array[AnyRef] =>
      args(0).asInstanceOf[AssessmentComponent].marksCode match {
        case "WAR" =>
          val resitAttempt = args(2).asInstanceOf[Option[Int]]
          val process = if (resitAttempt.nonEmpty) GradeBoundaryProcess.Reassessment else GradeBoundaryProcess.StudentAssessment
          val attempt = resitAttempt.getOrElse(1)

          val gradeBoundaries = Seq(
            Fixtures.gradeBoundary("WAR", process = process, attempt = attempt, rank = 1, grade = "1", minimumMark = Some(70), maximumMark = Some(100)),
            Fixtures.gradeBoundary("WAR", process = process, attempt = attempt, rank = 2, grade = "21", minimumMark = Some(60), maximumMark = Some(69)),
            Fixtures.gradeBoundary("WAR", process = process, attempt = attempt, rank = 3, grade = "22", minimumMark = Some(50), maximumMark = Some(59)),
            Fixtures.gradeBoundary("WAR", process = process, attempt = attempt, rank = 4, grade = "3", minimumMark = Some(40), maximumMark = Some(49)),
            Fixtures.gradeBoundary("WAR", process = process, attempt = attempt, rank = 5, grade = "F", minimumMark = Some(0), maximumMark = Some(39)),
            Fixtures.gradeBoundary("WAR", process = process, attempt = attempt, rank = 10, grade = GradeBoundary.WithdrawnGrade, minimumMark = Some(0), maximumMark = Some(100), signalStatus = GradeBoundarySignalStatus.SpecificOutcome),
            Fixtures.gradeBoundary("WAR", process = process, attempt = attempt, rank = 20, grade = "R", minimumMark = Some(0), maximumMark = Some(100), signalStatus = GradeBoundarySignalStatus.SpecificOutcome),
            Fixtures.gradeBoundary("WAR", process = process, attempt = attempt, rank = 30, grade = "AB", minimumMark = Some(0), maximumMark = Some(100), signalStatus = GradeBoundarySignalStatus.SpecificOutcome),
            Fixtures.gradeBoundary("WAR", process = process, attempt = attempt, rank = 50, grade = GradeBoundary.MitigatingCircumstancesGrade, minimumMark = Some(0), maximumMark = Some(100), signalStatus = GradeBoundarySignalStatus.SpecificOutcome),
            Fixtures.gradeBoundary("WAR", process = process, attempt = attempt, rank = 1000, grade = GradeBoundary.ForceMajeureMissingComponentGrade, signalStatus = GradeBoundarySignalStatus.SpecificOutcome),
          )

          gradeBoundaries.filter(_.isValidForMark(args(1).asInstanceOf[Option[Int]]))

        case _ => Seq.empty
      }
    }
  }

  @Test def calculateMissingMarks(): Unit = new UGModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "WAR")
    val smr1 = markRecord(Some(0), Some(GradeBoundary.WithdrawnGrade))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "WAR")
    val smr2 = markRecord(None, Some(GradeBoundary.WithdrawnGrade)) // Mark is required for W

    algorithm.calculate(modReg, Seq(ac1 -> (smr1, None), ac2 -> (smr2, None))) should be (ModuleMarkCalculation.Failure.MarksMissingFor(Seq("A02")))
  }

  @Test def calculateMissingWeightings(): Unit = new UGModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "WAR", weighting = 30)
    ac1.membershipService = algorithm.assessmentMembershipService
    val smr1 = markRecord(Some(72), Some("1"))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "WAR")
    ac2.membershipService = algorithm.assessmentMembershipService
    ac2.rawWeighting = null
    val smr2 = markRecord(Some(56), Some("22"))

    algorithm.assessmentMembershipService.getVariableAssessmentWeightingRules("IN101-30", occurrence) returns Seq.empty
    algorithm.assessmentMembershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns Seq(ac1, ac2)

    algorithm.calculate(modReg, Seq(ac1 -> (smr1, ac1.scaledWeighting), ac2 -> (smr2, ac2.scaledWeighting))) should be (ModuleMarkCalculation.Failure.WeightingsMissingFor(Seq("A02")))
  }

  @Test def calculateIndicatorGradesMatching(): Unit = new UGModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "WAR", weighting = 30)
    ac1.membershipService = algorithm.assessmentMembershipService
    val smr1 = markRecord(Some(0), Some(GradeBoundary.WithdrawnGrade))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "WAR", weighting = 70)
    ac2.membershipService = algorithm.assessmentMembershipService
    val smr2 = markRecord(Some(0), Some(GradeBoundary.WithdrawnGrade))

    algorithm.assessmentMembershipService.getVariableAssessmentWeightingRules("IN101-30", occurrence) returns Seq.empty
    algorithm.assessmentMembershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns Seq(ac1, ac2)

    algorithm.calculate(modReg, Seq(ac1 -> (smr1, ac1.scaledWeighting), ac2 -> (smr2, ac2.scaledWeighting))) should be (ModuleMarkCalculation.Success(Some(0), Some(GradeBoundary.WithdrawnGrade), Some(ModuleResult.Fail)))
  }

  @Test def calculateIndicatorGradesMismatch(): Unit = new UGModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "WAR", weighting = 30)
    ac1.membershipService = algorithm.assessmentMembershipService
    val smr1 = markRecord(Some(0), Some(GradeBoundary.WithdrawnGrade))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "WAR", weighting = 70)
    ac2.membershipService = algorithm.assessmentMembershipService
    val smr2 = markRecord(Some(17), Some("R"))

    algorithm.assessmentMembershipService.getVariableAssessmentWeightingRules("IN101-30", occurrence) returns Seq.empty
    algorithm.assessmentMembershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns Seq(ac1, ac2)

    algorithm.calculate(modReg, Seq(ac1 -> (smr1, ac1.scaledWeighting), ac2 -> (smr2, ac2.scaledWeighting))) should be (ModuleMarkCalculation.Failure.MismatchedIndicatorGrades(Seq("W", "R"), Seq("A01", "A02")))
  }

  @Test def calculateMissingGradeBoundary(): Unit = new UGModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "WAR", weighting = 30)
    ac1.membershipService = algorithm.assessmentMembershipService
    val smr1 = markRecord(Some(0), Some("AB"))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "WAR", weighting = 70)
    ac2.membershipService = algorithm.assessmentMembershipService
    val smr2 = markRecord(Some(17), Some("AB"))

    algorithm.assessmentMembershipService.getVariableAssessmentWeightingRules("IN101-30", occurrence) returns Seq.empty
    algorithm.assessmentMembershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns Seq(ac1, ac2)

    algorithm.calculate(modReg, Seq(ac1 -> (smr1, ac1.scaledWeighting), ac2 -> (smr2, ac2.scaledWeighting))) should be (ModuleMarkCalculation.Failure.NoGradeBoundary(12, "AB"))
  }

  @Test def calculatePartialMMA(): Unit = new UGModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "WAR", weighting = 30)
    ac1.membershipService = algorithm.assessmentMembershipService
    val smr1 = markRecord(Some(65), Some("21"))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "WAR", weighting = 70)
    ac2.membershipService = algorithm.assessmentMembershipService
    val smr2 = markRecord(None, Some(GradeBoundary.ForceMajeureMissingComponentGrade))

    algorithm.assessmentMembershipService.getVariableAssessmentWeightingRules("IN101-30", occurrence) returns Seq.empty
    algorithm.assessmentMembershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns Seq(ac1, ac2)

    // Non-MMA components only are used to calculate
    // We compare .toString because TemplateHTMLOutput isn't .equals() for the same HTML
    algorithm.calculate(modReg, Seq(ac1 -> (smr1, ac1.scaledWeighting), ac2 -> (smr2, ac2.scaledWeighting))).toString should be (ModuleMarkCalculation.MissingMarkAdjustment.SomeComponentsMissing(ModuleMarkCalculation.Success(Some(65), Some("21"), Some(ModuleResult.Pass), Some("Missing mark adjustment - learning outcomes assessed, weighted mark"))).toString)
  }

  @Test def calculatePartialMitCircs(): Unit = new UGModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "WAR", weighting = 30)
    ac1.membershipService = algorithm.assessmentMembershipService
    val smr1 = markRecord(Some(65), Some("21"))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "WAR", weighting = 70)
    ac2.membershipService = algorithm.assessmentMembershipService
    val smr2 = markRecord(Some(0), Some(GradeBoundary.MitigatingCircumstancesGrade))

    algorithm.assessmentMembershipService.getVariableAssessmentWeightingRules("IN101-30", occurrence) returns Seq.empty
    algorithm.assessmentMembershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns Seq(ac1, ac2)

    // TAB-8489 shouldn't be special cased
    algorithm.calculate(modReg, Seq(ac1 -> (smr1, ac1.scaledWeighting), ac2 -> (smr2, ac2.scaledWeighting))) should be (ModuleMarkCalculation.Success(Some(20), Some(GradeBoundary.MitigatingCircumstancesGrade), Some(ModuleResult.Deferred), None))
  }

  @Test def calculateMitCircsAndMMA(): Unit = new UGModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "WAR", weighting = 30)
    ac1.membershipService = algorithm.assessmentMembershipService
    val smr1 = markRecord(Some(65), Some(GradeBoundary.MitigatingCircumstancesGrade))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "WAR", weighting = 70)
    ac2.membershipService = algorithm.assessmentMembershipService
    val smr2 = markRecord(None, Some(GradeBoundary.ForceMajeureMissingComponentGrade))

    algorithm.assessmentMembershipService.getVariableAssessmentWeightingRules("IN101-30", occurrence) returns Seq.empty
    algorithm.assessmentMembershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns Seq(ac1, ac2)

    // Copies the M component over and excludes the FM one
    // We compare .toString because TemplateHTMLOutput isn't .equals() for the same HTML
    algorithm.calculate(modReg, Seq(ac1 -> (smr1, ac1.scaledWeighting), ac2 -> (smr2, ac2.scaledWeighting))).toString should be (ModuleMarkCalculation.MissingMarkAdjustment.SomeComponentsMissing(ModuleMarkCalculation.Success(Some(65), Some(GradeBoundary.MitigatingCircumstancesGrade), Some(ModuleResult.Deferred), Some("Missing mark adjustment - learning outcomes assessed, weighted mark"))).toString)
  }

  @Test def calculatePartialMMAMultipleComponents(): Unit = new UGModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "WAR", weighting = 10)
    ac1.membershipService = algorithm.assessmentMembershipService
    val smr1 = markRecord(Some(64), Some("21"))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "WAR", weighting = 20)
    ac2.membershipService = algorithm.assessmentMembershipService
    val smr2 = markRecord(Some(71), Some("1"))

    val ac3 = Fixtures.assessmentComponent(module, 3, marksCode = "WAR", weighting = 70)
    ac3.membershipService = algorithm.assessmentMembershipService
    val smr3 = markRecord(None, Some(GradeBoundary.ForceMajeureMissingComponentGrade))

    algorithm.assessmentMembershipService.getVariableAssessmentWeightingRules("IN101-30", occurrence) returns Seq.empty
    algorithm.assessmentMembershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns Seq(ac1, ac2)

    // Non-MMA components only are used to calculate. (64 * (10/30)) + (71 * (20/30)) = 68.67
    algorithm.calculate(modReg, Seq(ac1 -> (smr1, ac1.scaledWeighting), ac2 -> (smr2, ac2.scaledWeighting))) should be (ModuleMarkCalculation.Success(Some(69), Some("21"), Some(ModuleResult.Pass)))
  }

  @Test def calculatePartialMitCircsMultipleComponents(): Unit = new UGModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "WAR", weighting = 10)
    ac1.membershipService = algorithm.assessmentMembershipService
    val smr1 = markRecord(Some(64), Some("21"))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "WAR", weighting = 20)
    ac2.membershipService = algorithm.assessmentMembershipService
    val smr2 = markRecord(Some(71), Some("1"))

    val ac3 = Fixtures.assessmentComponent(module, 3, marksCode = "WAR", weighting = 70)
    ac3.membershipService = algorithm.assessmentMembershipService
    val smr3 = markRecord(Some(30), Some(GradeBoundary.MitigatingCircumstancesGrade))

    algorithm.assessmentMembershipService.getVariableAssessmentWeightingRules("IN101-30", occurrence) returns Seq.empty
    algorithm.assessmentMembershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns Seq(ac1, ac2)

    // Calculate and suggest M as the module grade
    algorithm.calculate(modReg, Seq(ac1 -> (smr1, ac1.scaledWeighting), ac2 -> (smr2, ac2.scaledWeighting), ac3 -> (smr3, ac3.scaledWeighting))) should be (ModuleMarkCalculation.Success(Some(42), Some(GradeBoundary.MitigatingCircumstancesGrade), Some(ModuleResult.Deferred),None))
  }

  @Test def calculatePartialMMAAndMitCircsMultipleComponents(): Unit = new UGModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "WAR", weighting = 10)
    ac1.membershipService = algorithm.assessmentMembershipService
    val smr1 = markRecord(Some(64), Some(GradeBoundary.MitigatingCircumstancesGrade))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "WAR", weighting = 20)
    ac2.membershipService = algorithm.assessmentMembershipService
    val smr2 = markRecord(Some(71), Some("1"))

    val ac3 = Fixtures.assessmentComponent(module, 3, marksCode = "WAR", weighting = 70)
    ac3.membershipService = algorithm.assessmentMembershipService
    val smr3 = markRecord(None, Some(GradeBoundary.MitigatingCircumstancesGrade))

    algorithm.assessmentMembershipService.getVariableAssessmentWeightingRules("IN101-30", occurrence) returns Seq.empty
    algorithm.assessmentMembershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns Seq(ac1, ac2)

    // Mit circs are not special cased
    algorithm.calculate(modReg, Seq(ac1 -> (smr1, ac1.scaledWeighting), ac2 -> (smr2, ac2.scaledWeighting))) should be (ModuleMarkCalculation.Success(Some(69), Some(GradeBoundary.MitigatingCircumstancesGrade), Some(ModuleResult.Deferred),None))
  }

  @Test def calculate(): Unit = new UGModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "WAR", weighting = 30)
    ac1.membershipService = algorithm.assessmentMembershipService
    val smr1 = markRecord(Some(64), Some("21"))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "WAR", weighting = 70)
    ac2.membershipService = algorithm.assessmentMembershipService
    val smr2 = markRecord(Some(71), Some("1"))

    algorithm.assessmentMembershipService.getVariableAssessmentWeightingRules("IN101-30", occurrence) returns Seq.empty
    algorithm.assessmentMembershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns Seq(ac1, ac2)

    // (64 * (30/100)) + (71 * (70/100)) = 68.9
    algorithm.calculate(modReg, Seq(ac1 -> (smr1, ac1.scaledWeighting), ac2 -> (smr2, ac2.scaledWeighting))) should be (ModuleMarkCalculation.Success(Some(69), Some("21"), Some(ModuleResult.Pass)))
  }

  @Test def calculateScaledWeightings(): Unit = new UGModuleFixture {
    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "WAR", weighting = 125)
    ac1.membershipService = algorithm.assessmentMembershipService
    val smr1 = markRecord(Some(64), Some("21"))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "WAR", weighting = 875)
    ac2.membershipService = algorithm.assessmentMembershipService
    val smr2 = markRecord(Some(71), Some("1"))

    algorithm.assessmentMembershipService.getVariableAssessmentWeightingRules("IN101-30", occurrence) returns Seq.empty
    algorithm.assessmentMembershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns Seq(ac1, ac2)

    // (64 * (125/1000)) + (71 * (875/1000)) = 70.125
    algorithm.calculate(modReg, Seq(ac1 -> (smr1, ac1.scaledWeighting), ac2 -> (smr2, ac2.scaledWeighting))) should be (ModuleMarkCalculation.Success(Some(70), Some("1"), Some(ModuleResult.Pass)))
  }

  @Test def calculateVariableWeightings(): Unit = new UGModuleFixture {
    // Weightings are (30, 30, 70) because only the higher marked essay will count
    val (a01, a02, e01) = (
      Fixtures.assessmentComponent(module, 1, weighting = 30),
      Fixtures.assessmentComponent(module, 2, weighting = 30),
      Fixtures.assessmentComponent(module, 1, AssessmentType.SummerExam, weighting = 70),
    )

    val components = Seq(a01, a02, e01)
    components.foreach(_.membershipService = algorithm.assessmentMembershipService)

    val rules = Seq(
      Fixtures.variableAssessmentWeightingRule(module, 1, weighting = 30),
      Fixtures.variableAssessmentWeightingRule(module, 2, weighting = 0),
      Fixtures.variableAssessmentWeightingRule(module, 3, assessmentType = AssessmentType.SummerExam, weighting = 70),
    )

    algorithm.assessmentMembershipService.getVariableAssessmentWeightingRules("IN101-30", "A") returns rules
    algorithm.assessmentMembershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns components

    val marksA01 = markRecord(Some(38), Some("F"))
    val marksA02 = markRecord(Some(55), Some("22"))
    val marksE01 = markRecord(Some(49), Some("3"))

    val marks: Seq[(AssessmentType, String, Option[Int])] = Seq(
      (a01.assessmentType, a01.sequence, marksA01.mark),
      (a02.assessmentType, a02.sequence, marksA02.mark),
      (e01.assessmentType, e01.sequence, marksE01.mark)
    )

    // (55 * (30/100)) + (49 * (70/100)) = 50.8
    algorithm.calculate(modReg, Seq(a01 -> (marksA01, a01.weightingFor(marks).get), a02 -> (marksA02, a02.weightingFor(marks).get), e01 -> (marksE01, e01.weightingFor(marks).get))) should be (ModuleMarkCalculation.Success(Some(51), Some("22"), Some(ModuleResult.Pass)))
  }

  @Test def calculateScaledVariableWeightingsWithPartialMMA(): Unit = new UGModuleFixture {
    // Weightings are (30, 30, 30, 40) because only the higher marked essay will count of the first two (with scaling)
    val (a01, a02, a03, e01) = (
      Fixtures.assessmentComponent(module, 1, weighting = 30),
      Fixtures.assessmentComponent(module, 2, weighting = 30),
      Fixtures.assessmentComponent(module, 3, AssessmentType.Performance, weighting = 30),
      Fixtures.assessmentComponent(module, 1, AssessmentType.SummerExam, weighting = 40),
    )

    val components = Seq(a01, a02, a03, e01)
    components.foreach(_.membershipService = algorithm.assessmentMembershipService)

    val rules = Seq(
      Fixtures.variableAssessmentWeightingRule(module, 1, weighting = 175),
      Fixtures.variableAssessmentWeightingRule(module, 2, weighting = 125),
      Fixtures.variableAssessmentWeightingRule(module, 3, assessmentType = AssessmentType.Performance, weighting = 300),
      Fixtures.variableAssessmentWeightingRule(module, 4, assessmentType = AssessmentType.SummerExam, weighting = 400),
    )

    algorithm.assessmentMembershipService.getVariableAssessmentWeightingRules("IN101-30", "A") returns rules
    algorithm.assessmentMembershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns components

    // Second essay missed due to strike action; exam missed due to COVID-19
    val marksA01 = markRecord(Some(38), Some("F"))
    val marksA02 = markRecord(None, Some(GradeBoundary.ForceMajeureMissingComponentGrade))
    val marksA03 = markRecord(Some(55), Some("22"))
    val marksE01 = markRecord(None, Some(GradeBoundary.ForceMajeureMissingComponentGrade))

    val marks: Seq[(AssessmentType, String, Option[Int])] = Seq(
      (a01.assessmentType, a01.sequence, marksA01.mark),
      (a02.assessmentType, a02.sequence, marksA02.mark),
      (a03.assessmentType, a03.sequence, marksA03.mark),
      (e01.assessmentType, e01.sequence, marksE01.mark)
    )

    // Total weighting of considered components (after VAW) is 175 + 300 = 475
    // (38 * (175/475)) + (55 * (300/475)) = 48.74
    // We compare .toString because TemplateHTMLOutput isn't .equals() for the same HTML
    algorithm.calculate(modReg, Seq(a01 -> (marksA01, a01.weightingFor(marks).get), a02 -> (marksA02, a02.weightingFor(marks).get), a03 -> (marksA03, a03.weightingFor(marks).get), e01 -> (marksE01, e01.weightingFor(marks).get))).toString should be (ModuleMarkCalculation.MissingMarkAdjustment.SomeComponentsMissing(ModuleMarkCalculation.Success(Some(49), Some("3"), Some(ModuleResult.Pass), Some("Missing mark adjustment - learning outcomes assessed, weighted mark"))).toString)
  }

  @Test def calculateResitCap(): Unit = new UGModuleFixture {

    val ac1 = Fixtures.assessmentComponent(module, 1, marksCode = "WAR", weighting = 30)
    ac1.membershipService = algorithm.assessmentMembershipService
    val smr1 = markRecord(Some(64), Some("21"))

    val ac2 = Fixtures.assessmentComponent(module, 2, marksCode = "WAR", weighting = 70)
    ac2.membershipService = algorithm.assessmentMembershipService
    val smr2 = markRecord(Some(71), Some("1"), resit = true)

    algorithm.assessmentMembershipService.passMark(any[ModuleRegistration], any[Option[Int]]) returns Some(40)
    algorithm.assessmentMembershipService.getVariableAssessmentWeightingRules("IN101-30", occurrence) returns Seq.empty
    algorithm.assessmentMembershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns Seq(ac1, ac2)

    // (64 * (30/100)) + (40 * (70/100)) = 47.2
    algorithm.calculate(modReg, Seq(ac1 -> (smr1, ac1.scaledWeighting), ac2 -> (smr2, ac2.scaledWeighting))) should be (ModuleMarkCalculation.Success(Some(40), Some("3"), Some(ModuleResult.Pass)))
  }

}
