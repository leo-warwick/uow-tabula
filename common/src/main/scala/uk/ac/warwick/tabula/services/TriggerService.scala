package uk.ac.warwick.tabula.services

import org.hibernate.ObjectNotFoundException
import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.triggers.Trigger
import uk.ac.warwick.tabula.data.{AutowriringTriggerDaoComponent, TriggerDaoComponent}
import uk.ac.warwick.tabula.helpers.Logging

trait TriggerService {
	def removeExistingTriggers(target: Any): Unit
	def push(trigger: Trigger[_ >: Null <: ToEntityReference, _]): Unit
	def processTriggers(): Unit
}

abstract class AbstractTriggerService extends TriggerService with Logging {

	self: TriggerDaoComponent =>

	val RunBatchSize = 10

	def removeExistingTriggers(target: Any): Unit = {
		val exisitingTriggers = triggerDao.getUncompletedTriggers(target)
		exisitingTriggers.foreach(triggerDao.delete)
	}

	def push(trigger: Trigger[_ >: Null <: ToEntityReference, _]): Unit = {
		triggerDao.save(trigger)
	}

	def processTriggers(): Unit = {
		val triggers = transactional(readOnly = true) {
			triggerDao.triggersToRun(RunBatchSize)
		}

		triggers.foreach { trigger =>
			transactional() {
				logger.info(s"Processing trigger $trigger")

				try {
					trigger.target.entity match {
						case entity: CanBeDeleted if entity.deleted =>
						case entity =>
							trigger.apply()
					}
				} catch {
					// Can happen if reference to an entity has since been deleted, e.g.
					// a submission is resubmitted and the old submission is removed. Skip this trigger.
					case onf: ObjectNotFoundException =>
						debug("Skipping scheduled notification %s as a referenced object was not found", trigger)
				}

				trigger.completedDate = DateTime.now
				triggerDao.save(trigger)
			}
		}
	}

}

@Service
class TriggerServiceImpl extends AbstractTriggerService with AutowriringTriggerDaoComponent