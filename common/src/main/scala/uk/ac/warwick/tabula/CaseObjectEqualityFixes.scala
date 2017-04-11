package uk.ac.warwick.tabula

import scala.reflect.ClassTag

/**
 * We need to override equals() here because under heavy load, the class loader will
 * (stupidly) return a different instance of the case object, which fails the equality
 * check because the default AnyRef implementation of equals is just this eq that.
 *
 * We also have to override hashCodes because their default is computed at compile time,
 * based only on the (unqualified) name of the current case object, so,
 * before override, Module.Create.hashCode() == Feedback.Create.hashCode()
 */
abstract class CaseObjectEqualityFixes[A <: CaseObjectEqualityFixes[A] : ClassTag] {
	def getName: String

	override def equals(other: Any): Boolean = other match {
		case that: A => getName == that.getName
		case _ => false
	}
	override def hashCode(): Int = getName.hashCode()
	override def toString: String = getName
}