package uk.ac.warwick.tabula.data

import org.hibernate.criterion._
import collection.JavaConverters._

/**
 * Nice wrapper for a Query object. You usually won't create
 * this explicitly - the Daoisms trait adds a newSQLQuery method
 * to Session which will return one of these.
 */
class ScalaSQLQuery[A](c: org.hibernate.SQLQuery) {
	
	def setString(name: String, value: String) = chainable { c.setString(name, value) }
	def setEntity(name: String, entity: Any) = chainable { c.setEntity(name, entity) }
	def setParameter(name: String, value: Any) = chainable { c.setParameter(name, value) }
	def setParameterList(name: String, list: Seq[_]) = chainable { c.setParameterList(name, list.toList.asJava) }
	// TODO add other methods on demand
	
	// Helper to neaten up the above chainable methods - returns this instead of plain Query
    @inline private def chainable(fn: => Unit) = { fn; this }

    /** Returns a typed Seq of the results. */
    def seq: Seq[A] = list.asScala

    /** Returns a typed list of the results.*/
    def list: java.util.List[A] = c.list().asInstanceOf[java.util.List[A]]

    def scroll() = c.scroll()

    /**
     * Return an untyped list of the results, in case you've
     * set the projection for the query to return something else.
     */
    def untypedList: java.util.List[_] = c.list()

    /** Return Some(result), or None if no row matched. */
    def uniqueResult: Option[A] = Option(c.uniqueResult().asInstanceOf[A])
    
    def run(): Int = c.executeUpdate()
}