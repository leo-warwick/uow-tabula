package uk.ac.warwick.tabula
import collection.JavaConverters._
import collection.mutable
import language.implicitConversions
import scala.collection.GenTraversableOnce
import java.math.MathContext

/**
 * Quick way to expose a bunch of Java type names under
 * different names like JBoolean, so they can be differentiated
 * from the Scala types.
 *
 * You can either import:
 * {{{
 * import uk.ac.warwick.tabula.JavaImports._
 * }}}
 * or you can add the JavaImports trait to your class:
 * {{{
 * class HatStand extends Furniture with JavaImports
 * }}}
 */

trait JavaImports {
	type JBoolean = java.lang.Boolean
	type JList[A] = java.util.List[A]
	type JArrayList[A] = java.util.ArrayList[A]
	type JMap[K, V] = java.util.Map[K, V]
	type JConcurrentMap[K, V] = java.util.concurrent.ConcurrentMap[K, V] with ScalaConcurrentMapHelpers[K, V]
	type JSet[A] = java.util.Set[A]
	type JInteger = java.lang.Integer
	type JLong = java.lang.Long
	type JBigDecimal = java.math.BigDecimal
	type JFloat = java.lang.Float

	def JBoolean(b: Option[Boolean]) = ToJBoolean(b)
	def JList[A](items: A*): JList[A] = mutable.Seq(items: _*).asJava
	def JMap[K, V](elems: (K, V)*): JMap[K, V] = mutable.Map[K, V](elems: _*).asJava
	def JConcurrentMap[K, V](elems: (K, V)*) = JConcurrentHashMap(elems: _*)
	def JSet[A](items: A*): JSet[A] = mutable.Set(items: _*).asJava
	def JInteger(i: Option[Int]) = ToJInteger(i)
	def JLong(l: Option[Long]) = ToJLong(l)
	def JBigDecimal(bd: Option[BigDecimal]) = ToJBigDecimal(bd)
	def JFloat(f: Option[Float]) = ToJFloat(f)

	/**
	 * Converts an Option[Boolean] to a Java Boolean, by interpreting
	 * None as null.
	 */
	protected implicit def ToJBoolean(b: Option[Boolean]): JBoolean = (b map (b => b: JBoolean)).orNull

	/**
	 * Converts an Option[Int] to a Java Integer, by interpreting
	 * None as null.
	 */
	protected implicit def ToJInteger(i: Option[Int]): JInteger = (i map (i => i: JInteger)).orNull

	/**
	 * Converts an Option[Long] to a Java Long, by interpreting
	 * None as null.
	 */
	protected implicit def ToJLong(l: Option[Long]): JLong = (l map (l => l: JLong)).orNull

	/**
	 * Converts an Option[BigDecimal] to a Java BigDecimal, by interpreting
	 * None as null.
	 */
	protected implicit def ToJBigDecimal(bd: Option[BigDecimal]): JBigDecimal = (bd map (bd => new java.math.BigDecimal(bd.toDouble, MathContext.DECIMAL128))).orNull

	/**
		* Converts an Option[Float] to a Java Float, by interpreting
		* None as null.
		*/
	protected implicit def ToJFloat(f: Option[Float]): JFloat = (f map (f => f: JFloat)).orNull

	/**
	 * Allows you to create an empty Java ArrayList, useful as an initial
	 * value for a variable that needs a mutable JList.
	 */
	object JArrayList {
		def apply[A](elements: A*): java.util.ArrayList[A] = {
			val list = new java.util.ArrayList[A]()
			if (elements.nonEmpty) list.addAll(elements.asJavaCollection)
			list
		}

		def apply[A](orig: List[A]): java.util.ArrayList[A] = {
			val list = new java.util.ArrayList[A]()
			if (orig.nonEmpty) list.addAll(orig.asJavaCollection)
			list
		}

		def apply[A](orig: JList[A]): java.util.ArrayList[A] = {
			val list = new java.util.ArrayList[A]()
			if (!orig.isEmpty) list.addAll(orig)
			list
		}

		def apply[A](orig: GenTraversableOnce[A]): java.util.ArrayList[A] = {
			if (orig.isEmpty)
				new java.util.ArrayList[A]
			else
				new java.util.ArrayList[A](orig.toIndexedSeq.asJavaCollection)
		}
	}

	/**
	 * Allows you to create an empty Java HashSet, useful as an initial
	 * value for a variable that needs a mutable JSet.
	 */
	object JHashSet {
		def apply[A](elements: A*): java.util.HashSet[A] = {
			val set = new java.util.HashSet[A]()
			if (elements.nonEmpty) set.addAll(elements.asJavaCollection)
			set
		}

		def apply[A](orig: Set[A]): java.util.HashSet[A] = {
			val set = new java.util.HashSet[A]()
			if (orig.nonEmpty) set.addAll(orig.asJavaCollection)
			set
		}

		def apply[A](orig: JSet[A]): java.util.HashSet[A] = {
			val set = new java.util.HashSet[A]()
			if (!orig.isEmpty) set.addAll(orig)
			set
		}
	}

	/**
	 * Allows you to create an empty Java HashMap, useful as an initial
	 * value for a variable that needs a mutable JMap.
	 */
	object JHashMap {
		def apply[K, V](elements: (K, V)*): java.util.HashMap[K, V] = {
			val map = new java.util.HashMap[K, V]()
			elements foreach { case (key, value) => map.put(key, value) }
			map
		}

		def apply[K, V](orig: Map[K, V]): java.util.HashMap[K, V] = {
			val map = new java.util.HashMap[K, V]()
			if (orig.nonEmpty) map.putAll(orig.asJava)
			map
		}

		def apply[K, V](orig: JMap[K, V]): java.util.HashMap[K, V] = {
			val map = new java.util.HashMap[K, V]()
			if (!orig.isEmpty) map.putAll(orig)
			map
		}
	}

	/**
	 * Allows you to create an empty Java HashMap, useful as an initial
	 * value for a variable that needs a mutable JMap.
	 */
	object JLinkedHashMap {
		def apply[K, V](elements: (K, V)*): java.util.LinkedHashMap[K, V] = {
			val map = new java.util.LinkedHashMap[K, V]()
			elements foreach { case (key, value) => map.put(key, value) }
			map
		}

		def apply[K, V](orig: Map[K, V]): java.util.LinkedHashMap[K, V] = {
			val map = new java.util.LinkedHashMap[K, V]()
			if (orig.nonEmpty) map.putAll(orig.asJava)
			map
		}

		def apply[K, V](orig: JMap[K, V]): java.util.LinkedHashMap[K, V] = {
			val map = new java.util.LinkedHashMap[K, V]()
			if (!orig.isEmpty) map.putAll(orig)
			map
		}
	}

	/**
	 * Allows you to create an empty Java ConcurrentHashMap, useful as an initial
	 * value for a variable that needs a mutable JConcurrentMap.
	 */
	object JConcurrentHashMap {
		def apply[K, V](elements: (K, V)*): java.util.concurrent.ConcurrentHashMap[K, V] with ScalaConcurrentMapHelpers[K, V] = {
			val map = new java.util.concurrent.ConcurrentHashMap[K, V]() with ScalaConcurrentMapHelpers[K, V]
			elements foreach { case (key, value) => map.put(key, value) }
			map
		}

		def apply[K, V](orig: Map[K, V]): java.util.concurrent.ConcurrentHashMap[K, V] with ScalaConcurrentMapHelpers[K, V] = {
			val map = new java.util.concurrent.ConcurrentHashMap[K, V]() with ScalaConcurrentMapHelpers[K, V]
			if (orig.nonEmpty) map.putAll(orig.asJava)
			map
		}

		def apply[K, V](orig: JMap[K, V]): java.util.concurrent.ConcurrentHashMap[K, V] with ScalaConcurrentMapHelpers[K, V] = {
			val map = new java.util.concurrent.ConcurrentHashMap[K, V]() with ScalaConcurrentMapHelpers[K, V]
			if (!orig.isEmpty) map.putAll(orig)
			map
		}
	}
}

trait ScalaConcurrentMapHelpers[K, V] {
	self: JavaImports.JConcurrentMap[K, V] =>

	def getOrElseUpdate(key: K, op: => V): V =
		Option(get(key)) match {
			case Some(v) => v
      case None =>
				op match {
					 case null => null.asInstanceOf[V] // We can't put a null in here. Sigh
					 case d => Option(putIfAbsent(key, d)).getOrElse(d)
				}
		}
}

object JavaImports extends JavaImports