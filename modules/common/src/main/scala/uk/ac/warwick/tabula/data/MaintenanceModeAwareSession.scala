package uk.ac.warwick.tabula.data

import org.hibernate.{ScrollableResults, FlushMode, SessionFactory, Filter}
import org.springframework.orm.hibernate4.SessionFactoryUtils
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent, RequestInfo}
import uk.ac.warwick.tabula.data.model.CanBeDeleted
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, MaintenanceModeServiceComponent}

import scala.reflect._

trait MaintenanceModeAwareSession {
	def newCriteria[A: ClassTag]: ScalaCriteria[A]
	def newCriteria[A: ClassTag](clazz: Class[_]): ScalaCriteria[A]
	def newQuery[A](hql: String): ScalaQuery[A]
	def newSQLQuery[A](hql: String): ScalaSQLQuery[A]
	def getById[A: ClassTag](id: java.io.Serializable): Option[A]

	def enableFilter(filterName: String): Filter
	def disableFilter(filterName: String): Unit

	def getEnabledFilter(filterName: String): Option[Filter]
	def isFilterEnabled(filterName: String): Boolean

	def scrollable[A](results: ScrollableResults): Scrollable[A]

	/* Mutating methods */

	// It'd be nice if these returned the saved object, but Hibernate is a fuck
	def saveOrUpdate[A](obj: A): Unit
	def save[A](obj: A): java.io.Serializable
	def update[A](obj: A): Unit
	def delete[A](obj: A): Unit

	def execute(query: ScalaQuery[_]): Int
	def execute(query: ScalaSQLQuery[_]): Int

	/* Utilities */

	def evict[A](obj: A): Unit
	def flush(): Unit
	def clear(): Unit
	def close(): Unit
}

object MaintenanceModeAwareSession {
	def apply(s: org.hibernate.Session): MaintenanceModeAwareSession = {
		val session =
			new MaintenanceModeAwareSessionImpl(s)
				with AutowiringFeaturesComponent
				with AutowiringMaintenanceModeServiceComponent

		session.init()
	}

	def apply(sessionFactory: SessionFactory): MaintenanceModeAwareSession =
		MaintenanceModeAwareSession(sessionFactory.getCurrentSession)


	/**
		* Nice wrapper for a Hibernate Session. Has awareness of maintenance mode,
		* so will throw exceptions when necessary.
		*/
	class MaintenanceModeAwareSessionImpl(s: org.hibernate.Session) extends MaintenanceModeAwareSession {
		self: MaintenanceModeServiceComponent with FeaturesComponent =>

		private[MaintenanceModeAwareSession] def init(): MaintenanceModeAwareSessionImpl = {
			if (isReadOnly) {
				s.setDefaultReadOnly(true)
				s.setFlushMode(FlushMode.MANUAL)
			}

			this
		}

		/* Query methods */

		def newCriteria[A: ClassTag] =
			new ScalaCriteria[A](
				s.createCriteria(
					classTag[A]
						.runtimeClass
				)
			)

		def newCriteria[A: ClassTag](clazz: Class[_]) =
			new ScalaCriteria[A](
				s.createCriteria(clazz)
			)

		def newQuery[A](hql: String) = new ScalaQuery[A](s.createQuery(hql), this)
		def newSQLQuery[A](sql: String) = new ScalaSQLQuery[A](s.createSQLQuery(sql), this)

		/**
			* type-safe session.get. returns an Option object, which will match None if
			* null is returned.
			*
			* For CanBeDeleted entities, it also checks if the entity is deleted and
			* the notDeleted filter is enabled, in which case it also returns None.
			*/
		def getById[A: ClassTag](id: java.io.Serializable): Option[A] = {
			val runtimeClass = classTag[A].runtimeClass
			s.get(runtimeClass.getName, id) match {
				case entity: CanBeDeleted if entity.deleted && isFilterEnabled("notDeleted") => None
				case entity: Any if runtimeClass.isInstance(entity) => Some(entity.asInstanceOf[A])
				case _ => None
			}
		}

		def scrollable[A](results: ScrollableResults): Scrollable[A] =
			new ScrollableImpl(results, s, { a: Array[AnyRef] => a(0).asInstanceOf[A] })

		/* Filter management */

		def enableFilter(filterName: String): Filter = s.enableFilter(filterName)
		def disableFilter(filterName: String): Unit = s.disableFilter(filterName)

		def getEnabledFilter(filterName: String): Option[Filter] = Option(s.getEnabledFilter(filterName))
		def isFilterEnabled(filterName: String): Boolean = getEnabledFilter(filterName).nonEmpty

		/* Mutating methods */

		def saveOrUpdate[A](obj: A): Unit = maintenanceGuard(s.saveOrUpdate(obj))
		def save[A](obj: A): java.io.Serializable = maintenanceGuard(s.save(obj))
		def update[A](obj: A): Unit = maintenanceGuard(s.update(obj))
		def delete[A](obj: A): Unit = maintenanceGuard(s.delete(obj))

		def execute(query: ScalaQuery[_]): Int = maintenanceGuard(query.executeUpdate())
		def execute(query: ScalaSQLQuery[_]): Int = maintenanceGuard(query.executeUpdate())

		/* Utilities */

		def evict[A](obj: A): Unit = s.evict(obj)
		def flush(): Unit = s.flush()
		def clear(): Unit = s.clear()
		def close(): Unit = SessionFactoryUtils.closeSession(s)

		private def maintenanceGuard[A](fn: => A) =
			if (!isReadOnly) fn
			else throw maintenanceModeService.exception(None)

		private def isReadOnlyMasquerade =
			RequestInfo.fromThread.exists { info =>
				info.user.masquerading && !info.user.sysadmin && !features.masqueradersCanWrite
			}

		private def isReadOnly = maintenanceModeService.enabled || isReadOnlyMasquerade

	}
}