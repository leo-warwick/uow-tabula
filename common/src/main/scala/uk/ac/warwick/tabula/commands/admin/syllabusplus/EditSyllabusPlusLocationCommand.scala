package uk.ac.warwick.tabula.commands.admin.syllabusplus

import org.springframework.validation.{Errors, ValidationUtils}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.SyllabusPlusLocation
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSyllabusPlusLocationServiceComponent, SyllabusPlusLocationServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object EditSyllabusPlusLocationCommand {
	def apply(location: SyllabusPlusLocation) =
		new EditSyllabusPlusLocationCommandInternal(location)
			with ComposableCommand[SyllabusPlusLocation]
			with AutowiringSyllabusPlusLocationServiceComponent
			with ModifySyllabusPlusLocationPermissions
			with EditSyllabusPlusLocationDescription
			with SyllabusPlusLocationCommandValidation
}

abstract class EditSyllabusPlusLocationCommandInternal(location: SyllabusPlusLocation) extends CommandInternal[SyllabusPlusLocation]
	with SyllabusPlusLocationCommandRequest
	with SyllabusPlusLocationServiceComponent {
	copyFrom(location)

	def copyFrom(location: SyllabusPlusLocation): Unit = {
		upstreamName = location.upstreamName
		name = location.name
		mapLocationId = location.mapLocationId
	}

	override protected def applyInternal(): SyllabusPlusLocation = {
		location.upstreamName = upstreamName
		location.name = name
		location.mapLocationId = mapLocationId

		syllabusPlusLocationService.save(location)
		location
	}
}

trait SyllabusPlusLocationCommandValidation extends SelfValidating
	with SyllabusPlusLocationCommandRequest {
	override def validate(errors: Errors): Unit = {
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "upstreamName", "NotEmpty")
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "syllabusPlusLocation.name.notEmpty")

		if (!mapLocationId.matches("^\\d+$")) {
			errors.rejectValue("name", "syllabusPlusLocation.noMapLocationId")
		}
	}
}

trait EditSyllabusPlusLocationDescription extends Describable[SyllabusPlusLocation]
	with SyllabusPlusLocationCommandRequest {
	override def describe(d: Description): Unit =
		d.properties(
			"upstreamName" -> upstreamName,
			"name" -> name,
			"mapLocationId" -> mapLocationId
		)

	override def describeResult(d: Description, result: SyllabusPlusLocation): Unit =
		d.property("syllabusPlusLocation" -> result)
}

trait ModifySyllabusPlusLocationPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.ManageSyllabusPlusLocations)
	}
}