package uk.ac.warwick.tabula.cm2.web

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.web.RoutesUtils

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

	private lazy val context = s"/$cm2Prefix"
	def home: String = context + "/"

	object assignment {
		def apply(assignment: Assignment): String = context + s"/submission/${encoded(assignment.id)}/"
	}

	object admin {
		def apply() = s"$context/admin"
		def feedbackTemplates(department: Department): String = apply() + s"/department/${encoded(department.code)}/settings/feedback-templates/"
		def extensionSettings (department: Department): String = apply() + "/department/%s/settings/extensions" format encoded(department.code)
		object extensions {
			def apply(): String = admin() + "/extensions"
			def detail(extension: Extension): String = extensions() + s"/${extension.id}/detail/"
			def modify(extension: Extension): String = extensions() + s"/${extension.id}/update/"
		}
		def feedbackReports (department: Department): String = apply() + "/department/%s/reports/feedback/" format encoded(department.code)

		object department {
			def apply(department: Department, academicYear: AcademicYear): String =
				admin() + s"/department/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}"
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
			def createAssignmentDetails(module: Module): String = admin() + s"/${encoded(module.code)}/assignments/new"
			def createAddFeedback(assignment: Assignment): String = admin()  + s"/assignments/new/${encoded(assignment.id)}/feedback"
			def createAddStudents(assignment: Assignment): String = admin()  + s"/assignments/new/${encoded(assignment.id)}/students"
			def createAddMarkers(assignment: Assignment): String = admin()  + s"/assignments/new/${encoded(assignment.id)}/markers"
			def createAddSubmissions(assignment: Assignment): String = admin()  + s"/assignments/new/${encoded(assignment.id)}/submissions"
			def createAddOptions(assignment: Assignment): String = admin()  + s"/assignments/new/${encoded(assignment.id)}/options"
			def reviewAssignment(assignment: Assignment): String = admin()  + s"/assignments/new/${encoded(assignment.id)}/review"

			private def assignmentroot(assignment: Assignment) = admin() + "/assignments/%s" format (encoded(assignment.id))

			def submissionsZip(assignment: Assignment): String = assignmentroot(assignment) + "/submissions.zip"

			object submissionsandfeedback {
				def apply(assignment: Assignment): String = assignmentroot(assignment) + "/list"
				def summary(assignment: Assignment): String = assignmentroot(assignment) + "/summary"
				def table(assignment: Assignment): String = assignmentroot(assignment) + "/table"
			}

			object audit {
				def apply(assignment: Assignment): String = admin() + s"/audit/assignment/${encoded(assignment.id)}"
			}
			def extensions(assignment: Assignment): String = admin() + s"/assignments/${encoded(assignment.id)}/manage/extensions"
		}
	}
}