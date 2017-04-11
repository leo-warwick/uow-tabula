package uk.ac.warwick.tabula.commands

import org.hibernate.criterion.{Restrictions, Projections, DetachedCriteria}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.{HibernateHelpers, AliasAndJoinType, ScalaRestriction}
import uk.ac.warwick.tabula.data.ScalaRestriction._
import uk.ac.warwick.tabula.data.model._

import scala.collection.JavaConverters._

object FiltersRelationships {
	val AliasPaths: Map[String, Seq[(String, AliasAndJoinType)]] = Seq(
		"studentCourseYearDetails" -> Seq(
			"studentCourseYearDetails" -> AliasAndJoinType("studentCourseYearDetails")
		),
		"moduleRegistration" -> Seq(
			"_moduleRegistrations" -> AliasAndJoinType("moduleRegistration")
		),
		"course" -> Seq(
			"course" -> AliasAndJoinType("course")
		),
		"currentRoute" -> Seq(
			"currentRoute" -> AliasAndJoinType("currentRoute")
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
		"currentRoute.code", routes.asScala.map {_.code},
		getAliasPaths("currentRoute") : _*
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

	protected override def latestYearDetailsForYear(year: AcademicYear): DetachedCriteria =
		DetachedCriteria.forClass(classOf[StudentCourseYearDetails], "latestSCYD")
			.setProjection(Projections.max("latestSCYD.sceSequenceNumber"))
			.add(HibernateHelpers.is("latestSCYD.academicYear", year))
			.add(Restrictions.eqProperty("latestSCYD.studentCourseDetails.scjCode", "this.scjCode"))
}
