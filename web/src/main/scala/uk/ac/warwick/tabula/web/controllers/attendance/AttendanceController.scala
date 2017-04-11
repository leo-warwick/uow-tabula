package uk.ac.warwick.tabula.web.controllers.attendance

import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.data.model.{Member, RuntimeMember}
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.AcademicYear
import java.text.DateFormatSymbols

import scala.collection.immutable.IndexedSeq

/**
 * Base class for controllers in Attendance Monitoring.
 */
abstract class AttendanceController extends BaseController with AttendanceBreadcrumbs
	with CurrentMemberComponent {

	final def optionalCurrentMember: Option[Member] = user.profile
	final def currentMember: Member = optionalCurrentMember getOrElse new RuntimeMember(user)

}

trait CurrentMemberComponent {
	def optionalCurrentMember: Option[Member]
	def currentMember: Member
}

trait HasMonthNames {

	@ModelAttribute("monthNames")
	def monthNames(@PathVariable academicYear: AcademicYear): IndexedSeq[String] = {
		MonthNames(academicYear)
	}

}

object MonthNames {

	private val monthNames = new DateFormatSymbols().getMonths.array

	def apply(academicYear: AcademicYear): IndexedSeq[String] =
		(8 to 11).map{ i => monthNames(i) + " " + academicYear.startYear.toString } ++
		(0 to 9).map{ i => monthNames(i) + " " + academicYear.endYear.toString }
}
