package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.data.model.Convertible
import uk.ac.warwick.tabula.system.TwoWayConverter
import scala.reflect.ClassTag
import scala.language.implicitConversions

/**
 * Converter for a class that implements Convertible and provides an
 * implicit factory in its companion object.
 */
class ConvertibleConverter[A >: Null <: String, B <: Convertible[A]](implicit factory: A => B, tagA: ClassTag[A], tagB: ClassTag[B])
	extends TwoWayConverter[A, B] {
	override def convertLeft(source: B): A = source.value
	override def convertRight(source: A): B = factory(source)
}
