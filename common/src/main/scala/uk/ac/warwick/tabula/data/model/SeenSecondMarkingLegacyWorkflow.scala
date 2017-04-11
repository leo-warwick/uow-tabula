package uk.ac.warwick.tabula.data.model

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model.MarkingMethod.SeenSecondMarkingLegacy

@Entity
@DiscriminatorValue(value="SeenSecondMarking")
class SeenSecondMarkingLegacyWorkflow extends MarkingWorkflow with NoThirdMarker with AssessmentMarkerMap {

	def this(dept: Department) = {
		this()
		this.department = dept
	}

	def markingMethod = SeenSecondMarkingLegacy

	override def firstMarkerRoleName: String = "First marker"
	def hasSecondMarker = true
	def secondMarkerRoleName = Some("Second marker")
	def secondMarkerVerb = Some("mark")
}
