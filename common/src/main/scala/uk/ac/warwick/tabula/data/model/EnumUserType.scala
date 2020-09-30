package uk.ac.warwick.tabula.data.model

import java.sql.Types

import enumeratum._
import org.hibernate.`type`.{StandardBasicTypes, StringType}
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.reflect.ClassTag

abstract class EnumUserType[E <: EnumEntry : ClassTag](enum: Enum[E]) extends AbstractBasicUserType[E, String] {
  override val basicType: StringType = StandardBasicTypes.STRING
  override val nullObject: E = null.asInstanceOf[E]
  override val nullValue: String = null

  override def convertToObject(s: String): E = enum.namesToValuesMap(s)
  override def convertToValue(e: E): String = e.entryName

  override def sqlTypes(): Array[Int] = Array(Types.VARCHAR)
}

abstract class OptionEnumUserType[E <: EnumEntry : ClassTag](enum: Enum[E]) extends AbstractBasicUserType[Option[E], String] {
  override val basicType: StringType = StandardBasicTypes.STRING
  override val nullObject: Option[E] = None
  override val nullValue: String = null

  override def convertToObject(s: String): Option[E] = s.maybeText.map(enum.namesToValuesMap)
  override def convertToValue(e: Option[E]): String = e.map(_.entryName).orNull

  override def sqlTypes(): Array[Int] = Array(Types.VARCHAR)
}
