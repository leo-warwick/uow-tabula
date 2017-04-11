package uk.ac.warwick.tabula.web.controllers.attendance

import javax.servlet.http.HttpServletResponse

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestBody, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.attendance.report.{CreateMonitoringPointReportCommand, CreateMonitoringPointReportCommandState, CreateMonitoringPointReportRequestState}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, SprCode}

import scala.beans.BeanProperty
import scala.collection.JavaConverters._

@Controller
@RequestMapping(Array("/attendance/report/{department}/create"))
class CreateMonitoringPointReportController extends AttendanceController {

	type CreateMonitoringPointReportCommand = Appliable[Seq[MonitoringPointReport]] with CreateMonitoringPointReportCommandState with SelfValidating

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, user: CurrentUser): CreateMonitoringPointReportCommand =
		CreateMonitoringPointReportCommand(department, user)

	@RequestMapping(method = Array(POST), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
	def createAsJson(@RequestBody request: CreateMonitoringPointReportRequest, @ModelAttribute("command") command: CreateMonitoringPointReportCommand, errors: Errors)(implicit response: HttpServletResponse): Mav = {
		request.copyTo(command, errors)
		command.validate(errors)

		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			Mav(new JSONView(Map(
				"success" -> true,
				"academicYear" -> command.academicYear.toString,
				"period" -> command.period,
				"missedPoints" -> command.apply().map { report => (report.student.universityId) -> report.missed }.toMap
			)))
		}
	}

}

@JsonAutoDetect
class CreateMonitoringPointReportRequest extends Serializable {
	@transient var profileService: ProfileService = Wire[ProfileService]

	@BeanProperty var period: String = _
	@BeanProperty var academicYear: String = _
	@BeanProperty var missedPoints: JMap[String, JInteger] = JHashMap()

	def copyTo(state: CreateMonitoringPointReportRequestState, errors: Errors) {
		try {
			state.academicYear = AcademicYear.parse(academicYear)
		} catch {
			case e: IllegalArgumentException => errors.rejectValue("academicYear", "typeMismatch")
		}

		state.missedPoints = missedPoints.asScala.flatMap { case (sprCode, missed) =>
			profileService.getMemberByUniversityId(SprCode.getUniversityId(sprCode)) match {
				case Some(student: StudentMember) => Some(student -> missed.intValue())
				case _ => errors.rejectValue("missedPoints", "invalid"); None
			}
		}.toMap

		state.period = period
	}
}
