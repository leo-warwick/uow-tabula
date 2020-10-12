package uk.ac.warwick.tabula.data.model

import javax.persistence.{Entity, Id, NamedQueries, NamedQuery}
import org.hibernate.annotations.Proxy
import org.joda.time.DateTime

@Entity
@Proxy
@NamedQueries(Array(
  new NamedQuery(name = "modeofattendance.code", query = "select modeOfAttendance from ModeOfAttendance modeOfAttendance where code = :code")))
class ModeOfAttendance {

  def this(code: String = null, shortName: String = null, fullName: String = null) = {
    this()
    this.code = code
    this.shortName = shortName
    this.fullName = fullName
  }

  @Id var code: String = _
  var shortName: String = _
  var fullName: String = _

  var lastUpdatedDate: DateTime = DateTime.now

  override def toString: String = fullName.toLowerCase()

  def fullNameAliased: String = {
    if (code.equals("F"))
      "Full-time"
    else fullName
  }

  // codes are same that cognos used
  def yearAbroad: Boolean =
    List("YO", "SW", "YOE", "SWE", "YM", "YME", "YV").contains(code)

}
