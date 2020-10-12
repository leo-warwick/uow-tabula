package uk.ac.warwick.tabula.data.model

import javax.persistence.{Entity, Id}
import org.hibernate.annotations.Proxy
import org.joda.time.DateTime

@Entity
@Proxy
class Award {

  def this(code: String = null, name: String = null) = {
    this()
    this.code = code
    this.name = name
  }

  @Id var code: String = _
  var shortName: String = _
  var name: String = _

  var lastUpdatedDate: DateTime = DateTime.now

  override def toString: String = name

}
