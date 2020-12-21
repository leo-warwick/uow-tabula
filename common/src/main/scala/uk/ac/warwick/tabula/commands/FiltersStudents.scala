package uk.ac.warwick.tabula.commands

import org.hibernate.criterion.{DetachedCriteria, Projections, Restrictions}
import org.hibernate.sql.JoinType
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.ScalaRestriction._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AliasAndJoinType, HibernateHelpers, ScalaRestriction}

import scala.jdk.CollectionConverters._

object FiltersStudents {
  val AliasPaths: Map[String, Seq[(String, AliasAndJoinType)]] = Seq(
    "mostSignificantCourse" -> Seq(
      "mostSignificantCourse" -> AliasAndJoinType("mostSignificantCourse")
    ),
    "studentCourseYearDetails" -> Seq(
      "mostSignificantCourse" -> AliasAndJoinType("mostSignificantCourse"),
      "mostSignificantCourse.studentCourseYearDetails" -> AliasAndJoinType("studentCourseYearDetails")
    ),
    "latestStudentCourseYearDetails" -> Seq(
      "mostSignificantCourse" -> AliasAndJoinType("mostSignificantCourse"),
      "mostSignificantCourse.latestStudentCourseYearDetails" -> AliasAndJoinType("latestStudentCourseYearDetails")
    ),
    "moduleRegistration" -> Seq(
      "mostSignificantCourse" -> AliasAndJoinType("mostSignificantCourse"),
      "mostSignificantCourse._moduleRegistrations" -> AliasAndJoinType("moduleRegistration")
    ),
    "course" -> Seq(
      "mostSignificantCourse" -> AliasAndJoinType("mostSignificantCourse"),
      "mostSignificantCourse.course" -> AliasAndJoinType("course")
    ),
    "route" -> Seq(
      "mostSignificantCourse" -> AliasAndJoinType("mostSignificantCourse"),
      "mostSignificantCourse.currentRoute" -> AliasAndJoinType("route")
    ),
    "department" -> Seq(
      "mostSignificantCourse" -> AliasAndJoinType("mostSignificantCourse"),
      "mostSignificantCourse.currentRoute" -> AliasAndJoinType("route"),
      "route.adminDepartment" -> AliasAndJoinType("department")
    ),
    "teachingInfo" -> Seq(
      "mostSignificantCourse" -> AliasAndJoinType("mostSignificantCourse"),
      "mostSignificantCourse.currentRoute" -> AliasAndJoinType("route"),
      "route.teachingInfo" -> AliasAndJoinType("teachingInfo", JoinType.LEFT_OUTER_JOIN)
    ),
    "allRelationships" -> Seq(
      "mostSignificantCourse" -> AliasAndJoinType("mostSignificantCourse"),
      "mostSignificantCourse.allRelationships" -> AliasAndJoinType("allRelationships")
    ),
    "statusOnRoute" -> Seq(
      "mostSignificantCourse" -> AliasAndJoinType("mostSignificantCourse"),
      "mostSignificantCourse.statusOnRoute" -> AliasAndJoinType("statusOnRoute")
    ),
    "termtimeAddress" -> Seq(
      "termtimeAddress" -> AliasAndJoinType("termtimeAddress")
    ),
    "currentAddress" -> Seq(
      "currentAddress" -> AliasAndJoinType("currentAddress")
    )
  ).toMap

  val MaxStudentsPerPage = FilterStudentsOrRelationships.MaxStudentsPerPage
  val DefaultStudentsPerPage = FilterStudentsOrRelationships.DefaultStudentsPerPage
}

trait FiltersStudents extends FilterStudentsOrRelationships {

  import FiltersStudents._

  def department: Department

  def routeRestriction: Option[ScalaRestriction] = inIfNotEmpty(
    "mostSignificantCourse.currentRoute.code", routes.asScala.map(mandatory(_)).map(_.code),
    getAliasPaths("mostSignificantCourse"): _*
  )

  def sprStatusRestriction: Option[ScalaRestriction] = inIfNotEmpty(
    "mostSignificantCourse.statusOnRoute", sprStatuses.asScala,
    getAliasPaths("mostSignificantCourse"): _*
  )

  def levelCodeRestriction: Option[ScalaRestriction] = inIfNotEmpty(
    "mostSignificantCourse.levelCode", levelCodes.asScala,
    getAliasPaths("mostSignificantCourse"): _*
  )

  def isFinalistRestriction: Option[ScalaRestriction] = isPropertyIfTicked(
    "studentCourseYearDetails.yearOfStudy", "mostSignificantCourse.courseYearLength",
    otherCriteria.contains("Finalist"),
    getAliasPaths("studentCourseYearDetails") ++ getAliasPaths("mostSignificantCourse"): _*
  )

  def isNotFinalistRestriction: Option[ScalaRestriction] = isNotPropertyIfTicked(
    "studentCourseYearDetails.yearOfStudy", "mostSignificantCourse.courseYearLength",
    otherCriteria.contains("Not Finalist"),
    getAliasPaths("studentCourseYearDetails") ++ getAliasPaths("mostSignificantCourse"): _*
  )

  override def getAliasPaths(sitsTable: String) = AliasPaths(sitsTable)

  override protected def latestYearDetailsForYear(year: AcademicYear): DetachedCriteria =
    DetachedCriteria.forClass(classOf[StudentCourseYearDetails], "latestSCYD")
      .setProjection(Projections.max("latestSCYD.sceSequenceNumber"))
      .add(HibernateHelpers.is("latestSCYD.academicYear", year))
      .add(Restrictions.eqProperty("latestSCYD.studentCourseDetails.scjCode", "mostSignificantCourse.scjCode"))

  lazy val allModules: Seq[Module] = ((modulesForDepartmentAndSubDepartments(mandatory(department)) match {
    case Nil => modulesForDepartmentAndSubDepartments(mandatory(department.rootDepartment))
    case modules => modules
  }) ++ modules.asScala).distinct.sorted
  lazy val allRoutes: Seq[Route] = ((routesForDepartmentAndSubDepartments(mandatory(department)) match {
    case Nil => routesForDepartmentAndSubDepartments(mandatory(department.rootDepartment))
    case routes => routes
  }) ++ routes.asScala).distinct.sorted(Route.DegreeTypeOrdering)

  lazy val allCourseTypes: Seq[CourseType] = mandatory(department).filterRule.courseTypes
  lazy val allSprStatuses: Seq[SitsStatus] = profileService.allSprStatuses(mandatory(department).rootDepartment)
  lazy val allModesOfAttendance: Seq[ModeOfAttendance] = profileService.allModesOfAttendance(mandatory(department).rootDepartment)
}
