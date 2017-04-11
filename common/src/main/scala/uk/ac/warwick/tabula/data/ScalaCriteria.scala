package uk.ac.warwick.tabula.data

import org.hibernate.criterion._
import org.hibernate.sql.JoinType

import collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.transform.DistinctRootEntityResultTransformer

import collection.mutable
import org.hibernate.transform.ResultTransformer
import org.hibernate.{FetchMode, ScrollableResults}

/**
 * Nice wrapper for a Criteria object. You usually won't create
 * this explicitly - the Daoisms trait adds a newCriteria method
 * to Session which will return one of these.
 */
class ScalaCriteria[A](c: org.hibernate.Criteria) {

	private val aliases: mutable.Map[String, String] = mutable.Map()

	def add(criterion: Criterion): ScalaCriteria[A] = chainable { c.add(criterion) }
	def add(restriction: ScalaRestriction): ScalaCriteria[A] = chainable {
		restriction.aliases.foreach { case (property, aliasAndJoinType) => createAlias(property, aliasAndJoinType.alias, aliasAndJoinType.joinType, aliasAndJoinType.withClause) }
		c.add(restriction.underlying)
	}
	def addOrder(order: Order): ScalaCriteria[A] = chainable { c.addOrder(order) }
	def addOrder(order: ScalaOrder): ScalaCriteria[A] = chainable {
		order.aliases.foreach { case (property, aliasAndJoinType) => createAlias(property, aliasAndJoinType.alias, aliasAndJoinType.joinType, aliasAndJoinType.withClause) }
		c.addOrder(order.underlying)
	}
	def setMaxResults(i: Int): ScalaCriteria[A] = chainable { c.setMaxResults(i) }
	def setFirstResult(i: Int): ScalaCriteria[A] = chainable { c.setFirstResult(i) }
	def setFetchMode(associationPath: String, mode: FetchMode): ScalaCriteria[A] = chainable { c.setFetchMode(associationPath, mode) }
	def createAlias(property: String, alias: String, joinType: JoinType = JoinType.INNER_JOIN, withClause: Option[Criterion] = None): ScalaCriteria[A] = chainable {
		aliases.put(property, alias) match {
			case None => c.createAlias(property, alias, joinType, withClause.orNull)
			case Some(existing) if existing == alias => // duplicate
			case Some(other) => throw new IllegalArgumentException("Tried to alias %s to %s, but it is already aliased to %s!".format(property, alias, other))
		}
	}
	def distinct: ScalaCriteria[A] = chainable { c.setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE) }
	def count: Number = project[Number](Projections.rowCount()).uniqueResult.get

	// Helper to neaten up the above chainable methods - returns this instead of plain Criteria
	@inline private def chainable(fn: => Unit) = { fn; this }

	def project[B](projection: Projection): ProjectedScalaCriteria[A, B] = {
		c.setProjection(projection)
		new ProjectedScalaCriteria[A, B](this)
	}

	/** Returns a typed Seq of the results. */
	def seq: Seq[A] = list.asScala

	/** Returns a typed list of the results. */
	def list: JList[A] = listOf[A]

	def scroll(): ScrollableResults = c.scroll()

	/**
	 * Return an untyped list of the results, in case you've
	 * set the projection for the Criteria to return something else.
	 */
	def untypedList: JList[_] = c.list()

	/**
	 * Returns a Java List of the given type. If the result of the query
	 * doesn't match the requested type you will get a runtime error.
	 */
	def listOf[A]: JList[A] = untypedList.asInstanceOf[JList[A]]

	/** Return Some(result), or None if no row matched. */
	def uniqueResult: Option[A] = uniqueResultOf[A]

	def uniqueResultOf[A]: Option[A] = Option(c.uniqueResult().asInstanceOf[A])

	def setResultTransformer[B](resultTransformer: ResultTransformer): ProjectedScalaCriteria[A, B] = {
		c.setResultTransformer(resultTransformer)
		new ProjectedScalaCriteria[A, B] (this)
	}
}

class ProjectedScalaCriteria[A, B](c: ScalaCriteria[A]) {
	// Helper to neaten up the above chainable methods - returns this instead of plain Criteria
	@inline private def chainable(fn: => Unit) = { fn; this }

	/** Returns a typed Seq of the results. */
	def seq: Seq[B] = c.listOf[B].asScala

	def uniqueResult: Option[B] = c.uniqueResultOf[B]

	def distinct: ProjectedScalaCriteria[A, B] = chainable { c.setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE) }
	def setResultTransformer(resultTransformer: ResultTransformer): ProjectedScalaCriteria[A, Nothing] = c.setResultTransformer(resultTransformer)
}


