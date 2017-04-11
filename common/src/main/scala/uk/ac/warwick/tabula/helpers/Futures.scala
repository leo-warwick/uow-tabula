package uk.ac.warwick.tabula.helpers

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeoutException}

import org.hibernate.{FlushMode, SessionFactory}
import org.springframework.orm.hibernate4.{SessionFactoryUtils, SessionHolder}
import org.springframework.transaction.support.TransactionSynchronizationManager
import uk.ac.warwick.spring.Wire

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

trait Futures {

	// TODO express this using combine()
	def flatten[A, M[X] <: TraversableOnce[X]](in: Future[M[A]]*)(implicit executor: ExecutionContext): Future[Seq[A]] = {
		val p = scala.concurrent.Promise[Seq[A]]

		// If any of the Futures fail, fire the first failure up to the Promise
		in.foreach { _.onFailure { case t => p.tryFailure(t) } }

		// Get the sequential result of the futures and flatten them
		Future.sequence(in).foreach { results => p.trySuccess(results.flatten) }

		p.future
	}

	def combine[A](in: Seq[Future[A]], fn: (Seq[A] => A))(implicit executor: ExecutionContext): Future[A] = {
		val p = scala.concurrent.Promise[A]

		// If any of the Futures fail, fire the first failure up to the Promise
		in.foreach { _.onFailure { case t => p.tryFailure(t) } }

		// Get the sequential result of the futures and flatten them
		Future.sequence(in).foreach { results => p.trySuccess(fn(results)) }

		p.future
	}

	/**
		* Constructs a Future that will return the result (or failure) of the passed Future if it completes within
		* the specified Duration, else will return a failure with a TimeoutException.
		*/
	def withTimeout[A](f: Future[A], duration: Duration)(implicit executor: ExecutionContext, scheduler: ScheduledExecutorService): Future[A] = {
		val p = scala.concurrent.Promise[A]

		// After the specified time, fail the promise
		scheduler.schedule(Runnable {
			p.tryFailure(new TimeoutException)
		}, duration.length, duration.unit)

		p.tryCompleteWith(f)

		p.future
	}

	/**
		* Returns a Future that recovers from a timeout by returning an empty Option, or a non-empty option if it completes
		* successfully within the specified timeout.
		*/
	def optionalTimeout[A](f: Future[A], duration: Duration)(implicit executor: ExecutionContext, scheduler: ScheduledExecutorService): Future[Option[A]] =
		withTimeout(f, duration).map { Some(_) }
			  .recover { case _: TimeoutException => None }

}

object Futures extends Futures {

	private lazy val executionContextExecutor = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

	/**
		* One big problem with threads is that Hibernate binds the current session to a thread local, and then other
		* things depend on it. What's a safe way to do that? Well there isn't one, really. We can open a new read-only
		* session and bind that, though, which is better than always having a null session.
		*/
	implicit lazy val executionContext = new ExecutionContext {
		private lazy val sessionFactory: Option[SessionFactory] = Wire.option[SessionFactory]

		override def execute(runnable: java.lang.Runnable): Unit = sessionFactory match {
			case None =>
				// Mostly for tests. To avoid there not being a session available, we'll re-use the existing
				// one - we don't do this in the general case because session's aren't supposed to be thread-safe

				// Grab any resources bound to ThreadLocals and re-bind them inside the Runnable
				val resources = Map(TransactionSynchronizationManager.getResourceMap.asScala.toSeq: _*)

				executionContextExecutor.execute(Runnable {
					// Don't try and rebind or unbind something already bound
					val resourcesToBind = resources.filterNot { case (key, _) => TransactionSynchronizationManager.hasResource(key) }

					try {
						resourcesToBind.foreach { case (key, value) => TransactionSynchronizationManager.bindResource(key, value) }
						runnable.run()
					} finally {
						resourcesToBind.keys.foreach(TransactionSynchronizationManager.unbindResource)
					}
				})

			case Some(sf) => executionContextExecutor.execute(Runnable {
				val session = sf.openSession()
				session.setDefaultReadOnly(true)
				session.setFlushMode(FlushMode.MANUAL)

				val sessionHolder = new SessionHolder(session)

				try {
					TransactionSynchronizationManager.bindResource(sf, sessionHolder)
					runnable.run()
				} finally {
					TransactionSynchronizationManager.unbindResourceIfPossible(sf)
					SessionFactoryUtils.closeSession(session)
				}
			})
		}
		override def reportFailure(cause: Throwable): Unit = executionContextExecutor.reportFailure(cause)
	}

}