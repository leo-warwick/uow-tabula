package uk.ac.warwick.tabula.helpers

import scala.collection.mutable

/**
 * Methods for generating a lazy map that will calculate the value behind
 * each key once and then never again.
 */
object LazyMaps {

	/**
	 * Return a HashMap which will populate empty key values using the given
	 * factory.
	 */
	def create[K,V](factory: (K)=>(V)) = new mutable.LinkedHashMap[K,V] {
		override def apply(key: K): V =
			superGetOrUpdate(key, { factory(key) })

		override def get(key: K) = Option(apply(key))

		// same as getOrElseUpdate, except we call super.get to prevent a stack overflow.
		private def superGetOrUpdate(key:K, op: =>V) =
			super.get(key) match {
				case Some(v) => v
				case None => val d = op; this(key) = d; d
			}
	}
}