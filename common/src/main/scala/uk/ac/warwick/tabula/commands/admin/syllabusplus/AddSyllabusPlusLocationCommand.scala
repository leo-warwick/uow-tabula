package uk.ac.warwick.tabula.commands.admin.syllabusplus

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand}
import uk.ac.warwick.tabula.data.model.SyllabusPlusLocation
import uk.ac.warwick.tabula.services.{AutowiringSyllabusPlusLocationServiceComponent, SyllabusPlusLocationServiceComponent}

object AddSyllabusPlusLocationCommand {
	def apply() =
		new AddSyllabusPlusLocationCommandInternal
			with ComposableCommand[SyllabusPlusLocation]
			with AutowiringSyllabusPlusLocationServiceComponent
			with EditSyllabusPlusLocationDescription
			with ModifySyllabusPlusLocationPermissions
			with SyllabusPlusLocationCommandValidation
}

abstract class AddSyllabusPlusLocationCommandInternal extends CommandInternal[SyllabusPlusLocation]
	with SyllabusPlusLocationCommandRequest
	with SyllabusPlusLocationServiceComponent {
	override protected def applyInternal(): SyllabusPlusLocation = {
		val location = new SyllabusPlusLocation
		location.upstreamName = upstreamName
		location.name = name
		location.mapLocationId = mapLocationId

		syllabusPlusLocationService.save(location)
		location
	}
}

trait SyllabusPlusLocationCommandRequest {
	var upstreamName: String = _
	var name: String = _
	var mapLocationId: String = _
}
