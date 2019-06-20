package uk.ac.warwick.tabula.events

import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.commands.Describable
import uk.ac.warwick.tabula.commands.Description

class CompositeEventListener(val listeners: JList[EventListener]) extends EventListener {

  override def beforeCommand(event: Event): Unit =
    for (listener <- listeners.asScala) listener.beforeCommand(event)

  override def afterCommand(event: Event, returnValue: Any, beforeEvent: Event): Unit =
    for (listener <- listeners.asScala) listener.afterCommand(event, returnValue, beforeEvent)

  override def onException(event: Event, exception: Throwable): Unit =
    for (listener <- listeners.asScala) listener.onException(event, exception)

}