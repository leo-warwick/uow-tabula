package uk.ac.warwick.tabula.helpers

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeoutException}

import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.{Mockito, TestBase}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class FuturesTest extends TestBase with Mockito {

  implicit val taskSchedulerService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

  @Test
  def flatten(): Unit = {
    val f1 = Future.successful(Seq(1, 2, 3))
    val f2 = Future.successful(Seq(4, 5, 6))
    val f3 = Future.successful(Seq(7, 8, 9))

    Futures.flatten(f1, f2, f3).futureValue should be(Seq(1, 2, 3, 4, 5, 6, 7, 8, 9))
    Futures.flatten(f3, f2, f1).futureValue should be(Seq(7, 8, 9, 4, 5, 6, 1, 2, 3))
    Futures.flatten(f1, f1, f1).futureValue should be(Seq(1, 2, 3, 1, 2, 3, 1, 2, 3))
  }

  @Test
  def combineSuccess(): Unit = {
    val good = Future.successful(7)
    Futures.combine(Seq(good, good), (rs: Seq[Int]) => rs.sum).futureValue shouldBe 14
  }

  @Test
  def combineFailure(): Unit = {
    val zing = new RuntimeException("zing")
    val bad = Future.failed(zing)
    Futures.combine(Seq(bad, bad), (rs: Seq[Int]) => rs.sum).failed.futureValue shouldBe zing
  }

  @Test
  def combinePartFailure(): Unit = {
    val zing = new RuntimeException("zing")
    val good = Future.successful(7)
    val bad = Future.failed(zing)
    Futures.combine(Seq(good, bad), (rs: Seq[Int]) => rs.sum).failed.futureValue shouldBe zing
  }

  @Test
  def flattenWithFailure(): Unit = {
    val e = new IllegalStateException

    val f1 = Future.successful(Seq(1, 2, 3))
    val f2 = Future.failed(e)
    val f3 = Future.successful(Seq(7, 8, 9))

    val flat = Await.ready(Futures.flatten(f1, f2, f3), 100.millis)
    flat.isCompleted should be(true)
    flat.value should be(Some(Failure(e)))
  }

  @Test
  def withTimeout(): Unit = {
    val fast = Futures.withTimeout(Future.successful(Seq(1, 2, 3)), 100.millis)
    fast.futureValue should be(Seq(1, 2, 3))

    val sluggish = Futures.withTimeout(Future {
      Thread.sleep(50)
      Seq(1, 2, 3)
    }, 100.millis)
    Await.ready(sluggish, 200.millis).value should be(Some(Success(Seq(1, 2, 3))))

    val slow = Futures.withTimeout(Future {
      Thread.sleep(150)
      Seq(1, 2, 3)
    }, 100.millis)

    Await.ready(slow, 200.millis).value match {
      case Some(Failure(e: TimeoutException)) =>
      case any => fail(s"Expected a timeout exception but was $any")
    }
  }

  @Test
  def optionalTimeout(): Unit = {
    val fast = Futures.optionalTimeout(Future.successful(Seq(1, 2, 3)), 100.millis)
    fast.futureValue should be(Some(Seq(1, 2, 3)))

    val sluggish = Futures.optionalTimeout(Future {
      Thread.sleep(50)
      Seq(1, 2, 3)
    }, 100.millis)
    Await.ready(sluggish, 200.millis).value should be(Some(Success(Some(Seq(1, 2, 3)))))

    val slow = Futures.optionalTimeout(Future {
      Thread.sleep(150)
      Seq(1, 2, 3)
    }, 100.millis)
    Await.ready(slow, 200.millis).value should be(Some(Success(None)))
  }

}
