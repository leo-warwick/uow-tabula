package uk.ac.warwick.tabula.data

import javax.sql.DataSource

import org.hibernate.criterion.Restrictions._
import org.hibernate.criterion.{Projection, DetachedCriteria, PropertySubqueryExpression}
import org.hibernate.proxy.HibernateProxy
import org.hibernate.{Hibernate, SessionFactory}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Member, StudentCourseDetails, StudentCourseYearDetails}
import uk.ac.warwick.tabula.helpers.Logging

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.Try

/** Trait for self-type annotation, declaring availability of a Session. */
trait SessionComponent {
  protected def session: MaintenanceModeAwareSession
}

trait HelperRestrictions extends Logging {
	@transient protected val maxInClause = Daoisms.MaxInClauseCount
	def is = org.hibernate.criterion.Restrictions.eqOrIsNull _
	def isNot = org.hibernate.criterion.Restrictions.neOrIsNotNull _
	def isSubquery(propertyName: String, subquery: DetachedCriteria) = new PropertySubqueryExpressionWithToString(propertyName, subquery)
	def isNull(propertyName: String) = org.hibernate.criterion.Restrictions.isNull(propertyName)
	def safeIn[A](propertyName: String, iterable: Seq[A]) = {
		if (iterable.isEmpty) {
			logger.warn("Empty iterable passed to safeIn() - query will never return any results, may be unnecessary")
			org.hibernate.criterion.Restrictions.sqlRestriction("1=0")
		} else if (iterable.length <= maxInClause) {
			org.hibernate.criterion.Restrictions.in(propertyName, iterable.asJavaCollection)
		} else {
			logger.warn("TAB-3888 - Use multiple queries where possible instead of passing more than 1000 entities")
			val or = disjunction()
			iterable.grouped(maxInClause).foreach { subitr =>
				or.add(org.hibernate.criterion.Restrictions.in(propertyName, subitr.asJavaCollection))
			}
			or
		}
	}
	def safeInSeq[A](criteriaFactory: () => ScalaCriteria[A], propertyName: String, iterable: Seq[_]): Seq[A] = {
		iterable.grouped(maxInClause).toSeq.flatMap(maxedIterable => {
			val c = criteriaFactory.apply()
			c.add(org.hibernate.criterion.Restrictions.in(propertyName, maxedIterable.asJavaCollection)).seq
		})
	}
	def safeInSeqWithProjection[A, B](criteriaFactory: () => ScalaCriteria[A], projection: Projection, propertyName: String, iterable: Seq[_]): Seq[B] = {
		iterable.grouped(maxInClause).toSeq.flatMap(maxedIterable => {
			val c = criteriaFactory.apply()
			c.add(org.hibernate.criterion.Restrictions.in(propertyName, maxedIterable.asJavaCollection))
			c.project[B](projection).seq
		})
	}
}

trait HibernateHelpers {
	def initialiseAndUnproxy[A >: Null](entity: A): A =
		Option(entity).map { proxy =>
			Hibernate.initialize(proxy)

			proxy match {
				case hibernateProxy: HibernateProxy => hibernateProxy.getHibernateLazyInitializer.getImplementation.asInstanceOf[A]
				case _ => proxy
			}
		}.orNull
}

object HibernateHelpers extends HibernateHelpers with HelperRestrictions

object Daoisms {
	// The maximum number of clauses supported in an IN(..) before it will
	// unceremoniously fail. Use `grouped` with this to split up work
	val MaxInClauseCount = 1000
}

/**
 * A trait for DAO classes to mix in to get useful things
 * like the current session.
 *
 * It's only really for Hibernate access to the default
 * session factory. If you want to do JDBC stuff or use a
 * different data source you'll need to look elsewhere.
 */
private[data] trait Daoisms extends SessionComponent with HelperRestrictions with HibernateHelpers {
	@transient private val _dataSource = Wire.option[DataSource]("dataSource")
	protected def dataSource = _dataSource.orNull

	@transient private var _sessionFactory = Wire.option[SessionFactory]

	// For tests
	def sessionFactory = _sessionFactory.orNull
	def sessionFactory_=(sessionFactory: SessionFactory): Unit = { _sessionFactory = Option(sessionFactory) }

	protected def session: MaintenanceModeAwareSession =
		_sessionFactory.flatMap { sf =>
			Try(MaintenanceModeAwareSession(sf)).toOption
		}.map { session =>
			session.enableFilter(Member.FreshOnlyFilter)
			session.enableFilter(StudentCourseDetails.FreshCourseDetailsOnlyFilter)
			session.enableFilter(StudentCourseYearDetails.FreshCourseYearDetailsOnlyFilter)
			session
		}.getOrElse({
			logger.error("Trying to access session, but it is null")
			null
		})

	protected def sessionWithoutFreshFilters: MaintenanceModeAwareSession =
		_sessionFactory.flatMap { sf =>
			Try(MaintenanceModeAwareSession(sf)).toOption
		}.map { session =>
			session.disableFilter(Member.FreshOnlyFilter)
			session.disableFilter(StudentCourseDetails.FreshCourseDetailsOnlyFilter)
			session.disableFilter(StudentCourseYearDetails.FreshCourseYearDetailsOnlyFilter)
			session
		}.getOrElse({
			logger.error("Trying to access session, but it is null")
			null
		})

	/**
	 * Do some work in a new session. Only needed outside of a request,
	 * since we already have sessions there. When you know there's already
	 * a session, you can access it through the `session` getter (within
	 * the callback of this method, it should work too).
	 */
	protected def inSession(fn: MaintenanceModeAwareSession => Unit) {
		val sess = MaintenanceModeAwareSession(_sessionFactory.get.openSession())
		try fn(sess) finally sess.close()
	}

}

class PropertySubqueryExpressionWithToString(propertyName: String, dc: DetachedCriteria) extends PropertySubqueryExpression(propertyName, "=", null, dc) {

	override def toString = propertyName + "=" + dc

}
