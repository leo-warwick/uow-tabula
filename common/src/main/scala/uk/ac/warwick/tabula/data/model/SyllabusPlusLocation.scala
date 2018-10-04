package uk.ac.warwick.tabula.data.model

import javax.persistence.{Column, Entity}
import uk.ac.warwick.tabula.ToString

@Entity
class SyllabusPlusLocation extends GeneratedId with ToString {
	@Column(name = "upstream_name")
	var upstreamName: String = _

	var name: String = _

	@Column(name = "map_location_id")
	var mapLocationId: String = _

	override def toStringProps: Seq[(String, Any)] = Seq(
		"upstreamName" -> upstreamName,
		"name" -> name,
		"mapLocationId" -> mapLocationId
	)

	def asMapLocation: MapLocation = MapLocation(name, mapLocationId, Some(upstreamName))
}

object SyllabusPlusLocation {
	def apply(upstreamName: String, name: String, mapLocationId: String) = {
		val location = new SyllabusPlusLocation
		location.upstreamName = upstreamName
		location.name = name
		location.mapLocationId = mapLocationId
		location
	}
}
