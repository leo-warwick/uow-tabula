package uk.ac.warwick.tabula.system

import scala.collection.JavaConversions._
import scala.collection.immutable.Stream
import java.lang.annotation.Annotation
import javax.servlet.ServletRequest

import org.springframework.stereotype

import scala.util.matching.Regex

/**
 * Data binder mixin that disallows binding to certain classes, such as ones that
 * look like service beans. This includes anything annotated with the @Component
 * family of stereotypes, such as Service and Controller.
 */
trait AllowedFieldsBinding extends CustomDataBinder {

	val parentPathPattern: Regex = """(.+)\.(.+?)""".r // match (top.part.of.path).(child)

	// We do not bind to a class if it has one of these annotations.
	val disallowedAnnotations: Set[Class[_ <: Annotation]] = Set(
			classOf[stereotype.Component],
			classOf[stereotype.Controller],
			classOf[stereotype.Repository],
			classOf[stereotype.Service]
		)

	// Annotation you can add to an individual property to disable binding to it
	val noBindAnnotation: Class[NoBind] = classOf[NoBind]

	override def isAllowed(field: String): Boolean = {
		super.isAllowed(field) &&
			! containedWithinDisallowedAnnotation(field) &&
			! hasNoBindAnnotation(field)
	}

	def hasNoBindAnnotation(field: String): Boolean = {
		val descriptor = propertyAccessor.getPropertyTypeDescriptor(field)
		descriptor != null && descriptor.getAnnotation(noBindAnnotation) != null
	}

	/**
	 * Returns whether this field is, or is contained by, a class that uses
	 * one of the annotations that we don't want to bind to.
	 *
	 * Setting a property only asks if that field path is allowed, so we have
	 * to manually go up through its parentage, looking for disallowed classes.
	 * Probably not worth caching the results since they are short-lived.
	 */
	def containedWithinDisallowedAnnotation(field: String): Boolean = {
		ancestors(field).flatMap(toPropertyType).exists(usesDisallowedAnnotation)
	}

	// A stream of field names, going up through each field's parent.
	def ancestors(field: String): Stream[String] = field match {
		case parentPathPattern(tail, head) => field #:: ancestors(tail)
		case _ => Stream(field)
	}

	def usesDisallowedAnnotation(c: Class[_]): Boolean = {
		disallowedAnnotations.exists { annotation => c.getAnnotation(annotation) != null }
	}

	def toPropertyType(field: String): Option[Class[_]] = Option(propertyAccessor.getPropertyType(field))

}