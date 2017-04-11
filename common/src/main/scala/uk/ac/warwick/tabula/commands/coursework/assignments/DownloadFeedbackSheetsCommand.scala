package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description, ReadOnly}
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, AssessmentService, ZipService}
import uk.ac.warwick.userlookup.User


/**
 * Downloads a feedback sheet per student in the assignment member list
 */
class DownloadFeedbackSheetsCommand(val module: Module, val assignment: Assignment) extends Command[RenderableFile]
	with ReadOnly with Logging {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.AssignmentFeedback.Read, assignment)

	var members: Seq[User] = _

	var zipService: ZipService = Wire.auto[ZipService]
	var assignmentService: AssessmentService = Wire.auto[AssessmentService]
	var assignmentMembershipService: AssessmentMembershipService = Wire.auto[AssessmentMembershipService]

	override def applyInternal(): RenderableFile = {
		if (assignment.feedbackTemplate == null) logger.error("No feedback sheet for assignment - " + assignment.id)
		if (members == null)
			members = assignmentMembershipService.determineMembershipUsers(assignment)

		zipService.getMemberFeedbackTemplates(members, assignment)
	}

	override def describe(d: Description): Unit = {
		val members = assignmentMembershipService.determineMembershipUsers(assignment)
		d.assignment(assignment)
		d.studentIds(members.flatMap(m => Option(m.getWarwickId)))
		d.studentUsercodes(members.map(_.getUserId))
	}
}
