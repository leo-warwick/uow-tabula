package uk.ac.warwick.tabula.data

import org.hibernate.{Hibernate, SessionFactory, Session}
import javax.sql.DataSource
import uk.ac.warwick.tabula.data.model.{StudentCourseYearDetails, CanBeDeleted, Member, StudentCourseDetails}
import uk.ac.warwick.spring.Wire
import language.implicitConversions
import scala.reflect._
import org.hibernate.proxy.HibernateProxy
import org.hibernate.criterion.Restrictions._
import uk.ac.warwick.tabula.data.Daoisms.NiceQueryCreator
import scala.collection.IterableLike
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.Logging

/** Trait for self-type annotation, declaring availability of a Session. */
trait SessionComponent{
  protected def session:Session
}

/**
 * This self-type trait is a bit of a cheat as it has behaviour in it - but only
 * some stuff that calls through to the provided session. Arguably better than
 * forcing a test to provide these methods.
 */
trait ExtendedSessionComponent extends SessionComponent {
	def isFilterEnabled(name: String) = session.getEnabledFilter(name) != null

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
	def is = org.hibernate.criterion.Restrictions.eqOrIsNull _
	def isNull(propertyName: String) = org.hibernate.criterion.Restrictions.isNull(propertyName)
	def safeIn[A](propertyName: String, iterable: Seq[A]) = {
		if (iterable.isEmpty) {
			logger.warn("Empty iterable passed to safeIn() - query will never return any results, may be unnecessary")
			org.hibernate.criterion.Restrictions.sqlRestriction("1=0")
		} else if (iterable.length <= Daoisms.MaxInClauseCount) {
			org.hibernate.criterion.Restrictions.in(propertyName, iterable.asJavaCollection)
		} else {
			val or = disjunction()
			iterable.grouped(Daoisms.MaxInClauseCount).foreach { subitr =>
				or.add(org.hibernate.criterion.Restrictions.in(propertyName, subitr.asJavaCollection))
			}
			or
		}
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

object HibernateHelpers extends HibernateHelpers

object Daoisms extends HelperRestrictions {
	/**
	 * Adds a method to Session which returns a wrapped Criteria or Query that works
	 * better with Scala's generics support.
	 */
	implicit class NiceQueryCreator(session: Session) {
		def newCriteria[A: ClassTag] = new ScalaCriteria[A](session.createCriteria(classTag[A].runtimeClass))
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
	var dataSource = Wire[DataSource]("dataSource")
	var sessionFactory = Wire.auto[SessionFactory]

	protected def session = {
		val session = sessionFactory.getCurrentSession
		session.enableFilter(Member.FreshOnlyFilter)
		session.enableFilter(StudentCourseDetails.FreshCourseDetailsOnlyFilter)
		session.enableFilter(StudentCourseYearDetails.FreshCourseYearDetailsOnlyFilter)
		session
	}

	protected def sessionWithoutFreshFilters = {
		val session = sessionFactory.getCurrentSession
		session.disableFilter(Member.FreshOnlyFilter)
		session.disableFilter(StudentCourseDetails.FreshCourseDetailsOnlyFilter)
		session.disableFilter(StudentCourseYearDetails.FreshCourseYearDetailsOnlyFilter)
		session
	}

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

