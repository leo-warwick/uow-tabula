package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, AssessmentMembershipInfo}
import collection.JavaConverters._
import uk.ac.warwick.userlookup.User

trait Assessment extends GeneratedId with CanBeDeleted with PermissionsTarget {

	@transient
	var assessmentMembershipService: AssessmentMembershipService = Wire[AssessmentMembershipService]("assignmentMembershipService")
	var module: Module
	var academicYear: AcademicYear
	var name: String
	var assessmentGroups: JList[AssessmentGroup]
	var markingWorkflow: MarkingWorkflow
	var firstMarkers: JList[FirstMarkersMap]
	var secondMarkers: JList[SecondMarkersMap]

	/** Map between first markers and the students assigned to them */
	def firstMarkerMap: Map[String, UserGroup] = Option(firstMarkers).map { markers => markers.asScala.map {
		markerMap => markerMap.marker_id -> markerMap.students
	}.toMap }.getOrElse(Map())

	/** Map between second markers and the students assigned to them */
	def secondMarkerMap: Map[String, UserGroup] = Option(secondMarkers).map { markers => markers.asScala.map {
		markerMap => markerMap.marker_id -> markerMap.students
	}.toMap }.getOrElse(Map())


	def addDefaultFeedbackFields(): Unit
	def addDefaultFields(): Unit
	def allFeedback: Seq[Feedback]

	// feedback that has been been through the marking process (not placeholders for marker feedback)
	def fullFeedback: Seq[Feedback] = allFeedback.filterNot(_.isPlaceholder)

	// returns feedback for a specified student
	def findFeedback(usercode: String): Option[Feedback] = allFeedback.find(_.usercode == usercode)

	// returns feedback for a specified student
	def findFullFeedback(usercode: String): Option[Feedback] = fullFeedback.find(_.usercode == usercode)

	// converts the assessmentGroups to upstream assessment groups
	def upstreamAssessmentGroups: Seq[UpstreamAssessmentGroup] =
		assessmentGroups.asScala.flatMap {
			_.toUpstreamAssessmentGroup(academicYear)
		}

	// Gets a breakdown of the membership for this assessment. Note that this cannot be sorted by seat number
	def membershipInfo: AssessmentMembershipInfo = assessmentMembershipService.determineMembership(this)

	def collectMarks: JBoolean

	def hasWorkflow: Boolean = markingWorkflow != null

	def isMarker(user: User): Boolean = isFirstMarker(user) || isSecondMarker(user)

	def isFirstMarker(user: User): Boolean = {
		if (markingWorkflow != null)
			markingWorkflow.firstMarkers.includesUser(user)
		else false
	}

	def isSecondMarker(user: User): Boolean = {
		if (markingWorkflow != null)
			markingWorkflow.secondMarkers.includesUser(user)
		else false
	}

	def isThirdMarker(user: User): Boolean = {
		if (markingWorkflow != null)
			markingWorkflow.thirdMarkers.includesUser(user)
		else false
	}


	def isReleasedForMarking(usercode: String): Boolean =
		allFeedback.find(_.usercode == usercode) match {
			case Some(f) => f.firstMarkerFeedback != null
			case _ => false
		}

	def isReleasedToSecondMarker(usercode: String): Boolean =
		allFeedback.find(_.usercode == usercode) match {
			case Some(f) => f.secondMarkerFeedback != null
			case _ => false
		}

	def isReleasedToThirdMarker(usercode: String): Boolean =
		allFeedback.find(_.usercode == usercode) match {
			case Some(f) => f.thirdMarkerFeedback != null
			case _ => false
		}

}
