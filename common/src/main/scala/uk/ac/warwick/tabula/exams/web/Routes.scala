package uk.ac.warwick.tabula.exams.web

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import uk.ac.warwick.tabula.web.RoutesUtils._
	private val context = "/exams"
	def home: String = context + "/"

	object Exams {

		private val context = "/exams/exams"

		def home: String = context + "/"

		object admin {

			private def departmentRoot(department: Department) = context + "/admin/department/%s" format encoded(department.code)

			def department(department: Department, academicYear: AcademicYear): String =
				departmentRoot(department) + "/%s" format encoded(academicYear.startYear.toString)

			def module(module: Module, academicYear: AcademicYear): String = context + "/admin/module/%s/%s" format(encoded(module.code), encoded(academicYear.startYear.toString))

			object markingWorkflow {
				def list(department: Department): String = admin.departmentRoot(department) + "/markingworkflows"
				def add(department: Department): String = list(department) + "/add"
				def edit(scheme: MarkingWorkflow): String = list(scheme.department) + "/edit/" + scheme.id
			}

			object exam {
				def apply(exam: Exam): String =
					context + "/admin/module/%s/%s/exams/%s" format(
						encoded(exam.module.code),
						encoded(exam.academicYear.startYear.toString),
						encoded(exam.id)
						)

				object assignMarkers {
					def apply(exam: Exam): String = admin.exam(exam) + "/assign-markers"
				}
			}

			private def markerroot(exam: Exam, marker: User) = admin.exam(exam) + s"/marker/${marker.getWarwickId}"

			object markerFeedback {
				def apply(exam: Exam, marker: User): String = markerroot(exam, marker) + "/marks"
				object onlineFeedback {
					def apply(exam: Exam, marker: User): String = markerroot(exam, marker) + "/feedback/online"
				}
			}

			object onlineModeration {
				def apply(exam: Exam, marker: User): String = admin.exam(exam) + s"/marker/${marker.getWarwickId}/feedback/online/moderation"
			}
		}

	}

	object Grids {

		private val context = "/exams/grids"

		def home: String = context + "/"

		def departmentAcademicYear(department: Department, academicYear: AcademicYear): String =
			context + "/%s/%s" format(encoded(department.code), encoded(academicYear.startYear.toString))

		def generate(department: Department, academicYear: AcademicYear): String =
			context + "/%s/%s/generate" format(encoded(department.code), encoded(academicYear.startYear.toString))

		def normalLoad(department: Department, academicYear: AcademicYear): String =
			context + "/%s/%s/normalload" format(encoded(department.code), encoded(academicYear.startYear.toString))

		def weightings(department: Department, academicYear: AcademicYear): String =
			context + "/%s/%s/weightings" format(encoded(department.code), encoded(academicYear.startYear.toString))

	}

}
