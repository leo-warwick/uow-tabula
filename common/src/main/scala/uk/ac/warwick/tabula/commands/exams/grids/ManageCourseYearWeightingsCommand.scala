package uk.ac.warwick.tabula.commands.exams.grids

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model.{Course, CourseYearWeighting, Department}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringCourseAndRouteServiceComponent, CourseAndRouteServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object ManageCourseYearWeightingsCommand {

	val RequiredPermission = Permissions.Department.ExamGrids
	
	case class Result(updated: Seq[CourseYearWeighting], removed: Seq[CourseYearWeighting])

	def apply(department: Department, startAcademicYear: AcademicYear) =
		new ManageCourseYearWeightingsCommandInternal(department, startAcademicYear)
			with AutowiringCourseAndRouteServiceComponent
			with ComposableCommand[ManageCourseYearWeightingsCommand.Result]
			with PopulatesManageCourseYearWeightingsCommand
			with ManageCourseYearWeightingsValidation
			with ManageCourseYearWeightingsDescription
			with ManageCourseYearWeightingsPermissions
			with ManageCourseYearWeightingsCommandState
			with ManageCourseYearWeightingsCommandRequest
}


class ManageCourseYearWeightingsCommandInternal(val department: Department, val startAcademicYear: AcademicYear)
	extends CommandInternal[ManageCourseYearWeightingsCommand.Result] {

	self: ManageCourseYearWeightingsCommandRequest with CourseAndRouteServiceComponent
		with ManageCourseYearWeightingsCommandState =>

	override def applyInternal(): ManageCourseYearWeightingsCommand.Result = {
		val weightings = yearWeightings.asScala.toSeq.map { case (course, yearMap) =>
			val (toRemove, toUpdate) = yearMap.asScala.partition { case (_, weighting) => Option(weighting).isEmpty }

			val removed = for {
				(year, _) <- toRemove
				weightingsForRoute <- originalWeightings.get(course)
				yearWeighting <- weightingsForRoute.get(year)
			} yield yearWeighting
			removed.foreach(courseAndRouteService.delete)

			val updated = toUpdate.map { case (year, weighting) =>
				val yearWeighting = originalWeightings.getOrElse(course, Map()).getOrElse(year, new CourseYearWeighting(course, startAcademicYear, year, BigDecimal(weighting)))
				yearWeighting.weightingAsPercentage = BigDecimal(weighting)
				yearWeighting
			}
			updated.foreach(courseAndRouteService.saveOrUpdate)

			(removed, updated)
		}

		ManageCourseYearWeightingsCommand.Result(
			weightings.flatMap { case (_, updated) => updated },
			weightings.flatMap { case (removed, _) => removed }
		)
		
	}

}

trait PopulatesManageCourseYearWeightingsCommand extends PopulateOnForm {

	self: CourseAndRouteServiceComponent with ManageCourseYearWeightingsCommandState
		with ManageCourseYearWeightingsCommandRequest with CourseAndRouteServiceComponent =>

	def populate(): Unit = {
		originalWeightings.foreach { case (course, yearMap) =>
			yearWeightings.put(course, JHashMap(yearMap.map { case (year, yearWeighting) => JInteger(Option(year)) -> yearWeighting.weightingAsPercentage }))
		}
	}

}

trait ManageCourseYearWeightingsValidation extends SelfValidating {

	self: ManageCourseYearWeightingsCommandRequest with ManageCourseYearWeightingsCommandState =>

	override def validate(errors: Errors) {
		yearWeightings.asScala.foreach { case (course, yearOfStudyMap) =>
			if (!allCourses.contains(course)) {
				errors.reject("examGrids.yearWeighting.course.invalid", Array(course.code.toUpperCase), "")
			}
			yearOfStudyMap.asScala.foreach { case (yearOfStudy, jNormalLoad) =>
				Option(jNormalLoad).foreach(weighting =>
					if (BigDecimal(weighting) < 0 || BigDecimal(weighting) > 100) {
						errors.rejectValue("weightings", "examGrids.yearWeighting.weighting.invalid", Array(course.code.toUpperCase, yearOfStudy.toString), "")
					}
				)
			}
		}
	}

}

trait ManageCourseYearWeightingsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ManageCourseYearWeightingsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(ManageCourseYearWeightingsCommand.RequiredPermission, department)
	}

}

trait ManageCourseYearWeightingsDescription extends Describable[ManageCourseYearWeightingsCommand.Result] {

	self: ManageCourseYearWeightingsCommandState =>

	override lazy val eventName = "ManageCourseYearWeightings"

	override def describe(d: Description) {
		d.department(department)
	}

	override def describeResult(d: Description, result: ManageCourseYearWeightingsCommand.Result): Unit = {
		d.property("updated", result.updated.map(weighting => Map(
			"course" -> weighting.course.code,
			"academicYear" -> weighting.sprStartAcademicYear,
			"yearOfStudy" -> weighting.yearOfStudy,
			"weighting" -> weighting.weighting
		)))
		d.property("removed", result.removed.map(weighting => Map(
			"course" -> weighting.course.code,
			"academicYear" -> weighting.sprStartAcademicYear,
			"yearOfStudy" -> weighting.yearOfStudy,
			"weighting" -> weighting.weighting
		)))
	}
}

trait ManageCourseYearWeightingsCommandState {

	self: CourseAndRouteServiceComponent with CourseAndRouteServiceComponent =>

	def department: Department
	def startAcademicYear: AcademicYear

	lazy val allCourses: Seq[Course] = courseAndRouteService.findCoursesInDepartment(department).sortBy(_.code)
	def getWeightings(thisStartAcademicYear: AcademicYear): Map[Course, Map[YearOfStudy, Option[CourseYearWeighting]]] = {
		val weightings = courseAndRouteService.findAllCourseYearWeightings(allCourses, thisStartAcademicYear)
		val yearsOfStudy = 1 to FilterStudentsOrRelationships.MaxYearsOfStudy
		allCourses.map(course => course ->
			yearsOfStudy.map(year => year ->
				weightings.find(CourseYearWeighting.find(course, thisStartAcademicYear, year))
			).toMap
		).toMap
	}
	lazy val originalWeightings: Map[Course, Map[YearOfStudy, CourseYearWeighting]] = {
		getWeightings(startAcademicYear).mapValues(_.filter { case (_, weightingOption) => weightingOption.isDefined }.mapValues(_.get))
			.filter { case (_, yearMap) => yearMap.nonEmpty }
	}
}

trait ManageCourseYearWeightingsCommandRequest {
	val yearWeightings: JMap[Course, JMap[JInteger, JBigDecimal]] = LazyMaps.create{_: Course => JMap[JInteger, JBigDecimal]() }.asJava
}
