package uk.ac.warwick.tabula.data.model

import javax.persistence._

import org.hibernate.annotations.{BatchSize, Type}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.services.exams.grids.UpstreamRouteRuleService

import collection.JavaConverters._
import scala.collection.mutable

/**
	* Tabula store for a Pathway Module Rule (CAM_PMR) from SITS.
	* Pathways and Routes are synonymous.
	*/
@Entity
class UpstreamRouteRule extends GeneratedId {

	def this(academicYear: Option[AcademicYear], route: Route, yearOfStudy: Integer) {
		this()
		this.academicYear = academicYear
		this.route = route
		this.yearOfStudy = yearOfStudy
	}

	/**
		* If the academic year is empty, the rule applies to every academic year
		*/
	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Column(name="academicYear")
	private var _academicYear: AcademicYear = _
	def academicYear_=(academicYearOption: Option[AcademicYear]): Unit = {
		_academicYear = academicYearOption.orNull
	}
	def academicYear: Option[AcademicYear] = Option(_academicYear)

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "routeCode", referencedColumnName="code")
	var route: Route = _

	var yearOfStudy: JInteger = _

	@OneToMany(mappedBy = "rule", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=200)
	val entries: JSet[UpstreamRouteRuleEntry] = JHashSet()

	def passes(moduleRegistrations: Seq[ModuleRegistration]): Boolean = {
		entries.asScala.forall(_.passes(moduleRegistrations))
	}

}

class UpstreamRouteRuleLookup(academicYear: AcademicYear, yearOfStudy: YearOfStudy, upstreamRouteRuleService: UpstreamRouteRuleService) {
	private val cache = mutable.Map[Route, Seq[UpstreamRouteRule]]()
	def apply(route: Route): Seq[UpstreamRouteRule] = cache.get(route) match {
		case Some(rules) =>
			rules
		case _ =>
			cache.put(route, upstreamRouteRuleService.list(route, academicYear, yearOfStudy))
			cache(route)
	}
}
