package uk.ac.warwick.tabula.data.model

import javax.persistence._

import org.hibernate.annotations.{BatchSize, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.permissions.PermissionsTarget

import scala.collection.JavaConverters._
import scala.collection.mutable

@Entity
@NamedQueries(Array(
	new NamedQuery(name = "route.code", query = "select r from Route r where code = :code"),
	new NamedQuery(name = "route.adminDepartment", query = "select r from Route r where adminDepartment = :adminDepartment")))
class Route extends GeneratedId with Serializable with PermissionsTarget {

	def this(code: String = null, adminDepartment: Department = null) {
		this()
		this.code = code
		this.adminDepartment = adminDepartment
	}

	@Column(unique=true)
	var code: String = _

	var name: String = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "department_id")
	var adminDepartment: Department = _

	@deprecated("TAB-2589 to be explicit, this should use adminDepartment or teachingDepartments", "84")
	def department: Department = adminDepartment

	@deprecated("TAB-2589 to be explicit, this should use adminDepartment or teachingDepartments", "84")
	def department_=(d: Department) { adminDepartment = d }

	@OneToMany(mappedBy = "route", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=200)
	var teachingInfo: JSet[RouteTeachingInformation] = JHashSet()

	def teachingDepartments: mutable.Set[Department] =
		if (teachingDepartmentsActive)
			teachingInfo.asScala.map { _.department } + adminDepartment
		else
			mutable.Set(adminDepartment)

	@Type(`type` = "uk.ac.warwick.tabula.data.model.DegreeTypeUserType")
	var degreeType: DegreeType = _

	var active: Boolean = _

	override def toString: String = "Route[" + code + "]"

	def permissionsParents: Stream[Department] = teachingDepartments.toStream
	override def humanReadableId: String = code.toUpperCase + " " + name
	override def urlSlug: String = code

	var missingFromImportSince: DateTime = _

	var teachingDepartmentsActive: Boolean = false

}

trait HasRoute {
	def route: Route
}

object Route {
	// For sorting a collection by route code. Either pass to the sort function,
	// or expose as an implicit val.
	val CodeOrdering: Ordering[Route] = Ordering.by[Route, String] ( _.code )
	val NameOrdering: Ordering[Route] = Ordering.by { route: Route => (route.name, route.code) }
	val DegreeTypeOrdering: Ordering[Route] = Ordering.by { route: Route => (Option(route.degreeType), route.code) }

	// Companion object is one of the places searched for an implicit Ordering, so
	// this will be the default when ordering a list of routes.
	implicit val defaultOrdering = CodeOrdering
}