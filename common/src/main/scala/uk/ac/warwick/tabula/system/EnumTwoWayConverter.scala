package uk.ac.warwick.tabula.system

import enumeratum._
import enumeratum.values.{IntEnum, IntEnumEntry, ValueEnum, ValueEnumEntry}
import uk.ac.warwick.tabula.JavaImports.JInteger
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.reflect.ClassTag

abstract class EnumTwoWayConverter[E >: Null <: EnumEntry : ClassTag](enum: Enum[E]) extends TwoWayConverter[String, E] {
  override def convertRight(code: String): E = if (code.hasText) enum.namesToValuesMap(code) else null
  override def convertLeft(enumEntry: E): String = Option(enumEntry).map(_.entryName).orNull
}

abstract class IntEnumTwoWayConverter[E >: Null <: IntEnumEntry : ClassTag](enum: IntEnum[E]) extends TwoWayConverter[String, E] {
  override def convertRight(code: String): E = code.toIntOption.map(enum.withValue).orNull
  override def convertLeft(enumEntry: E): String = Option(enumEntry).map(_.value.toString).orNull
}
