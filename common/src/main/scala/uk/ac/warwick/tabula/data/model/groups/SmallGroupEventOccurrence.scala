package uk.ac.warwick.tabula.data.model.groups

import javax.persistence._

import org.hibernate.annotations.BatchSize
import org.joda.time.{DateTime, Interval, LocalDate}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.TermService

@Access(AccessType.FIELD)
@Entity
@Table(uniqueConstraints = Array(
	new UniqueConstraint(columnNames = Array("event_id", "week"))
))
class SmallGroupEventOccurrence extends GeneratedId with PermissionsTarget with Serializable with ToEntityReference {

	override type Entity = SmallGroupEventOccurrence

	@transient var termService: TermService = Wire[TermService]

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="event_id", updatable = false)
	var event: SmallGroupEvent = _

	var week: SmallGroupEventOccurrence.WeekNumber = _

	@OneToMany(mappedBy = "occurrence", cascade=Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=200)
	var attendance: JSet[SmallGroupEventAttendance] = JHashSet()

	def permissionsParents = Stream(event)

	def date: Option[LocalDate] = {
		date(termService.getAcademicWeeksForYear(event.group.groupSet.academicYear.dateInTermOne).toMap)
	}
	def date(weeksForYear: Map[Integer, Interval]): Option[LocalDate] = {
		event.day match {
			case null => None
			case d: DayOfWeek =>
				def weekNumberToDate(weekNumber: Int, dayOfWeek: DayOfWeek) =
					weeksForYear(weekNumber).getStart.withDayOfWeek(dayOfWeek.jodaDayOfWeek)

				Option(weekNumberToDate(week, event.day).toLocalDate)
		}
	}

	def dateTime: Option[DateTime] = date.map(_.toDateTime(event.startTime))
	def dateTime(weeksForYear: Map[Integer, Interval]): Option[DateTime] = date(weeksForYear).map(_.toDateTime(event.startTime))

	override def toEntityReference: SmallGroupEventOcurrenceEntityReference = new SmallGroupEventOcurrenceEntityReference().put(this)
}

object SmallGroupEventOccurrence {
	type WeekNumber = Int
}
