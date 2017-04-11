package uk.ac.warwick.tabula


/**
 * Replacement for React.
 */
object Reactor {
	def EventSource[A]() = new EventSource[A]

	/* An observable event source.

		object Service {
			val stateChanged = new EventSource[Boolean]

			def activate() {
				stateChanged.emit(true)
			}
		}

		Service.stateChanged.observe {
			active => println("Service active: " + active)
		}

		Service.activate()
	 */
	class EventSource[A]() {
		type Observer = (A) => Unit
		var observers: List[Observer] = List[Observer]()

		/**
		 * Observe events from this event source.
		 * WARNING - exceptions thrown in the observer
		 * will not be caught here, so it will caught
		 * the event source to fail. You might want to
		 * catch any exceptions that you wouldn't want
		 * to effect the operation of the event source.
		 */
		def observe(ob: Observer) {
			observers = (ob :: observers)
		}
		def emit(a:A) {
			observers.foreach { _(a) }
		}
	}

}


