package uk.ac.warwick.tabula.data

import org.springframework.transaction._
import org.springframework.transaction.annotation._
import org.springframework.transaction.interceptor._
import org.springframework.transaction.support._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.MaintenanceModeService

trait TransactionalComponent {
	def transactional[A](
		readOnly: Boolean = false,
		propagation: Propagation = Propagation.REQUIRED
	)(f: => A)(implicit maintenanceModeService: MaintenanceModeService): A
}

trait AutowiringTransactionalComponent extends TransactionalComponent {
	def transactional[A](
		readOnly: Boolean = false,
		propagation: Propagation = Propagation.REQUIRED
	)(f: => A)(implicit maintenanceModeService: MaintenanceModeService): A = Transactions.transactional(readOnly, propagation)(f)
}

object Transactions extends TransactionAspectSupport {

	// unused as we skip the method that calls it, but it checks that an attribute source is set.
	setTransactionAttributeSource(new MatchAlwaysTransactionAttributeSource)

	var transactionManager = Wire.auto[PlatformTransactionManager]

	override def getTransactionManager = transactionManager

	var enabled = true

	/** Disable transaction processing inside this method block.
		* Should be for testing only.
		* TODO better way of disabling transactions inside tests?
		*/
	def disable(during: => Unit) = {
		try {
			enabled = false
			during
		} finally {
			enabled = true
		}
	}

	/** Does some code in a transaction.
		*/
	def transactional[A](
		readOnly: Boolean = false,
		propagation: Propagation = Propagation.REQUIRED
	)(f: => A): A = {

		if (enabled) {
			val attribute = new DefaultTransactionAttribute
			attribute.setReadOnly(readOnly || Wire.option[MaintenanceModeService].exists(_.enabled))
			attribute.setPropagationBehavior(propagation.value())
			handle(f, attribute)
		} else {
			// transactions disabled, just do the work.
			f
		}
	}

	/** Similar to transactional but a bit more involved. Provides access
		* to the TransactionStatus.
		*/
	def useTransaction[A](
		readOnly: Boolean = false,
		propagation: Propagation = Propagation.REQUIRED
	)(f: TransactionStatus => A) = {

		if (enabled) {
			val template = new TransactionTemplate(getTransactionManager())
			template.setReadOnly(readOnly || Wire.option[MaintenanceModeService].exists(_.enabled))
			template.setPropagationBehavior(propagation.value())
			template.execute(new TransactionCallback[A] {
				override def doInTransaction(status: TransactionStatus) = f(status)
			})
		} else {
			f
		}

	}

	private def handle[A](f: => A, attribute: TransactionAttribute): A = {
		try {
			createTransactionIfNecessary(getTransactionManager(), attribute, "Transactions.transactional()")
			val result = f
			commitTransactionAfterReturning(TransactionSupport.currentTransactionInfo)
			result
		} catch {
			case t: Throwable =>
				val info = TransactionSupport.currentTransactionInfo
				if (info != null) {
					val status = info.getTransactionStatus
					if (status != null && !status.isCompleted) {
						completeTransactionAfterThrowing(info, t)
					}
				}

				throw t
		} finally {
			cleanupTransactionInfo(TransactionSupport.currentTransactionInfo)
		}
	}


}