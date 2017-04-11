package uk.ac.warwick.tabula.web.controllers.attendance.agent

import javax.validation.Valid

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{InitBinder, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.{GroupsPoints, StudentRecordCommand}
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.web.controllers.attendance.{AttendanceController, HasMonthNames}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringNote, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.util.termdates.Term

@Controller
@RequestMapping(Array("/attendance/agent/{relationshipType}/{academicYear}/{student}/record"))
class AgentStudentRecordController extends AttendanceController
	with HasMonthNames with GroupsPoints with AutowiringTermServiceComponent {

	@Autowired var attendanceMonitoringService: AttendanceMonitoringService = _

	validatesSelf[SelfValidating]

	var points: Seq[AttendanceMonitoringPoint] = _

	@InitBinder // do on each request
	def populatePoints(@PathVariable academicYear: AcademicYear, @PathVariable student: StudentMember): Unit = {
			points = attendanceMonitoringService.listStudentsPoints(mandatory(student), None, mandatory(academicYear))
	}

	@ModelAttribute("command")
	def command(
		@PathVariable academicYear: AcademicYear,
		@PathVariable student: StudentMember
	) =
		StudentRecordCommand(mandatory(academicYear), mandatory(student), user)

	@ModelAttribute("attendanceNotes")
	def attendanceNotes(@PathVariable student: StudentMember): Map[AttendanceMonitoringPoint, AttendanceMonitoringNote] =
		attendanceMonitoringService.getAttendanceNoteMap(mandatory(student))

	@ModelAttribute("groupedPointMap")
	def groupedPointMap(@PathVariable student: StudentMember): Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]] = {
		val checkpointMap = attendanceMonitoringService.getCheckpoints(points, mandatory(student))
		val groupedPoints = groupByTerm(points, groupSimilar = false) ++
			groupByMonth(points, groupSimilar = false)
		groupedPoints.map{case(period, thesePoints) =>
			period -> thesePoints.map{ groupedPoint =>
				groupedPoint.templatePoint -> checkpointMap.getOrElse(groupedPoint.templatePoint, null)
			}
		}
	}

	@ModelAttribute("reportedPointMap")
	def reportedPointMap(@PathVariable academicYear: AcademicYear, @PathVariable student: StudentMember): Map[AttendanceMonitoringPoint, Option[Term]] = {
		val nonReportedTerms = attendanceMonitoringService.findNonReportedTerms(Seq(mandatory(student)), mandatory(academicYear))
		points.map{ point => point -> {
			val term = termService.getTermFromDateIncludingVacations(point.startDate.toDateTimeAtStartOfDay)
			if (nonReportedTerms.contains(term.getTermTypeAsString)) {
				None
			} else {
				Option(term)
			}
		}}.toMap
	}

	@RequestMapping(method = Array(GET))
	def form(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringCheckpoint]] with PopulateOnForm,
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable academicYear: AcademicYear,
		@PathVariable student: StudentMember
	): Mav = {
		cmd.populate()
		render(relationshipType, academicYear, student)
	}

	private def render(relationshipType: StudentRelationshipType, academicYear: AcademicYear, student: StudentMember) = {
		Mav("attendance/record",
			"department" -> currentMember.homeDepartment,
			"returnTo" -> getReturnTo(Routes.Agent.student(relationshipType, academicYear, student))
		).crumbs(
			Breadcrumbs.Agent.RelationshipForYear(relationshipType, academicYear),
			Breadcrumbs.Agent.Student(relationshipType, academicYear, student)
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringCheckpoint]] with PopulateOnForm,
		errors: Errors,
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable academicYear: AcademicYear,
		@PathVariable student: StudentMember
	): Mav = {
		if (errors.hasErrors) {
			render(relationshipType, academicYear, student)
		} else {
			cmd.apply()
			Redirect(Routes.Agent.student(relationshipType, academicYear, student))
		}
	}

}
