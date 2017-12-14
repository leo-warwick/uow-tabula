package uk.ac.warwick.tabula.cm2.web

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.cm2.web.Routes.admin.assignment
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.markingworkflow.{CM2MarkingWorkflow, MarkingWorkflowStage}
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
	var _cm2Prefix: Option[String] = Wire.optionProperty("${cm2.prefix}")
	def cm2Prefix: String = _cm2Prefix.orNull

	def zipFileJob(jobInstance: JobInstance): String = "/zips/%s" format encoded(jobInstance.id)

	private lazy val context = s"/$cm2Prefix"
	def home: String = context + "/"

	object assignment {
		def apply(assignment: Assignment): String = context + s"/submission/${encoded(assignment.id)}/"
		def feedback(assignment: Assignment): String = apply(assignment) + "all/feedback.zip"
		def feedbackPdf(assignment: Assignment, feedback: AssignmentFeedback): String = apply(assignment) + s"${encoded(feedback.usercode)}/feedback.pdf"
	}

	object admin {
		def apply() = s"$context/admin"
		def feedbackTemplates(department: Department): String = apply() + s"/department/${encoded(department.code)}/settings/feedback-templates/"
		def extensionSettings(department: Department): String = apply() + "/department/%s/settings/extensions" format encoded(department.code)
		object extensions {
			def apply(academicYear: AcademicYear): String = admin() + s"/extensions/${encoded(academicYear.startYear.toString)}"
			def detail(extension: Extension): String = extensions(extension.assignment.academicYear) + s"/${extension.id}/detail/"
			def modify(extension: Extension): String = extensions(extension.assignment.academicYear) + s"/${extension.id}/update/"
		}
		def feedbackReports(dept: Department, academicYear: AcademicYear): String =
			department(dept, academicYear) + "/reports/feedback"
		def setupSitsAssignments(dept: Department, academicYear: AcademicYear): String =
			department(dept, academicYear) + "/setup-assignments"
		def copyAssignments(dept: Department, academicYear: AcademicYear): String =
			department(dept, academicYear) + "/copy-assignments"

		object department {
			def apply(department: Department): String =
				admin() + s"/department/${encoded(department.code)}"

			def apply(department: Department, academicYear: AcademicYear): String =
				admin() + s"/department/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}"
		}
		object module {
			def apply(module: Module, academicYear: AcademicYear): String =
				admin() + s"/${encoded(module.code)}/${encoded(academicYear.startYear.toString)}"
			def copyAssignments(module: Module, academicYear: AcademicYear): String =
				apply(module, academicYear) + "/copy-assignments"
		}

		object moduleWithinDepartment {
			def apply(module: Module, academicYear: AcademicYear): String = department(module.adminDepartment, academicYear) + s"?moduleFilters=Module(${encoded(module.code)})#module-${encoded(module.code)}"
		}

		object workflows {
			def apply(dept: Department, academicYear: AcademicYear): String =
				department(dept, academicYear) + "/markingworkflows"
			def add(department: Department, academicYear: AcademicYear): String =
				workflows(department, academicYear) + "/add"
			def addToCurrentYear(department: Department, academicYear: AcademicYear, workflow: CM2MarkingWorkflow): String =
				workflows(department, academicYear) + "/%s/copy" format encoded(workflow.id)
			def edit(department: Department, academicYear: AcademicYear, workflow: CM2MarkingWorkflow): String =
				workflows(department, academicYear) + "/%s/edit" format encoded(workflow.id)
			def delete(department: Department, academicYear: AcademicYear, workflow: CM2MarkingWorkflow): String =
				workflows(department, academicYear) + "/%s/delete" format encoded(workflow.id)
			def replaceMarker(department: Department, academicYear: AcademicYear, workflow: CM2MarkingWorkflow): String =
				workflows(department, academicYear) + "/%s/replace" format encoded(workflow.id)
		}

		object assignment {
			def createAssignmentDetails(module: Module, academicYear: AcademicYear): String = admin() + s"/${encoded(module.code)}/${encoded(academicYear.startYear.toString)}/assignments/new"
			def editAssignmentDetails(assignment: Assignment): String = admin()  + s"/assignments/${encoded(assignment.id)}/edit"
			def createOrEditFeedback(assignment: Assignment, createOrEditMode: String): String = admin() + s"/assignments/${encoded(assignment.id)}/${encoded(createOrEditMode)}/feedback"
			def createOrEditStudents(assignment: Assignment, createOrEditMode: String): String = admin() + s"/assignments/${encoded(assignment.id)}/${encoded(createOrEditMode)}/students"
			def createOrEditMarkers(assignment: Assignment, createOrEditMode: String): String = admin() + s"/assignments/${encoded(assignment.id)}/${encoded(createOrEditMode)}/markers"
			def createOrEditMarkersTemplate(assignment: Assignment, createOrEditMode: String): String = createOrEditMarkers(assignment, createOrEditMode) + "template"
			def createOrEditMarkersTemplateDownload(assignment: Assignment, createOrEditMode: String): String = createOrEditMarkers(assignment, createOrEditMode) + "template/download"
			def createOrEditSubmissions(assignment: Assignment, createOrEditMode: String): String = admin() + s"/assignments/${encoded(assignment.id)}/${encoded(createOrEditMode)}/submissions"
			def createOrEditOptions(assignment: Assignment, createOrEditMode: String): String = admin() + s"/assignments/${encoded(assignment.id)}/${encoded(createOrEditMode)}/options"
			def reviewAssignment(assignment: Assignment): String = admin()  + s"/assignments/${encoded(assignment.id)}/review"

			private def assignmentroot(assignment: Assignment) = admin() + s"/assignments/${encoded(assignment.id)}"

			object feedbackAdjustment {
				def apply(assignment: Assignment, student: User): String = assignmentroot(assignment) + " /feedback/adjustments"
			}

			object marks {
				def apply(assignment: Assignment): String = assignmentroot(assignment) + "/marks"
			}

			object marksTemplate {
				def apply(assignment: Assignment): String = assignmentroot(assignment) + "/marks-template"
			}

			object feedback {
				def online(assignment: Assignment): String = assignmentroot(assignment) + "/feedback/online"
				def batch(assignment: Assignment): String = assignmentroot(assignment) + "/feedback/batch"
				def generic(assignment: Assignment): String = assignmentroot(assignment) + "/feedback/generic"
			}

			def uploadToSits(assignment: Assignment): String = assignmentroot(assignment) + "/upload-to-sits"

			def publishFeedback(assignment: Assignment): String = assignmentroot(assignment) + "/publish"

			def submissionsZip(assignment: Assignment): String = assignmentroot(assignment) + "/submissions.zip"

			object submissionsandfeedback {
				def apply(assignment: Assignment): String = assignmentroot(assignment)
				def summary(assignment: Assignment): String = assignmentroot(assignment) + "/summary"
				def table(assignment: Assignment): String = assignmentroot(assignment) + "/table"
				def list(assignment: Assignment): String = assignmentroot(assignment) + "/list"
			}

			object moderationSampling {
				def apply(assignment: Assignment): String = assignmentroot(assignment) + "/moderator-sampling"
				def allocate(assignment: Assignment): String = apply(assignment) + "/allocate"
				def finalise(assignment: Assignment): String = apply(assignment) + "/finalise-feedback"
			}

			private def markerroot(assignment: Assignment, marker: User) = assignmentroot(assignment) + s"/marker/${marker.getUserId}"

			object markerFeedback {
				def apply(assignment: Assignment, marker: User): String = markerroot(assignment, marker)
				object complete {
					def apply(assignment: Assignment, stagePosition:Int, marker: User): String = markerroot(assignment, marker) + s"/$stagePosition/marking-completed"
				}
				object finish {
					def apply(assignment: Assignment, stagePosition:Int, marker: User): String = markerroot(assignment, marker) + s"/$stagePosition/finish-marking"
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
					def apply(assignment: Assignment, stage: MarkingWorkflowStage, marker: User): String = markerroot(assignment, marker) + s"/${encoded(stage.name)}/feedback/online"

					object student {
						def apply(assignment: Assignment, stage: MarkingWorkflowStage, marker: User, student: User): String =
							onlineFeedback.apply(assignment, stage, marker) + s"/feedback/online/${student.getUserId}/"
					}
					object moderation {
						def apply(assignment: Assignment, stage: MarkingWorkflowStage, marker: User, student: User): String =
							onlineFeedback.apply(assignment, stage, marker) + s"/feedback/online/moderation/${student.getUserId}/"
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
					def apply(assignment: Assignment): String = assignmentroot(assignment) + "/return-submissions"
				}
				object moderationSampling {
					def apply(assignment: Assignment): String = assignmentroot(assignment) + "/moderator-sampling"
				}
			}

			object markerTemplates {
				def apply(assignment: Assignment, marker: User): String = markerroot(assignment, marker) + "/marker-templates.zip"
			}

			object turnitin {
				def status(assignment: Assignment): String = assignmentroot(assignment) + "/turnitin/status"
				def report(assignment: Assignment, report: OriginalityReport): String = assignmentroot(assignment) + s"/turnitin/report/${encoded(report.attachment.id)}"
			}

			def extensions(assignment: Assignment): String = assignmentroot(assignment) + "/extensions"

			def submitToTurnitin(assignment: Assignment): String = assignmentroot(assignment) + "/turnitin"
		}
	}
}