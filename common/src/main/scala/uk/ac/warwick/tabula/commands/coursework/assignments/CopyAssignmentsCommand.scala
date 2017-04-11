package uk.ac.warwick.tabula.commands.coursework.assignments

import org.joda.time.{DateTime, Duration}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.triggers.{AssignmentClosedTrigger, Trigger}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking

import scala.collection.JavaConverters._

object CopyAssignmentsCommand {
	def apply(department: Department, modules: Seq[Module]) =
		new CopyAssignmentsCommand(department, modules)
			with ComposableCommand[Seq[Assignment]]
			with CopyAssignmentsPermissions
			with CopyAssignmentsDescription
			with CopyAssignmentsCommandTriggers
			with CopyAssignmentsCommandNotifications
			with AutowiringAssessmentServiceComponent
			with AutowiringAssessmentMembershipServiceComponent {
				override lazy val eventName = "CopyAssignmentsFromPrevious"
			}
}

abstract class CopyAssignmentsCommand(val department: Department, val modules: Seq[Module]) extends CommandInternal[Seq[Assignment]]
	with Appliable[Seq[Assignment]] with CopyAssignmentsState with FindAssignmentFields {

	self: AssessmentServiceComponent with AssessmentMembershipServiceComponent =>

	def applyInternal(): Seq[Assignment] = {

		val scalaAssignments = assignments.asScala

		if (archive) {
			for (assignment <- scalaAssignments) {
				assignment.archive()
				assessmentService.save(assignment)
			}
		}

		scalaAssignments.map { assignment =>
			val newAssignment = copy(assignment)
			assessmentService.save(newAssignment)
			newAssignment
		}
	}

	def copy(assignment: Assignment) : Assignment = {
		val newAssignment = new Assignment()
		newAssignment.assignmentService = assignment.assignmentService // FIXME Used in testing
		newAssignment.academicYear = academicYear

		// best guess of new open and close dates. likely to be wrong by up to a few weeks but better than out by years
		val yearOffest = academicYear.startYear - assignment.academicYear.startYear
		newAssignment.openDate = assignment.openDate.plusYears(yearOffest).withDayOfWeek(assignment.openDate.getDayOfWeek)
		newAssignment.closeDate = newAssignment.openDate.plus(new Duration(assignment.openDate, assignment.closeDate))

		// copy the other fields from the target assignment
		newAssignment.module = assignment.module
		newAssignment.name = assignment.name
		newAssignment.openEnded = assignment.openEnded
		newAssignment.collectMarks = assignment.collectMarks
		newAssignment.collectSubmissions = assignment.collectSubmissions
		newAssignment.restrictSubmissions = assignment.restrictSubmissions
		newAssignment.allowLateSubmissions = assignment.allowLateSubmissions
		newAssignment.allowResubmission = assignment.allowResubmission
		newAssignment.displayPlagiarismNotice = assignment.displayPlagiarismNotice
		newAssignment.allowExtensions = assignment.allowExtensions
		newAssignment.extensionAttachmentMandatory = assignment.extensionAttachmentMandatory
		newAssignment.allowExtensionsAfterCloseDate = assignment.allowExtensionsAfterCloseDate
		newAssignment.summative = assignment.summative
		newAssignment.dissertation = assignment.dissertation
		newAssignment.feedbackTemplate = assignment.feedbackTemplate
		newAssignment.markingWorkflow = assignment.markingWorkflow
		newAssignment.includeInFeedbackReportWithoutSubmissions = assignment.includeInFeedbackReportWithoutSubmissions
		newAssignment.automaticallyReleaseToMarkers = assignment.automaticallyReleaseToMarkers
		newAssignment.automaticallySubmitToTurnitin = assignment.automaticallySubmitToTurnitin
		newAssignment.anonymousMarking = assignment.anonymousMarking

		newAssignment.addDefaultFields()

		newAssignment.addFields(assignment.fields.asScala.sortBy(_.position).map(field => {
			newAssignment.findField(field.name).foreach(newAssignment.removeField)
			field.duplicate(newAssignment)
		}):_*)

		// TAB-1175 Guess SITS links
		assignment.assessmentGroups.asScala
			.filter { _.toUpstreamAssessmentGroup(newAssignment.academicYear).isDefined } // Only where defined in the new year
			.foreach { group =>
				val newGroup = new AssessmentGroup
				newGroup.assessmentComponent = group.assessmentComponent
				newGroup.occurrence = group.occurrence
				newGroup.assignment = newAssignment
				newAssignment.assessmentGroups.add(newGroup)
			}

		newAssignment
	}
}

trait CopyAssignmentsPermissions extends ArchiveAssignmentsPermissions {
	self: CopyAssignmentsState =>
	override def permissionsCheck(p: PermissionsChecking) {
		if (archive) {
			super.permissionsCheck(p)
		}

		if (modules.isEmpty) p.PermissionCheck(Permissions.Assignment.Create, mandatory(department))
		else for (module <- modules) {
			p.mustBeLinked(p.mandatory(module), mandatory(department))
			p.PermissionCheck(Permissions.Assignment.Create, module)
		}
	}
}

trait CopyAssignmentsState extends ArchiveAssignmentsState {
	var academicYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(new DateTime)
	var archive: JBoolean = false
}

trait CopyAssignmentsDescription extends Describable[Seq[Assignment]] {
	self: CopyAssignmentsState =>
	def describe(d: Description): Unit = d
		.properties("modules" -> modules.map(_.id))
		.properties("assignments" -> assignments.asScala.map(_.id))
		.properties("isArchiving" -> archive)
}

trait CopyAssignmentsCommandTriggers extends GeneratesTriggers[Seq[Assignment]] {

	def generateTriggers(assignments: Seq[Assignment]): Seq[Trigger[_ >: Null <: ToEntityReference, _]] = {
		assignments.filter(assignment => assignment.closeDate != null && assignment.closeDate.isAfterNow).map(assignment =>
			AssignmentClosedTrigger(assignment.closeDate, assignment)
		)
	}
}

trait CopyAssignmentsCommandNotifications extends SchedulesNotifications[Seq[Assignment], Assignment] with SharedAssignmentCommandNotifications {

	override def transformResult(assignments: Seq[Assignment]): Seq[Assignment] = assignments

	override def scheduledNotifications(assignment: Assignment): Seq[ScheduledNotification[Assignment]] = generateScheduledNotifications(assignment)

}