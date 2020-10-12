package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.JavaImports._

@Entity
@Proxy
@Access(AccessType.FIELD)
class ModuleTeachingInformation extends GeneratedId with Serializable {

  def this(m: Module, d: Department, p: JBigDecimal) = {
    this()
    module = m
    department = d
    percentage = p
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  var department: Department = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "module_id")
  var module: Module = _

  var percentage: JBigDecimal = null

  override def toString = s"ModuleTeachingInformation[${module} -> ${department} (${percentage}%)]"

}
