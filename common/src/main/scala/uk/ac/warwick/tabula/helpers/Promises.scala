package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.util.concurrency.promise.UnfulfilledPromiseException
import uk.ac.warwick.tabula.commands.Command

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

	def promise[A]: MutablePromise[A] = new FunctionalPromise(null.asInstanceOf[A])

	def promise[A](fn: => A): MutablePromise[A] = new FunctionalPromise(fn)

	def optionPromise[A](fn: => Option[A]) = new OptionalPromise(fn)

}

object Promises extends Promises

private class FunctionalPromise[A](fn: => A) extends MutablePromise[A] {

	private var value: A = _
	private var defined = false

	def get: A = {
		if (!defined) {
			// eval
			val value = fn
			if (value == null) throw new UnfulfilledPromiseException("Promised value was null")
			else set(value)
		}

		value
	}
	def set(newValue: => A): FunctionalPromise[A] = {
		value = newValue
		defined = true
		this
	}

}

private object FunctionalPromise {
	def empty[A]: Unit = {

	}
}

class OptionalPromise[A](fn: => Option[A]) extends Promise[A] {

	private var result: A = _

	def get: A = {
		if (result == null) result = fn match {
			case Some(value) => value
			case _ => throw new UnfulfilledPromiseException("Fulfilled promise on Option(None)")
		}

		result
	}

}