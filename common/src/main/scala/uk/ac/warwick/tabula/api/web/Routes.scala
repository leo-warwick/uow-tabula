package uk.ac.warwick.tabula.api.web

import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, RoleOverride}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.web.RoutesUtils

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import RoutesUtils._
	private val context = "/api/v1"

	object assignment {
		def apply(assignment: Assignment): String =
			context + "/module/%s/assignments/%s" format (encoded(assignment.module.code), encoded(assignment.id))
	}

	object submission {
		def apply(submission: Submission): String =
			context + "/module/%s/assignments/%s/submissions/%s" format (encoded(submission.assignment.module.code), encoded(submission.assignment.id), encoded(submission.id))
	}

	object turnitin {
		def submitAssignmentCallback(assignment: Assignment): String =
			context + "/turnitin/turnitin-submit-assignment-response/assignment/%s" format encoded(assignment.id)
	}

	object attachment {
		def apply(attachment: FileAttachment): String =
			context + "/attachments/%s" format encoded(attachment.id)
	}

	object job {
		def apply(job: JobInstance): String =
			context + "/job/%s" format encoded(job.id)
	}

	object timetables {
		def calendar(member: Member): String =
			context + "/member/%s/timetable/calendar" format encoded(member.universityId)

		def calendarICal(member: Member): String =
			context + "/member/%s/timetable/calendar.ics" format encoded(member.universityId)

		def calendarICalForHash(timetableHash: String): String =
			context + "/timetable/calendar/%s.ics" format encoded(timetableHash)
	}

	object groupSet {
		def apply(groupSet: SmallGroupSet): String =
			context + "/module/%s/groups/%s" format (encoded(groupSet.module.code), encoded(groupSet.id))
	}

	object group {
		def apply(group: SmallGroup): String =
			context + "/module/%s/groups/%s/groups/%s" format (encoded(group.groupSet.module.code), encoded(group.groupSet.id), encoded(group.id))
	}

	object event {
		def apply(event: SmallGroupEvent): String =
			context + "/module/%s/groups/%s/groups/%s/events/%s" format (encoded(event.group.groupSet.module.code), encoded(event.group.groupSet.id), encoded(event.group.id), encoded(event.id))
	}

}
