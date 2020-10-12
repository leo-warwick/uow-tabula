package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntityYear
import uk.ac.warwick.tabula.data.model.CourseType.{PGT, UG}
import uk.ac.warwick.tabula.data.model.DegreeType.{Postgraduate, Undergraduate}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.grids.columns.ExamGridYearMarksToUse

import scala.collection.mutable
import scala.math.BigDecimal.RoundingMode

sealed abstract class ProgressionResult(val description: String)

object ProgressionResult {

  case object Proceed extends ProgressionResult("Proceed")

  case object PossiblyProceed extends ProgressionResult("Proceed(?)")

  case object Resit extends ProgressionResult("Resit")

  case object Pass extends ProgressionResult("Pass")

  case class Unknown(details: String) extends ProgressionResult("?")

}

sealed abstract class FinalYearGrade(val description: String, val lowerBound: BigDecimal, val upperBound: BigDecimal, val details: Option[String] = None) {
  def this(description: String, min: Double, max: Double) = {
    this(description, FinalYearGrade.toBigDecimal(min), FinalYearGrade.toBigDecimal(max), None)
  }

  def fail: Boolean = description == "Fail"

  def applies(mark: BigDecimal): Boolean = {
    mark <= upperBound && mark >= lowerBound
  }

  def withMark(mark: BigDecimal, details: Option[String] = None): FinalYearMark = FinalYearMark(mark, description, lowerBound, upperBound, details)

  override def equals(other: Any): Boolean = other match {
    case other: FinalYearGrade => description == other.description
    case _ => false
  }
}

case class FinalYearMark(mark: BigDecimal, override val description: String, override val lowerBound: BigDecimal, override val upperBound: BigDecimal, override val details: Option[String])
  extends FinalYearGrade(description, lowerBound, upperBound, details)

object FinalYearGrade {
  def toBigDecimal(d: Double): BigDecimal = BigDecimal(d).setScale(1, RoundingMode.HALF_UP)

  object Undergraduate {
    val all: Seq[FinalYearGrade] = Seq(
      First, FirstBorderline, UpperSecond, UpperSecondBorderline, LowerSecond, LowerSecondBorderline,
      Third, ThirdBorderline, Pass, PassBorderline, Fail
    )

    case object First extends FinalYearGrade("1", min = 71, max = 200)

    case object FirstBorderline extends FinalYearGrade("1 (b)", min = 70, max = 70.9)

    case object UpperSecond extends FinalYearGrade("2.1", min = 60, max = 67.9)

    case object UpperSecondBorderline extends FinalYearGrade("2.1 (b)", min = 68, max = 69.9)

    case object LowerSecond extends FinalYearGrade("2.2", min = 50, max = 57.9)

    case object LowerSecondBorderline extends FinalYearGrade("2.2 (b)", min = 58, max = 59.9)

    case object Third extends FinalYearGrade("3", min = 40, max = 47.9)

    case object ThirdBorderline extends FinalYearGrade("3 (b)", min = 48, max = 49.9)

    case object Pass extends FinalYearGrade("Pass", min = 35, max = 37.9)

    case object PassBorderline extends FinalYearGrade("Pass (b)", min = 38, max = 39.9)

    case object Fail extends FinalYearGrade("Fail", min = -100, max = 34.9)

  }

  object Postgraduate {
    val all: Seq[FinalYearGrade] = Seq(HighDistinction, Distinction, Merit, Pass, Fail)

    case object HighDistinction extends FinalYearGrade("High Distinction", min = 80, max = 200)

    case object Distinction extends FinalYearGrade("Distinction", min = 70, max = 79.9)

    case object Merit extends FinalYearGrade("Merit", min = 60, max = 69.9)

    case object Pass extends FinalYearGrade("Pass", min = 50, max = 59.9)

    case object Fail extends FinalYearGrade("Fail", min = -100, max = 49.9)

  }

  case class Unknown(reason: String) extends FinalYearGrade("?", null, null, Some(reason))

  case object Ignore extends FinalYearGrade("-", null, null)

  def fail(mark: BigDecimal, courseType: CourseType, details: String): FinalYearGrade = {
    if (courseType == CourseType.UG)
      Undergraduate.Fail.withMark(mark, Some(details))
    else
      Postgraduate.Fail.withMark(mark, Some(details))
  }

  def fromMark(mark: BigDecimal, courseType: CourseType): FinalYearGrade = {
    val grades = if (courseType == CourseType.UG) Undergraduate.all else Postgraduate.all

    grades.find(_.applies(mark)).map(_.withMark(mark))
      .getOrElse(Unknown(s"Could not find matching grade for mark ${mark.toString}"))
  }
}

object ProgressionService {

  def modulePassMark(degreeType: DegreeType): Int = degreeType match {
    case Undergraduate => UndergradPassMark
    case Postgraduate => PostgraduatePassMark
    case _ => DefaultPassMark
  }

  def allowEmptyYearMarks(yearWeightings: Seq[CourseYearWeighting], entityYear: ExamGridEntityYear): Boolean = {
    lazy val yearAbroad = entityYear.yearAbroad
    yearWeightings.exists(w => w.yearOfStudy == entityYear.yearOfStudy && w.weighting == 0) || yearAbroad
  }

  def abroadYearWeightings(yearWeightings: Seq[CourseYearWeighting], studentCourseYearDetails: StudentCourseYearDetails): Seq[CourseYearWeighting] = {
    val allYearStudentCourseDetails = studentCourseYearDetails.studentCourseDetails.student.toExamGridEntity(studentCourseYearDetails).years
    yearWeightings.map { yearWeighting =>
      // if any year weightings are non zero they will still be considered 0 if student has gone abroad. We would display 0 if abroad for that course year
      val abroad = allYearStudentCourseDetails.get(yearWeighting.yearOfStudy).exists {
        case Some(ey) => allowEmptyYearMarks(yearWeightings, ey)
        case _ => false
      }
      if (abroad) yearWeighting.copyZeroWeighted else yearWeighting
    }
  }

  def getEntityPerYear(scyd: StudentCourseYearDetails, groupByLevel: Boolean, finalYearOfStudy: Int): Map[Int, ExamGridEntityYear] = {
    val scds = scyd.studentCourseDetails.student.freshStudentCourseDetails.sorted.takeWhile(_.scjCode != scyd.studentCourseDetails.scjCode) ++ Seq(scyd.studentCourseDetails)
    val allScydsUnfiltered = scds.flatMap(_.freshStudentCourseYearDetails).filter(d => d.studentCourseDetails.courseType == scyd.studentCourseDetails.courseType && d.academicYear <= scyd.academicYear)

    // Where there are multiple SYCDs with the same SPR code and academic year, only retain the last one.
    var seen: mutable.Set[(String, AcademicYear)] = mutable.Set()
    val allScyds: Seq[StudentCourseYearDetails] =
      allScydsUnfiltered.reverse.filter { scyd =>
        if (seen.contains((scyd.studentCourseDetails.sprCode, scyd.academicYear))) false
        else {
          seen += scyd.studentCourseDetails.sprCode -> scyd.academicYear
          true
        }
      }.reverse

    if (groupByLevel) {
      allScyds.groupBy(_.level.orNull)
        .map { case (level, scyds) =>
          if (level == null) throw new RuntimeException(s"Invalid SITS data. SITS Student course level not set ${scyds.map(_.studentCourseDetails.scjCode).distinct.mkString(", ")}")

          // Only where the SPR code and SCJ code matches the last one - don't undo this or it will merge over course transfers
          // also needs to be for the same level, we don't allow grouping of more than one level, just use the last
          val allScydsForSameCourse = scyds.filter { scyd =>
            scyd.studentCourseDetails.sprCode == scyds.last.studentCourseDetails.sprCode &&
            scyd.studentCourseDetails.scjCode == scyds.last.studentCourseDetails.scjCode
          }

          level.toYearOfStudy -> StudentCourseYearDetails.toExamGridEntityYearGrouped(level.toYearOfStudy, allScydsForSameCourse: _*)
        }
    } else {
      (1 to finalYearOfStudy).map { block =>
        val allScydsForYear = allScyds.filter(_.yearOfStudy.toInt == block)

        // Only where the SPR code and SCJ code matches the last one - don't undo this or it will merge over course transfers
        // also needs to be for the same level, we don't allow grouping of more than one level, just use the last
        val allScydsForYearForSameCourse = allScydsForYear.filter { scyd =>
          scyd.studentCourseDetails.sprCode == allScydsForYear.last.studentCourseDetails.sprCode &&
          scyd.studentCourseDetails.scjCode == allScydsForYear.last.studentCourseDetails.scjCode &&
          scyd.studyLevel == allScydsForYear.last.studyLevel
        }

        block -> (allScydsForYearForSameCourse.toList match {
          case Nil => null
          case single :: Nil => single.toExamGridEntityYear
          case multiple => StudentCourseYearDetails.toExamGridEntityYearGrouped(block, multiple: _*)
        })
      }.toMap
    }
  }

  final val DefaultPassMark = 40
  final val UndergradPassMark = 40
  final val PostgraduatePassMark = 50

  final val FirstYearPassMark = 40
  final val FirstYearRequiredCredits = 80
  final val IntermediateYearPassMark = 40
  final val IntermediateRequiredCredits = 60
  final val FinalTwoYearsRequiredCredits = 168
  final val FinalYearRequiredCredits = 80
}

trait ProgressionService {
  def getYearMark(entityYear: ExamGridEntityYear, normalLoad: BigDecimal, routeRules: Seq[UpstreamRouteRule], yearWeightings: Seq[CourseYearWeighting]): Either[String, BigDecimal]

  def marksPerYear(scyd: StudentCourseYearDetails, normalLoad: BigDecimal, routeRulesPerYear: Map[Int, Seq[UpstreamRouteRule]], yearMarksToUse: ExamGridYearMarksToUse, groupByLevel: Boolean, weightings: Seq[CourseYearWeighting], markForFinalYear: Boolean): Either[String, Map[Int, BigDecimal]]

  def graduationBenchmark(studentCourseYearDetails: Option[StudentCourseYearDetails], yearOfStudy: Int, normalLoad: BigDecimal, routeRulesPerYear: Map[Int, Seq[UpstreamRouteRule]], yearMarksToUse: ExamGridYearMarksToUse, groupByLevel: Boolean, weightings: Seq[CourseYearWeighting]): Either[String, BigDecimal]

  def postgraduateBenchmark(scyd: StudentCourseYearDetails, moduleRegistrations: Seq[ModuleRegistration]): BigDecimal

  def bestPGModules(moduleRegistrations: Seq[ModuleRegistration], bestCats: BigDecimal): (Seq[ModuleRegistration], BigDecimal)

  def numberCatsToConsiderPG(scyd: StudentCourseYearDetails): BigDecimal

  def suggestedResult(entityYear: ExamGridEntityYear, normalLoad: BigDecimal, routeRulesPerYear: Map[Int, Seq[UpstreamRouteRule]], yearMarksToUse: ExamGridYearMarksToUse, groupByLevel: Boolean, applyBenchmark: Boolean, yearWeightings: Seq[CourseYearWeighting]): ProgressionResult

  def suggestedFinalYearGrade(entityYear: ExamGridEntityYear, normalLoad: BigDecimal, routeRulesPerYear: Map[Int, Seq[UpstreamRouteRule]], yearMarksToUse: ExamGridYearMarksToUse, groupByLevel: Boolean, applyBenchmark: Boolean, yearWeightings: Seq[CourseYearWeighting]): FinalYearGrade

  def isPassed(mr: ModuleRegistration): Boolean
  def isFailed(mr: ModuleRegistration): Boolean
}

abstract class AbstractProgressionService extends ProgressionService {

  self: ModuleRegistrationServiceComponent with CourseAndRouteServiceComponent =>

  def getYearMark(entityYear: ExamGridEntityYear, normalLoad: BigDecimal, routeRules: Seq[UpstreamRouteRule], yearWeightings: Seq[CourseYearWeighting]): Either[String, BigDecimal] = {
    /** TODO (TAB-6397)- We need to check MOA categories similar to what cognos does currently which will resolve issue for different years abroad for the same course. If those specific categories, then allowEmpty should be set as true.
      * Will need same checking at other places too. Currently, for those  courses  year weightings are set as non zero for one of them (2nd or 3rd year) by modern language making it unable to calculate final year overall marks
      * even though they are abroad. A further validation  will be required to ensure weighted %age is 100 when we calculate final overall marks.
      */
    val allowEmpty = ProgressionService.allowEmptyYearMarks(yearWeightings, entityYear)

    val possibleWeightedMeanMark = moduleRegistrationService.weightedMeanYearMark(entityYear.moduleRegistrations, Map(), allowEmpty = allowEmpty)
      .left.map(msg => s"$msg for year ${entityYear.yearOfStudy}")

    val overcatSubsets = moduleRegistrationService.overcattedModuleSubsets(entityYear.moduleRegistrations, Map(), normalLoad, routeRules)
    if (overcatSubsets.size <= 1) {
      // If the there's only one valid subset, just choose the mean mark
      possibleWeightedMeanMark
    } else if (entityYear.studentCourseYearDetails.flatMap(_.overcattingModules).isDefined) {
      // If the student has overcatted and a subset of modules has been chosen for the overcatted mark,
      // find the subset that matches those modules, and show that mark if found
      overcatSubsets.find { case (_, subset) => subset.size == entityYear.overcattingModules.get.size && subset.map(_.module).forall(entityYear.overcattingModules.get.contains) }
        .map { case (overcatMark, _) =>
          possibleWeightedMeanMark match {
            case Right(mark) => Right(Seq(mark, overcatMark).max)
            case Left(message) => Left(message)
          }
        }.getOrElse(Left("Could not find valid module registration subset matching chosen subset"))
    } else if (allowEmpty) {
      // Just pick the highest subset, it's zero-weighted anyway
      val overcatMark = overcatSubsets.map(_._1).max

      possibleWeightedMeanMark match {
        case Right(mark) => Right(Seq(mark, overcatMark).max)
        case Left(message) => Left(message)
      }
    } else {
      Left("The overcat adjusted mark subset has not been chosen")
    }
  }

  private def finalYearOfStudy(scyd: StudentCourseYearDetails, groupByLevel: Boolean): Int = if (groupByLevel && scyd.isFinalYear) {
    scyd.level.map(_.toYearOfStudy).getOrElse(scyd.studentCourseDetails.courseYearLength)
  } else {
    scyd.studentCourseDetails.courseYearLength
  }

  def marksPerYear(
    scyd: StudentCourseYearDetails,
    normalLoad: BigDecimal,
    routeRulesPerYear: Map[Int, Seq[UpstreamRouteRule]],
    yearMarksToUse: ExamGridYearMarksToUse,
    groupByLevel: Boolean,
    weightings: Seq[CourseYearWeighting],
    markForFinalYear: Boolean,
  ): Either[String, Map[Int, BigDecimal]] = {
    val finalYear = finalYearOfStudy(scyd, groupByLevel)
    lazy val entityPerYear = ProgressionService.getEntityPerYear(scyd, groupByLevel, finalYear)
    getMarkPerYear(entityPerYear, finalYear, normalLoad, routeRulesPerYear, yearMarksToUse, weightings, markForFinalYear)
  }

  def graduationBenchmark(studentCourseYearDetails: Option[StudentCourseYearDetails], yearOfStudy: Int, normalLoad: BigDecimal, routeRulesPerYear: Map[Int, Seq[UpstreamRouteRule]], yearMarksToUse: ExamGridYearMarksToUse, groupByLevel: Boolean, weightings: Seq[CourseYearWeighting]): Either[String, BigDecimal] = {
    studentCourseYearDetails.map(scyd => {
      val finalYear = finalYearOfStudy(scyd, groupByLevel)
      if (yearOfStudy >= finalYear) {
        lazy val entityPerYear = ProgressionService.getEntityPerYear(scyd, groupByLevel, finalYear)
        lazy val markPerYear = getMarkPerYear(entityPerYear, finalYear, normalLoad, routeRulesPerYear, yearMarksToUse, weightings, markForFinalYear = false)
        lazy val yearWeightings = getWeightingsPerYear(scyd, weightings, entityPerYear.keys.toSeq)
        for (mpy <- markPerYear; yw <- yearWeightings) yield {
          calculateBenchmark(entityPerYear, mpy, yw, normalLoad)
        }
      } else {
        Left("Only applies to the final year of study")
      }
    }).getOrElse(Left(s"Missing year details for $yearOfStudy"))
  }

  def postgraduateBenchmark(scyd: StudentCourseYearDetails, moduleRegistrations: Seq[ModuleRegistration]): BigDecimal = {
    val bestCats = numberCatsToConsiderPG(scyd)
    val (bestModules, catsConsidered) = bestPGModules(moduleRegistrations, bestCats)
    val total = bestModules.map(mr => BigDecimal(mr.firstDefinedMark.get) * mr.safeCats.getOrElse(BigDecimal(0))).sum
    if(catsConsidered > 0) (total / catsConsidered).setScale(1, RoundingMode.HALF_UP) else BigDecimal(0)
  }

  def bestPGModules(moduleRegistrations: Seq[ModuleRegistration], bestCats: BigDecimal): (Seq[ModuleRegistration], BigDecimal) = {
    val sortedByMark = moduleRegistrations.filter(_.firstDefinedMark.isDefined).sortBy(mr => (mr.firstDefinedMark.get, mr.safeCats.getOrElse(BigDecimal(0)))).reverse
    var catsConsidered = BigDecimal(0)
    val bestModules = sortedByMark.takeWhile { mr =>
      val takeMore = catsConsidered < bestCats
      if(takeMore) catsConsidered += mr.safeCats.getOrElse(BigDecimal(0))
      takeMore
    }
    (bestModules, catsConsidered)
  }

  def numberCatsToConsiderPG(scyd: StudentCourseYearDetails): BigDecimal = {
    if (Option(scyd.studentCourseDetails.award).map(_.code).contains("PGDIP")) BigDecimal(90)
    else BigDecimal(120)
  }

  private def calculateBenchmark(entityPerYear: Map[Int, ExamGridEntityYear], marksPerYear: Map[Int, BigDecimal], yearWeightings: Map[Int, CourseYearWeighting], normalLoad: BigDecimal): BigDecimal = {
    val finalYear :: otherYears = entityPerYear.keys.toSeq.sorted.reverse
    val previousYearWeightedMarks = otherYears.map { year =>
      val weighting = yearWeightings(year).weighting
      if (weighting > 0) weighting * marksPerYear(year)
      else BigDecimal(0)
    }.sum
    val finalYearModules = entityPerYear(finalYear).moduleRegistrations
    val percentageOfFinalYearAssessments = moduleRegistrationService.percentageOfAssessmentTaken(finalYearModules, normalLoad) / 100
    val benchmarkWeightedFinalYear = yearWeightings(finalYear).weighting * moduleRegistrationService.benchmarkWeightedAssessmentMark(finalYearModules) * percentageOfFinalYearAssessments
    val weightedPercentageOfCompletedAssessments = otherYears.map(year => yearWeightings(year).weighting).sum + (yearWeightings(finalYear).weighting * percentageOfFinalYearAssessments)
    if (weightedPercentageOfCompletedAssessments == 0) BigDecimal(0)
    else ((previousYearWeightedMarks + benchmarkWeightedFinalYear) / weightedPercentageOfCompletedAssessments).setScale(1, RoundingMode.HALF_UP)
  }

  def suggestedResult(entityYear: ExamGridEntityYear, normalLoad: BigDecimal, routeRulesPerYear: Map[Int, Seq[UpstreamRouteRule]], yearMarksToUse: ExamGridYearMarksToUse, groupByLevel: Boolean, applyBenchmark: Boolean, yearWeightings: Seq[CourseYearWeighting]): ProgressionResult = {
    entityYear.studentCourseYearDetails.map(scyd => {
      val emptyExpectingMarks = entityYear.moduleRegistrations.filter(mr => !mr.passFail && mr.firstDefinedMark.isEmpty && !mr.firstDefinedGrade.contains(GradeBoundary.ForceMajeureMissingComponentGrade))
      val emptyExpectingGrades = entityYear.moduleRegistrations.filter(mr => mr.passFail && mr.firstDefinedGrade.isEmpty)

      if (emptyExpectingMarks.nonEmpty) {
        ProgressionResult.Unknown(s"No agreed mark or actual mark for modules: ${emptyExpectingMarks.map(_.module.code.toUpperCase).mkString(", ")}")
      } else if (emptyExpectingGrades.nonEmpty) {
        ProgressionResult.Unknown(s"No agreed grade or actual grade for modules: ${emptyExpectingGrades.map(_.module.code.toUpperCase).mkString(", ")}")
      } else if (entityYear.moduleRegistrations.isEmpty) {
        ProgressionResult.Unknown(s"No module registrations found for ${scyd.studentCourseDetails.scjCode} ${scyd.academicYear.toString}")
      } else if (entityYear.yearOfStudy == 1) {
        suggestedResultFirstYear(entityYear, normalLoad, routeRulesPerYear.getOrElse(scyd.yearOfStudy, Seq()), yearWeightings)
      } else if (scyd.isFinalYear) {
        val sfyg = suggestedFinalYearGrade(entityYear, normalLoad, routeRulesPerYear, yearMarksToUse, groupByLevel, applyBenchmark, yearWeightings)

        sfyg match {
          case g if g.fail => ProgressionResult.Resit
          case unknown: FinalYearGrade.Unknown => ProgressionResult.Unknown(unknown.reason)
          case _ => ProgressionResult.Pass
        }
      } else {
        suggestedResultIntermediateYear(entityYear, normalLoad, routeRulesPerYear.getOrElse(scyd.yearOfStudy, Seq()), yearWeightings)
      }
    }).getOrElse(ProgressionResult.Unknown(s"Missing year details for ${entityYear.level.map(_.code).getOrElse("an unknown level")}"))
  }

  // a definition of a passed module that handles pass-fail modules
  def isPassed(mr: ModuleRegistration): Boolean =
    if (Option(mr.moduleResult).nonEmpty) mr.moduleResult == ModuleResult.Pass
    else if (mr.passFail) mr.firstDefinedGrade.contains("P")
    else mr.firstDefinedMark.exists(_ >= ProgressionService.modulePassMark(mr.module.degreeType))

  def isFailed(mr: ModuleRegistration): Boolean =
    if (Option(mr.moduleResult).nonEmpty) mr.moduleResult == ModuleResult.Fail
    else if (mr.passFail) mr.firstDefinedGrade.contains("F")
    else mr.firstDefinedMark.exists(_ < ProgressionService.modulePassMark(mr.module.degreeType))

  /**
    * Regulation defined at: http://www2.warwick.ac.uk/services/aro/dar/quality/categories/examinations/conventions/fyboe
    */
  private def suggestedResultFirstYear(entityYear: ExamGridEntityYear, normalLoad: BigDecimal, routeRules: Seq[UpstreamRouteRule], yearWeightings: Seq[CourseYearWeighting]): ProgressionResult = {
    entityYear.studentCourseYearDetails.map(scyd => {
      val coreRequiredModules = moduleRegistrationService.findCoreRequiredModules(
        scyd.studentCourseDetails.currentRoute,
        scyd.academicYear,
        scyd.yearOfStudy
      )

      val passedModuleRegistrations = entityYear.moduleRegistrations.filter(isPassed)
      val passedCredits = passedModuleRegistrations.map(mr => mr.safeCats.getOrElse(BigDecimal(0))).sum >= ProgressionService.FirstYearRequiredCredits
      val passedCoreRequired = coreRequiredModules.forall(cr => passedModuleRegistrations.exists(_.module == cr.module))
      val overallMark = getYearMark(entityYear, normalLoad, routeRules, yearWeightings)

      if (overallMark.isLeft) {
        ProgressionResult.Unknown(overallMark.swap.toOption.get)
      } else {
        val overallMarkSatisfied = overallMark.toOption.get >= ProgressionService.FirstYearPassMark
        if (passedCredits && passedCoreRequired && overallMarkSatisfied) {
          ProgressionResult.Proceed
        } else if (passedCoreRequired && overallMarkSatisfied) {
          ProgressionResult.PossiblyProceed
        } else {
          ProgressionResult.Resit
        }
      }
    }).getOrElse(ProgressionResult.Unknown(s"Missing year details for ${entityYear.level.map(_.code).getOrElse("an unknown level")}"))
  }

  /**
    * Regulation defined at: http://www2.warwick.ac.uk/services/aro/dar/quality/categories/examinations/conventions/ugprogression09/
    */
  private def suggestedResultIntermediateYear(entityYear: ExamGridEntityYear, normalLoad: BigDecimal, routeRules: Seq[UpstreamRouteRule], yearWeightings: Seq[CourseYearWeighting]): ProgressionResult = {
    entityYear.studentCourseYearDetails.map(scyd => {
      val passedModuleRegistrations = entityYear.moduleRegistrations.filter(isPassed)
      val passedCredits = passedModuleRegistrations.map(mr => mr.safeCats.getOrElse(BigDecimal(0))).sum >= ProgressionService.IntermediateRequiredCredits
      val overallMark = getYearMark(entityYear, normalLoad, routeRules, yearWeightings)

      if (overallMark.isLeft) {
        ProgressionResult.Unknown("Over Catted Mark not yet chosen")
      } else {
        val overallMarkSatisfied = overallMark.toOption.get >= ProgressionService.IntermediateYearPassMark
        if (passedCredits && overallMarkSatisfied) {
          ProgressionResult.Proceed
        } else {
          ProgressionResult.Resit
        }
      }
    }).getOrElse(ProgressionResult.Unknown(s"Missing year details for ${entityYear.level.map(_.code).getOrElse("an unknown level")}"))
  }

  /**
    * Regulation defined at: http://www2.warwick.ac.uk/services/aro/dar/quality/categories/examinations/conventions/ug13
    */
  def suggestedFinalYearGrade(
    entityYear: ExamGridEntityYear,
    normalLoad: BigDecimal,
    routeRulesPerYear: Map[Int, Seq[UpstreamRouteRule]],
    yearMarksToUse: ExamGridYearMarksToUse,
    groupByLevel: Boolean,
    applyBenchmark: Boolean,
    weightings: Seq[CourseYearWeighting]
  ): FinalYearGrade = {
    entityYear.studentCourseYearDetails.map { scyd =>
      val finalYear = finalYearOfStudy(scyd, groupByLevel)
      if (entityYear.yearOfStudy >= finalYear) {
        lazy val entityPerYear = ProgressionService.getEntityPerYear(scyd, groupByLevel, finalYear)
        lazy val markPerYear = getMarkPerYear(entityPerYear, finalYear, normalLoad, routeRulesPerYear, yearMarksToUse, weightings, markForFinalYear = true)
        lazy val yearWeightings = getWeightingsPerYear(scyd, weightings, entityPerYear.keys.toSeq)

        (for(mpy <- markPerYear; yw <- yearWeightings) yield {
          weightedFinalYearGrade(
            scyd,
            entityPerYear,
            mpy,
            yw,
            entityYear.moduleRegistrations,
            applyBenchmark,
            normalLoad
          )
        }).fold(FinalYearGrade.Unknown.apply, identity)
      } else {
        FinalYearGrade.Ignore
      }
    }.getOrElse(FinalYearGrade.Ignore)
  }

  private def getMarkPerYear(
    entityPerYear: Map[Int, ExamGridEntityYear],
    finalYearOfStudy: Int,
    normalLoad: BigDecimal,
    routeRulesPerYear: Map[Int, Seq[UpstreamRouteRule]],
    yearMarksToUseForPreviousYearMarks: ExamGridYearMarksToUse,
    yearWeightings: Seq[CourseYearWeighting],
    markForFinalYear: Boolean
  ): Either[String, Map[Int, BigDecimal]] = {

    val markPerYear = entityPerYear
      .filter { case (year, entityYear) => entityYear != null && (markForFinalYear || year < finalYearOfStudy)}
      .map { case (year, entityYear) =>
        year -> entityYear.studentCourseYearDetails.map { thisScyd =>
          lazy val uploadedYearMark: Option[BigDecimal] = entityYear.agreedMark

          lazy val calculatedYearMark: Either[String, BigDecimal] =
            getYearMark(entityYear, normalLoad, routeRulesPerYear.getOrElse(year, Seq()), yearWeightings)

          yearMarksToUseForPreviousYearMarks match {
            case _ if year == finalYearOfStudy =>
              calculatedYearMark

            case ExamGridYearMarksToUse.CalculateYearMarks =>
              calculatedYearMark

            case ExamGridYearMarksToUse.UploadedYearMarksOnly =>
              uploadedYearMark.toRight(s"Could not find agreed mark for year $year")

            case ExamGridYearMarksToUse.UploadedYearMarksIfAvailable =>
              uploadedYearMark.fold(calculatedYearMark)(Right.apply)
          }
        }.getOrElse(Left(s"Could not find course details for year $year"))
      }

    if (markPerYear.exists { case (_, possibleMark) => possibleMark.isLeft }) {
      Left("The final overall mark cannot be calculated because there is no mark for " +
        markPerYear.filter { case (_, possibleMark) => possibleMark.isLeft }
          .map { case (year, _) => year }
          .toSeq
          .sorted
          .map { year => s"year $year" }
          .mkString(", ")
      )
    } else {
      Right(markPerYear.map { case (year, marks) => year -> marks.toOption.get })
    }
  }

  private def getWeightingsPerYear(scyd: StudentCourseYearDetails, rawWeightings: Seq[CourseYearWeighting], years: Seq[Int]): Either[String, Map[Int, CourseYearWeighting]] = {
    val weightings = ProgressionService.abroadYearWeightings(rawWeightings, scyd)

    val (yearsWithoutWeightings, yearWeightings) = years.map { year =>
      val yearWeighting = weightings.filter(_.yearOfStudy == year)
      yearWeighting.headOption.map(w => Right(year -> w)).getOrElse(Left(year))
    }.partitionMap(identity)

    if(yearsWithoutWeightings.nonEmpty) {
      val missingYearsString = yearsWithoutWeightings.map(y =>
        s"${scyd.studentCourseDetails.course.code.toUpperCase} ${scyd.studentCourseDetails.sprStartAcademicYear.toString} Year $y"
      ).mkString(", ")
      Left(s"Could not find year weightings for: $missingYearsString")
    } else {
      Right(yearWeightings.toMap)
    }
  }

  private def invalidTotalYearWeightings(markPerYear: Map[Int, BigDecimal], yearWeightings: Map[Int, CourseYearWeighting]): Boolean = {
    val baseTotalWeighting = markPerYear.map { case (year, _) => yearWeightings(year).weighting }.sum

    if (baseTotalWeighting > 1) {
      // you can set up 0/50/50/50 initially. One of those could be abroad(2nd or 3rd year year). Excluding any abroad year we still should have total as 100
      //0 marks  generated for the year  are valid allowed marks with 0 weightings based on year abroad. Check the remaining ones total are  still 100%
      markPerYear.filter(_._2 > 0).map { case (year, _) => yearWeightings(year).weighting }.toSeq.sum != 1
    } else baseTotalWeighting != 1
  }

  private def weightedFinalYearGrade(
    scyd: StudentCourseYearDetails,
    entityPerYear: Map[Int, ExamGridEntityYear],
    markPerYear: Map[Int, BigDecimal],
    yearWeightings: Map[Int, CourseYearWeighting],
    moduleRegistrations: Seq[ModuleRegistration],
    applyBenchmark: Boolean,
    normalLoad: BigDecimal
  ): FinalYearGrade = {
    // This only considers years where the weighting counts  when they are not not abroad - so for a course with an
    // intercalated year weighted 0,50,0,50, this would consider years 2 and 4. For weightings set like 0/50/50/50 (2nd or 3rd year abroad for same course), it will consider last 2 years non- abroad ones
    val finalTwoYearsModuleRegistrations =
      entityPerYear.toSeq.reverse
        .filter { case (_, gridEntityYear) => gridEntityYear != null && !ProgressionService.allowEmptyYearMarks(yearWeightings.values.toSeq, gridEntityYear) }
        .take(2)
        .flatMap { case (_, yearDetails) => yearDetails.moduleRegistrations }

    // Don't take into account the FM grade here - it's not valid for the final two years, only for first years
    if (finalTwoYearsModuleRegistrations.filterNot(_.passFail).exists(mr => mr.firstDefinedMark.isEmpty && !mr.firstDefinedGrade.contains(GradeBoundary.ForceMajeureMissingComponentGrade))) {
      FinalYearGrade.Unknown(s"No agreed mark or actual mark for modules: ${
        finalTwoYearsModuleRegistrations.filter(_.firstDefinedMark.isEmpty).map(mr => "%s %s".format(mr.module.code.toUpperCase, mr.academicYear.toString)).mkString(", ")
      }")
    } else if (invalidTotalYearWeightings(markPerYear, yearWeightings)) {
      FinalYearGrade.Unknown("Total year weightings for all course years excluding abroad are not 100%")
    } else if (scyd.studentCourseDetails.courseType.isEmpty) {
      FinalYearGrade.Unknown("Unknown course type")
    } else {
      val yearWeightedMark: BigDecimal = markPerYear.map { case (year, _) =>
        markPerYear(year) * yearWeightings(year).weighting
      }.sum.setScale(1, RoundingMode.HALF_UP)

      val finalMark = if(applyBenchmark) {
        val graduationBenchmark = scyd.studentCourseDetails.courseType match {
          case Some(PGT) => postgraduateBenchmark(scyd, moduleRegistrations)
          case Some(UG) => calculateBenchmark(entityPerYear, markPerYear, yearWeightings, normalLoad)
          case _ => BigDecimal(0)
        }

        Seq(yearWeightedMark, graduationBenchmark).max
      } else {
        yearWeightedMark
      }

      val passedModuleRegistrationsInFinalTwoYears: Seq[ModuleRegistration] = finalTwoYearsModuleRegistrations.filter(isPassed)
      val sumFinalTwoYearsPassedCredits = passedModuleRegistrationsInFinalTwoYears.map(mr => mr.safeCats.getOrElse(BigDecimal(0))).sum
      val passedCreditsInFinalTwoYears = sumFinalTwoYearsPassedCredits >= ProgressionService.FinalTwoYearsRequiredCredits

      val passedModuleRegistrationsFinalYear: Seq[ModuleRegistration] = entityPerYear.toSeq.last._2.moduleRegistrations.filter(isPassed)
      val sumFinalYearPassedCredits = passedModuleRegistrationsFinalYear.map(mr => mr.safeCats.getOrElse(BigDecimal(0))).sum
      val passedCreditsFinalYear = sumFinalYearPassedCredits >= ProgressionService.FinalYearRequiredCredits

      val onlyAttendedOneYear = entityPerYear.size == 1

      if (passedCreditsFinalYear) {
        if (passedCreditsInFinalTwoYears || onlyAttendedOneYear) {
          FinalYearGrade.fromMark(finalMark, courseType = scyd.studentCourseDetails.courseType.get)
        } else {
          FinalYearGrade.fail(finalMark, courseType = scyd.studentCourseDetails.courseType.get, s"Final two years passed CATS ${sumFinalTwoYearsPassedCredits.underlying.stripTrailingZeros().toPlainString} < ${ProgressionService.FinalTwoYearsRequiredCredits}")
        }
      } else {
        FinalYearGrade.fail(finalMark, courseType = scyd.studentCourseDetails.courseType.get, s"Final year passed CATS ${sumFinalYearPassedCredits.underlying.stripTrailingZeros().toPlainString} < ${ProgressionService.FinalYearRequiredCredits}")
      }
    }
  }

}

@Service("progressionService")
class ProgressionServiceImpl
  extends AbstractProgressionService
    with AutowiringModuleRegistrationServiceComponent
    with AutowiringCourseAndRouteServiceComponent

trait ProgressionServiceComponent {
  def progressionService: ProgressionService
}

trait AutowiringProgressionServiceComponent extends ProgressionServiceComponent {
  var progressionService: ProgressionService = Wire[ProgressionService]
}
