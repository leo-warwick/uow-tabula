package uk.ac.warwick.tabula.data.model

import java.sql.Types

import javax.persistence.{Entity, Id}
import org.hibernate.`type`.{StandardBasicTypes, StringType}
import org.hibernate.annotations.Proxy
import org.joda.time.DateTime

sealed abstract class DisabilityFundingStatus(val code: String, val description: String)

object Disability {
  val notReportableDefinition = "NONE"

  object FundingStatus {

    case object InReceipt extends DisabilityFundingStatus("4", "In receipt of DSA")

    case object NotInReceipt extends DisabilityFundingStatus("5", "Not in receipt of DSA")

    case object Unknown extends DisabilityFundingStatus("9", "Information about DSA is not known/not sought")

    def fromCode(code: String): DisabilityFundingStatus = code match {
      case "4" => InReceipt
      case "5" => NotInReceipt
      case "9" => Unknown
      case _ => null
    }
  }

}

class DisabilityFundingStatusUserType extends AbstractBasicUserType[DisabilityFundingStatus, String] {

  val basicType: StringType = StandardBasicTypes.STRING

  override def sqlTypes = Array(Types.VARCHAR)

  val nullValue: String = null
  val nullObject: DisabilityFundingStatus = null

  override def convertToObject(string: String): DisabilityFundingStatus = Disability.FundingStatus.fromCode(string)

  override def convertToValue(status: DisabilityFundingStatus): String = status.code

}

@Entity
@Proxy
class Disability {
  def this(code: String = null, definition: String = null) = {
    this()
    this.code = code
    this.sitsDefinition = definition
  }

  @Id var code: String = _
  var shortName: String = _
  var sitsDefinition: String = _
  var tabulaDefinition: String = _
  var lastUpdatedDate: DateTime = DateTime.now

  /**
    * Use our override out of preference, where available.
    * It's manually maintained in the database.
    */
  def definition: String = Option(tabulaDefinition).getOrElse(sitsDefinition)

  /**
    * If tabulaDefinition is null (ie. there's been a new SITS definition added), fail safe to true.
    * Otherwise, true only for tabulaDefinitions we have defined as not reportably disabled in the database
    */
  def reportable: Boolean = !Option(tabulaDefinition).contains(Disability.notReportableDefinition)

  override def toString: String = definition
}
