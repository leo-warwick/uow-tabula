package uk.ac.warwick.tabula.data.model

import javax.persistence.{Entity, Id, NamedQueries, NamedQuery}
import org.hibernate.annotations.Proxy
import org.joda.time.DateTime

object SitsStatus {
  // The reason this method isn't on SitsStatus is that P* can have a meaning other than
  // permanently withdrawn in the context of applicants, but not in the context of
  // the student's route status (sprStatus)
  def isPermanentlyWithdrawnStatusOnRoute(status: SitsStatus): Boolean =
    status.code.startsWith("P") || // Permanently withdrawn
    status.code.startsWith("D")    // Deceased

  // The reason this method isn't on SitsStatus is that P* can have a meaning other than
  // permanently withdrawn in the context of applicants, but not in the context of
  // the student's route status (sprStatus)
  def isWithdrawnStatusOnRoute(status: SitsStatus): Boolean =
    status.code.startsWith("P") || // Permanently withdrawn
    status.code.startsWith("T") || // Temporarily withdrawn
    status.code.startsWith("D")    // Deceased
}

@Entity
@Proxy
@NamedQueries(Array(
  new NamedQuery(name = "status.code", query = "select sitsStatus from SitsStatus sitsStatus where code = :code")))
class SitsStatus {

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

}
