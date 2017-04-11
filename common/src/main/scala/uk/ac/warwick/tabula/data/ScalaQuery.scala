package uk.ac.warwick.tabula.data

import org.hibernate.criterion._

import collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.transform.{DistinctRootEntityResultTransformer, ResultTransformer}
import org.hibernate.{Criteria, ScrollableResults}

/**
 * Nice wrapper for a Query object. You usually won't create
 * this explicitly - the Daoisms trait adds a newQuery method
 * to Session which will return one of these.
 */
class ScalaQuery[A](c: org.hibernate.Query) {

	def distinct: ScalaQuery[A] = chainable { c.setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE) }

	def setString(name: String, value: String): ScalaQuery[A] = chainable { c.setString(name, value) }
	def setEntity(name: String, entity: Any): ScalaQuery[A] = chainable { c.setEntity(name, entity) }
	def setParameter(name: String, value: Any): ScalaQuery[A] = chainable { c.setParameter(name, value) }
	def setParameterList(name: String, list: Seq[_]): ScalaQuery[A] = chainable { c.setParameterList(name, list.toList.asJava) }
	def setMaxResults(maxResults: Int): ScalaQuery[A] = chainable { c.setMaxResults(maxResults) }
	def setFirstResult(firstResult: Int): ScalaQuery[A] = chainable { c.setFirstResult(firstResult) }
	// TODO add other methods on demand

	// Helper to neaten up the above chainable methods - returns this instead of plain Query
    @inline private def chainable(fn: => Unit) = { fn; this }

    /** Returns a typed Seq of the results. */
    def seq: Seq[A] = list.asScala

    /** Returns a typed list of the results.*/
    def list: JList[A] = c.list().asInstanceOf[JList[A]]

    def scroll(): ScrollableResults = c.scroll()

    /**
     * Return an untyped list of the results, in case you've
     * set the projection for the query to return something else.
     */
    def untypedList: JList[_] = c.list()

    /** Return Some(result), or None if no row matched. */
    def uniqueResult: Option[A] = Option(c.uniqueResult().asInstanceOf[A])

    def run(): Int = c.executeUpdate()

    def executeUpdate(): Int = c.executeUpdate();
}