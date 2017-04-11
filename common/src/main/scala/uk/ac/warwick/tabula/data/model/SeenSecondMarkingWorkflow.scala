package uk.ac.warwick.tabula.data.model

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model.MarkingMethod.SeenSecondMarking

@Entity
@DiscriminatorValue(value="SeenSecondMarkingNew")
class SeenSecondMarkingWorkflow extends MarkingWorkflow with AssessmentMarkerMap {

	def this(dept: Department) = {
		this()
		this.department = dept
	}

	def markingMethod = SeenSecondMarking

	override def firstMarkerRoleName: String = "First marker"
	def hasSecondMarker = true
	def secondMarkerRoleName = Some("Second marker")
	def secondMarkerVerb = Some("mark")
	def hasThirdMarker = true
	def thirdMarkerRoleName = Some("Final marker")
	def thirdMarkerVerb = Some("mark")

	override def thirdMarkers: UnspecifiedTypeUserGroup = this.firstMarkers

	def getStudentsThirdMarker(assessment: Assessment, usercode: Usercode): Option[String] =
		MarkingWorkflow.getMarkerFromAssessmentMap(userLookup, usercode, assessment.firstMarkerMap)
}
