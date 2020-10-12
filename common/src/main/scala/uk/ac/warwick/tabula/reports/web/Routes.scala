package uk.ac.warwick.tabula.reports.web

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.RoutesUtils

object Routes {

  import RoutesUtils._

  private val context = "/reports"

  def home: String = context + "/"

  def departmentHome(department: Department): String =
    context + "/%s" format encoded(department.code)

  def departmentAcademicYear(department: Department, academicYear: AcademicYear): String =
    context + "/%s/%s".format(encoded(department.code), encoded(academicYear.startYear.toString))

  object Attendance {
    def home(department: Department, academicYear: AcademicYear): String =
      context + "/%s/%s/attendance".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def all(department: Department, academicYear: AcademicYear): String =
      context + "/%s/%s/attendance/all".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def missed(department: Department, academicYear: AcademicYear): String =
      context + "/%s/%s/attendance/unrecorded".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def unrecorded(department: Department, academicYear: AcademicYear): String =
      context + "/%s/%s/attendance/missed".format(encoded(department.code), encoded(academicYear.startYear.toString))
  }

  object SmallGroups {
    def home(department: Department, academicYear: AcademicYear): String =
      context + "/%s/%s/groups".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def all(department: Department, academicYear: AcademicYear): String =
      context + "/%s/%s/groups/all".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def unrecorded(department: Department, academicYear: AcademicYear): String =
      context + "/%s/%s/groups/unrecorded".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def unrecordedByModule(department: Department, academicYear: AcademicYear): String =
      context + "/%s/%s/groups/unrecorded/bymodule".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def missed(department: Department, academicYear: AcademicYear): String =
      context + "/%s/%s/groups/missed".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def missedByModule(department: Department, academicYear: AcademicYear): String =
      context + "/%s/%s/groups/missed/bymodule".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def events(department: Department, academicYear: AcademicYear): String =
      context + "/%s/%s/groups/events".format(encoded(department.code), encoded(academicYear.startYear.toString))

    object AttendanceRegisters {
      def all(department: Department, academicYear: AcademicYear): String =
        s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/groups/attendance/all"

      def unrecorded(department: Department, academicYear: AcademicYear): String =
        s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/groups/attendance/unrecorded"

      def recordedLate(department: Department, academicYear: AcademicYear): String =
        s"$context/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/groups/attendance/recorded-late"
    }
  }

  object Profiles {
    def home(department: Department, academicYear: AcademicYear): String =
      context + "/%s/%s/profiles".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def export(department: Department, academicYear: AcademicYear): String =
      context + "/%s/%s/profiles/export".format(encoded(department.code), encoded(academicYear.startYear.toString))
  }

  object Coursework {
    def home(department: Department, academicYear: AcademicYear): String =
      context + "/%s/%s/coursework".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def missed(department: Department, academicYear: AcademicYear): String =
      context + "/%s/%s/coursework/missed".format(encoded(department.code), encoded(academicYear.startYear.toString))
  }

}
