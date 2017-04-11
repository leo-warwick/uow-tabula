package uk.ac.warwick.tabula.data.model

import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

import scala.collection.mutable

/**
 * For storing comma-separated strings in Hibernate.
 *
 * Doesn't handle values with commas in them so this is not appropriate
 * for user-inputted data.
 */
class StringListUserType extends AbstractBasicUserType[Seq[String], String] {

	val separator = ","
	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = Nil

	override def convertToObject(string: String): mutable.WrappedArray[String] = string.split(separator)
	override def convertToValue(list: Seq[String]): String = if (list.isEmpty) null else list.mkString(separator)

}