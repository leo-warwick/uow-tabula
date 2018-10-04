package uk.ac.warwick.tabula.commands.admin.syllabusplus

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description}
import uk.ac.warwick.tabula.data.model.SyllabusPlusLocation
import uk.ac.warwick.tabula.services.{AutowiringSyllabusPlusLocationServiceComponent, SyllabusPlusLocationServiceComponent}

object DeleteSyllabusPlusLocationCommand {
	def apply(location: SyllabusPlusLocation) =
		new DeleteSyllabusPlusLocationCommandInternal(location)
			with ComposableCommand[SyllabusPlusLocation]
			with DeleteSyllabusPlusLocationCommandState
			with AutowiringSyllabusPlusLocationServiceComponent
			with DeleteSyllabusPlusLocationCommandDescription
			with ModifySyllabusPlusLocationPermissions
}

trait DeleteSyllabusPlusLocationCommandState {
	val location: SyllabusPlusLocation
}

abstract class DeleteSyllabusPlusLocationCommandInternal(val location: SyllabusPlusLocation) extends CommandInternal[SyllabusPlusLocation]
	with SyllabusPlusLocationServiceComponent {
	override protected def applyInternal(): SyllabusPlusLocation = {
		syllabusPlusLocationService.delete(location)
		location
	}
}

trait DeleteSyllabusPlusLocationCommandDescription extends Describable[SyllabusPlusLocation]
with DeleteSyllabusPlusLocationCommandState {
	override def describe(d: Description): Unit = d.property("syllabusPlusLocation" -> location)
}

