package uk.ac.warwick.tabula.commands.profiles

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order._
import org.joda.time.DateTime
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.profiles.ViewRelatedStudentsCommand.Result
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, FiltersRelationships, ReadOnly, TaskBenchmarking, Unaudited, _}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}


object ViewRelatedStudentsCommand{

	type LastMeetingWithTotalPendingApprovalsMap = Map[String, (Option[MeetingRecord], Int)]
	type CommandType = Appliable[Result]

	case class Result(
		entities: Seq[StudentCourseDetails],
		lastMeetingWithTotalPendingApprovalsMap: LastMeetingWithTotalPendingApprovalsMap
	)

	def apply(currentMember: Member, relationshipType: StudentRelationshipType): Command[Result] = {
		new ViewRelatedStudentsCommandInternal(currentMember, relationshipType)
			with ComposableCommand[Result]
			with AutowiringProfileServiceComponent
			with AutowiringMeetingRecordServiceComponent
			with AutowiringRelationshipServiceComponent
			with ViewRelatedStudentsCommandPermissions
			with Unaudited with ReadOnly
	}
}


trait ViewRelatedStudentsCommandState extends FiltersRelationships {
	self: ProfileServiceComponent =>

	val currentMember: Member
	val relationshipType: StudentRelationshipType

	var studentsPerPage = FiltersRelationships.DefaultStudentsPerPage
	var page = 1

	var departments: JList[Department] = JArrayList()
	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
	var sortOrder: JList[Order] = JArrayList()

	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()

	lazy val allCourses: Seq[StudentCourseDetails] =
		profileService.getSCDsByAgentRelationshipAndRestrictions(relationshipType, currentMember, Nil)
	lazy val allDepartments: Seq[Department] = allCourses.flatMap(c => Option(c.department)).distinct
	lazy val allRoutes: Seq[Route] = allCourses.flatMap(c => Option(c.currentRoute)).distinct
}

abstract class ViewRelatedStudentsCommandInternal(val currentMember: Member, val relationshipType: StudentRelationshipType)
	extends CommandInternal[Result] with TaskBenchmarking with ViewRelatedStudentsCommandState with BindListener {
	self: ProfileServiceComponent with MeetingRecordServiceComponent with RelationshipServiceComponent =>

	def applyInternal(): Result =  {
		val year = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		val studentCourseDetails = profileService.getSCDsByAgentRelationshipAndRestrictions(relationshipType, currentMember, buildRestrictions(year))

		val lastMeetingWithTotalPendingApprovalsMap: Map[String, (Option[MeetingRecord], Int)] = studentCourseDetails.map(scd => {
			val rels = relationshipService.getRelationships(relationshipType, scd.student)
			val lastMeeting = benchmarkTask("lastMeeting"){
				meetingRecordService.list(rels.toSet, Some(currentMember)).filterNot(_.deleted).headOption
			}
			val totalPendingApprovals = benchmarkTask("totalPendingStudentApprovals"){
				meetingRecordService.countPendingApprovals(scd.student.universityId)
			}
			scd.student.universityId -> (lastMeeting, totalPendingApprovals)
		}).toMap

		Result(studentCourseDetails, lastMeetingWithTotalPendingApprovalsMap)
	}

	def onBind(result: BindingResult) {
		// Add all non-withdrawn codes to SPR statuses by default
		if (sprStatuses.isEmpty) {
			allSprStatuses.filter { status => !status.code.startsWith("P") && !status.code.startsWith("T") }.foreach { sprStatuses.add }
		}
	}
}

trait ViewRelatedStudentsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self:ViewRelatedStudentsCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), currentMember)
	}
}
