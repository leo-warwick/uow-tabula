package uk.ac.warwick.tabula.sysadmin.web

import org.quartz.TriggerKey
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplate
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
	private val context = "/sysadmin"

	object Departments {
		def home: String = context + "/departments"
		def department(department: Department): String = context + "/departments/%s" format encoded(department.code)
	}

	object AttendanceTemplates {
		def home: String = context + "/attendancetemplates"
		def edit(template: AttendanceMonitoringTemplate): String = context + "/attendancetemplates/%s/edit" format encoded(template.id)
	}

	object Relationships {
		def home: String = context + "/relationships"
	}

	object jobs {
		def list: String = context + "/jobs/list"
		def status(instance: JobInstance): String = context + "/jobs/job-status?id=%s" format encoded(instance.id)
		def quartzStatus(triggerKey: TriggerKey): String = context + "/jobs/quartz-status?key=%s" format encoded(triggerKey.getName)
	}
}
