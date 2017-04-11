package uk.ac.warwick.tabula.api.web.controllers.attendance

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestBody, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.api.commands.JsonApiRequest
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, SprCode}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.attendance.report.{CreateMonitoringPointReportCommand, CreateMonitoringPointReportCommandState, CreateMonitoringPointReportRequestState}
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport
import uk.ac.warwick.tabula.services.ProfileService

import scala.beans.BeanProperty
import scala.collection.JavaConverters._
import MonitoringPointReportController._
import uk.ac.warwick.tabula.web.Mav

object MonitoringPointReportController {
	type CreateMonitoringPointReportCommand = Appliable[Seq[MonitoringPointReport]] with CreateMonitoringPointReportCommandState with SelfValidating

	def toJson(request: CreateMonitoringPointReportRequest, result: Seq[MonitoringPointReport]) = Map(
		"academicYear" -> request.academicYear,
		"period" -> request.period,
		"missedPoints" -> result.map { report => report.student.universityId -> report.missed }.toMap
	)
}

@Controller
@RequestMapping(Array("/v1/department/{department}/monitoring-point-reports"))
class MonitoringPointReportController extends ApiController
	with MonitoringPointReportCreateApi

// POST - Create a new report
trait MonitoringPointReportCreateApi {
	self: ApiController =>

	@ModelAttribute("createCommand")
	def command(@PathVariable department: Department, user: CurrentUser): CreateMonitoringPointReportCommand =
		CreateMonitoringPointReportCommand(department, user)

	@RequestMapping(method = Array(POST), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
	def create(@RequestBody request: CreateMonitoringPointReportRequest, @ModelAttribute("createCommand") command: CreateMonitoringPointReportCommand, errors: Errors): Mav = {
		request.copyTo(command, errors)

		globalValidator.validate(command, errors)
		command.validate(errors)

		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val result = command.apply()
			Mav(new JSONView(Map("success" -> true, "status" -> "ok") ++ toJson(request, result)))
		}
	}
}

@JsonAutoDetect
class CreateMonitoringPointReportRequest extends JsonApiRequest[CreateMonitoringPointReportRequestState] {
	@transient var profileService: ProfileService = Wire[ProfileService]

	@BeanProperty var period: String = _
	@BeanProperty var academicYear: AcademicYear = _
	@BeanProperty var missedPoints: JMap[String, JInteger] = JHashMap()

	override def copyTo(state: CreateMonitoringPointReportRequestState, errors: Errors) {
		state.academicYear = academicYear
		state.missedPoints = missedPoints.asScala.flatMap { case (sprCode, missed) =>
			profileService.getMemberByUniversityId(SprCode.getUniversityId(sprCode)) match {
				case Some(student: StudentMember) =>
					Some(student -> missed.intValue())
				case _ =>
					errors.rejectValue("missedPoints", "monitoringPointReport.student.notFound", Array(sprCode), "")
					None
			}
		}.toMap

		state.period = period
	}
}