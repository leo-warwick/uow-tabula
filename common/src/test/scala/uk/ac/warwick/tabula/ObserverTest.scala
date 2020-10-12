package uk.ac.warwick.tabula

import java.security.InvalidKeyException


/**
  * Test our hand-rolled observer classes in Reactor.
  */
class ObserverTest extends TestBase {

  import Reactor._

  class Service {
    var changedState: EventSource[Boolean] = EventSource[Boolean]()

    def updateState(state: Boolean): Unit = {
      changedState.emit(state)
    }
  }

  class ServiceObserver(service: Service) {
    var enabled = false

    service.changedState.observe { value =>
      enabled = value
    }

  }

  @Test def listening: Unit = {
    val service = new Service
    val observer = new ServiceObserver(service)

    observer.enabled should be(false)
    service updateState true

    observer.enabled should be(true)
    service updateState false

    observer.enabled should be(false)

  }

  // Exceptions thrown in observers bubble up to emit().
  @Test(expected = classOf[InvalidKeyException])
  def exceptions(): Unit = {
    val state = EventSource[Boolean]()
    state.observe { b =>
      throw new InvalidKeyException()
    }
    state.emit(true)
  }

}
