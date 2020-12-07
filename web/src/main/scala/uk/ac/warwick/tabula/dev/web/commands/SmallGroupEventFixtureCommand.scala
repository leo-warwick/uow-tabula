package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{Appliable, Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.data.model.NamedLocation
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.{AutowiringSmallGroupDaoComponent, Daoisms, SmallGroupDaoComponent}
import scala.jdk.CollectionConverters._
import org.joda.time.LocalTime
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.data.Transactions._

class SmallGroupEventFixtureCommand extends CommandInternal[SmallGroupEvent] with Logging {
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

  protected def applyInternal(): SmallGroupEvent =
    transactional() {
      val set = smallGroupDao.getSmallGroupSetById(setId).get
      val group = set.groups.asScala(groupNumber - 1)
      val event = new SmallGroupEvent()
      event.group = group
      group.addEvent(event)
      event.day = DayOfWeek.members.find(_.name == day).getOrElse(DayOfWeek.Monday)
      event.startTime = start
      event.weekRanges = Seq(WeekRange.fromString(weekRange))
      event.endTime = event.startTime.plusHours(1)
      event.location = NamedLocation(location)
      event.title = title
      event.deliveryMethod = EventDeliveryMethod.Hybrid

      smallGroupDao.saveOrUpdate(group)

      event
    }
}

object SmallGroupEventFixtureCommand {
  def apply(): SmallGroupEventFixtureCommand with ComposableCommand[SmallGroupEvent] with AutowiringSmallGroupDaoComponent with Unaudited with PubliclyVisiblePermissions = {
    new SmallGroupEventFixtureCommand
      with ComposableCommand[SmallGroupEvent]
      with AutowiringSmallGroupDaoComponent
      with Unaudited
      with PubliclyVisiblePermissions

  }
}