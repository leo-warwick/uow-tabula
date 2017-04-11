package uk.ac.warwick.tabula.data.model

import org.springframework.util.StringUtils
import javax.persistence._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

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
class Address extends GeneratedId with ToString {
	var line1: String = _
	var line2: String = _
	var line3: String = _
	var line4: String = _
	var line5: String = _
	var postcode: String = _
	var telephone: String = _

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