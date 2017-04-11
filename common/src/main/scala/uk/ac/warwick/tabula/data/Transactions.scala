package uk.ac.warwick.tabula.data

import org.springframework.transaction._
import org.springframework.transaction.support._
import org.springframework.transaction.annotation._
import org.springframework.transaction.interceptor._
import uk.ac.warwick.spring.Wire

trait TransactionalComponent {
	def transactional[A](
												readOnly: Boolean = false,
												propagation: Propagation = Propagation.REQUIRED
												)(f: => A): A
}

trait AutowiringTransactionalComponent extends TransactionalComponent {
	def transactional[A](
												readOnly: Boolean = false,
												propagation: Propagation = Propagation.REQUIRED
												)(f: => A): A = Transactions.transactional(readOnly, propagation)(f)

}

object Transactions extends TransactionAspectSupport {

	// unused as we skip the method that calls it, but it checks that an attribute source is set.
	setTransactionAttributeSource(new MatchAlwaysTransactionAttributeSource)

	var transactionManager: PlatformTransactionManager = Wire.auto[PlatformTransactionManager]
	override def getTransactionManager(): PlatformTransactionManager = transactionManager

	var enabled = true

	/** Disable transaction processing inside this method block.
	  * Should be for testing only.
	  * TODO better way of disabling transactions inside tests?
	  */
	def disable(during: =>Unit): Unit = {
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
				attribute.setReadOnly(readOnly)
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
		)(f: TransactionStatus => A): Any = {

		if (enabled) {
			val template = new TransactionTemplate(getTransactionManager())
			template.setReadOnly(readOnly)
			template.setPropagationBehavior(propagation.value())
			template.execute(new TransactionCallback[A] {
				override def doInTransaction(status: TransactionStatus): A = f(status)
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
			case t: Throwable => {

				val info = TransactionSupport.currentTransactionInfo
				if (info != null) {
					val status = info.getTransactionStatus()
					if (status != null && !status.isCompleted) {
						completeTransactionAfterThrowing(info, t)
					}
				}

				throw t
			}
		} finally {
			cleanupTransactionInfo(TransactionSupport.currentTransactionInfo);
		}
	}


}