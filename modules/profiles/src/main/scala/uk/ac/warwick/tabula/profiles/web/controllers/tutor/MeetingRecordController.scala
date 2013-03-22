package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._

import javax.validation.Valid
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.RelationshipType._
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.profiles.commands.CreateMeetingRecordCommand
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController

@Controller
@RequestMapping(value = Array("/tutor/meeting/{student}/create"))
class MeetingRecordController extends ProfilesController {
	
	validatesSelf[CreateMeetingRecordCommand]

	@ModelAttribute("command")
	def getCommand(@PathVariable("student") member: Member) = member match {
		case student: StudentMember => {
			profileService.findCurrentRelationship(PersonalTutor, student.studyDetails.sprCode) match {
				case Some(rel) => new CreateMeetingRecordCommand(currentMember, rel)
				case None => throw new ItemNotFoundException
			}
		}
		case _ => throw new ItemNotFoundException
	}
	
	// blank async form
	@RequestMapping(method = Array(GET, HEAD), params = Array("modal"))
	def showModalForm(@ModelAttribute("command") command: CreateMeetingRecordCommand, @PathVariable("student") student: Member) = {
		Mav("tutor/meeting/edit",
			"modal" -> true,
			"command" -> command,
			"student" -> student,
			"tutorName" -> command.relationship.agentName,
			"creator" -> command.creator).noLayout()
	}
	
	// submit async
	@RequestMapping(method = Array(POST), params = Array("modal"))
	def saveModalMeetingRecord(@Valid @ModelAttribute("command") command: CreateMeetingRecordCommand, errors: Errors, @PathVariable("student") student: Member) = {
		transactional() {
			if (errors.hasErrors) {
				showModalForm(command, student)
			} else {
				val meeting = command.apply()
				Redirect(Routes.profile.view(student, meeting))
			}
		}
	}

	// blank sync form
	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute("command") command: CreateMeetingRecordCommand, @PathVariable("student") student: Member) = {
		Mav("tutor/meeting/edit",
			"command" -> command,
			"student" -> student,
			"tutorName" -> command.relationship.agentName,
			"creator" -> command.creator)
	}
	
	// cancel sync
	@RequestMapping(method = Array(POST), params = Array("!submit", "!modal"))
	def cancel(@PathVariable("student") student: Member) = {
		Redirect(Routes.profile.view(student))
	}
		
	// submit sync
	@RequestMapping(method = Array(POST), params = Array("submit"))
	def saveMeetingRecord(@Valid @ModelAttribute("command") command: CreateMeetingRecordCommand, errors: Errors, @PathVariable("student") student: Member) = {
		transactional() {
			if (errors.hasErrors) {
				showForm(command, student)
			} else {
				val meeting = command.apply()
				Redirect(Routes.profile.view(student, meeting))
			}
		}
	}
}