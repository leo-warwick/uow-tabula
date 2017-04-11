package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, LocalDate}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam}
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/sysadmin/metrics"))
class MetricsController extends BaseSysadminController with Daoisms {

	@RequestMapping
	def home(@RequestParam(value="startDate", required=false) queryDate: LocalDate): Mav = {
		val startDate = Option(queryDate).map(_.toDateTimeAtStartOfDay).getOrElse(
			DateTime.now.minusMonths(1).withDayOfMonth(1)
		)
		val endDate = startDate.plusMonths(2).withDayOfMonth(1)

		val submissions = {
			val q = session.createSQLQuery(
				"""
					select count(*) as submissions from SUBMISSION
					where SUBMITTED_DATE >= :startDate and SUBMITTED_DATE < :endDate
				""")
			q.setTimestamp("startDate", startDate.toDate)
			q.setTimestamp("endDate", endDate.toDate)
			q.uniqueResult().asInstanceOf[JBigDecimal]
		}

		val assignments = {
			val q = session.createSQLQuery(
				"""
					select count(distinct assignment_id) as submitted_assignments from SUBMISSION
					where SUBMITTED_DATE >= :startDate and SUBMITTED_DATE < :endDate
				""")
			q.setTimestamp("startDate", startDate.toDate)
			q.setTimestamp("endDate", endDate.toDate)
			q.uniqueResult().asInstanceOf[JBigDecimal]
		}

		val feedback = {
			val q = session.createSQLQuery(
				"""
					select count(*) as feedback_items from FEEDBACK
					where UPLOADED_DATE >= :startDate and UPLOADED_DATE < :endDate
				""")
			q.setTimestamp("startDate", startDate.toDate)
			q.setTimestamp("endDate", endDate.toDate)
			q.uniqueResult().asInstanceOf[JBigDecimal]
		}

		val meetings = {
			val q = session.createSQLQuery(
				"""
					select count(*) as meeting_records from meetingrecord
					where creation_date >= :startDate and creation_date < :endDate
				""")
			q.setTimestamp("startDate", startDate.toDate)
			q.setTimestamp("endDate", endDate.toDate)
			q.uniqueResult().asInstanceOf[JBigDecimal]
		}

		val groups = {
			val q = session.createSQLQuery(
				"""
					select count(*) from auditevent where eventdate >= :startDate and eventdate < :endDate
					and eventstage = 'after' and eventtype = 'AllocateSelfToGroup'
				""")
			q.setTimestamp("startDate", startDate.toDate)
			q.setTimestamp("endDate", endDate.toDate)
			q.uniqueResult().asInstanceOf[JBigDecimal]
		}

		val checkpoints = {
			val q = session.createSQLQuery(
				"""
					select count(*) from attendancemonitoringcheckpoint
					where updated_date >= :startDate and updated_date < :endDate
				""")
			q.setTimestamp("startDate", startDate.toDate)
			q.setTimestamp("endDate", endDate.toDate)
			q.uniqueResult().asInstanceOf[JBigDecimal]
		}

		Mav("sysadmin/metrics",
			"period" -> startDate.toString("MMMM YYYY"),
			"startDate" -> startDate.toString(DateTimeFormat.forPattern(DateFormats.DatePickerPattern)),
			"submissions" -> submissions,
			"assignments" -> assignments,
			"feedback" -> feedback,
			"meetings" -> meetings,
			"groups" -> groups,
			"checkpoints" -> checkpoints
		)
	}
}
