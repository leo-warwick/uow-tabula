package uk.ac.warwick.tabula.profiles.commands

import org.joda.time.DateTime
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils.rejectIfEmptyOrWhitespace
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{NotificationSource, Command, SelfValidating, UploadedFile}
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.MeetingRecordDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.BindListener
import collection.JavaConverters._
import uk.ac.warwick.tabula.data.FileDao
import org.joda.time.LocalDate
import scala.Some
import uk.ac.warwick.tabula.data.model.MeetingApprovalState.Pending
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.tabula.profiles.web.Routes
import freemarker.template.Configuration

abstract class ModifyMeetingRecordCommand(val creator: Member, var relationship: StudentRelationship)
	extends Command[MeetingRecord] with NotificationSource[MeetingRecord] with SelfValidating with FormattedHtml
	with BindListener with Daoisms {

	var features = Wire.auto[Features]
	var meetingRecordDao = Wire.auto[MeetingRecordDao]
	var fileDao = Wire.auto[FileDao]

	var title: String = _
	var description: String = _
	var meetingDate: LocalDate = _
	var format: MeetingFormat = _

	var file: UploadedFile = new UploadedFile
	var attachedFiles:JList[FileAttachment] = _

	var attachmentTypes = Seq[String]()

	var posted: Boolean = false

	PermissionCheck(Permissions.Profiles.MeetingRecord.Create, relationship.studentMember)

	val meeting: MeetingRecord

	override def applyInternal() = {

		def persistAttachments(meeting: MeetingRecord) {
			// delete attachments that have been removed

			if (meeting.attachments != null){
				val filesToKeep = Option(attachedFiles).map(_.asScala.toList).getOrElse(List())
				val filesToRemove = (meeting.attachments.asScala -- filesToKeep)
				meeting.attachments = JArrayList[FileAttachment](filesToKeep)
				filesToRemove.foreach(session.delete(_))
			}

			file.attached.asScala map(attachment => {
				attachment.meetingRecord = meeting
				meeting.attachments.add(attachment)
				fileDao.savePermanent(attachment)
			})
		}

		meeting.title = title
		meeting.description = description
		meeting.meetingDate = meetingDate.toDateTimeAtStartOfDay().withHourOfDay(MeetingRecord.DefaultMeetingTimeOfDay)
		meeting.format = format
		persistAttachments(meeting)

		// persist the meeting record
		meetingRecordDao.saveOrUpdate(meeting)

		if (features.meetingRecordApproval){
			val meetingApprovals = updateMeetingApproval(meeting)
			meetingApprovals.foreach(meetingApproval => {
				//TODO-Ritchie notification
			})
		}

		meeting
	}

	def updateMeetingApproval(meetingRecord: MeetingRecord) : Option[MeetingRecordApproval] = {

		def getMeetingRecord(approver: Member) : MeetingRecordApproval = {

			val meetingRecordApproval = meetingRecord.approvals.asScala.find(_.approver == approver).getOrElse{
				val newMeetingRecordApproval = new MeetingRecordApproval()
				newMeetingRecordApproval.approver = approver
				newMeetingRecordApproval.meetingRecord = meetingRecord
				meetingRecord.approvals.add(newMeetingRecordApproval)
				newMeetingRecordApproval
			}

			meetingRecordApproval.state = Pending
			session.saveOrUpdate(meetingRecordApproval)
			meetingRecordApproval
		}

		val approver = Seq(relationship.agentMember, Some(relationship.studentMember)).flatten.find(_ != creator)
		approver.map(getMeetingRecord(_))

	}

	override def validate(errors: Errors) {
		rejectIfEmptyOrWhitespace(errors, "title", "NotEmpty")
		if (title.length > MeetingRecord.MaxTitleLength){
			errors.rejectValue("title", "meetingRecord.title.long", new Array(MeetingRecord.MaxTitleLength), "")
		}

		rejectIfEmptyOrWhitespace(errors, "format", "NotEmpty")

		meetingDate match {
			case date:LocalDate => {
				if (meetingDate.isAfter(DateTime.now.toLocalDate)) {
					errors.rejectValue("meetingDate", "meetingRecord.date.future")
				} else if (meetingDate.isBefore(DateTime.now.minusYears(MeetingRecord.MeetingTooOldThresholdYears).toLocalDate)) {
					errors.rejectValue("meetingDate", "meetingRecord.date.prehistoric")
				}
			}
			case _ => errors.rejectValue("meetingDate", "meetingRecord.date.missing")
		}
	}

	def emit = new MeetingRecordApprovalNotification(meeting)

	class MeetingRecordApprovalNotification(meeting: MeetingRecord)
		extends Notification[MeetingRecord] with FreemarkerRendering {

		implicit var freemarker = Wire.auto[Configuration]

		val actor = meeting.creator.asSsoUser
		val verb = "create"
		val target = Some(meeting.relationship)
		val _object = meeting

		def title = "Meeting record approval required"
		def url = Routes.profile.view(meeting.relationship.studentMember, meeting)
		def content = renderToString("/WEB-INF/freemarker/notifications/meeting_record_approval_notification.ftl", Map(
			"meetingRecord" -> meeting,
			"profileLink" -> url
		))
		def recipients = meeting.pendingApprovers.map(_.asSsoUser)
	}

}