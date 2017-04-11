package uk.ac.warwick.tabula.data.model

import java.sql.Types

import org.hibernate.`type`.StandardBasicTypes
import uk.ac.warwick.tabula.JavaImports._

class OptionStringUserType extends AbstractBasicUserType[Option[String], String] {
	def convertToObject(input: String) = Option(input)
	def convertToValue(obj: Option[String]): String = obj.orNull
	def sqlTypes = Array(Types.VARCHAR)
	val basicType = StandardBasicTypes.STRING
	val nullValue = null
	val nullObject = None
}

class OptionIntegerUserType extends AbstractBasicUserType[Option[Int], JInteger] {
	def convertToObject(input: JInteger) = Option(input)
	def convertToValue(obj: Option[Int]) = JInteger(obj)
	def sqlTypes = Array(Types.INTEGER)
	val basicType = StandardBasicTypes.INTEGER
	val nullValue = null
	val nullObject = None
}

class OptionBooleanUserType extends AbstractBasicUserType[Option[Boolean], JBoolean] {
	def convertToObject(input: JBoolean) = Option(input)
	def convertToValue(obj: Option[Boolean]) = JBoolean(obj)
	def sqlTypes = Array(Types.BOOLEAN)
	val basicType = StandardBasicTypes.BOOLEAN
	val nullValue = null
	val nullObject = None
}

class OptionBigDecimalUserType extends AbstractBasicUserType[Option[BigDecimal], JBigDecimal] {
	def convertToObject(input: JBigDecimal) = Option(input)
	def convertToValue(obj: Option[BigDecimal]) = JBigDecimal(obj)
	def sqlTypes = Array(Types.NUMERIC)
	val basicType = StandardBasicTypes.BIG_DECIMAL
	val nullValue = null
	val nullObject = None
}