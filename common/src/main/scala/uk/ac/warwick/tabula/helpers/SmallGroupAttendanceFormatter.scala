package uk.ac.warwick.tabula.helpers

import freemarker.core.TemplateHTMLOutputModel
import org.joda.time.{DateTime, Days}
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.commands.groups.SmallGroupAttendanceState
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroupEventAttendance, SmallGroupEventOccurrence, WeekRange}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.views.BaseTemplateMethodModelEx
import uk.ac.warwick.tabula.{NoCurrentUser, RequestInfo}
import uk.ac.warwick.userlookup.User

case class SmallGroupAttendanceFormatterResult(
  iconClass: String,
  status: TemplateHTMLOutputModel,
  metadata: Option[TemplateHTMLOutputModel],
  wasRecordedLate: Boolean, // Whether the recorded attendance was outside of the 24 hour period that allows it to be used as evidence
)

/**
 * Freemarker helper to build the necessary fields to display attendance at a small group.
 */
class SmallGroupAttendanceFormatter extends BaseTemplateMethodModelEx
  with KnowsUserNumberingSystem
  with AutowiringUserSettingsServiceComponent
  with AutowiringUserLookupComponent {

  override def execMethod(args: Seq[_]): SmallGroupAttendanceFormatterResult =
    args match {
      case Seq(event: SmallGroupEvent, week: SmallGroupEventOccurrence.WeekNumber, user: User, state: SmallGroupAttendanceState, attendance: SmallGroupEventAttendance, _*) =>
        result(event, week, MemberOrUser(user), state, Option(attendance))

      case Seq(event: SmallGroupEvent, week: SmallGroupEventOccurrence.WeekNumber, user: User, state: SmallGroupAttendanceState, _*) =>
        result(event, week, MemberOrUser(user), state, None)

      case Seq(event: SmallGroupEvent, week: SmallGroupEventOccurrence.WeekNumber, member: Member, state: SmallGroupAttendanceState, attendance: SmallGroupEventAttendance, _*) =>
        result(event, week, MemberOrUser(member), state, Option(attendance))

      case Seq(event: SmallGroupEvent, week: SmallGroupEventOccurrence.WeekNumber, member: Member, state: SmallGroupAttendanceState, _*) =>
        result(event, week, MemberOrUser(member), state, None)

      case _ => throw new IllegalArgumentException("Bad args")
    }

  private def result(event: SmallGroupEvent, week: SmallGroupEventOccurrence.WeekNumber, user: MemberOrUser, state: SmallGroupAttendanceState, attendance: Option[SmallGroupEventAttendance]): SmallGroupAttendanceFormatterResult = {
    val result = SmallGroupAttendanceFormatterResult(
      iconClass = "fa fa-minus",
      status = FormattedHtml(formatEvent(event, week)),
      metadata = Some(FormattedHtml(metadata(event, week, user, attendance))),

      // TAB-7814 The auditors would like to see attendance recorded on the same day as the event
      wasRecordedLate = attendance.exists { sgea =>
        event.endDateTimeForWeek(week).exists { endTime =>
          endTime.toLocalDate.isBefore(sgea.updatedDate.toLocalDate)
        }
      },
    )

    state match {
      case SmallGroupAttendanceState.Attended =>
        result.copy(
          iconClass = "fa fa-check attended",
          status = FormattedHtml(s"${user.fullName.getOrElse(user.universityId)} attended: ${formatEvent(event, week)}"),
        )

      case SmallGroupAttendanceState.AttendedRemotely =>
        result.copy(
          iconClass = "fa fa-laptop-house attended-remotely",
          status = FormattedHtml(s"${user.fullName.getOrElse(user.universityId)} attended remotely: ${formatEvent(event, week)}"),
        )

      case SmallGroupAttendanceState.MissedAuthorised =>
        result.copy(
          iconClass = "fa fa-times-circle-o authorised",
          status = FormattedHtml(s"${user.fullName.getOrElse(user.universityId)} did not attend (authorised absence): ${formatEvent(event, week)}"),
        )

      case SmallGroupAttendanceState.MissedUnauthorised =>
        result.copy(
          iconClass = "fa fa-times unauthorised",
          status = FormattedHtml(s"${user.fullName.getOrElse(user.universityId)} did not attend (unauthorised): ${formatEvent(event, week)}"),
        )

      case SmallGroupAttendanceState.Late =>
        result.copy(
          iconClass = "fa fa-exclamation-triangle late",
          status = FormattedHtml(s"No data: ${formatEvent(event, week)}"),
        )

      // The user is no longer in the group so is not expected to attend
      case SmallGroupAttendanceState.NotExpected =>
        result.copy(
          iconClass = "fal fa-user-slash",
          status = FormattedHtml(s"${user.fullName.getOrElse(user.universityId)} is no longer in this group"),
          metadata = None,
          wasRecordedLate = false
        )

      // The user wasn't in the group when this event took place
      case SmallGroupAttendanceState.NotExpectedPast =>
        result.copy(
          iconClass = "fal fa-user-slash",
          status = FormattedHtml(s"${user.fullName.getOrElse(user.universityId)} wasn't a member of this group before ${DateBuilder.format(attendance.get.joinedOn)}"),
          metadata = None,
          wasRecordedLate = false
        )

      case _ => result
    }
  }

  private def formatEvent(event: SmallGroupEvent, week: SmallGroupEventOccurrence.WeekNumber): String =
    Seq(
      event.title.maybeText.map(_ + " ").getOrElse(""),
      event.day.shortName,
      s"${TimeBuilder.format(event.startTime)},",
      WholeWeekFormatter.format(
        ranges = Seq(WeekRange(week)),
        dayOfWeek = event.day,
        year = event.group.groupSet.academicYear,
        numberingSystem = numberingSystem(
          user = RequestInfo.fromThread.map(_.user).getOrElse(NoCurrentUser()),
          department = Some(event.group.groupSet.module.adminDepartment),
        ),
        short = false,
      )
    ).filter(_.hasText).mkString(" ")

  private def formatDifference(endDate: DateTime, recordedDate: DateTime): String = {
    val daysBetween = Days.daysBetween(endDate, recordedDate).getDays

    if (daysBetween == 0) "same day as event"
    else if (daysBetween < 0) s"${-daysBetween} day${if (daysBetween == -1) "" else "s"} before the event"
    else s"$daysBetween day${if (daysBetween == 1) "" else "s"} after the event"
  }

  private def metadata(event: SmallGroupEvent, week: SmallGroupEventOccurrence.WeekNumber, user: MemberOrUser, attendance: Option[SmallGroupEventAttendance]): String = {
    val isStudent = RequestInfo.fromThread.map(_.user.apparentUser.getUserId).contains(user.usercode)

    Seq(
      attendance.map { sgea =>
        val byWho = userLookup.getUserByUserId(sgea.updatedBy) match {
          case FoundUser(user) => s"by ${user.getFullName}, "
          case _ => ""
        }
        val onDate = DateBuilder.format(sgea.updatedDate)

        val howLongAfter = event.endDateTimeForWeek(week).map { endDate =>
          s" (${formatDifference(endDate.toDateTime(sgea.updatedDate.getZone), sgea.updatedDate)})"
        }.getOrElse("")

        s"Recorded $byWho$onDate$howLongAfter."
      }.getOrElse(""),
      if (isStudent) s" If you have any queries, please contact your department${attendance.map(_ => " rather than the individual named here").getOrElse("")}." else ""
    ).filter(_.hasText).mkString(" ")
  }

}
