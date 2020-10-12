package uk.ac.warwick.tabula.data.model

import javax.persistence.{CascadeType, Entity, _}
import org.hibernate.annotations.{Proxy, NamedQueries => _, NamedQuery => _, _}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.permissions.PermissionsTarget

import scala.collection.mutable
import scala.jdk.CollectionConverters._

@Entity
@Proxy
@NamedQueries(Array(
  new NamedQuery(name = "route.code", query = "select r from Route r where code = :code"),
  new NamedQuery(name = "route.adminDepartment", query = "select r from Route r where adminDepartment = :adminDepartment")))
class Route extends GeneratedId with Serializable with PermissionsTarget {

  def this(code: String = null, adminDepartment: Department = null) = {
    this()
    this.code = code
    this.adminDepartment = adminDepartment
  }

  @Column(unique = true)
  var code: String = _

  var name: String = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  var adminDepartment: Department = _

  @deprecated("TAB-2589 to be explicit, this should use adminDepartment or teachingDepartments", "84")
  def department: Department = adminDepartment

  @deprecated("TAB-2589 to be explicit, this should use adminDepartment or teachingDepartments", "84")
  def department_=(d: Department): Unit = {
    adminDepartment = d
  }

  @OneToMany(mappedBy = "route", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @BatchSize(size = 200)
  var teachingInfo: JSet[RouteTeachingInformation] = JHashSet()

  @OneToMany(mappedBy = "route", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @BatchSize(size = 200)
  var upstreamModuleLists: JSet[UpstreamModuleList] = JHashSet()

  def namedOptions(yearOfStudy: YearOfStudy, academicYear: AcademicYear, moduleRegistrations: Seq[ModuleRegistration]): Seq[ModuleRegistration] = {
    val namedModuleLists = upstreamModuleLists.asScala.filter(uml => !uml.genericAndUnusualOptions && uml.academicYear == academicYear && uml.yearOfStudy == yearOfStudy)
    moduleRegistrations.filter(mr => namedModuleLists.exists(_.matches(mr.sitsModuleCode)))
  }

  def teachingDepartments: mutable.Set[Department] =
    if (teachingDepartmentsActive)
      teachingInfo.asScala.map(_.department) + adminDepartment
    else
      mutable.Set(adminDepartment)

  @Type(`type` = "uk.ac.warwick.tabula.data.model.DegreeTypeUserType")
  var degreeType: DegreeType = _

  var active: Boolean = _

  override def toString: String = "Route[" + code + "]"

  def permissionsParents: LazyList[Department] = teachingDepartments.to(LazyList)

  override def humanReadableId: String = code.toUpperCase + " " + name

  override def urlSlug: String = code

  var missingFromImportSince: DateTime = _

  var teachingDepartmentsActive: Boolean = false

  var sitsDepartmentCode: String = _

}

trait HasRoute {
  def route: Route
}

object Route {
  // For sorting a collection by route code. Either pass to the sort function,
  // or expose as an implicit val.
  val CodeOrdering: Ordering[Route] = Ordering.by[Route, String](_.code)
  val NameOrdering: Ordering[Route] = Ordering.by { route: Route => (route.name, route.code) }
  val DegreeTypeOrdering: Ordering[Route] = Ordering.by { route: Route => (Option(route.degreeType), route.code) }

  // Companion object is one of the places searched for an implicit Ordering, so
  // this will be the default when ordering a list of routes.
  implicit val defaultOrdering: Ordering[Route] = CodeOrdering

}
