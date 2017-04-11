package uk.ac.warwick.tabula.commands.exams.grids

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model.{Department, NormalCATSLoad, Route}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringCourseAndRouteServiceComponent, CourseAndRouteServiceComponent}
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringNormalCATSLoadServiceComponent, NormalCATSLoadServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object ManageNormalCATSLoadCommand {

	val RequiredPermission = Permissions.Department.ExamGrids
	
	case class Result(updated: Seq[NormalCATSLoad], removed: Seq[NormalCATSLoad])

	def apply(department: Department, academicYear: AcademicYear) =
		new ManageNormalCATSLoadCommandInternal(department, academicYear)
			with AutowiringNormalCATSLoadServiceComponent
			with AutowiringCourseAndRouteServiceComponent
			with ComposableCommand[ManageNormalCATSLoadCommand.Result]
			with PopulatesManageNormalCATSLoadCommand
			with ManageNormalCATSLoadValidation
			with ManageNormalCATSLoadDescription
			with ManageNormalCATSLoadPermissions
			with ManageNormalCATSLoadCommandState
			with ManageNormalCATSLoadCommandRequest
}


class ManageNormalCATSLoadCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[ManageNormalCATSLoadCommand.Result] {

	self: ManageNormalCATSLoadCommandRequest with NormalCATSLoadServiceComponent
		with ManageNormalCATSLoadCommandState =>

	override def applyInternal(): ManageNormalCATSLoadCommand.Result = {
		val loads = normalLoads.asScala.toSeq.map { case (route, yearMap) =>
			val (toRemove, toUpdate) = yearMap.asScala.partition { case (_, load) => Option(load).isEmpty }

			val removed = for {
				(year, _) <- toRemove
				loadsForRoute <- originalNormalLoads.get(route)
				normalLoad <- loadsForRoute.get(year)
			} yield normalLoad
			removed.foreach(normalCATSLoadService.delete)

			val updated = toUpdate.map { case (year, normalLoad) =>
				val load = originalNormalLoads.getOrElse(route, Map()).getOrElse(year, new NormalCATSLoad(academicYear, route, year, BigDecimal(normalLoad)))
				load.normalLoad = BigDecimal(normalLoad)
				load
			}
			updated.foreach(normalCATSLoadService.saveOrUpdate)

			(removed, updated)
		}

		ManageNormalCATSLoadCommand.Result(
			loads.flatMap { case (_, updated) => updated },
			loads.flatMap { case (removed, _) => removed }
		)
		
	}

}

trait PopulatesManageNormalCATSLoadCommand extends PopulateOnForm {

	self: CourseAndRouteServiceComponent with ManageNormalCATSLoadCommandState
		with ManageNormalCATSLoadCommandRequest with NormalCATSLoadServiceComponent =>

	def populate(): Unit = {
		originalNormalLoads.foreach { case (route, yearMap) =>
			normalLoads.put(route, JHashMap(yearMap.map { case (year, load) => JInteger(Option(year)) -> JBigDecimal(Option(load.normalLoad)) }))
		}
	}

}

trait ManageNormalCATSLoadValidation extends SelfValidating {

	self: ManageNormalCATSLoadCommandRequest with ManageNormalCATSLoadCommandState =>

	override def validate(errors: Errors) {
		normalLoads.asScala.foreach { case (route, yearOfStudyMap) =>
			if (!allRoutes.contains(route)) {
				errors.reject("examGrids.normalCATSLoad.route.invalid", Array(route.code.toUpperCase), "")
			}
			yearOfStudyMap.asScala.foreach { case (yearOfStudy, jNormalLoad) =>
				Option(jNormalLoad).foreach(normalLoad =>
					if (BigDecimal(normalLoad) <= 0) {
						errors.rejectValue("normalLoads", "examGrids.normalCATSLoad.normalLoad.invalid", Array(route.code.toUpperCase, yearOfStudy.toString), "")
					}
				)
			}
		}
	}

}

trait ManageNormalCATSLoadPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ManageNormalCATSLoadCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(ManageNormalCATSLoadCommand.RequiredPermission, department)
	}

}

trait ManageNormalCATSLoadDescription extends Describable[ManageNormalCATSLoadCommand.Result] {

	self: ManageNormalCATSLoadCommandState =>

	override lazy val eventName = "ManageNormalCATSLoad"

	override def describe(d: Description) {
		d.department(department)
	}

	override def describeResult(d: Description, result: ManageNormalCATSLoadCommand.Result): Unit = {
		d.property("updated", result.updated.map(load => Map(
			"route" -> load.route.code,
			"academicYear" -> load.academicYear,
			"yearOfStudy" -> load.yearOfStudy,
			"normalLoad" -> load.normalLoad
		)))
		d.property("removed", result.removed.map(load => Map(
			"route" -> load.route.code,
			"academicYear" -> load.academicYear,
			"yearOfStudy" -> load.yearOfStudy,
			"normalLoad" -> load.normalLoad
		)))
	}
}

trait ManageNormalCATSLoadCommandState {

	self: CourseAndRouteServiceComponent with NormalCATSLoadServiceComponent =>

	def department: Department
	def academicYear: AcademicYear

	lazy val allRoutes: Seq[Route] = courseAndRouteService.findRoutesInDepartment(department).sorted
	def getNormalLoads(thisAcademicYear: AcademicYear): Map[Route, Map[YearOfStudy, Option[NormalCATSLoad]]] = {
		val loads = normalCATSLoadService.findAll(allRoutes, thisAcademicYear)

		val yearsOfStudy = 1 to FilterStudentsOrRelationships.MaxYearsOfStudy
		allRoutes.map(route => route ->
			yearsOfStudy.map(year => year ->
				loads.find(NormalCATSLoad.find(route, thisAcademicYear, year))
			).toMap
		).toMap
	}
	lazy val originalNormalLoads: Map[Route, Map[YearOfStudy, NormalCATSLoad]] = {
		getNormalLoads(academicYear).mapValues(_.filter { case (_, loadOption) => loadOption.isDefined }.mapValues(_.get))
			.filter { case (_, yearMap) => yearMap.nonEmpty }
	}
}

trait ManageNormalCATSLoadCommandRequest {
	val normalLoads: JMap[Route, JMap[JInteger, JBigDecimal]] = LazyMaps.create{_: Route => JMap[JInteger, JBigDecimal]() }.asJava
}
