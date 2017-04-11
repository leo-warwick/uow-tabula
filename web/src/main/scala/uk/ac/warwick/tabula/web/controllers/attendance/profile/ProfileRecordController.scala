package uk.ac.warwick.tabula.web.controllers.attendance.profile

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
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringNote, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringTermServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.util.termdates.Term

@Controller
@RequestMapping(Array("/attendance/profile/{student}/{academicYear}/record"))
class ProfileRecordController extends AttendanceController
	with HasMonthNames with GroupsPoints with AutowiringTermServiceComponent
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent {

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
		@PathVariable academicYear: AcademicYear,
		@PathVariable student: StudentMember
	): Mav = {
		cmd.populate()
		render(academicYear, student)
	}

	private def render(academicYear: AcademicYear, student: StudentMember) = {
		Mav("attendance/record",
			"department" -> student.homeDepartment,
			"returnTo" -> getReturnTo(Routes.Profile.profileForYear(mandatory(student), mandatory(academicYear)))
		).crumbs(
			Breadcrumbs.Profile.ProfileForYear(mandatory(student), mandatory(academicYear))
		).secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.Profile.record(student, year)):_*)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringCheckpoint]] with PopulateOnForm,
		errors: Errors,
		@PathVariable academicYear: AcademicYear,
		@PathVariable student: StudentMember
	): Mav = {
		if (errors.hasErrors) {
			render(academicYear, student)
		} else {
			cmd.apply()
			Redirect(Routes.Profile.profileForYear(mandatory(student), mandatory(academicYear)))
		}
	}

}
