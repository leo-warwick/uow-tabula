package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.data.model.Department
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.system.permissions.Public
import org.springframework.util.AutoPopulatingList
import uk.ac.warwick.tabula.helpers.StringUtils._

object AddMonitoringPointCommand {
	def apply(dept: Department) =
		new AddMonitoringPointCommand(dept)
		with AutowiringTermServiceComponent
		with AddMonitoringPointValidation
		with Public
}

/**
 * Adds a new monitoring point to the set of points in the command's state.
 * Does not persist the change (no monitoring point set yet exists)
 */
abstract class AddMonitoringPointCommand(val dept: Department) extends Command[Unit] with ReadOnly with Unaudited with AddMonitoringPointState {

	override def applyInternal() = {
		val point = new MonitoringPoint
		point.name = name
		point.defaultValue = defaultValue
		point.week = week
		monitoringPoints.add(point)
	}
}

trait AddMonitoringPointValidation extends SelfValidating {
	self: AddMonitoringPointState =>

	override def validate(errors: Errors) {
		week match {
			case y if y < 1  => errors.rejectValue("week", "monitoringPoint.week.min")
			case y if y > 52 => errors.rejectValue("week", "monitoringPoint.week.max")
			case _ =>
		}

		if (!name.hasText) {
			errors.rejectValue("name", "NotEmpty")
		} else if (name.length > 4000) {
			errors.rejectValue("name", "monitoringPoint.name.toolong")
		}

		if (monitoringPoints.asScala.count(p => p.name == name && p.week == week) > 0) {
			errors.rejectValue("name", "monitoringPoint.name.exists")
			errors.rejectValue("week", "monitoringPoint.name.exists")
		}
	}
}

trait AddMonitoringPointState extends GroupMonitoringPointsByTerm {
	val dept: Department
	var monitoringPoints = new AutoPopulatingList(classOf[MonitoringPoint])
	var name: String = _
	var defaultValue: Boolean = true
	var week: Int = 0
	var academicYear: AcademicYear = AcademicYear.guessByDate(new DateTime())
	def monitoringPointsByTerm = groupByTerm(monitoringPoints.asScala, academicYear)
}

