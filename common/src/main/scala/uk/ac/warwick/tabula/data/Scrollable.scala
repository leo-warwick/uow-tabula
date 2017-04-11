package uk.ac.warwick.tabula.data

import org.hibernate.{Session, ScrollableResults}
import uk.ac.warwick.tabula.helpers.Closeables
import java.io.Closeable

object Scrollable {
	def apply[A](results: ScrollableResults, session: Session) =
		new ScrollableImpl(results, session, { a: Array[AnyRef] => a(0).asInstanceOf[A] })
}

trait Scrollable[A] {
	def take(count: Int): Iterator[A] with Closeable
	def takeWhile(f: (A) => Boolean): Iterator[A] with Closeable
	def all: Iterator[A] with Closeable

	def map[B](f: Array[AnyRef] => B): Scrollable[B]
}

/**
 * Wrapper for ScrollableResults. The Scrollable class does not have
 * any methods on it for iterating - first call take or takeWhile to
 * specify a condition for when to stop. Then you can call map or foreach
 * on the result.
 *
 * scrollable.take(100).map(..)
 * scrollable.takeWhile { a => ... }.map(..)
 *
 * You MUST call map or foreach exactly, to ensure the results are closed.
 */
class ScrollableImpl[A](results: ScrollableResults, session: Session, mappingFunction: (Array[AnyRef] => A)) extends Scrollable[A] {
	/**
	 * Return a result that only returns this number of results as a maximum.
	 * If there's always a maximum then it might be more efficient to set max
	 * results on the query itself. But this is useful if you want a DAO method
	 * to return a large set that can be limited later by the calling code.
	 */
	def take(count: Int): Iterator[A] with Closeable = new CountLimitedScrollable(count)
	def takeWhile(f: (A) => Boolean): Iterator[A] with Closeable = new WhileLimitedScrollable(f)
	def all: Iterator[A] with Closeable = new UnlimitedScrollable()

	def map[B](f: Array[AnyRef] => B): Scrollable[B] = new ScrollableImpl(results, session, f)

	trait ScrollableIterator extends Iterator[A] with Closeable {
		private var _nextChecked = false
		private var _next = false

		def hasNext: Boolean = {
			if (!_nextChecked) {
				_nextChecked = true
				_next = results.next()
			}

			_next
		}
		def next(): A = {
			if (!_nextChecked) _next = results.next()

			if (_next) {
				_nextChecked = false
				mappingFunction(results.get())
			} else throw new NoSuchElementException
		}

		override def foreach[U](f: A => U): Unit = Closeables.closeThis(results) { r =>
			super.foreach { entity: A =>
				f(entity)
				safeEvict(entity)
			}
		}

		def close(): Unit = results.close()
	}

	private def safeEvict(entity: A) =
		try { session.evict(entity) }
		catch { case e: IllegalArgumentException => /* Do nothing */ }

	class CountLimitedScrollable(count: Int) extends ScrollableIterator {
		private var _i = 0
		override def hasNext: Boolean = _i < count && super.hasNext
		override def next(): A = {
			val ret = super.next()
			_i += 1
			ret
		}
	}

	class WhileLimitedScrollable(f: (A) => Boolean) extends ScrollableIterator {
		private var _nextChecked = false
		private var _next = false
		private var _nextEntity: A = _

		override def hasNext: Boolean = {
			if (!_nextChecked) {
				_nextChecked = true
				_next = results.next()

				if (_next) {
					_nextEntity = mappingFunction(results.get())
					_next = f(_nextEntity)
				}
			}

			_next
		}
		override def next(): A = {
			if (!_nextChecked) _next = results.next()

			if (_next) {
				_nextChecked = false
				_nextEntity
			} else throw new NoSuchElementException
		}
	}

	class UnlimitedScrollable extends ScrollableIterator
}
