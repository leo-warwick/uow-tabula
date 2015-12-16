package uk.ac.warwick.tabula.data.commands

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

import scala.collection.JavaConverters._

class UpstreamAssessmentGroupCreationFixtureCommandInternal extends CommandInternal[UpstreamAssessmentGroup] {
	 self: AssessmentMembershipServiceComponent with TransactionalComponent =>

	 var moduleCode: String = _
	 var assessmentGroup = "A"
	 var occurrence = "A"
	 var sequence = "A01"
	 var universityIds: JList[String] = JArrayList()

	 def applyInternal() = transactional() {
		 val group = new UpstreamAssessmentGroup
		 group.moduleCode = moduleCode
		 group.occurrence = occurrence
		 group.assessmentGroup = assessmentGroup
		 group.sequence = sequence
		 group.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		 group.members.knownType.staticUserIds = universityIds.asScala

		 assessmentMembershipService.save(group)
		 group
	 }

 }

object UpstreamAssessmentGroupCreationFixtureCommand {
	def apply() =
		new UpstreamAssessmentGroupCreationFixtureCommandInternal
			with ComposableCommand[UpstreamAssessmentGroup]
			with AutowiringAssessmentMembershipServiceComponent
			with AutowiringTransactionalComponent
			with Unaudited
			with PubliclyVisiblePermissions
}
