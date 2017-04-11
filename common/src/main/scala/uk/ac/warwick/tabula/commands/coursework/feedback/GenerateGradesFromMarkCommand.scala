package uk.ac.warwick.tabula.commands.coursework.feedback

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{GeneratesGradesFromMarks, AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object GenerateGradesFromMarkCommand {
	def apply(module: Module, assessment: Assessment) =
		new GenerateGradesFromMarkCommandInternal(module, assessment)
			with AutowiringAssessmentMembershipServiceComponent
			with ComposableCommand[Map[String, Seq[GradeBoundary]]]
			with GenerateGradesFromMarkPermissions
			with GenerateGradesFromMarkCommandState
			with GenerateGradesFromMarkCommandRequest
			with ReadOnly with Unaudited
}

class GenerateGradesFromMarkCommandInternal(val module: Module, val assessment: Assessment)
	extends CommandInternal[Map[String, Seq[GradeBoundary]]] with GeneratesGradesFromMarks {

	self: GenerateGradesFromMarkCommandRequest with AssessmentMembershipServiceComponent =>

	lazy val assignmentUpstreamAssessmentGroupMap: Map[AssessmentGroup, Option[UpstreamAssessmentGroup]] = assessment.assessmentGroups.asScala.map(group =>
		group -> group.toUpstreamAssessmentGroup(assessment.academicYear)
	).toMap

	private def isNotNullAndInt(intString: String): Boolean = {
		if (intString == null) {
			false
		} else {
			try {
				intString.toInt
				true
			} catch {
				case _ @ (_: NumberFormatException | _: IllegalArgumentException) =>
					false
			}
		}
	}

	override def applyInternal(): Map[String, Seq[GradeBoundary]] = {
		val membership = assessmentMembershipService.determineMembershipUsers(assessment)
		val studentMarksMap: Map[User, Int] = studentMarks.asScala
			.filter(s => isNotNullAndInt(s._2))
			.flatMap{case(uniID, mark) =>
				membership.find(_.getWarwickId == uniID).map(u => u -> mark.toInt)
			}.toMap

		val studentAssesmentComponentMap: Map[String, AssessmentComponent] = studentMarksMap.flatMap { case (student, _) =>
			assignmentUpstreamAssessmentGroupMap.find { case (group, upstreamGroup) =>
				upstreamGroup.exists(_.membersIncludes(student))
			}.map { case (group, _) => student.getWarwickId -> group.assessmentComponent }
		}

		studentMarks.asScala.map{case(uniId, mark) =>
			uniId -> studentAssesmentComponentMap.get(uniId).map(component => assessmentMembershipService.gradesForMark(component, mark.toInt)).getOrElse(Seq())
		}.toMap
	}

	override def applyForMarks(marks: Map[String, Int]): Map[String, Seq[GradeBoundary]] = {
		studentMarks = marks.mapValues(m => m.toString).asJava
		applyInternal()
	}

}

trait GenerateGradesFromMarkPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateGradesFromMarkCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assessment, module)
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assessment)
	}

}

trait GenerateGradesFromMarkCommandState {
	def module: Module
	def assessment: Assessment
}

trait GenerateGradesFromMarkCommandRequest {
	var studentMarks: JMap[String, String] = JHashMap()
	var selected: JMap[String, String] = JHashMap()
}
