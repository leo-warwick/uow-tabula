package uk.ac.warwick.tabula.events
import uk.ac.warwick.tabula.commands.Describable
import uk.ac.warwick.tabula.commands.Description

trait EventListener {
	def beforeCommand(event: Event)
	def afterCommand(event: Event, returnValue: Any)
	def onException(event: Event, exception: Throwable)
}