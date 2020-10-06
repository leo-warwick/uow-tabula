package uk.ac.warwick.tabula.services.timetables

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, CurrentUser}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventOccurrenceList
import uk.ac.warwick.tabula.timetables.TimetableEvent
import uk.ac.warwick.tabula.helpers.ExecutionContexts.timetable
import uk.ac.warwick.tabula.helpers.{Futures, Logging}
import uk.ac.warwick.tabula.services.mitcircs.AutowiringMitCircsPanelServiceComponent
import uk.ac.warwick.tabula.services.permissions.AutowiringCacheStrategyComponent

import scala.concurrent.Future

// A searchable source of EventOccurences.
trait EventOccurrenceSource {
  def occurrencesFor(member: Member, currentUser: CurrentUser, context: TimetableEvent.Context, start: LocalDate, end: LocalDate): Future[EventOccurrenceList]
}

trait EventOccurrenceSourceComponent {
  def eventOccurrenceSource: EventOccurrenceSource
}

class CombinedEventOccurrenceSource(sources: Seq[(String, EventOccurrenceSource)]) extends EventOccurrenceSource with Logging {
  def occurrencesFor(member: Member, currentUser: CurrentUser, context: TimetableEvent.Context, start: LocalDate, end: LocalDate) = {

    val results = sources.map { case (id, source) =>
      val result = source.occurrencesFor(member, currentUser, context, start, end)
      result.failed.foreach { e =>
        logger.warn(s"Event source $id failed to fetch for user ${member.universityId}/${member.userId}", e)
      }
      result
    }

    Futures.combineAnySuccess(results, EventOccurrenceList.combine)
  }
}

trait AutowiringEventOccurrenceSourceComponent
  extends EventOccurrenceSourceComponent
    with CachedEventOccurrenceSourceComponent
    with AutowiringCacheStrategyComponent {

  val eventOccurrenceSource: EventOccurrenceSource = {
    val meetingRecordSource = (new MeetingRecordEventOccurrenceSourceComponent
      with AutowiringRelationshipServiceComponent
      with AutowiringMeetingRecordServiceComponent
      with AutowiringSecurityServiceComponent
    ).eventOccurrenceSource

    val mitCircsPanelsSource = (new MitCircsPanelEventOccurrenceSourceComponent
      with AutowiringMitCircsPanelServiceComponent
      with AutowiringFeaturesComponent
      with AutowiringSecurityServiceComponent
    ).eventOccurrenceSource

    val skillsforgeSource = (new SkillsforgeServiceComponent
      with AutowiringCacheStrategyComponent
      with AutowiringFeaturesComponent
      with AutowiringSkillsforgeConfigurationComponent
      with AutowiringApacheHttpClientComponent
    ).eventOccurrenceSource

    val skillsforgeCached = new CachedEventOccurrenceSource(cacheName = "skillsforge", delegate = skillsforgeSource)

    new CombinedEventOccurrenceSource(Seq(
      "meeting records" -> meetingRecordSource,
      "mitcircs_panels" -> mitCircsPanelsSource,
      "Skillsforge" -> skillsforgeCached,
    ))
  }
}
