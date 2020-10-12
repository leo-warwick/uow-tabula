package uk.ac.warwick.tabula.data.model

import java.util.regex.Pattern

import javax.persistence._
import org.hibernate.annotations.Proxy

/**
  * Tabula store for a Formed Module Entity (CAM_FME) from SITS.
  */
@Entity
@Proxy
class UpstreamModuleListEntry extends GeneratedId {

  def this(list: UpstreamModuleList, globString: String) = {
    this()
    this.list = list
    setMatchString(globString)
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "listcode", referencedColumnName = "code")
  var list: UpstreamModuleList = _

  @Column(name = "matchstring")
  private var _matchString: String = _

  def setMatchString(globString: String): Unit = {
    _matchString = "^%s".format(globString.replace(".", "\\.").replace("?", ".").replace("*", ".*"))
  }

  def pattern: Pattern = Pattern.compile(_matchString)

}
