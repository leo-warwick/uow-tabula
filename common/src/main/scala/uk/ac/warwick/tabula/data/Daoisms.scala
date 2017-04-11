package uk.ac.warwick.tabula.data

import javax.sql.DataSource

import org.hibernate.criterion.Restrictions._
import org.hibernate.criterion.{Criterion, DetachedCriteria, Projection, PropertySubqueryExpression}
import org.hibernate.proxy.HibernateProxy
import org.hibernate.{Hibernate, Session, SessionFactory}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Daoisms.NiceQueryCreator
import uk.ac.warwick.tabula.data.model.{CanBeDeleted, Member, StudentCourseDetails, StudentCourseYearDetails}
import uk.ac.warwick.tabula.helpers.Logging

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.reflect._
import scala.util.Try

/** Trait for self-type annotation, declaring availability of a Session. */
trait SessionComponent {
  protected def session:Session
}

/**
 * This self-type trait is a bit of a cheat as it has behaviour in it - but only
 * some stuff that calls through to the provided session. Arguably better than
 * forcing a test to provide these methods.
 */
trait ExtendedSessionComponent extends SessionComponent {
	def isFilterEnabled(name: String): Boolean = session.getEnabledFilter(name) != null

	/**
	 * type-safe session.get. returns an Option object, which will match None if
	 * null is returned.
	 *
	 * For CanBeDeleted entities, it also checks if the entity is deleted and
	 * the notDeleted filter is enabled, in which case it also returns None.
	 */
	protected def getById[A:ClassTag](id: String): Option[A] = {
		val runtimeClass = classTag[A].runtimeClass
		session.get(runtimeClass.getName, id) match {
			case entity: CanBeDeleted if entity.deleted && isFilterEnabled("notDeleted") => None
			case entity: Any if runtimeClass.isInstance(entity) => Some(entity.asInstanceOf[A])
			case _ => None
		}
	}
}

trait HelperRestrictions extends Logging {
	@transient protected val maxInClause = Daoisms.MaxInClauseCount
	def is: (String, Any) => Criterion = org.hibernate.criterion.Restrictions.eqOrIsNull _
	def isNot: (String, Any) => Criterion = org.hibernate.criterion.Restrictions.neOrIsNotNull _
	def isSubquery(propertyName: String, subquery: DetachedCriteria) = new PropertySubqueryExpressionWithToString(propertyName, subquery)
	def safeIn[A](propertyName: String, iterable: Seq[A]): Criterion = {
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
			if (proxy.isInstanceOf[HibernateProxy]) proxy.asInstanceOf[HibernateProxy].getHibernateLazyInitializer.getImplementation.asInstanceOf[A]
			else proxy
		}.orNull
}

object HibernateHelpers extends HibernateHelpers with HelperRestrictions

object Daoisms {
	/**
	 * Adds a method to Session which returns a wrapped Criteria or Query that works
	 * better with Scala's generics support.
	 */
	implicit class NiceQueryCreator(session: Session) {
		def newCriteria[A: ClassTag] =
			new ScalaCriteria[A](
				session.createCriteria(
					classTag[A]
						.runtimeClass
				)
			)
		def newCriteria[A: ClassTag](clazz: Class[_]) =
			new ScalaCriteria[A](
				session.createCriteria(clazz)
			)
		def newQuery[A](hql: String) = new ScalaQuery[A](session.createQuery(hql))
	}

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
trait Daoisms extends ExtendedSessionComponent with HelperRestrictions with HibernateHelpers {
	@transient private var _dataSource = Wire.option[DataSource]("dataSource")
	def dataSource: DataSource = _dataSource.orNull
	def dataSource_=(dataSource: DataSource) { _dataSource = Option(dataSource) }

	@transient private var _sessionFactory = Wire.option[SessionFactory]
	def sessionFactory: SessionFactory = _sessionFactory.orNull
	def sessionFactory_=(sessionFactory: SessionFactory) { _sessionFactory = Option(sessionFactory) }

	protected def optionalSession: Option[Session] =
		_sessionFactory.flatMap { sf => Try(sf.getCurrentSession).toOption }
			.map { session =>
				session.enableFilter(Member.FreshOnlyFilter)
				session.enableFilter(StudentCourseDetails.FreshCourseDetailsOnlyFilter)
				session.enableFilter(StudentCourseYearDetails.FreshCourseYearDetailsOnlyFilter)
				session
			}

	protected def session: Session = optionalSession.getOrElse({
		logger.error("Trying to access session, but it is null")
		null
	})

	protected def optionalSessionWithoutFreshFilters: Option[Session] =
		_sessionFactory.flatMap { sf => Try(sf.getCurrentSession).toOption }
			.map { session =>
				session.disableFilter(Member.FreshOnlyFilter)
				session.disableFilter(StudentCourseDetails.FreshCourseDetailsOnlyFilter)
				session.disableFilter(StudentCourseYearDetails.FreshCourseYearDetailsOnlyFilter)
				session
			}

	protected def sessionWithoutFreshFilters: Session = optionalSessionWithoutFreshFilters.orNull

	/**
	 * Do some work in a new session. Only needed outside of a request,
	 * since we already have sessions there. When you know there's already
	 * a session, you can access it through the `session` getter (within
	 * the callback of this method, it should work too).
	 */
	protected def inSession(fn: (Session) => Unit) {
		val sess = sessionFactory.openSession()
		try fn(sess) finally sess.close()
	}

	implicit def implicitNiceSession(session: Session) = new NiceQueryCreator(session)

}

class PropertySubqueryExpressionWithToString(propertyName: String, dc: DetachedCriteria) extends PropertySubqueryExpression(propertyName, "=", null, dc) {

	override def toString(): String = propertyName + "=" + dc

}
