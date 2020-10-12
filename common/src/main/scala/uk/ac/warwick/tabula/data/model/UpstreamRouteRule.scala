package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntityYear
import uk.ac.warwick.tabula.services.LevelService
import uk.ac.warwick.tabula.services.exams.grids.UpstreamRouteRuleService

import scala.collection.mutable
import scala.jdk.CollectionConverters._

/**
  * Tabula store for a Pathway Module Rule (CAM_PMR) from SITS.
  * Pathways and Routes are synonymous.
  */
@Entity
@Proxy
class UpstreamRouteRule extends GeneratedId {

  def this(academicYear: Option[AcademicYear], route: Route, levelCode: String) = {
    this()
    this.academicYear = academicYear
    this.route = route
    this.levelCode = levelCode
  }

  /**
    * If the academic year is empty, the rule applies to every academic year
    */
  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(name = "academicYear")
  private var _academicYear: AcademicYear = _

  def academicYear_=(academicYearOption: Option[AcademicYear]): Unit = {
    _academicYear = academicYearOption.orNull
  }

  def academicYear: Option[AcademicYear] = Option(_academicYear)

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "routeCode", referencedColumnName = "code")
  var route: Route = _

  @transient
  var levelService: LevelService = Wire.auto[LevelService]

  var levelCode: String = _

  def level: Option[Level] = levelService.levelFromCode(levelCode)

  @OneToMany(mappedBy = "rule", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @BatchSize(size = 200)
  val entries: JSet[UpstreamRouteRuleEntry] = JHashSet()

  def passes(moduleRegistrations: Seq[ModuleRegistration]): Boolean = {
    entries.asScala.forall(_.passes(moduleRegistrations))
  }

}

case class UpstreamRouteRuleLookup(upstreamRouteRuleService: UpstreamRouteRuleService) {
  private val cache = mutable.Map[(Route, Level, AcademicYear), Seq[UpstreamRouteRule]]()

  def apply(route: Route, academicYear: AcademicYear, level: Option[Level]): Seq[UpstreamRouteRule] = level.map(l =>
    cache.get((route, l, academicYear)) match {
      case Some(rules) => rules
      case _ =>
        cache.put((route, l, academicYear), upstreamRouteRuleService.list(route, academicYear, l))
        cache((route, l, academicYear))
    }).getOrElse(Seq())

  def apply(year: ExamGridEntityYear): Seq[UpstreamRouteRule] = apply(year.route, year.baseAcademicYear, year.level)
}
