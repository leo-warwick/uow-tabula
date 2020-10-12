package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.JavaImports._

/**
  * Tabula store for a Pathway Module Rule Entry (CAM_PMB) from SITS.
  */
@Entity
@Proxy
class UpstreamRouteRuleEntry extends GeneratedId {

  def this(
    rule: UpstreamRouteRule,
    list: UpstreamModuleList,
    minCats: Option[BigDecimal],
    maxCats: Option[BigDecimal],
    minModules: Option[Int],
    maxModules: Option[Int]
  ) = {
    this()
    this.rule = rule
    this.list = list
    this.minCats = minCats
    this.maxCats = maxCats
    this.minModules = minModules
    this.maxModules = maxModules
  }

  @ManyToOne(fetch = FetchType.LAZY)
  var rule: UpstreamRouteRule = _

  @OneToOne(cascade = Array(CascadeType.REFRESH), fetch = FetchType.EAGER)
  @JoinColumn(name = "listcode", referencedColumnName = "code")
  var list: UpstreamModuleList = _

  @Column(name = "min_cats")
  private var _minCats: JBigDecimal = null

  def minCats_=(minCatsOption: Option[BigDecimal]): Unit = {
    _minCats = minCatsOption.map(_.underlying).orNull
  }

  def minCats: Option[BigDecimal] = Option(_minCats).map(BigDecimal.apply)

  @Column(name = "max_cats")
  private var _maxCats: JBigDecimal = null

  def maxCats_=(maxCatsOption: Option[BigDecimal]): Unit = {
    _maxCats = maxCatsOption.map(_.underlying).orNull
  }

  def maxCats: Option[BigDecimal] = Option(_maxCats).map(BigDecimal.apply)

  @Column(name = "min_modules")
  private var _minModules: JInteger = null

  def minModules_=(minModulesOption: Option[Int]): Unit = {
    _minModules = JInteger(minModulesOption)
  }

  def minModules: Option[Int] = if (_minModules == null) None else Option(_minModules)

  @Column(name = "max_modules")
  private var _maxModules: JInteger = null

  def maxModules_=(maxModulesOption: Option[Int]): Unit = {
    _maxModules = JInteger(maxModulesOption)
  }

  def maxModules: Option[Int] = if (_maxModules == null) None else Option(_maxModules)

  def passes(moduleRegistrations: Seq[ModuleRegistration]): Boolean = {
    val registrationsInList = moduleRegistrations.filter(mr => list.matches(mr.sitsModuleCode))
    val cats = registrationsInList.map(mr => mr.safeCats.getOrElse(BigDecimal(0))).sum
    val quantity = registrationsInList.size
    (minCats.isEmpty || minCats.get <= cats) &&
      (maxCats.isEmpty || maxCats.get >= cats) &&
      (minModules.isEmpty || minModules.get <= quantity) &&
      (maxModules.isEmpty || maxModules.get >= quantity)
  }

}
