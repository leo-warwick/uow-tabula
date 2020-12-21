package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.{Formula, Proxy}
import org.springframework.util.StringUtils
import uk.ac.warwick.tabula.ToString

sealed abstract class AddressType(val dbValue: String) extends Convertible[String] {
  def value: String = dbValue
}

object AddressType {

  case object Home extends AddressType("H")

  case object TermTime extends AddressType("C")

  implicit def factory: (String) => AddressType = fromCode _

  def fromCode(code: String): AddressType = code match {
    case Home.dbValue => Home
    case TermTime.dbValue => TermTime
    case null => null
    case _ => throw new IllegalArgumentException()
  }
}

@Entity
@Proxy
class Address extends GeneratedId with ToString {
  var line1: String = _
  var line2: String = _
  var line3: String = _
  var line4: String = _
  var line5: String = _
  var postcode: String = _
  var telephone: String = _

  @Formula("upper(regexp_replace(postcode, '[^A-Za-z0-9]', '', 'g'))")
  private var _normalisedPostcode: String = _

  @transient var addressType: AddressType = null

  def isEmpty: Boolean = {
    !(StringUtils.hasText(line1) || StringUtils.hasText(line2) || StringUtils.hasText(line3) ||
      StringUtils.hasText(line4) || StringUtils.hasText(line5) || StringUtils.hasText(postcode) ||
      StringUtils.hasText(telephone))
  }

  def toStringProps = Seq(
    "line1" -> line1,
    "line2" -> line2,
    "line3" -> line3,
    "line4" -> line4,
    "line5" -> line5,
    "postcode" -> postcode)

}

class AddressTypeUserType extends ConvertibleStringUserType[AddressType]
