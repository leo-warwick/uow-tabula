package uk.ac.warwick.tabula.services.timetables

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.{CurrentUser, FeaturesComponent}
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.ExecutionContexts.timetable
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.mitcircs.MitCircsPanelServiceComponent
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventOccurrenceList
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent}

import scala.concurrent.Future

trait MitCircsPanelEventOccurrenceSourceComponent extends EventOccurrenceSourceComponent {
  self: MitCircsPanelServiceComponent with FeaturesComponent with SecurityServiceComponent =>

  def eventOccurrenceSource = new MitCircsPanelEventOccurrenceSource

  class MitCircsPanelEventOccurrenceSource extends EventOccurrenceSource {
    def occurrencesFor(member: Member, currentUser: CurrentUser, context: TimetableEvent.Context, start: LocalDate, end: LocalDate): Future[EventOccurrenceList] =
      if (features.timetableIncludeMitCircsPanelEvents) {
        Future {
          // TODO apply date filtering in the DB so we don't have to filter in code at the end
          val panels = mitCircsPanelService.getPanels(MemberOrUser(member))

          val eventOccurrences = panels.toSeq
            .filter(p => Option(p.date).nonEmpty)
            .sortBy(_.date)
            .map(p => p -> p.toEventOccurrence(context))
            .flatMap {
              case (p, event) if securityService.can(currentUser, Permissions.MitigatingCircumstancesSubmission.Read, p) =>
                event

              // No permission to read panel details, just show as busy
              case (_, event) => event.map(EventOccurrence.busy)
            }

          EventOccurrenceList.fresh(eventOccurrences)
            .map(_.filterNot { event =>
              event.end.toLocalDate.isBefore(start) || event.start.toLocalDate.isAfter(end)
            })
        }
      } else {
        Future.successful(EventOccurrenceList.empty)
      }
  }

}

