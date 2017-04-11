package uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.AutowiringFeaturesComponent
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.profiles.relationships.meetings._
import uk.ac.warwick.tabula.commands.{Appliable, ComposableCommand, SelfValidating, TaskBenchmarking}
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, _}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringMeetingRecordServiceComponent}
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

import scala.collection.JavaConverters._

@Controller
@RequestMapping(value = Array("/profiles/{relationshipType}/meeting/bulk/create"))
class BulkMeetingRecordController extends ProfilesController with TaskBenchmarking {

	validatesSelf[SelfValidating]

	@ModelAttribute("studentRelationships")
	def studentRelationships(
		@PathVariable relationshipType: StudentRelationshipType,
		@RequestParam studentCourseDetails: JList[StudentCourseDetails]
	): Seq[StudentRelationship] = {
		benchmarkTask("Get StudentRelationships") {
			studentCourseDetails.asScala.flatMap { studentCourse =>
				relationshipService.getCurrentRelationship(relationshipType, studentCourse.student, currentMember)
			}
		}
	}



	@ModelAttribute("command")
	def getCommand(
		@PathVariable relationshipType: StudentRelationshipType,
		@ModelAttribute("studentRelationships") studentRelationships: Seq[StudentRelationship]
	): BulkMeetingRecordCommandInternal with AutowiringMeetingRecordServiceComponent with AutowiringFeaturesComponent with AutowiringAttendanceMonitoringMeetingRecordServiceComponent with AutowiringFileAttachmentServiceComponent with ComposableCommand[Seq[MeetingRecord]] with MeetingRecordCommandBindListener with MeetingRecordValidation with BulkMeetingRecordDescription with BulkMeetingRecordPermissions with BulkMeetingRecordCommandState with MeetingRecordCommandRequest with BulkMeetingRecordCommandNotifications = {
		BulkMeetingRecordCommand(mandatory(studentRelationships), currentMember)
	}


	@RequestMapping(method = Array(GET, HEAD), params = Array("iframe"))
	def getIframe(
		@ModelAttribute("command") cmd: Appliable[Seq[MeetingRecord]],
		@PathVariable relationshipType: StudentRelationshipType,
		@RequestParam studentCourseDetails: JList[StudentCourseDetails]
	): Mav = {
		form(cmd, relationshipType, studentCourseDetails, iframe = true)
	}


	@RequestMapping(method = Array(GET, HEAD))
	def get(
		@ModelAttribute("command") cmd: Appliable[Seq[MeetingRecord]],
		@PathVariable relationshipType: StudentRelationshipType,
		@RequestParam studentCourseDetails: JList[StudentCourseDetails]
	): Mav = {
		form(cmd, relationshipType, studentCourseDetails)
	}

	private def form(
		cmd: Appliable[Seq[MeetingRecord]],
		relationshipType: StudentRelationshipType,
		studentCourseDetails: JList[StudentCourseDetails],
		iframe: Boolean = false
	) = {
		val mav = Mav("profiles/related_students/meeting/bulk_edit",
			"returnTo" -> getReturnTo(Routes.students(relationshipType)),
			"isModal" -> ajax,
			"formats" -> MeetingFormat.members,
			"isModal" -> ajax,
			"isIframe" -> iframe,
			"studentList" -> studentCourseDetails
		)
		if (ajax)
			mav.noLayout()
		else if (iframe)
			mav.noNavigation()
		else
			mav
	}


	@RequestMapping(method = Array(POST), params = Array("iframe"))
	def submitIframe(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[MeetingRecord]],
		errors: Errors,
		@PathVariable relationshipType: StudentRelationshipType,
		@RequestParam studentCourseDetails: JList[StudentCourseDetails]
	): Mav = {
		if (errors.hasErrors) {
			form(cmd, relationshipType, studentCourseDetails, iframe = true)
		} else {
			cmd.apply()
			Mav("profiles/related_students/meeting/bulk_edit",
				"success" -> true
			)
		}
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[MeetingRecord]],
		errors: Errors,
		@PathVariable relationshipType: StudentRelationshipType,
		@RequestParam studentCourseDetails: JList[StudentCourseDetails]
	): Mav = {
		if (errors.hasErrors) {
			form(cmd, relationshipType, studentCourseDetails)
		} else {
			cmd.apply()
			Redirect(Routes.students(relationshipType))
		}
	}

}
