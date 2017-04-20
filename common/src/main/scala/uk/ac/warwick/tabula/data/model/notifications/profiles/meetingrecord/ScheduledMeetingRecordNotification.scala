package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import javax.activation.{DataHandler, DataSource}
import javax.mail.Part
import javax.mail.internet.{MimeBodyPart, MimeMultipart}

import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.property._
import org.springframework.mail.javamail.MimeMessageHelper
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.services.timetables.{EventOccurrenceService, TermBasedEventOccurrenceService}
import uk.ac.warwick.tabula.timetables.TimetableEvent

abstract class ScheduledMeetingRecordNotification
	extends NotificationWithTarget[ScheduledMeetingRecord, StudentRelationship]
	with SingleItemNotification[ScheduledMeetingRecord]
	with AutowiringTermServiceComponent {

	self: MyWarwickDiscriminator =>

	def meeting: ScheduledMeetingRecord = item.entity

	def verbSetting = StringSetting("verb", "")
	def verb: String = verbSetting.value

	def studentNotFoundMessage: String = "Student member for SCJ code " + meeting.relationship.studentCourseDetails.scjCode + " not found"
	def agentNotFoundMessage: String = "Agent member for code " + meeting.relationship.agent + " not found"

	def academicYear: AcademicYear = AcademicYear.findAcademicYearContainingDate(meeting.meetingDate)

	def url: String = Routes.Profile.relationshipType(
		meeting.relationship.studentCourseDetails,
		academicYear,
		meeting.relationship.relationshipType
	)

	def urlTitle = "view this meeting"

	def agentRole: String = meeting.relationship.relationshipType.agentRole
}

private case class ByteArrayDataSource(bytes: Array[Byte], contentType: String, fileName: String) extends DataSource {
	override def getInputStream: InputStream = new ByteArrayInputStream(bytes)
	override def getName: String = fileName
	override def getOutputStream: OutputStream = throw new UnsupportedOperationException("Read-only javax.activation.DataSource")
	override def getContentType: String = contentType
}

trait AddsIcalAttachmentToScheduledMeetingNotification extends HasNotificationAttachment {

	self: ScheduledMeetingRecordNotification =>

	@transient
	lazy val eventOccurrenceService: EventOccurrenceService = Wire[TermBasedEventOccurrenceService]

	override def generateAttachments(helper: MimeMessageHelper): Unit = {
		val message = helper.getMimeMessage
		val mp = new MimeMultipart("alternative")

		// Text part of the original message
		val textPart = new MimeBodyPart
		textPart.setText(message.getContent.asInstanceOf[String])

		// Create ical
		val cal = new Calendar
		cal.getProperties.add(Version.VERSION_2_0)
		cal.getProperties.add(new ProdId("-//Tabula//University of Warwick IT Services//EN"))
		cal.getProperties.add(CalScale.GREGORIAN)
		cal.getProperties.add(new XProperty("X-PUBLISHED-TTL", "PT12H"))

		val vEvent = eventOccurrenceService.toVEvent(
			item.entity.toEventOccurrence(TimetableEvent.Context.Staff).get
		)

		verb match {
			case "deleted" =>
				cal.getProperties.add(Method.CANCEL)
				vEvent.getProperties.add(Status.VEVENT_CANCELLED)
				vEvent.getProperties.add(new Sequence(1))
			case ("rescheduled"|"updated") =>
				cal.getProperties.add(Method.PUBLISH)
				vEvent.getProperties.add(Status.VEVENT_CONFIRMED)
				vEvent.getProperties.add(new Sequence(1))
			case _ =>
				cal.getProperties.add(Method.PUBLISH)
				vEvent.getProperties.add(Status.VEVENT_CONFIRMED)
				vEvent.getProperties.add(new Sequence(0))
		}

		cal.getComponents.add(vEvent)

		val outputter = new CalendarOutputter
		val baos = new ByteArrayOutputStream
		outputter.output(cal, baos)
		baos.close()
		val iCal = baos.toByteArray

		// iCal part for Outlook/Office 365
		val icalPart = new MimeBodyPart
		icalPart.setDataHandler(new DataHandler(ByteArrayDataSource(iCal, "text/calendar; method=" + cal.getMethod.getValue, "meeting.ics")))

		// Attachment for everyone else
		val attachmentBodyPart = new MimeBodyPart
		attachmentBodyPart.setDisposition(Part.ATTACHMENT)
		attachmentBodyPart.setFileName("meeting.ics")
		attachmentBodyPart.setDataHandler(new DataHandler(ByteArrayDataSource(iCal, "text/calendar", "meeting.ics")))

		mp.addBodyPart(textPart)
		mp.addBodyPart(icalPart)
		mp.addBodyPart(attachmentBodyPart)

		message.setContent(mp)
	}

}

