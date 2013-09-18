package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{Appliable, Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.data.model.groups.{WeekRange, DayOfWeek, SmallGroupEvent, SmallGroupSet}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.{AutowiringSmallGroupDaoComponent, Daoisms, SmallGroupDaoComponent}
import collection.JavaConverters._
import org.joda.time.LocalTime
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.data.Transactions._

class SmallGroupEventFixtureCommand extends CommandInternal[Unit] with Logging {
	this: SmallGroupDaoComponent =>
	var setId: String = _
	var groupNumber: Int = 1
	// 1-based index into the groups. Most of the time there will only be a single group
	// so this can be ignored
	var day: String = DayOfWeek.Monday.name
	var start: LocalTime = new LocalTime(12, 0, 0, 0)
	var weekRange: String = "1"
	var location = "Test Place"
	var title = "Test event"

	protected def applyInternal() {
		transactional() {
			val set = smallGroupDao.getSmallGroupSetById(setId).get
			val group = set.groups.asScala(groupNumber - 1)
			val event = new SmallGroupEvent()
			event.group = group
			group.events.add(event)
			event.day = DayOfWeek.members.find(_.name == day).getOrElse(DayOfWeek.Monday)
			event.startTime = start
			event.weekRanges = Seq(WeekRange.fromString(weekRange))
			event.endTime = event.startTime.plusHours(1)
			event.location = location
			event.title = title
		}
	}
}

object SmallGroupEventFixtureCommand {
	def apply(): Appliable[Unit] = {
		new SmallGroupEventFixtureCommand
			with ComposableCommand[Unit]
			with AutowiringSmallGroupDaoComponent
			with Unaudited
			with PubliclyVisiblePermissions

	}
}