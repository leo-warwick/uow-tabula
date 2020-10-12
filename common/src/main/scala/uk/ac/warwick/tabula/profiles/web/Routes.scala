package uk.ac.warwick.tabula.profiles.web

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.web.RoutesUtils

/**
  * Generates URLs to various locations, to reduce the number of places where URLs
  * are hardcoded and repeated.
  *
  * For methods called "apply", you can leave out the "apply" and treat the object like a function.
  */
object Routes {

  import RoutesUtils._

  private val context = "/profiles"

  def home: String = context + "/"

  def search: String = context + "/search"

  object Profile {
    def identity(member: Member): String =
      context + "/view/%s" format encoded(member.universityId)

    def identity(scyd: StudentCourseYearDetails): String =
      context + "/view/course/%s/%s".format(encoded(scyd.studentCourseDetails.urlSafeId), encoded(scyd.academicYear.value.toString))

    def timetable(member: Member): String = context + "/view/%s/timetable" format encoded(member.universityId)

    def timetable(scyd: StudentCourseYearDetails): String =
      context + "/view/course/%s/%s/timetable".format(encoded(scyd.studentCourseDetails.urlSafeId), encoded(scyd.academicYear.value.toString))

    def relationshipType(member: Member, relationshipType: StudentRelationshipType): String =
      context + "/view/%s/%s".format(encoded(member.universityId), encoded(relationshipType.urlPart))

    def relationshipType(scyd: StudentCourseYearDetails, relationshipType: StudentRelationshipType): String =
      context + "/view/course/%s/%s/%s".format(encoded(scyd.studentCourseDetails.urlSafeId), encoded(scyd.academicYear.value.toString), encoded(relationshipType.urlPart))

    def relationshipType(scd: StudentCourseDetails, academicYear: AcademicYear, relationshipType: StudentRelationshipType): String =
      context + "/view/course/%s/%s/%s".format(encoded(scd.urlSafeId), encoded(academicYear.value.toString), encoded(relationshipType.urlPart))

    def assignments(scyd: StudentCourseYearDetails): String =
      context + "/view/course/%s/%s/assignments".format(encoded(scyd.studentCourseDetails.urlSafeId), encoded(scyd.academicYear.value.toString))

    def modules(scyd: StudentCourseYearDetails): String =
      context + "/view/course/%s/%s/modules".format(encoded(scyd.studentCourseDetails.urlSafeId), encoded(scyd.academicYear.value.toString))

    def events(universityId: String): String =
      context + "/view/%s/events" format encoded(universityId)

    def events(member: Member): String =
      context + "/view/%s/events" format encoded(member.universityId)

    def events(scyd: StudentCourseYearDetails): String =
      context + "/view/course/%s/%s/events".format(encoded(scyd.studentCourseDetails.urlSafeId), encoded(scyd.academicYear.value.toString))

    def marking(member: Member): String = context + "/view/%s/marking" format encoded(member.universityId)

    def marking(scyd: StudentCourseYearDetails): String =
      context + "/view/course/%s/%s/marking".format(encoded(scyd.studentCourseDetails.urlSafeId), encoded(scyd.academicYear.value.toString))

    def attendance(member: Member): String = context + "/view/%s/attendance" format encoded(member.universityId)

    def attendance(scyd: StudentCourseYearDetails): String =
      context + "/view/course/%s/%s/attendance".format(encoded(scyd.studentCourseDetails.urlSafeId), encoded(scyd.academicYear.value.toString))

    def students(member: Member): String = context + "/view/%s/students" format encoded(member.universityId)

    def students(department: Department, academicYear: AcademicYear): String = context + "/department/%s/students/%s".format(encoded(department.code), encoded(academicYear.startYear.toString))

    def examTimetable(universityId: String): String = context + "/view/%s/exams" format encoded(universityId)

    def download(member: Member): String = s"$context/view/${member.universityId}/download"

    def download(scyd: StudentCourseYearDetails): String = s"$context/view/${scyd.studentCourseDetails.urlSafeId}/${scyd.academicYear.value.toString}/download"

    object PersonalCircumstances {
      def apply(): String = context + "/view/me/personalcircs"
      def apply(student: StudentMember): String = context + "/view/%s/personalcircs" format encoded(student.universityId)
      def create(student: StudentMember): String = apply(student) + "/mitcircs/new"
      def edit(submission: MitigatingCircumstancesSubmission): String = apply(submission.student) + "/mitcircs/edit/%s" format encoded(submission.key.toString)
      def pendingEvidence(submission: MitigatingCircumstancesSubmission): String = apply(submission.student) + "/mitcircs/pendingevidence/%s" format encoded(submission.key.toString)
      def view(submission: MitigatingCircumstancesSubmission): String = apply(submission.student) + "/mitcircs/view/%s" format encoded(submission.key.toString)
      def viewMessages(submission: MitigatingCircumstancesSubmission): String = view(submission) + "#messages"
    }
  }

  def students(relationshipType: StudentRelationshipType): String = context + "/%s/students" format encoded(relationshipType.urlPart)

  object relationships {
    def apply(department: Department, relationshipType: StudentRelationshipType): String =
      context + "/department/%s/%s".format(encoded(department.code), encoded(relationshipType.urlPart))

    def missing(department: Department, relationshipType: StudentRelationshipType): String =
      context + "/department/%s/%s/missing".format(encoded(department.code), encoded(relationshipType.urlPart))

    def scheduled(department: Department, relationshipType: StudentRelationshipType): String =
      context + "/department/%s/%s/scheduled".format(encoded(department.code), encoded(relationshipType.urlPart))

    def allocate(department: Department, relationshipType: StudentRelationshipType): String =
      context + "/department/%s/%s/allocate".format(encoded(department.code), encoded(relationshipType.urlPart))

    def template(department: Department, relationshipType: StudentRelationshipType): String =
      context + "/department/%s/%s/template".format(encoded(department.code), encoded(relationshipType.urlPart))
  }

  object admin {
    def apply(department: Department): String = Routes.home // TODO https://repo.elab.warwick.ac.uk/projects/TAB/repos/tabula/pull-requests/145/overview?commentId=1012
    def departmentPermissions(department: Department): String = context + "/admin/department/%s/permissions" format encoded(department.code)
  }

  object scheduledMeeting {
    def confirm(meetingRecord: ScheduledMeetingRecord, studentCourseDetails: StudentCourseDetails, relationshipType: StudentRelationshipType): String =
      context + "/%s/meeting/%s/schedule/%s/confirm".format(
        encoded(relationshipType.urlPart),
        encoded(studentCourseDetails.urlSafeId),
        encoded(meetingRecord.id)
      )

    def reschedule(meetingRecord: ScheduledMeetingRecord, studentCourseDetails: StudentCourseDetails, relationshipType: StudentRelationshipType): String =
      context + "/%s/meeting/%s/schedule/%s/edit".format(
        encoded(relationshipType.urlPart),
        encoded(studentCourseDetails.urlSafeId),
        encoded(meetingRecord.id)
      )

    def missed(meetingRecord: ScheduledMeetingRecord, studentCourseDetails: StudentCourseDetails, relationshipType: StudentRelationshipType): String =
      context + "/%s/meeting/%s/missed".format(encoded(relationshipType.urlPart), encoded(meetingRecord.id))
  }

  object Note {
    def apply(student: StudentMember, point: AttendanceMonitoringPoint): String =
      context + "/attendance/note/%s/%s".format(encoded(student.universityId), encoded(point.id))

  }

}
