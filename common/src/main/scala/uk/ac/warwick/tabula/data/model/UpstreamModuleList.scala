package uk.ac.warwick.tabula.data.model

import javax.persistence._

import org.hibernate.annotations.Type
import org.hibernate.annotations.BatchSize
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConverters._

/**
	* Tabula store for a Formed Module Collection (CAM_FMC) from SITS.
	*/
@Entity
class UpstreamModuleList {

	def this(code: String, academicYear: AcademicYear, route: Route, yearOfStudy: Integer) {
		this()
		this.code = code
		this.academicYear = academicYear
		this.route = route
		this.yearOfStudy = yearOfStudy
	}

	@Id
	var code: String = _
	def id: String = code

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	var academicYear: AcademicYear = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "routeCode", referencedColumnName="code")
	var route: Route = _

	var yearOfStudy: JInteger = _

	@OneToMany(mappedBy = "list", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=200)
	val entries: JSet[UpstreamModuleListEntry] = JHashSet()

	def matches(moduleCode: String): Boolean = entries.asScala.exists(_.pattern.matcher(moduleCode).matches())

}