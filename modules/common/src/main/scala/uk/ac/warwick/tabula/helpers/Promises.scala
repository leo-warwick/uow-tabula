package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.util.concurrency.promise.UnfulfilledPromiseException

/**
 * A Promise is effectively a synchronous version of a Future. What
 * you're effectively saying to the code that you pass the Promise to is that
 * you agree to the contract that by the time the other code wants to use it,
 * the Promise will have been fulfilled.
 * <p>
 * An example of this is where you have a command to create a forum topic. This
 * might, internally, use a command to create the initial forum post, which
 * would usually require a topic to already exist. By passing the Promise of a
 * topic instead, you guarantee that by the time the post creation command gets
 * around to creating the post, the Promise of a topic will have been fulfilled.
 * <p>
 * In effect, any producer is itself a promise of the thing that it's producing.
 * The create post command is, effectively, a promise of a Post.
 */
trait Promise[A] {
	def get: A
}

trait MutablePromise[A] extends Promise[A] {
	def set(value: => A): MutablePromise[A]
}

trait Promises {
	
	def promise[A] = new FunctionalPromise(null.asInstanceOf[A])
	
	def promise[A](fn: => A) = new FunctionalPromise(fn)
	
	def optionPromise[A](fn: => Option[A]) = new OptionalPromise(fn)
	
}

object Promises extends Promises

class FunctionalPromise[A](fn: => A) extends MutablePromise[A] {
	
	private var value: A = _
	private var defined = false
	
	def get = {
		if (!defined) set(fn)
		
		value
	}
	def set(newValue: => A) = {
		value = newValue
		defined = true
		this
	}
	
}

class OptionalPromise[A](fn: => Option[A]) extends Promise[A] {
	
	private var result: A = _
	
	def get = {
		if (result == null) result = fn match {
			case Some(value) => value
			case _ => throw new UnfulfilledPromiseException("Fulfilled promise on Option(None)")
		}
		
		result
	}
	
}