package uk.ac.warwick.tabula.data.model

import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types
import scala.reflect.ClassTag

/**
 * User type that is converted to and from a String.
 */
abstract class AbstractStringUserType[A >: Null <: AnyRef : ClassTag] extends AbstractBasicUserType[A, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

}

/**
 * If a type implements Convertible and provides a factory method in a companion object,
 * then you can extend this to define a UserType in one line
 */
class ConvertibleStringUserType[A >: Null <: Convertible[String]](implicit factory: String => A, classTag: ClassTag[A])
	extends AbstractStringUserType[A] {
	override def convertToValue(obj: A): String = obj.value
	override def convertToObject(value: String): A = factory(value)
}

