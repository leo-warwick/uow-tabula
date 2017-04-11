package uk.ac.warwick.tabula.commands.cm2.assignments.extensions

import org.joda.time.Days
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.helpers.coursework.ExtensionGraph
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, UserLookupService}
import scala.collection.JavaConverters._

class ListExtensionsForAssignmentCommand(val module: Module, val assignment: Assignment, val user: CurrentUser)
	extends Command[Seq[ExtensionGraph]] with ReadOnly with Unaudited {

	mustBeLinked(mandatory(assignment), mandatory(module))
	PermissionCheck(Permissions.Extension.Read, assignment)

	if (assignment.openEnded) throw new ItemNotFoundException

	var assignmentMembershipService: AssessmentMembershipService = Wire.auto[AssessmentMembershipService]
	var userLookup: UserLookupService = Wire.auto[UserLookupService]

	def applyInternal(): Seq[ExtensionGraph] = {
		val assignmentUsers = assignmentMembershipService.determineMembershipUsers(assignment)

		val assignmentMembership = assignmentUsers.map(u => u.getUserId -> u).toMap

		// all the users that aren't members of this assignment, but have submitted work to it
		val extensionsFromNonMembers = assignment.extensions.asScala.filterNot(x => assignmentMembership.contains(x.usercode))
		val nonMembers = userLookup.getUsersByUserIds(extensionsFromNonMembers.map(_.usercode))

		// build lookup of names from non members of the assignment that have submitted work plus members
		val students = nonMembers ++ assignmentMembership

		(for ((usercode, user) <- students) yield {
			val extension = assignment.extensions.asScala.find(_.usercode == usercode)
			val isAwaitingReview = extension exists (_.awaitingReview)
			val hasApprovedExtension = extension exists (_.approved)
			val hasRejectedExtension = extension exists (_.rejected)

			// use real days not working days, for displayed duration, as markers need to know how late it *actually* would be after deadline
			val duration = extension.flatMap(_.expiryDate).map(Days.daysBetween(assignment.closeDate, _).getDays).getOrElse(0)

			val requestedExtraDuration = (for (e <- extension; requestedExpiryDate <- e.requestedExpiryDate) yield {
				Days.daysBetween(e.expiryDate.getOrElse(assignment.closeDate), requestedExpiryDate).getDays
			}).getOrElse(0)

			new ExtensionGraph(
				user,
				assignment.submissionDeadline(user),
				isAwaitingReview,
				hasApprovedExtension,
				hasRejectedExtension,
				duration,
				requestedExtraDuration,
				extension
			)
		}).toSeq
	}
}
