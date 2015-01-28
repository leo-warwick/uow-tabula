package uk.ac.warwick.tabula.api.web.controllers.attendance

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.api.commands.JsonApiRequest
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.{CurrentUser, SprCode, AcademicYear}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.api.web.controllers.{CreateApi, ApiController}
import uk.ac.warwick.tabula.attendance.commands.report.{CreateMonitoringPointReportCommand, CreateMonitoringPointReportCommandState, CreateMonitoringPointReportRequestState}
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport
import uk.ac.warwick.tabula.services.ProfileService

import scala.beans.BeanProperty
import scala.collection.JavaConverters._

import MonitoringPointReportController._

object MonitoringPointReportController {
	type CreateMonitoringPointReportCommand = Appliable[Seq[MonitoringPointReport]] with CreateMonitoringPointReportCommandState with SelfValidating
}

@Controller
@RequestMapping(Array("/v1/attendance/report/{department}"))
class MonitoringPointReportController extends ApiController
	with MonitoringPointReportCreateApi

// POST - Create a new report
trait MonitoringPointReportCreateApi
	extends CreateApi[CreateMonitoringPointReportRequestState, Seq[MonitoringPointReport], CreateMonitoringPointReportRequest, CreateMonitoringPointReportCommand] {
	self: ApiController =>

	@ModelAttribute("createCommand")
	def command(@PathVariable department: Department, user: CurrentUser): CreateMonitoringPointReportCommand =
		CreateMonitoringPointReportCommand(department, user)

	def toJson(request: CreateMonitoringPointReportRequest, result: Seq[MonitoringPointReport]) = Map(
		"academicYear" -> request.academicYear,
		"period" -> request.period,
		"missedPoints" -> result.map { report => (report.student.universityId) -> report.missed }.toMap
	)

}

@JsonAutoDetect
class CreateMonitoringPointReportRequest extends JsonApiRequest[CreateMonitoringPointReportRequestState] {
	@transient var profileService = Wire[ProfileService]

	@BeanProperty var period: String = _
	@BeanProperty var academicYear: AcademicYear = _
	@BeanProperty var missedPoints: JMap[String, JInteger] = JHashMap()

	override def copyTo(state: CreateMonitoringPointReportRequestState, errors: Errors) {
		state.academicYear = academicYear
		state.missedPoints = missedPoints.asScala.flatMap { case (sprCode, missed) =>
			profileService.getMemberByUniversityId(SprCode.getUniversityId(sprCode)) match {
				case Some(student: StudentMember) => Some(student -> missed.intValue())
				case _ => errors.rejectValue("missedPoints", "invalid"); None
			}
		}.toMap

		state.period = period
	}
}