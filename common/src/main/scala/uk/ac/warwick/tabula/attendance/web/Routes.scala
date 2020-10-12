package uk.ac.warwick.tabula.attendance.web

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.data.model.{Department, Member, StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.web.RoutesUtils

/**
  * Generates URLs to various locations, to reduce the number of places where URLs
  * are hardcoded and repeated.
  *
  * For methods called "apply", you can leave out the "apply" and treat the object like a function.
  */
object Routes {

  import RoutesUtils._

  private val context = "/attendance"

  def home: String = context + "/"

  def homeForYear(academicYear: AcademicYear): String = context + "/%s" format encoded(academicYear.startYear.toString)

  object Manage {
    def homeForYear(academicYear: AcademicYear): String = context + "/manage/%s" format encoded(academicYear.startYear.toString)

    def departmentForYear(department: Department, academicYear: AcademicYear): String =
      context + "/manage/%s/%s".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def addStudentsToScheme(scheme: AttendanceMonitoringScheme): String =
      context + "/manage/%s/%s/new/%s/students".format(
        encoded(scheme.department.code), encoded(scheme.academicYear.startYear.toString), encoded(scheme.id)
      )

    def addPointsToNewScheme(scheme: AttendanceMonitoringScheme): String =
      context + "/manage/%s/%s/new/%s/points".format(
        encoded(scheme.department.code), encoded(scheme.academicYear.startYear.toString), encoded(scheme.id)
      )

    def editScheme(scheme: AttendanceMonitoringScheme): String =
      context + "/manage/%s/%s/%s/edit".format(
        encoded(scheme.department.code), encoded(scheme.academicYear.startYear.toString), encoded(scheme.id)
      )

    def editSchemeStudents(scheme: AttendanceMonitoringScheme): String =
      context + "/manage/%s/%s/%s/edit/students".format(
        encoded(scheme.department.code), encoded(scheme.academicYear.startYear.toString), encoded(scheme.id)
      )

    def editSchemePoints(scheme: AttendanceMonitoringScheme): String =
      context + "/manage/%s/%s/%s/edit/points".format(
        encoded(scheme.department.code), encoded(scheme.academicYear.startYear.toString), encoded(scheme.id)
      )

    def editPoints(department: Department, academicYear: AcademicYear): String =
      context + "/manage/%s/%s/editpoints".format(
        encoded(department.code), encoded(academicYear.startYear.toString)
      )

    def recheckPoint(department: Department, academicYear: AcademicYear, point: AttendanceMonitoringPoint, serializedFilter: String, returnTo: String): String =
      context + "/manage/%s/%s/editpoints/%s/recheck?returnTo=%s&%s".format(
        encoded(department.code), encoded(academicYear.startYear.toString), encoded(point.id), encoded(returnTo), serializedFilter
      )

    def addPointsToExistingSchemes(department: Department, academicYear: AcademicYear): String =
      context + "/manage/%s/%s/addpoints".format(
        encoded(department.code), encoded(academicYear.startYear.toString)
      )

  }

  object Note {
    def view(academicYear: AcademicYear, student: StudentMember, point: AttendanceMonitoringPoint): String =
      context + "/note/%s/%s/%s".format(encoded(academicYear.startYear.toString), encoded(student.universityId), encoded(point.id))

  }

  object View {
    def homeForYear(academicYear: AcademicYear): String = context + "/view/%s" format encoded(academicYear.startYear.toString)

    def departmentForYear(department: Department, academicYear: AcademicYear): String =
      context + "/view/%s/%s".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def students(department: Department, academicYear: AcademicYear): String =
      context + "/view/%s/%s/students".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def studentsUnrecorded(department: Department, academicYear: AcademicYear): String =
      context + "/view/%s/%s/students?hasBeenFiltered=true&otherCriteria=Unrecorded".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def student(department: Department, academicYear: AcademicYear, student: StudentMember): String =
      context + "/view/%s/%s/students/%s".format(
        encoded(department.code),
        encoded(academicYear.startYear.toString),
        encoded(student.universityId)
      )

    def points(department: Department, academicYear: AcademicYear): String =
      context + "/view/%s/%s/points".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def pointsUnrecorded(department: Department, academicYear: AcademicYear): String =
      context + "/view/%s/%s/points?hasBeenFiltered=true&otherCriteria=Unrecorded".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def pointRecordUpload(department: Department, academicYear: AcademicYear, templatePoint: AttendanceMonitoringPoint, args: String = ""): String =
      context + "/view/%s/%s/points/%s/record/upload%s".format(encoded(department.code), encoded(academicYear.startYear.toString), encoded(templatePoint.id), if (args.length > 0) "?" + args else "")

    def pointRecordTemplate(department: Department, academicYear: AcademicYear, templatePoint: AttendanceMonitoringPoint, args: String = ""): String =
      context + "/view/%s/%s/points/%s/record/upload/template%s".format(encoded(department.code), encoded(academicYear.startYear.toString), encoded(templatePoint.id), if (args.length > 0) "?" + args else "")

    def agents(department: Department, academicYear: AcademicYear, relationshipType: StudentRelationshipType): String =
      context + "/view/%s/%s/agents/%s".format(
        encoded(department.code),
        encoded(academicYear.startYear.toString),
        encoded(relationshipType.urlPart)
      )

    def agent(department: Department, academicYear: AcademicYear, relationshipType: StudentRelationshipType, agent: Member): String =
      context + "/view/%s/%s/agents/%s/%s".format(
        encoded(department.code),
        encoded(academicYear.startYear.toString),
        encoded(relationshipType.urlPart),
        encoded(agent.universityId)
      )
  }

  object Agent {
    def home: String = context + "/agent"

    def relationshipForYear(relationshipType: StudentRelationshipType, academicYear: AcademicYear): String =
      context + "/agent/%s/%s".format(encoded(relationshipType.urlPart), encoded(academicYear.startYear.toString))

    def student(relationshipType: StudentRelationshipType, academicYear: AcademicYear, student: StudentMember): String =
      context + "/agent/%s/%s/%s".format(encoded(relationshipType.urlPart), encoded(academicYear.startYear.toString), encoded(student.universityId))

    def pointRecordUpload(relationshipType: StudentRelationshipType, academicYear: AcademicYear, templatePoint: AttendanceMonitoringPoint): String =
      context + "/agent/%s/%s/point/%s/upload".format(encoded(relationshipType.urlPart), encoded(academicYear.startYear.toString), encoded(templatePoint.id))
  }

  object Profile {
    def home: String = context + "/profile"

    def profileForYear(student: StudentMember, academicYear: AcademicYear): String =
      context + "/profile/%s/%s".format(encoded(student.universityId), encoded(academicYear.startYear.toString))

    def record(student: StudentMember, academicYear: AcademicYear): String =
      context + "/profile/%s/%s/record".format(encoded(student.universityId), encoded(academicYear.startYear.toString))
  }

}
