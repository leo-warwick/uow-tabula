package uk.ac.warwick.tabula.attendance.commands.manage

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceMonitoringPoint, AttendanceMonitoringPointType}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{Assignment, Module, MeetingFormat, StudentRelationshipType}
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.services.TermServiceComponent
import collection.JavaConverters._

trait AttendancePointCommandState {

	self: TermServiceComponent =>

	def pointStyle: AttendanceMonitoringPointStyle

	// Bind variables

	// The point's properties
	var name: String = _
	var startWeek: Int = 0
	var endWeek: Int = 0
	var startDate: LocalDate = _
	var endDate: LocalDate = _

	var pointType: AttendanceMonitoringPointType = _

	var meetingRelationships: JSet[StudentRelationshipType] = JHashSet()
	var meetingFormats: JSet[MeetingFormat] = JHashSet()
	meetingFormats.addAll(MeetingFormat.members.asJava)
	var meetingQuantity: Int = 1

	var smallGroupEventQuantity: JInteger = 1
	var smallGroupEventQuantityAll: Boolean = false
	var smallGroupEventModules: JSet[Module] = JHashSet()
	var isAnySmallGroupEventModules: Boolean = true

	var isSpecificAssignments: Boolean = true
	var assignmentSubmissionQuantity: JInteger = 1
	var assignmentSubmissionModules: JSet[Module] = JHashSet()
	var assignmentSubmissionAssignments: JSet[Assignment] = JHashSet()
	var isAssignmentSubmissionDisjunction: Boolean = false

	def copyTo(point: AttendanceMonitoringPoint): AttendanceMonitoringPoint = {
		point.name = name
		pointStyle match {
			case AttendanceMonitoringPointStyle.Date =>
				point.startDate = startDate
				point.endDate = endDate
			case AttendanceMonitoringPointStyle.Week =>
				val weeksForYear = termService.getAcademicWeeksForYear(point.scheme.academicYear.dateInTermOne).toMap
				point.startWeek = startWeek
				point.endWeek = endWeek
				point.startDate = weeksForYear(startWeek).getStart.withDayOfWeek(DayOfWeek.Monday.jodaDayOfWeek).toLocalDate
				point.endDate = weeksForYear(endWeek).getStart.withDayOfWeek(DayOfWeek.Monday.jodaDayOfWeek).toLocalDate
		}
		point.pointType = pointType
		pointType match {
			case AttendanceMonitoringPointType.Meeting =>
				point.meetingRelationships = meetingRelationships.asScala.toSeq
				point.meetingFormats = meetingFormats.asScala.toSeq
				point.meetingQuantity = meetingQuantity
			case AttendanceMonitoringPointType.SmallGroup =>
				point.smallGroupEventQuantity = smallGroupEventQuantityAll match {
					case true => 0
					case _ => smallGroupEventQuantity.toInt
				}
				point.smallGroupEventModules = isAnySmallGroupEventModules match {
					case true => Seq()
					case false => smallGroupEventModules match {
						case modules: JSet[Module] => modules.asScala.toSeq
						case _ => Seq()
					}
				}
			case AttendanceMonitoringPointType.AssignmentSubmission =>
				point.assignmentSubmissionIsSpecificAssignments = isSpecificAssignments
				point.assignmentSubmissionQuantity = assignmentSubmissionQuantity.toInt
				point.assignmentSubmissionModules = assignmentSubmissionModules match {
					case modules: JSet[Module] => modules.asScala.toSeq
					case _ => Seq()
				}
				point.assignmentSubmissionAssignments = assignmentSubmissionAssignments match {
					case assignments: JSet[Assignment] => assignments.asScala.toSeq
					case _ => Seq()
				}
				point.assignmentSubmissionIsDisjunction = isAssignmentSubmissionDisjunction
			case _ =>
		}
		point
	}

	def copyFrom(point: AttendanceMonitoringPoint): AttendanceMonitoringPoint = {
		name = point.name
		pointStyle match {
			case AttendanceMonitoringPointStyle.Date =>
				startDate = point.startDate
				endDate = point.endDate
			case AttendanceMonitoringPointStyle.Week =>
				startWeek = point.startWeek
				endWeek = point.endWeek
		}
		pointType = point.pointType
		pointType match {
			case AttendanceMonitoringPointType.Meeting =>
				meetingRelationships.clear()
				meetingRelationships.addAll(point.meetingRelationships.asJava)
				meetingFormats.clear()
				meetingFormats.addAll(point.meetingFormats.asJava)
				meetingQuantity = point.meetingQuantity
			case AttendanceMonitoringPointType.SmallGroup =>
				smallGroupEventModules.clear()
				smallGroupEventModules.addAll(point.smallGroupEventModules.asJava)
				smallGroupEventQuantity = point.smallGroupEventQuantity
				smallGroupEventQuantityAll = point.smallGroupEventQuantity == 0
				isAnySmallGroupEventModules = point.smallGroupEventModules.size == 0
			case AttendanceMonitoringPointType.AssignmentSubmission =>
				isSpecificAssignments = point.assignmentSubmissionIsSpecificAssignments
				assignmentSubmissionQuantity = point.assignmentSubmissionQuantity
				assignmentSubmissionModules.clear()
				assignmentSubmissionModules.addAll(point.assignmentSubmissionModules.asJava)
				assignmentSubmissionAssignments.clear()
				assignmentSubmissionAssignments.addAll(point.assignmentSubmissionAssignments.asJava)
				isAssignmentSubmissionDisjunction = point.assignmentSubmissionIsDisjunction
			case _ =>
		}
		point
	}

}
