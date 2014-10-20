package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.data.{AliasAndJoinType, ScalaRestriction}
import uk.ac.warwick.tabula.data.ScalaRestriction._
import uk.ac.warwick.tabula.data.model._

import scala.collection.JavaConverters._

object FiltersRelationships {
	val AliasPaths: Map[String, Seq[(String, AliasAndJoinType)]] = Seq(
		"studentCourseYearDetails" -> Seq(
			"latestStudentCourseYearDetails" -> AliasAndJoinType("studentCourseYearDetails")
		),
		"moduleRegistration" -> Seq(
			"_moduleRegistrations" -> AliasAndJoinType("moduleRegistration")
		),
		"course" -> Seq(
			"course" -> AliasAndJoinType("course")
		),
		"route" -> Seq(
			"route" -> AliasAndJoinType("route")
		),
		"statusOnRoute" -> Seq(
			"statusOnRoute" -> AliasAndJoinType("statusOnRoute")
		),
		"department" -> Seq(
			"route" -> AliasAndJoinType("route"),
			"route.adminDepartment" -> AliasAndJoinType("department")
		)
	).toMap

	val MaxStudentsPerPage = FilterStudentsOrRelationships.MaxStudentsPerPage
	val DefaultStudentsPerPage = FilterStudentsOrRelationships.DefaultStudentsPerPage
}
trait FiltersRelationships extends FilterStudentsOrRelationships {
	import uk.ac.warwick.tabula.commands.FiltersRelationships._

	def routeRestriction: Option[ScalaRestriction] = inIfNotEmpty(
		"route.code", routes.asScala.map {_.code},
		getAliasPaths("route") : _*
	)

	def sprStatusRestriction: Option[ScalaRestriction] = inIfNotEmpty(
		"statusOnRoute", sprStatuses.asScala,
		getAliasPaths("statusOnRoute") : _*
	)

	def allDepartments: Seq[Department]
	def allRoutes: Seq[Route]

	override def getAliasPaths(sitsTable: String) = AliasPaths(sitsTable)

	// Do we need to consider out-of-department modules/routes or can we rely on users typing them in manually?
	lazy val allModules: Seq[Module] = allDepartments.map(modulesForDepartmentAndSubDepartments).flatten
	lazy val allCourseTypes: Seq[CourseType] = CourseType.all
	lazy val allSprStatuses: Seq[SitsStatus] = allDepartments.map(dept => profileService.allSprStatuses(dept.rootDepartment)).flatten.distinct
	lazy val allModesOfAttendance: Seq[ModeOfAttendance] = allDepartments.map(profileService.allModesOfAttendance).flatten.distinct

}
