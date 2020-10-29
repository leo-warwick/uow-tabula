package uk.ac.warwick.tabula.commands.reports.profiles

import freemarker.core.TemplateHTMLOutputModel
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.profiles.PhotosWarwickMemberPhotoUrlGeneratorComponent
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringPointType}
import uk.ac.warwick.tabula.data.{AutowiringFileDaoComponent, FileDaoComponent}
import uk.ac.warwick.tabula.pdf.FreemarkerXHTMLPDFGeneratorWithFileStorageComponent
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.AutowiredTextRendererComponent
import uk.ac.warwick.tabula.{AcademicYear, AutowiringTopLevelUrlComponent, CurrentUser}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

object ProfileExportSingleCommand {
  type CommandType = Appliable[Seq[FileAttachment]]

  val DateFormat: DateTimeFormatter = DateTimeFormat.forPattern("dd/MM/yyyy")
  val TimeFormat: DateTimeFormatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm")

  def apply(student: StudentMember, academicYear: AcademicYear, user: CurrentUser) =
    new ProfileExportSingleCommandInternal(student, academicYear, user)
      with AutowiredTextRendererComponent
      with FreemarkerXHTMLPDFGeneratorWithFileStorageComponent
      with PhotosWarwickMemberPhotoUrlGeneratorComponent
      with AutowiringAttendanceMonitoringServiceComponent
      with AutowiringUserLookupComponent
      with AutowiringAssessmentServiceComponent
      with AutowiringRelationshipServiceComponent
      with AutowiringMeetingRecordServiceComponent
      with AutowiringSmallGroupServiceComponent
      with AutowiringSecurityServiceComponent
      with AutowiringMemberNoteServiceComponent
      with AutowiringFileDaoComponent
      with AutowiringTopLevelUrlComponent
      with ComposableCommand[Seq[FileAttachment]]
      with ProfileExportSingleDescription
      with ProfileExportSinglePermissions
      with ProfileExportSingleCommandState
}


class ProfileExportSingleCommandInternal(val student: StudentMember, val academicYear: AcademicYear, user: CurrentUser)
  extends CommandInternal[Seq[FileAttachment]] with TaskBenchmarking {

  self: FreemarkerXHTMLPDFGeneratorWithFileStorageComponent with AttendanceMonitoringServiceComponent
    with AssessmentServiceComponent with RelationshipServiceComponent with MeetingRecordServiceComponent
    with SmallGroupServiceComponent with UserLookupComponent
    with SecurityServiceComponent with MemberNoteServiceComponent
    with FileDaoComponent =>

  import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

  case class PointData(
    departmentName: String,
    term: String,
    state: String,
    name: String,
    pointType: String,
    pointTypeInfo: String,
    startDate: String,
    endDate: String,
    recordedBy: User,
    recordedDate: String,
    attendanceNote: Option[AttendanceNote]
  )

  case class AssignmentData(
    module: String,
    name: String,
    submissionDeadline: String,
    submissionDate: String,
    attachments: Seq[FileAttachment],
    feedback: Option[FeedbackData]
  )

  case class FeedbackData(
    releasedDate: String,
    mark: Option[Int],
    grade: Option[String],
    comments: Option[String],
    attachments: Seq[FileAttachment],
    adjustments: Seq[AdjustmentData]
  )

  case class AdjustmentData(
    mark: Int,
    grade: Option[String],
    reason: String,
    comments: String,
    date: String
  )

  case class SmallGroupData(
    eventId: String,
    title: String,
    day: String,
    location: String,
    tutors: String,
    week: Int,
    state: String,
    recordedBy: User,
    recordedDate: String,
    attendanceNote: Option[AttendanceNote]
  )

  case class MeetingData(
    relationshipType: String,
    agent: String,
    meetingDate: String,
    title: String,
    format: String,
    description: TemplateHTMLOutputModel,
    attachments: Seq[FileAttachment]
  )

  case class AdministrativeNote(
    date: String,
    title: String,
    note: String,
    noteHTML: TemplateHTMLOutputModel,
    attachments: Seq[FileAttachment]
  )

  case class CourseData(
    courseName: Option[String],
    courseCode: Option[String],
    beginYear: Option[String],
    endYear: Option[String],
    departmentName: Option[String],
    departmentCode: Option[String],
    degreeType: Option[String],
    awardName: Option[String],
    modeOfAttendance: Option[String],
    statusOnCourse: Option[String],
    endDate: Option[String],
    expectedEndDate: Option[String],
    routeName: Option[String],
    routeCode: Option[String],
    yearOfStudy: String,
    hasCurrentEnrolment: Boolean,
    courseLevelCode: Option[String],
    courseLevelName: Option[String],
    sprCode: String,
    scjCode: String
  )

  override def applyInternal(): Seq[FileAttachment] = {
    // Get point data
    val pointData = if (securityService.can(user, Permissions.MonitoringPoints.View, student)) {
      benchmarkTask("pointData") {
        getPointData(academicYear)
      }
    } else {
      Nil
    }

    def viewableAdjustments(feedback: Feedback): Seq[Mark] = {
      if (securityService.can(user, Permissions.Feedback.Manage, feedback)) {
        feedback.adminViewableAdjustments
      } else {
        feedback.studentViewableAdjustments
      }
    }

    val administrativeNotesData = if (securityService.can(user, Permissions.MemberNotes.Read, student))
      memberNoteService.listNonDeletedNotes(student)
        .map(memberNote => AdministrativeNote(
          date = memberNote.creationDate.toString(ProfileExportSingleCommand.DateFormat),
          title = memberNote.title,
          note = memberNote.note,
          noteHTML = memberNote.escapedNote,
          attachments = memberNote.attachments.asScala.toSeq
        ))
      else Nil

    // Get coursework
    val assignmentData = benchmarkTask("assignmentData") {
      assessmentService.getAssignmentsWithSubmission(student.userId)
        .filter(_.academicYear == academicYear)
        .filter(assignment => securityService.can(user, Permissions.Assignment.Read, assignment) || securityService.can(user, Permissions.Submission.Create, assignment))
        .sortBy(assignment => Option(assignment.closeDate).getOrElse(assignment.openDate))
        .flatMap(assignment =>
          assignment.findSubmission(student.userId)
            .filter(securityService.can(user, Permissions.Submission.Read, _))
            .map(submission =>
              AssignmentData(
                assignment.module.code.toUpperCase,
                assignment.name,
                Option(assignment.submissionDeadline(submission)).map(_.toString(ProfileExportSingleCommand.TimeFormat)).getOrElse(""),
                submission.submittedDate.toString(ProfileExportSingleCommand.TimeFormat),
                submission.allAttachments,
                assignment.findFeedback(student.userId)
                  .filter(_.released)
                  .filter(securityService.can(user, Permissions.Feedback.Read, _))
                  .map(feedback =>
                    FeedbackData(
                      releasedDate = feedback.releasedDate.toString(ProfileExportSingleCommand.TimeFormat),
                      mark = feedback.latestMark,
                      grade = feedback.latestGrade,
                      comments = feedback.comments,
                      attachments = feedback.attachments.asScala.toSeq,
                      adjustments = viewableAdjustments(feedback).map(mark =>
                        AdjustmentData(
                          mark = mark.mark,
                          grade = mark.grade,
                          reason = mark.reason,
                          comments = mark.comments,
                          date = mark.uploadedDate.toString(ProfileExportSingleCommand.TimeFormat)
                        )
                      )
                    )
                  )
              )
            )
        )
    }

    // Get small groups
    val smallGroupData = benchmarkTask("smallGroupData") {
      getSmallGroupData(academicYear)
    }

    // Get meetings
    val startOfYear = academicYear.firstDay
    val endOfYear = academicYear.lastDay
    val meetingData = benchmarkTask("meetingData") {
      meetingRecordService.list(relationshipService.getAllPastAndPresentRelationships(student).toSet, None)
        .distinct
        .filter(m => !m.meetingDate.isBefore(startOfYear.toDateTimeAtStartOfDay) && m.meetingDate.isBefore(endOfYear.plusDays(1).toDateTimeAtStartOfDay) && m.isApproved)
        .filter(_.relationshipTypes.exists(relationshipType => securityService.can(user, Permissions.Profiles.MeetingRecord.ReadDetails(relationshipType), student)))
        .sortBy(_.meetingDate)
        .map(meeting => MeetingData(
          meeting.relationships.map(_.relationshipType.agentRole.capitalize).distinct.mkString(", "),
          meeting.relationships.map(_.agentName).mkString(", "),
          meeting.meetingDate.toString(ProfileExportSingleCommand.TimeFormat),
          meeting.title,
          meeting.format.description,
          meeting.escapedDescription,
          meeting.attachments.asScala.toSeq
        ))
    }


    // Build model
    val summary = pointData
      .groupBy(_.departmentName).mapValues(_
      .groupBy(_.term).mapValues(_
      .groupBy(_.state).mapValues(_
      .size)))

    val groupedPoints = pointData
      .groupBy(_.departmentName).mapValues(_
      .groupBy(_.state).mapValues(_
      .groupBy(_.term)))

    val courseData = student.freshStudentCourseDetails.map { scd =>
      val scyd = scd.latestStudentCourseYearDetails

      CourseData(
        courseName = Option(scd.course).map(_.name),
        courseCode = Option(scd.course).map(_.code),
        beginYear = Option(scd.beginYear).map(_.formatted("%04d")),
        endYear = Option(scd.endYear).map(_.formatted("%04d")),
        departmentName = Option(scd.department).map(_.name),
        departmentCode = Option(scd.department).map(_.code.toUpperCase),
        degreeType = Option(scd.currentRoute).map(_.degreeType.toString),
        awardName = Option(scd.award).map(_.name),
        modeOfAttendance = Option(scyd.modeOfAttendance).map(_.fullNameAliased),
        statusOnCourse = Option(scd.statusOnCourse).map(_.fullName.toLowerCase.capitalize),
        endDate = Option(scd.endDate).map(_.toString(ProfileExportSingleCommand.DateFormat)),
        expectedEndDate = Option(scd.expectedEndDate).map(_.toString(ProfileExportSingleCommand.DateFormat)),
        routeName = Option(scyd.route).map(_.name),
        routeCode = Option(scyd.route).map(_.code.toUpperCase),
        yearOfStudy = scyd.yearOfStudy.toString,
        hasCurrentEnrolment = scd.hasCurrentEnrolment,
        courseLevelCode = scd.level.map(_.code),
        courseLevelName = scd.level.map(_.name),
        sprCode = scd.sprCode,
        scjCode = scd.scjCode
      )
    }

    // Render PDF and create file
    val pdfFileAttachment = pdfGenerator.renderTemplateAndStore(
      "/WEB-INF/freemarker/reports/profile-export.ftl",
      s"Tabula-${student.universityId}-profile.pdf",
      Map(
        "student" -> student,
        "courseData" -> courseData,
        "academicYear" -> academicYear,
        "user" -> user,
        "summary" -> summary,
        "groupedPoints" -> groupedPoints,
        "assignmentData" -> assignmentData,
        "smallGroupData" -> smallGroupData.groupBy(_.eventId),
        "meetingData" -> meetingData.groupBy(_.relationshipType),
        "administrativeNotesData" -> administrativeNotesData
      )
    )

    // Return results
    Seq(pdfFileAttachment) ++
      pointData.flatMap(_.attendanceNote.flatMap(note => Option(note.attachment))) ++
      smallGroupData.flatMap(_.attendanceNote.flatMap(note => Option(note.attachment))) ++
      assignmentData.flatMap(_.attachments) ++
      assignmentData.flatMap(_.feedback).flatMap(_.attachments) ++
      administrativeNotesData.flatMap(_.attachments) ++
      meetingData.flatMap(_.attachments)
  }

  private def getPointData(academicYear: AcademicYear): Seq[PointData] = {
    val checkpoints = benchmarkTask("attendanceMonitoringService.getAllAttendance") {
      attendanceMonitoringService.getAllAttendanceInAcademicYear(student, academicYear)
    }
    val attendanceNoteMap = benchmarkTask("attendanceMonitoringService.getAttendanceNoteMap") {
      attendanceMonitoringService.getAttendanceNoteMap(student)
    }
    val users = benchmarkTask("userLookup.getUsersByUserIds") {
      userLookup.getUsersByUserIds(checkpoints.map(_.updatedBy).asJava).asScala
    }
    checkpoints.map(checkpoint => {
      PointData(
        checkpoint.point.scheme.department.name,
        checkpoint.point.scheme.academicYear.termOrVacationForDate(checkpoint.point.startDate).periodType.toString,
        checkpoint.state.dbValue,
        checkpoint.point.name,
        checkpoint.point.pointType.description,
        serializePointTypeOptions(checkpoint.point),
        checkpoint.point.startDate.toString(ProfileExportSingleCommand.DateFormat),
        checkpoint.point.endDate.toString(ProfileExportSingleCommand.DateFormat),
        users(checkpoint.updatedBy),
        checkpoint.updatedDate.toString(ProfileExportSingleCommand.TimeFormat),
        attendanceNoteMap.get(checkpoint.point)
      )
    })
  }

  private def serializePointTypeOptions(point: AttendanceMonitoringPoint): String = {
    point.pointType match {
      case AttendanceMonitoringPointType.Standard =>
        "None"
      case AttendanceMonitoringPointType.Meeting =>
        "%s %s with the student's %s".format(
          point.meetingQuantity,
          if (point.meetingFormats.isEmpty)
            "meeting of any format"
          else
            point.meetingFormats.map(_.getDescription).mkString(" or "),
          point.meetingRelationships.map(_.agentRole).mkString(" or ")
        )
      case AttendanceMonitoringPointType.SmallGroup =>
        "Attend %s event%s for %s".format(
          point.smallGroupEventQuantity,
          if (point.smallGroupEventQuantity == 1) "" else "s",
          if (point.smallGroupEventModules.isEmpty)
            "any module"
          else
            point.smallGroupEventModules.map(_.code.toUpperCase).mkString(" or ")
        )
      case AttendanceMonitoringPointType.AssignmentSubmission =>
        point.assignmentSubmissionType match {
          case AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Any =>
            "Submit to %s assignment%s in any module".format(
              point.assignmentSubmissionTypeAnyQuantity,
              if (point.assignmentSubmissionTypeAnyQuantity != 1) "s" else ""
            )
          case AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Modules =>
            "Submit to %s assignment%s in %s".format(
              point.assignmentSubmissionTypeModulesQuantity,
              if (point.assignmentSubmissionTypeModulesQuantity != 1) "s" else "",
              point.assignmentSubmissionModules.map(_.code.toUpperCase).mkString(" or ")
            )
          case AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Assignments =>
            "Submit to %s %s assignment%s: %s".format(
              if (point.assignmentSubmissionIsDisjunction) "any" else "all",
              point.assignmentSubmissionAssignments.size,
              if (point.assignmentSubmissionAssignments.size != 1) "s" else "",
              point.assignmentSubmissionAssignments.map(a => a.module.code.toUpperCase + " " + a.name).mkString(", ")
            )
        }
    }
  }

  private def getSmallGroupData(academicYear: AcademicYear): Seq[SmallGroupData] = {
    val allAttendance = benchmarkTask("smallGroupService.findAllAttendanceForStudentInAcademicYear") {
      smallGroupService.findAllAttendanceForStudentInAcademicYear(student, academicYear)
    }
    val users = benchmarkTask("userLookup.getUsersByUserIds") {
      userLookup.getUsersByUserIds(allAttendance.map(_.updatedBy).asJava).asScala
    }
    val attendanceNotes = smallGroupService.findAttendanceNotes(Seq(student.universityId), allAttendance.map(_.occurrence))

    allAttendance.map(attendance => SmallGroupData(
      attendance.occurrence.event.id,
      Seq(
        Option(attendance.occurrence.event.group.groupSet.module.code.toUpperCase),
        Option(attendance.occurrence.event.group.groupSet.name),
        Option(attendance.occurrence.event.group.name),
        Option(attendance.occurrence.event.title)
      ).flatten.mkString(", "),
      Option(attendance.occurrence.event.day).map(_.name).getOrElse(""),
      Option(attendance.occurrence.event.location).map(_.name).getOrElse(""),
      attendance.occurrence.event.tutors.users.map(_.getFullName).mkString(", "),
      attendance.occurrence.week,
      attendance.state.description,
      users(attendance.updatedBy),
      attendance.updatedDate.toString(ProfileExportSingleCommand.TimeFormat),
      attendanceNotes.find(_.occurrence == attendance.occurrence)
    ))
  }

}

trait ProfileExportSinglePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: ProfileExportSingleCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheckAny(Seq(
      CheckablePermission(Permissions.Department.Reports, student),
      CheckablePermission(Permissions.Profiles.Read.Core, student)
    ))
  }

}

trait ProfileExportSingleDescription extends Describable[Seq[FileAttachment]] {

  self: ProfileExportSingleCommandState =>

  override lazy val eventName = "ProfileExportSingle"

  override def describe(d: Description): Unit = {
    d.studentIds(Seq(student.universityId))
  }
}

trait ProfileExportSingleCommandState {
  def student: StudentMember

  def academicYear: AcademicYear
}
