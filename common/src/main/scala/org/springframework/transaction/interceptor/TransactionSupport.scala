package org.springframework.transaction.interceptor

/**
 * Required by the Transactions object to access the currentTransactionInfo, which is
 * static and protected meaning it can only be access from within the same package.
 */
object TransactionSupport extends TransactionAspectSupport {
  def currentTransactionInfo: TransactionAspectSupport#TransactionInfo = TransactionAspectSupport.currentTransactionInfo()
}