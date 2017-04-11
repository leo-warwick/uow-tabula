package uk.ac.warwick.tabula.events

import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConversions._
import uk.ac.warwick.tabula.commands.Describable
import uk.ac.warwick.tabula.commands.Description

class CompositeEventListener(val listeners: JList[EventListener]) extends EventListener {

	override def beforeCommand(event: Event): Unit =
		for (listener <- listeners) listener.beforeCommand(event)

	override def afterCommand(event: Event, returnValue: Any): Unit =
		for (listener <- listeners) listener.afterCommand(event, returnValue)

	override def onException(event: Event, exception: Throwable): Unit =
		for (listener <- listeners) listener.onException(event, exception)

}