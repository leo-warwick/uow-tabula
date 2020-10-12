package uk.ac.warwick.tabula.helpers

import java.util.concurrent.{ScheduledExecutorService, TimeoutException}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

trait Futures {

  def flatten[A, M[X] <: TraversableOnce[X]](in: Future[M[A]]*)(implicit executor: ExecutionContext): Future[Seq[A]] =
    Future.sequence(in).map(_.flatten)

  /**
    * Combine a set of futures if they all succeeded.
    */
  def combine[A](in: Seq[Future[A]], fn: Seq[A] => A)(implicit executor: ExecutionContext): Future[A] =
    Future.sequence(in).map(fn)

  /**
    * Combine a set of futures if at least 1 of them succeeded.
    *
    * @param in A non-empty sequence of futures.
    */
  def combineAnySuccess[A](in: Seq[Future[A]], fn: Seq[A] => A)(implicit executor: ExecutionContext): Future[A] = {
    require(in.nonEmpty, "in cannot be empty")

    // Convert each Future[A] to Future[Try[A]] that will always "succeed"
    val tryFutures = in.map(_.transformWith(Future.successful))

    Future.sequence(tryFutures).map { trys =>
      if (trys.forall(_.isFailure)) {
        // Everything failed - return the first one as the failure
        trys.head.get
      } else {
        //
        fn(trys.flatMap(_.toOption))
      }
    }
  }

  /**
    * Constructs a Future that will return the result (or failure) of the passed Future if it completes within
    * the specified Duration, else will return a failure with a TimeoutException.
    */
  def withTimeout[A](f: Future[A], duration: Duration)(implicit executor: ExecutionContext, scheduler: ScheduledExecutorService): Future[A] = {
    val p = scala.concurrent.Promise[A]()

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
    withTimeout(f, duration).map {
      Some(_)
    }
      .recover { case _: TimeoutException => None }

}

object Futures extends Futures

object ExecutionContexts {
  implicit lazy val global: ExecutionContext = new SessionAwareExecutionContext(parallelism = 100)

  implicit lazy val email: ExecutionContext = new SessionAwareExecutionContext(parallelism = 50)

  implicit lazy val timetable: ExecutionContext = new SessionAwareExecutionContext(parallelism = 200)
}
