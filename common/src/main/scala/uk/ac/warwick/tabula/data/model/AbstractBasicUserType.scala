package uk.ac.warwick.tabula.data.model

import java.io.Serializable

import scala.reflect._
import org.hibernate.usertype.UserType
import org.hibernate.`type`.AbstractSingleColumnStandardBasicType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.{io => jio}

import org.hibernate.engine.spi.SessionImplementor

/** Trait for a class that can be converted to a value for storing,
	* usually a string or jinteger.
	*/
trait Convertible[A >: Null <: AnyRef] {
	def value: A
}

/**
 * Handles a lot of the junk that isn't necessary if all you want to do is
 * convert between a class and a simple single value.
 *
 * convertTo* methods can assume a non-null input - implement
 * the nullObject and nullValue values to tell it what value to
 * use in the case of null coming from other direction
 */
abstract class AbstractBasicUserType[A <: Object : ClassTag, B : ClassTag] extends UserType {

	val basicType: AbstractSingleColumnStandardBasicType[B]
	// what to use when NULL comes out of the DB
	val nullObject: A
	// what to put in the DB when saving null
	val nullValue: B
	def convertToObject(input: B): A
	def convertToValue(obj: A): B


	final override def nullSafeGet(resultSet: ResultSet, names: Array[String], impl: SessionImplementor, owner: Object): A = {
		basicType.nullSafeGet(resultSet, names(0), impl) match {
			case s: Any if s == nullValue => nullObject
			case b: B => convertToObject(b)
			case null => nullObject
		}
	}

	final override def nullSafeSet(stmt: PreparedStatement, value: Any, index: Int, impl: SessionImplementor) {
		basicType.nullSafeSet(stmt, toValue(value), index, impl)
  }

	private final def toValue(value: Any): B = value match {
		case a: A => convertToValue(a)
		case null => nullValue
	}

	override def returnedClass: Class[_] = classTag[A].runtimeClass

	override def isMutable = false
	override def equals(x: Object, y: Object): Boolean = x == y
	override def hashCode(x: Object): Int = Option(x).getOrElse("").hashCode
	override def deepCopy(x: Object): Object = x
	override def replace(original: Object, target: Object, owner: Object): Object = original
	override def disassemble(value: Object): Serializable = value.asInstanceOf[jio.Serializable]
	override def assemble(cached: jio.Serializable, owner: Object): AnyRef = cached.asInstanceOf[AnyRef]
}
