package uk.ac.warwick.tabula.coursework.web

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.web.RoutesUtils
import uk.ac.warwick.userlookup.User

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import RoutesUtils._

	// FIXME this isn't really an optional property, but testing is a pain unless it's made so
	var _cm1Prefix: Option[String] = Wire.optionProperty("${cm1.prefix}")
	def cm1Prefix: String = _cm1Prefix.orNull

	private lazy val context = s"/$cm1Prefix"
	def home: String = context + "/"

	def zipFileJob(jobInstance: JobInstance): String = "/zips/%s" format encoded(jobInstance.id)

	object assignment {
		def apply(assignment: Assignment): String = context + "/module/%s/%s/" format (encoded(assignment.module.code), encoded(assignment.id))
		def receipt(assignment: Assignment): String = apply(assignment)
		def feedback(assignment: Assignment): String = apply(assignment) + "all/feedback.zip"
		def feedbackPdf(assignment: Assignment, feedback: AssignmentFeedback): String = apply(assignment) + "%s/feedback.pdf" format encoded(feedback.usercode)
	}

	object admin {
		def department(department: Department): String = context + "/admin/department/%s/" format encoded(department.code)
		def feedbackTemplates (department: Department): String = context + "/admin/department/%s/settings/feedback-templates/" format encoded(department.code)
		def extensionSettings (department: Department): String = context + "/admin/department/%s/settings/extensions" format encoded(department.code)
		def feedbackReports (department: Department): String = context + "/admin/department/%s/reports/feedback/" format encoded(department.code)

		object markingWorkflow {
			def list(department: Department): String = admin.department(department) + "markingworkflows"
			def add(department: Department): String = list(department) + "/add"
			def edit(scheme: MarkingWorkflow): String = list(scheme.department) + "/edit/" + scheme.id
		}

		object module {
			def apply(module: Module): String = department(module.adminDepartment) + "#module-" + encoded(module.code)
		}

		object assignment {

			private def markerroot(assignment: Assignment, marker: User) = assignmentroot(assignment) + s"/marker/${marker.getWarwickId}"

			object assignMarkers {
				def apply(assignment: Assignment): String = assignmentroot(assignment) + "/assign-markers"
			}

			object markerFeedback {
				def apply(assignment: Assignment, marker: User): String = markerroot(assignment, marker) + "/list"
				object complete {
					def apply(assignment: Assignment, marker: User): String = markerroot(assignment, marker) + "/marking-completed"
				}
				object uncomplete {
					def apply(assignment: Assignment, marker: User): String = markerroot(assignment, marker) + "/marking-uncompleted"
					def apply(assignment: Assignment, marker: User, previousRole: String): String = markerroot(assignment, marker) + "/marking-uncompleted?previousStageRole="+previousRole
				}
				object bulkApprove {
					def apply(assignment: Assignment, marker: User): String = markerroot(assignment, marker) + "/moderation/bulk-approve"
				}
				object marksTemplate {
					def apply(assignment: Assignment, marker: User): String = markerroot(assignment, marker) + "/marks-template"
				}
				object onlineFeedback {
					def apply(assignment: Assignment, marker: User): String = markerroot(assignment, marker) + "/feedback/online"

					object student {
						def apply(assignment: Assignment, marker: User, student: User): String =
							markerroot(assignment, marker) + s"/feedback/online/${student.getUserId}/"
					}
					object moderation {
						def apply(assignment: Assignment, marker: User, student: User): String =
							markerroot(assignment, marker) + s"/feedback/online/moderation/${student.getUserId}/"
					}
				}
				object marks {
					def apply(assignment: Assignment, marker: User): String = markerroot(assignment, marker) + "/marks"
				}
				object feedback {
					def apply(assignment: Assignment, marker: User): String = markerroot(assignment, marker) + "/feedback"
				}
				object submissions {
					def apply(assignment: Assignment, marker: User): String = markerroot(assignment, marker) + "/submissions.zip"
				}
				object downloadFeedback {
					object marker {
						def apply(assignment: Assignment, marker: User, feedbackId: String, filename: String): String =
							markerroot(assignment, marker) + s"/feedback/download/$feedbackId/$filename"
					}

					object all {
						def apply(assignment: Assignment, marker: User, markerFeedback: String): String = markerroot(assignment, marker) + s"/feedback/download/$markerFeedback/attachments/"
					}

					object one {
						def apply(assignment: Assignment, marker: User, markerFeedback: String, filename: String): String = markerroot(assignment, marker) + s"/feedback/download/$markerFeedback/attachment/$filename"
					}
				}
				object returnsubmissions {
					def apply(assignment: Assignment): String = assignmentroot(assignment) + "/submissionsandfeedback/return-submissions"
				}
			}

			object feedbackAdjustment {
				def apply(assignment: Assignment, student: User): String = assignmentroot(assignment) + "/feedback/adjustments"
			}

			object onlineFeedback {
				def apply(assignment: Assignment): String = assignmentroot(assignment) + "/feedback/online"
			}

			object onlineModeration {
				def apply(assignment: Assignment, marker: User): String = assignmentroot(assignment) + s"/marker/${marker.getWarwickId}/feedback/online/moderation"
			}

			def create(module: Module): String = context + "/admin/module/%s/assignments/new" format encoded(module.code)

			private def assignmentroot(assignment: Assignment) = context + "/admin/module/%s/assignments/%s" format (encoded(assignment.module.code), encoded(assignment.id))

			def edit(assignment: Assignment): String = assignmentroot(assignment) + "/edit"

			def delete(assignment: Assignment): String = assignmentroot(assignment) + "/delete"

			def submissionsZip(assignment: Assignment): String = assignmentroot(assignment) + "/submissions.zip"

			object submissionsandfeedback {
				def apply(assignment: Assignment): String = assignmentroot(assignment) + "/list"
				def summary(assignment: Assignment): String = assignmentroot(assignment) + "/summary"
				def table(assignment: Assignment): String = assignmentroot(assignment) + "/table"
			}

			object turnitin {
				def status(assignment: Assignment): String = assignmentroot(assignment) + "/turnitin/status"
				def report(submission: Submission, report: OriginalityReport): String =
					if (report.turnitinId.hasText)
						assignmentroot(submission.assignment) + "/turnitin/lti-report/%s".format (encoded(report.attachment.id))
					else
						assignmentroot(submission.assignment) + "/turnitin/report/%s".format (encoded(report.attachment.id))
			}

			object turnitinlti {
				def fileByToken(submission: Submission, attachment: FileAttachment, token: FileAttachmentToken) =
					s"/turnitin/submission/${submission.id}/attachment/${attachment.id}?token=${token.id}"
			}

			object extension {
				def expandrow (assignment: Assignment, usercode: String): String = assignmentroot(assignment) + "/extensions?usercode=" + usercode

				// def detail doesn't use assignmentroot since assignmentroot includes the assignment ID in the middle, but
				// it needs to be on the end for managing extension requests across department so that
				// it can be passed as a unique contentId when toggling rows (jquery-expandingTable.js)
				def detail (assignment: Assignment): String = context + "/admin/module/%s/assignments/extensions/detail" format encoded(assignment.module.code)
				def revoke (assignment: Assignment, usercode: String): String = assignmentroot(assignment) + "/extensions/revoke/" + usercode
			}
		}
	}

}
