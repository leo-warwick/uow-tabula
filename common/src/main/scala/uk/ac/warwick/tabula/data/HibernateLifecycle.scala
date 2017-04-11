package uk.ac.warwick.tabula.data
import org.hibernate.event.spi._
import javax.annotation.PostConstruct
import org.springframework.stereotype.Component
import org.springframework.beans.factory.InitializingBean
import org.hibernate.SessionFactory
import org.hibernate.internal.SessionFactoryImpl
import org.hibernate.event.service.spi.EventListenerRegistry

/**
 * Method for Hibernate to call whenever it loads an object from the database.
 */

trait PreLoadBehaviour {
	def preLoad()
}

/** Happens before save */
trait PreSaveBehaviour {
	def preSave(newRecord: Boolean)
}

trait PostLoadBehaviour {
	def postLoad()
}

class HibernateLifecycle extends InitializingBean with PostLoadEventListener with PreLoadEventListener with PreInsertEventListener with PreUpdateEventListener {

	var sessionFactory: SessionFactory = _

	override def afterPropertiesSet() {
		assert(sessionFactory != null)

		// Supposed correct way to register listeners is to wire in your own Integrator,
		// but documentation on how to do that is scant. So hack into SessionFactoryImpl!
		val ipl = sessionFactory.asInstanceOf[SessionFactoryImpl]
		val serviceRegistry = ipl.getServiceRegistry
		val registry = serviceRegistry.getService(classOf[EventListenerRegistry])
		registry.appendListeners(EventType.POST_LOAD, this)
		registry.appendListeners(EventType.PRE_LOAD, this)
		registry.appendListeners(EventType.PRE_INSERT, this)
		registry.appendListeners(EventType.PRE_UPDATE, this)
	}

	override def onPostLoad(event: PostLoadEvent) {
		event.getEntity match {
			case listener: PostLoadBehaviour => listener.postLoad()
			case _ =>
		}
	}

	override def onPreInsert(event: PreInsertEvent): Boolean = {
		event.getEntity match {
			case listener: PreSaveBehaviour => listener.preSave(true)
			case _ =>
		}
		false
	}

	override def onPreUpdate(event: PreUpdateEvent): Boolean = {
		event.getEntity match {
			case listener: PreSaveBehaviour => listener.preSave(false)
			case _ =>
		}
		false
	}

	override def onPreLoad(event: PreLoadEvent) {
		event.getEntity match {
			case listener: PreLoadBehaviour => listener.preLoad()
			case _ =>
		}
	}
}

