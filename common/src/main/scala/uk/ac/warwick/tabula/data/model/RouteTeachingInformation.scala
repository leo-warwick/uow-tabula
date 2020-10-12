package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.JavaImports._

@Entity
@Proxy
@Access(AccessType.FIELD)
class RouteTeachingInformation extends GeneratedId with Serializable {

  def this(r: Route, d: Department, p: JBigDecimal) = {
    this()
    route = r
    department = d
    percentage = p
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  var department: Department = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "route_id")
  var route: Route = _

  var percentage: JBigDecimal = null

  override def toString = s"RouteTeachingInformation[${route} -> ${department} (${percentage}%)]"

}
