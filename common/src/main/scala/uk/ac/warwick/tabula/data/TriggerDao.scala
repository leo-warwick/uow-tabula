package uk.ac.warwick.tabula.data

import org.hibernate.criterion.{Order, Restrictions}
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.ToEntityReference
import uk.ac.warwick.tabula.data.model.triggers.Trigger

trait TriggerDaoComponent {
	def triggerDao: TriggerDao
}

trait AutowriringTriggerDaoComponent extends TriggerDaoComponent{
	val triggerDao: TriggerDao = Wire[TriggerDao]
}

trait TriggerDao {
	def triggersToRun(limit: Int): Seq[Trigger[_  >: Null <: ToEntityReference, _]]
	def save(trigger: Trigger[_  >: Null <: ToEntityReference, _]): Unit
	def getUncompletedTriggers(entity: Any): Seq[Trigger[_  >: Null <: ToEntityReference, _]]
	def delete(trigger: Trigger[_  >: Null <: ToEntityReference, _]): Unit
}

@Repository
class TriggerDaoImpl extends TriggerDao with Daoisms {

	override def triggersToRun(limit: Int): Seq[Trigger[_  >: Null <: ToEntityReference, _]] = {
		session.newCriteria[Trigger[_  >: Null <: ToEntityReference, _]]
			.add(Restrictions.isNull("completedDate"))
			.add(Restrictions.le("scheduledDate", DateTime.now))
			.addOrder(Order.asc("scheduledDate"))
			.setMaxResults(limit)
			.seq
	}

	override def save(trigger: Trigger[_  >: Null <: ToEntityReference, _]): Unit = {
		session.saveOrUpdate(trigger)
	}

	override def getUncompletedTriggers(entity: Any): Seq[Trigger[_ >: Null <: ToEntityReference, _]] = {
		val targetEntity = entity match {
			case ref: ToEntityReference => ref.toEntityReference.entity
			case _ => entity
		}
		session.newCriteria[Trigger[_  >: Null <: ToEntityReference, _]]
			.createAlias("target", "target")
			.add(Restrictions.eq("target.entity", targetEntity))
			.add(Restrictions.isNull("completedDate"))
			.addOrder(Order.asc("scheduledDate"))
			.seq
	}

	override def delete(trigger: Trigger[_  >: Null <: ToEntityReference, _]): Unit = session.delete(trigger)

}
