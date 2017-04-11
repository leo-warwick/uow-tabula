package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._

import scala.math.BigDecimal.RoundingMode

sealed abstract class ProgressionResult(val description: String)

object ProgressionResult {
	case object Proceed extends ProgressionResult("Proceed")
	case object PossiblyProceed extends ProgressionResult("Proceed(?)")
	case object Resit extends ProgressionResult("Resit")
	case object Pass extends ProgressionResult("Pass")
	case class Unknown(details: String) extends ProgressionResult("?")
}

sealed abstract class FinalYearGrade(val description: String, val lowerBound: BigDecimal, val upperBound: BigDecimal) {
	def applies(mark: BigDecimal): Boolean = {
		mark <= upperBound && mark >= lowerBound
	}
	def withMark(mark: BigDecimal): FinalYearMark = FinalYearMark(mark, description, lowerBound, upperBound)
}

case class FinalYearMark(mark: BigDecimal, override val description: String, override val lowerBound: BigDecimal, override val upperBound: BigDecimal)
	extends FinalYearGrade(description, lowerBound, upperBound)

object FinalYearGrade {
	case object First extends FinalYearGrade(
		"1",
		BigDecimal(71.0).setScale(1, RoundingMode.HALF_UP),
		BigDecimal(200.0).setScale(1, RoundingMode.HALF_UP)
	)
	case object FirstBorderline extends FinalYearGrade(
		"1 (b)",
		BigDecimal(70.0).setScale(1, RoundingMode.HALF_UP),
		BigDecimal(70.9).setScale(1, RoundingMode.HALF_UP)
	)
	case object UpperSecond extends FinalYearGrade(
		"2.1",
		BigDecimal(60.0).setScale(1, RoundingMode.HALF_UP),
		BigDecimal(67.9).setScale(1, RoundingMode.HALF_UP)
	)
	case object UpperSecondBorderline extends FinalYearGrade(
		"2.1 (b)",
		BigDecimal(68.0).setScale(1, RoundingMode.HALF_UP),
		BigDecimal(69.9).setScale(1, RoundingMode.HALF_UP)
	)
	case object LowerSecond extends FinalYearGrade(
		"2.2",
		BigDecimal(50.0).setScale(1, RoundingMode.HALF_UP),
		BigDecimal(57.9).setScale(1, RoundingMode.HALF_UP)
	)
	case object LowerSecondBorderline extends FinalYearGrade(
		"2.2 (b)",
		BigDecimal(58.0).setScale(1, RoundingMode.HALF_UP),
		BigDecimal(59.9).setScale(1, RoundingMode.HALF_UP)
	)
	case object Third extends FinalYearGrade(
		"3",
		BigDecimal(40.0).setScale(1, RoundingMode.HALF_UP),
		BigDecimal(47.9).setScale(1, RoundingMode.HALF_UP)
	)
	case object ThirdBorderline extends FinalYearGrade(
		"3 (b)",
		BigDecimal(48.0).setScale(1, RoundingMode.HALF_UP),
		BigDecimal(49.9).setScale(1, RoundingMode.HALF_UP)
	)
	case object Pass extends FinalYearGrade(
		"Pass",
		BigDecimal(35.0).setScale(1, RoundingMode.HALF_UP),
		BigDecimal(37.9).setScale(1, RoundingMode.HALF_UP)
	)
	case object PassBorderline extends FinalYearGrade(
		"Pass (b)",
		BigDecimal(38.0).setScale(1, RoundingMode.HALF_UP),
		BigDecimal(39.9).setScale(1, RoundingMode.HALF_UP)
	)
	case object Fail extends FinalYearGrade(
		"Fail",
		BigDecimal(-100).setScale(1, RoundingMode.HALF_UP),
		BigDecimal(34.9).setScale(1, RoundingMode.HALF_UP)
	)
	case class Unknown(details: String) extends FinalYearGrade("?", null, null)
	case object Ignore extends FinalYearGrade("-", null, null)
	private val all = Seq(
		First, FirstBorderline, UpperSecond, UpperSecondBorderline, LowerSecond, LowerSecondBorderline,
		Third, ThirdBorderline, Pass, PassBorderline, Fail
	)
	def fromMark(mark: BigDecimal): FinalYearGrade =
		all.find(_.applies(mark)).map(_.withMark(mark))
			.getOrElse(Unknown(s"Could not find matching grade for mark ${mark.toString}"))
}

object ProgressionService {
	final val ModulePassMark = 40
	final val FirstYearPassMark = 40
	final val FirstYearRequiredCredits = 80
	final val IntermediateYearPassMark = 40
	final val IntermediateRequiredCredits = 60
	final val FinalTwoYearsRequiredCredits = 168
	final val FinalYearRequiredCredits = 80
}

trait ProgressionService {
	def getYearMark(scyd: StudentCourseYearDetails, normalLoad: BigDecimal, routeRules: Seq[UpstreamRouteRule]): Either[String, BigDecimal]
	def suggestedResult(scyd: StudentCourseYearDetails, normalLoad: BigDecimal, routeRules: Seq[UpstreamRouteRule]): ProgressionResult
	def suggestedFinalYearGrade(scyd: StudentCourseYearDetails, normalLoad: BigDecimal, routeRules: Seq[UpstreamRouteRule]): FinalYearGrade
}

abstract class AbstractProgressionService extends ProgressionService {

	self: ModuleRegistrationServiceComponent with CourseAndRouteServiceComponent =>

	def getYearMark(scyd: StudentCourseYearDetails, normalLoad: BigDecimal, routeRules: Seq[UpstreamRouteRule]): Either[String, BigDecimal] = {
		val possibleWeightedMeanMark = moduleRegistrationService.weightedMeanYearMark(scyd.moduleRegistrations, Map())
		val entityYear = scyd.toExamGridEntityYear
		val overcatSubsets = moduleRegistrationService.overcattedModuleSubsets(entityYear, Map(), normalLoad, routeRules)
		if (overcatSubsets.size <= 1) {
			// If the there's only one valid subset, just choose the mean mark
			possibleWeightedMeanMark
		} else if (scyd.overcattingModules.isDefined) {
			// If the student has overcatted and a subset of modules has been chosen for the overcatted mark,
			// find the subset that matches those modules, and show that mark if found
			overcatSubsets.find { case (_, subset) => subset.size == entityYear.overcattingModules.get.size && subset.map(_.module).forall(entityYear.overcattingModules.get.contains) }
				.map { case (overcatMark, _) => possibleWeightedMeanMark match {
					case Right(mark) => Right(Seq(mark, overcatMark).max)
					case Left(message) => Left(message)
				}
				}.getOrElse(Left("Could not find valid module registration subset matching chosen subset"))
		} else {
			Left("The overcat adjusted mark subset has not been chosen")
		}
	}

	def suggestedResult(scyd: StudentCourseYearDetails, normalLoad: BigDecimal, routeRules: Seq[UpstreamRouteRule]): ProgressionResult = {
		if (scyd.moduleRegistrations.exists(_.firstDefinedMark.isEmpty)) {
			ProgressionResult.Unknown(s"No agreed mark or actual mark for modules: ${scyd.moduleRegistrations.filter(_.firstDefinedMark.isEmpty).map(_.module.code.toUpperCase).mkString(", ")}")
		} else if (scyd.moduleRegistrations.isEmpty) {
				ProgressionResult.Unknown(s"No module registrations found for ${scyd.studentCourseDetails.scjCode} ${scyd.academicYear.toString}")
		} else if (scyd.yearOfStudy.toInt == 1) {
			suggestedResultFirstYear(scyd, normalLoad, routeRules)
		} else if (scyd.isFinalYear) {
			val sfyg = suggestedFinalYearGrade(scyd, normalLoad, routeRules)
			if (sfyg == FinalYearGrade.Fail) {
				ProgressionResult.Resit
			} else {
				sfyg match {
					case unknown: FinalYearGrade.Unknown => ProgressionResult.Unknown(unknown.details)
					case _ => ProgressionResult.Pass
				}
			}
		} else {
			suggestedResultIntermediateYear(scyd, normalLoad, routeRules)
		}

	}

	/**
		* Regulation defined at: http://www2.warwick.ac.uk/services/aro/dar/quality/categories/examinations/conventions/fyboe
		*/
	private def suggestedResultFirstYear(scyd: StudentCourseYearDetails, normalLoad: BigDecimal, routeRules: Seq[UpstreamRouteRule]): ProgressionResult = {
		val coreRequiredModules = moduleRegistrationService.findCoreRequiredModules(
			scyd.toExamGridEntityYear.route,
			scyd.academicYear,
			scyd.yearOfStudy
		)
		val passedModuleRegistrations = scyd.moduleRegistrations.filter(mr => BigDecimal(mr.firstDefinedMark.get) >= ProgressionService.ModulePassMark)
		val passedCredits = passedModuleRegistrations.map(mr => BigDecimal(mr.cats)).sum > ProgressionService.FirstYearRequiredCredits
		val passedCoreRequired = coreRequiredModules.forall(cr => passedModuleRegistrations.exists(_.module == cr.module))
		val overallMark = getYearMark(scyd, normalLoad, routeRules)

		if (overallMark.isLeft) {
			ProgressionResult.Unknown(overallMark.left.get)
		} else {
			val overallMarkSatisfied = overallMark.right.get >= ProgressionService.FirstYearPassMark
			if (passedCredits && passedCoreRequired && overallMarkSatisfied) {
				ProgressionResult.Proceed
			} else if (passedCoreRequired && overallMarkSatisfied) {
				ProgressionResult.PossiblyProceed
			} else {
				ProgressionResult.Resit
			}
		}
	}

	/**
		* Regulation defined at: http://www2.warwick.ac.uk/services/aro/dar/quality/categories/examinations/conventions/ugprogression09/
		*/
	private def suggestedResultIntermediateYear(scyd: StudentCourseYearDetails, normalLoad: BigDecimal, routeRules: Seq[UpstreamRouteRule]): ProgressionResult = {
		val passedModuleRegistrations = scyd.moduleRegistrations.filter(mr => BigDecimal(mr.firstDefinedMark.get) >= ProgressionService.ModulePassMark)
		val passedCredits = passedModuleRegistrations.map(mr => BigDecimal(mr.cats)).sum > ProgressionService.IntermediateRequiredCredits
		val overallMark = getYearMark(scyd, normalLoad, routeRules)

		if (overallMark.isLeft) {
			ProgressionResult.Unknown("Over Catted Mark not yet chosen")
		} else {
			val overallMarkSatisfied = overallMark.right.get >= ProgressionService.IntermediateYearPassMark
			if (passedCredits && overallMarkSatisfied) {
				ProgressionResult.Proceed
			} else {
				ProgressionResult.Resit
			}
		}
	}

	/**
		* Regulation defined at: http://www2.warwick.ac.uk/services/aro/dar/quality/categories/examinations/conventions/ug13
		*/
	def suggestedFinalYearGrade(scyd: StudentCourseYearDetails, normalLoad: BigDecimal, routeRules: Seq[UpstreamRouteRule]): FinalYearGrade = {
		if (scyd.isFinalYear) {
			val finalYearOfStudy = scyd.yearOfStudy.toInt
			val scydsFromThisAndOlderCourses: Seq[StudentCourseYearDetails] = {
				val scds = scyd.studentCourseDetails.student.freshStudentCourseDetails.sorted.takeWhile(_.scjCode != scyd.studentCourseDetails.scjCode) ++ Seq(scyd.studentCourseDetails)
				scds.flatMap(_.freshStudentCourseYearDetails)
			}
			val scydPerYear: Seq[(Int, Option[StudentCourseYearDetails])] = (1 to finalYearOfStudy).map(year => {
				if (year != finalYearOfStudy) {
					val scydsForThisYear = scydsFromThisAndOlderCourses.filter(scyd => scyd.yearOfStudy.toInt == year)
					val latestSCYDForThisYear = scydsForThisYear.lastOption // SCDs and SCYDs are sorted collections
					(year, latestSCYDForThisYear)
				} else {
					(year, Option(scyd))
				}
			})
			lazy val markPerYear: Seq[(Int, Either[String, BigDecimal])] = getMarkPerYear(scyd, scydPerYear, finalYearOfStudy, normalLoad, routeRules)
			lazy val yearWeightings: Seq[(Int, Option[CourseYearWeighting])] = markPerYear.map { case (year, _) =>
				(year, courseAndRouteService.getCourseYearWeighting(scyd.studentCourseDetails.course.code, scyd.studentCourseDetails.sprStartAcademicYear, year))
			}
			if (markPerYear.exists { case (_, possibleMark) => possibleMark.isLeft }) {
				FinalYearGrade.Unknown(markPerYear.flatMap { case (_, possibleMark) => possibleMark.left.toOption }.mkString(", "))
			} else if (yearWeightings.exists { case (_, possibleYearWeighting) => possibleYearWeighting.isEmpty } ) {
				FinalYearGrade.Unknown("Could not find year weightings for: %s".format(
					yearWeightings.filter { case (_, possibleWeighting) => possibleWeighting.isEmpty }.map { case (year, _) =>
						s"${scyd.studentCourseDetails.course.code.toUpperCase} ${scyd.studentCourseDetails.sprStartAcademicYear.toString} Year $year"
					}.mkString(", ")
				))
			} else {
				weightedFinalYearGrade(
					scyd,
					scydPerYear.map { case (year, option) => (year, option.get)},
					markPerYear.map { case (year, possibleMark) => (year, possibleMark.right.get)},
					yearWeightings.map { case (year, option) => (year, option.get)}
				)
			}
		} else {
			FinalYearGrade.Ignore
		}
	}

	private def getMarkPerYear(
		scyd: StudentCourseYearDetails,
		scydPerYear: Seq[(Int, Option[StudentCourseYearDetails])],
		finalYearOfStudy: Int,
		normalLoad: BigDecimal,
		routeRules: Seq[UpstreamRouteRule]
	): Seq[(Int, Either[String, BigDecimal])] = {
		scydPerYear.map{ case (year, scydOption) =>
			(year, scydOption.map(thisScyd => {
				if (year != finalYearOfStudy) {
					Option(thisScyd.agreedMark) match {
						case Some(mark) => Right(BigDecimal(mark))
						case _ => Left(s"Could not find agreed mark for year $year")
					}
				} else {
					getYearMark(scyd, normalLoad, routeRules)
				}
			}).getOrElse(Left(s"Could not find course details for year $year")))
		}
	}

	private def weightedFinalYearGrade(
		scyd: StudentCourseYearDetails,
		scydPerYear: Seq[(Int, StudentCourseYearDetails)],
		markPerYear: Seq[(Int, BigDecimal)],
		yearWeightings: Seq[(Int, CourseYearWeighting)]
	): FinalYearGrade = {
		val finalTwoYearsModuleRegistrations = scydPerYear.reverse.take(2).flatMap { case (_, yearDetails) => yearDetails.moduleRegistrations }

		if (finalTwoYearsModuleRegistrations.exists(_.firstDefinedMark.isEmpty)) {
			FinalYearGrade.Unknown(s"No agreed mark or actual mark for modules: ${
				finalTwoYearsModuleRegistrations.filter(_.firstDefinedMark.isEmpty).map(mr => "%s %s".format(mr.module.code.toUpperCase, mr.academicYear.toString)).mkString(", ")
			}")
		} else {
			val finalMark: BigDecimal = markPerYear.map { case (year, _) =>
				markPerYear.toMap.apply(year) * yearWeightings.toMap.apply(year).weighting
			}.sum.setScale(1, RoundingMode.HALF_UP)

			val passedModuleRegistrationsInFinalTwoYears: Seq[ModuleRegistration] = finalTwoYearsModuleRegistrations
				.filter(mr => BigDecimal(mr.firstDefinedMark.get) >= ProgressionService.ModulePassMark)
			val passedCreditsInFinalTwoYears = passedModuleRegistrationsInFinalTwoYears.map(mr => BigDecimal(mr.cats)).sum > ProgressionService.FinalTwoYearsRequiredCredits

			val passedModuleRegistrationsFinalYear: Seq[ModuleRegistration] = scydPerYear.reverse.head._2
				.moduleRegistrations.filter(mr => BigDecimal(mr.firstDefinedMark.get) >= ProgressionService.ModulePassMark)
			val passedCreditsFinalYear = passedModuleRegistrationsFinalYear.map(mr => BigDecimal(mr.cats)).sum > ProgressionService.FinalYearRequiredCredits

			if (passedCreditsInFinalTwoYears && passedCreditsFinalYear) {
				FinalYearGrade.fromMark(finalMark)
			} else {
				FinalYearGrade.Fail.withMark(finalMark)
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
