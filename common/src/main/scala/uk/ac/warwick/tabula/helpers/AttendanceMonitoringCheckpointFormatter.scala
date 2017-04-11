package uk.ac.warwick.tabula.helpers

import org.springframework.beans.factory.annotation.Autowired

import freemarker.template._
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import freemarker.template.utility.DeepUnwrap
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceState, AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint}
import freemarker.core.Environment
import uk.ac.warwick.tabula.data.model.{AttendanceNote, StudentMember, Department}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.profiles.web.{Routes => ProfileRoutes}
import uk.ac.warwick.tabula.attendance.web.{Routes => AttendanceRoutes}


case class AttendanceMonitoringCheckpointFormatterResult(
	labelText: String,
	labelClass: String,
	iconClass: String,
	status: String,
	metadata: String,
	noteType: String,
	noteText: String,
	noteUrl: String
)

/**
 * Freemarker helper to build the necessary fields to display a checkpoint
 */
class AttendanceMonitoringCheckpointFormatter extends TemplateMethodModelEx {

	@Autowired var userLookup: UserLookupService = _
	@Autowired var attendanceMonitoringService: AttendanceMonitoringService = _

	override def exec(list: JList[_]): AttendanceMonitoringCheckpointFormatterResult = {
		val args = list.asScala.map {
			model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel])
		}
		args match {
			case Seq(department: Department, checkpoint: AttendanceMonitoringCheckpoint, urlProfile: JBoolean, _*) =>
				result(department, checkpoint, None, urlProfile)
			case Seq(department: Department, checkpoint: AttendanceMonitoringCheckpoint, note: AttendanceNote, urlProfile: JBoolean, _*) =>
				result(department, checkpoint, Option(note), urlProfile)
			case Seq(department: Department, point: AttendanceMonitoringPoint, student: StudentMember, urlProfile: JBoolean, _*) =>
				result(department, point, student, None, urlProfile)
			case Seq(department: Department, point: AttendanceMonitoringPoint, student: StudentMember, note: AttendanceNote, urlProfile: JBoolean, _*) =>
				result(department, point, student, Option(note), urlProfile)
			case _ => throw new IllegalArgumentException("Bad args")
		}
	}

	private def describeCheckpoint(checkpoint: AttendanceMonitoringCheckpoint) = {
		val userString = userLookup.getUserByUserId(checkpoint.updatedBy) match {
			case FoundUser(user) => s"by ${user.getFullName}, "
			case _ => ""
		}

		s"Recorded $userString${DateBuilder.format(checkpoint.updatedDate)}"
	}

	private def pointDuration(point: AttendanceMonitoringPoint, department: Department) = {
		val wrapper = new DefaultObjectWrapper(Configuration.VERSION_2_3_0)
		point.scheme.pointStyle match {
			case AttendanceMonitoringPointStyle.Date =>
				val intervalFormatter = Environment.getCurrentEnvironment.getGlobalVariable("intervalFormatter").asInstanceOf[TemplateMethodModelEx]
				intervalFormatter.exec(JList(
					wrapper.wrap(point.startDate.toDate),
					wrapper.wrap(point.endDate.toDate)
				)).asInstanceOf[String]
			case AttendanceMonitoringPointStyle.Week =>
				val wholeWeekFormatter = Environment.getCurrentEnvironment.getGlobalVariable("wholeWeekFormatter").asInstanceOf[TemplateMethodModelEx]
				val userFormat = wholeWeekFormatter.exec(JList(
					wrapper.wrap(point.startWeek),
					wrapper.wrap(point.endWeek),
					wrapper.wrap(point.scheme.academicYear),
					wrapper.wrap(department),
					wrapper.wrap(false)
				)).asInstanceOf[String]
				if (userFormat.indexOf("w/c") == -1) {
					userFormat + s" (${
						wholeWeekFormatter.exec(JList(
							wrapper.wrap(point.startWeek),
							wrapper.wrap(point.endWeek),
							wrapper.wrap(point.scheme.academicYear),
							wrapper.wrap(false)
						)).asInstanceOf[String]
					})"
				} else {
					userFormat
				}
		}
	}

	private def generateUrl(point: AttendanceMonitoringPoint, student: StudentMember, urlProfile: Boolean): String = {
		if (urlProfile) {
			ProfileRoutes.Note(student, point)
		} else {
			AttendanceRoutes.Note.view(point.scheme.academicYear, student, point)
		}
	}

	private def result(department: Department, checkpoint: AttendanceMonitoringCheckpoint, noteOption: Option[AttendanceNote], urlProfile:Boolean): AttendanceMonitoringCheckpointFormatterResult = {
		val point = checkpoint.point
		val (noteType, noteText, noteUrl) = (noteOption match {
			case None => attendanceMonitoringService.getAttendanceNote(checkpoint.student, point)
			case Some(note) => Option(note)
		}).fold(("", "", ""))(note =>
			(note.absenceType.description, note.truncatedNote, generateUrl(point, checkpoint.student, urlProfile))
		)

		checkpoint.state match {
			case AttendanceState.Attended =>
				AttendanceMonitoringCheckpointFormatterResult(
					"Attended",
					"label-success",
					"fa fa-check attended",
					s"Attended: ${point.name} ${pointDuration(point, department)}",
					describeCheckpoint(checkpoint),
					noteType,
					noteText,
					noteUrl
				)
			case AttendanceState.MissedAuthorised =>
				AttendanceMonitoringCheckpointFormatterResult(
					"Missed (authorised)",
					"label-info",
					"fa fa-times-circle-o authorised",
					s"Missed (authorised): ${point.name} ${pointDuration(point, department)}",
					describeCheckpoint(checkpoint),
					noteType,
					noteText,
					noteUrl
				)
			// Monitoring point still use Id6 -label-important can be removed when we later migrate that
			case AttendanceState.MissedUnauthorised =>
				AttendanceMonitoringCheckpointFormatterResult(
					"Missed (unauthorised)",
					"label-danger label-important",
					"fa fa-times unauthorised",
					s"Missed (unauthorised): ${point.name} ${pointDuration(point, department)}",
					describeCheckpoint(checkpoint),
					noteType,
					noteText,
					noteUrl
				)
			// Should never be the case, but stops a compile warning
			case _ => AttendanceMonitoringCheckpointFormatterResult("","","","","","","","")
		}
	}

	private def result(department: Department, point: AttendanceMonitoringPoint, student: StudentMember, noteOption: Option[AttendanceNote], urlProfile:Boolean): AttendanceMonitoringCheckpointFormatterResult = {
		val (noteType, noteText, noteUrl) = (noteOption match {
			case None => attendanceMonitoringService.getAttendanceNote(student, point)
			case Some(note) => Option(note)
		}).fold(("", "", ""))(note =>
			(note.absenceType.description, note.truncatedNote, generateUrl(point, student, urlProfile))
		)

		if (point.endDate.isBefore(DateTime.now.toLocalDate)) {
			// is late
			AttendanceMonitoringCheckpointFormatterResult(
				"Unrecorded",
				"label-warning",
				"fa fa-exclamation-triangle late",
				s"${point.name} ${pointDuration(point, department)}",
				"",
				noteType,
				noteText,
				noteUrl
			)
		} else {
			AttendanceMonitoringCheckpointFormatterResult(
				"",
				"",
				"fa fa-minus",
				s"${point.name} ${pointDuration(point, department)}",
				"",
				noteType,
				noteText,
				noteUrl
			)
		}
	}
}
