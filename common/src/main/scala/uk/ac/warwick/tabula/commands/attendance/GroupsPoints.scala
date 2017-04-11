package uk.ac.warwick.tabula.commands.attendance

import java.text.DateFormatSymbols

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringPointStyle, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.services.TermServiceComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService

case class GroupedPoint(
	templatePoint: AttendanceMonitoringPoint,
	schemes: Seq[AttendanceMonitoringScheme],
	points: Seq[AttendanceMonitoringPoint]){
		var attendanceMonitoringService: AttendanceMonitoringService = Wire[AttendanceMonitoringService]
		def hasRecordedCheckpoints: Boolean = attendanceMonitoringService.hasRecordedCheckpoints(points)
}

trait GroupsPoints {

	self: TermServiceComponent =>

	def groupByTerm(points: Seq[AttendanceMonitoringPoint], groupSimilar: Boolean = true): Map[String, Seq[GroupedPoint]] = {
		val ungroupedPoints =
			// only week points group by term
			points.filter(_.scheme.pointStyle == AttendanceMonitoringPointStyle.Week)
			// group by term (using pre-calculated date)
			.groupBy{ point =>
				termService.getTermFromDateIncludingVacations(point.startDate.toDateTimeAtStartOfDay).getTermTypeAsString
			}

		if (groupSimilar)
			// group each term's similar points (by name and weeks)
			ungroupedPoints.map{ case (term, nonGroupedPoints) => term ->
				nonGroupedPoints.groupBy{ point => (point.name.toLowerCase, point.startWeek, point.endWeek)}
			}
			// transform similar points into 1 grouped point
			.map{ case (term, groupedPoints) => term ->
				groupedPoints.map{ case(_, pointGroup) => GroupedPoint(pointGroup.head, pointGroup.map(_.scheme), pointGroup)}
					.toSeq.sortBy(p => (p.templatePoint.startWeek, p.templatePoint.endWeek))
			}
		else
			ungroupedPoints.map{ case (term, nonGroupedPoints) => term ->
				nonGroupedPoints.map{ p => GroupedPoint(p, Seq(p.scheme), Seq(p))}
					.sortBy(p => (p.templatePoint.startWeek, p.templatePoint.endWeek))
			}
	}

	def groupByMonth(points: Seq[AttendanceMonitoringPoint], groupSimilar: Boolean = true): Map[String, Seq[GroupedPoint]] = {

		// do not remove; import needed for sorting
		// should be: import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

		val ungroupedPoints =
			// only date points group by month
			points.filter(_.scheme.pointStyle == AttendanceMonitoringPointStyle.Date)
			// group by month
			.groupBy{ point => (point.startDate.monthOfYear, point.startDate.year) }
		if (groupSimilar)
			// group each month's similar points (by name and weeks)
			ungroupedPoints.map{ case (monthYearPair, nonGroupedPoints) => monthYearPair ->
				nonGroupedPoints.groupBy{ point => (point.name.toLowerCase, point.startDate, point.endDate)}
			}
			// transform similar points into 1 grouped point
			.map{ case (monthYearPair, groupedPoints) =>
				new DateFormatSymbols().getMonths.array(monthYearPair._1.get - 1) + " " + monthYearPair._2.get ->
					groupedPoints.map{ case(_, pointGroup) => GroupedPoint(pointGroup.head, pointGroup.map(_.scheme), pointGroup)}
						.toSeq.sortBy(p => (p.templatePoint.startDate, p.templatePoint.endDate))
			}
		else
			ungroupedPoints.map{ case (monthYearPair, nonGroupedPoints) =>
				new DateFormatSymbols().getMonths.array(monthYearPair._1.get - 1) + " " + monthYearPair._2.get ->
					nonGroupedPoints.map{ p => GroupedPoint(p, Seq(p.scheme), Seq(p))}
						.sortBy(p => (p.templatePoint.startDate, p.templatePoint.endDate))
			}
	}
}
