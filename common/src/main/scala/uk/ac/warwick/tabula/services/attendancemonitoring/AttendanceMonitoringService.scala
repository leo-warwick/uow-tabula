package uk.ac.warwick.tabula.services.attendancemonitoring

import org.joda.time.{DateTime, DateTimeConstants, LocalDate}
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.services.UserLookupService.UniversityId
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.queue.Queue

import scala.collection.JavaConverters._

trait AttendanceMonitoringServiceComponent {
	def attendanceMonitoringService: AttendanceMonitoringService
}

trait AutowiringAttendanceMonitoringServiceComponent extends AttendanceMonitoringServiceComponent {
	val attendanceMonitoringService: AttendanceMonitoringService = Wire[AttendanceMonitoringService]
}

trait AttendanceMonitoringService {
	def getSchemeById(id: String): Option[AttendanceMonitoringScheme]
	def getPointById(id: String): Option[AttendanceMonitoringPoint]
	def saveOrUpdate(scheme: AttendanceMonitoringScheme): Unit
	def saveOrUpdate(point: AttendanceMonitoringPoint): Unit
	def saveOrUpdate(template: AttendanceMonitoringTemplate): Unit
	def saveOrUpdate(templatePoint: AttendanceMonitoringTemplatePoint): Unit
	def saveOrUpdate(note: AttendanceMonitoringNote): Unit
	def saveOrUpdate(report: MonitoringPointReport): Unit
	def saveOrUpdateDangerously(checkpoint: AttendanceMonitoringCheckpoint): Unit
	def updateCheckpointTotalLow(total: AttendanceMonitoringCheckpointTotal): Unit
	def updateCheckpointTotalMedium(total: AttendanceMonitoringCheckpointTotal): Unit
	def updateCheckpointTotalHigh(total: AttendanceMonitoringCheckpointTotal): Unit
	def deleteScheme(scheme: AttendanceMonitoringScheme)
	def deletePoint(point: AttendanceMonitoringPoint)
	def deleteTemplate(template: AttendanceMonitoringTemplate)
	def deleteTemplatePoint(point: AttendanceMonitoringTemplatePoint)
	def getTemplateSchemeById(id: String): Option[AttendanceMonitoringTemplate]
	def getTemplatePointById(id: String): Option[AttendanceMonitoringTemplatePoint]
	def listAllSchemes(department: Department): Seq[AttendanceMonitoringScheme]
	def listSchemes(department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringScheme]
	def listAllTemplateSchemes: Seq[AttendanceMonitoringTemplate]
	def listTemplateSchemesByStyle(style: AttendanceMonitoringPointStyle): Seq[AttendanceMonitoringTemplate]
	def listSchemesForMembershipUpdate: Seq[AttendanceMonitoringScheme]
	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String]
	def studentAlreadyReportedThisTerm(student: StudentMember, point: AttendanceMonitoringPoint): Boolean
	def findReports(studentIds: Seq[String], year: AcademicYear, period: String): Seq[MonitoringPointReport]
	def listUnreportedReports: Seq[MonitoringPointReport]
	def markReportAsPushed(report: MonitoringPointReport): Unit
	def findSchemeMembershipItems(universityIds: Seq[String], itemType: SchemeMembershipItemType, academicYear: AcademicYear): Seq[SchemeMembershipItem]
	def findPoints(
		department: Department,
		academicYear: AcademicYear,
		schemes: Seq[AttendanceMonitoringScheme],
		types: Seq[AttendanceMonitoringPointType],
		styles: Seq[AttendanceMonitoringPointStyle]
	): Seq[AttendanceMonitoringPoint]
	def listStudentsPoints(student: StudentMember, departmentOption: Option[Department], academicYear: AcademicYear): Seq[AttendanceMonitoringPoint]
	def listStudentsPoints(studentData: AttendanceMonitoringStudentData, department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringPoint]
	def listStudentsPointsForDate(student: StudentMember, departmentOption: Option[Department], date: LocalDate): Seq[AttendanceMonitoringPoint]
	def listStudentsPointsForDate(student: StudentMember, departmentOption: Option[Department], date: DateTime): Seq[AttendanceMonitoringPoint]
	def getAllCheckpoints(point: AttendanceMonitoringPoint): Seq[AttendanceMonitoringCheckpoint]
	def getAllCheckpointData(points: Seq[AttendanceMonitoringPoint]): Seq[AttendanceMonitoringCheckpointData]
	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], student: StudentMember, withFlush: Boolean = false): Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint]
	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], students: Seq[StudentMember]): Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint]]
	def countCheckpointsForPoint(point: AttendanceMonitoringPoint): Int
	def getNonActiveCheckpoints(
		student: StudentMember,
		departmentOption: Option[Department],
		academicYear: AcademicYear,
		activeCheckpoints: Seq[AttendanceMonitoringCheckpoint]
	): Seq[AttendanceMonitoringCheckpoint]
	def hasRecordedCheckpoints(points: Seq[AttendanceMonitoringPoint]): Boolean
	def getAllAttendance(studentId: String): Seq[AttendanceMonitoringCheckpoint]
	def getAllAttendanceInAcademicYear(student: StudentMember, academicYear: AcademicYear): Seq[AttendanceMonitoringCheckpoint]
	def getAttendanceNote(student: StudentMember, point: AttendanceMonitoringPoint): Option[AttendanceMonitoringNote]
	def getAttendanceNoteMap(student: StudentMember): Map[AttendanceMonitoringPoint, AttendanceMonitoringNote]
	def setAttendance(
		student: StudentMember,
		attendanceMap: Map[AttendanceMonitoringPoint, AttendanceState],
		user: CurrentUser
	): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])
	def setAttendance(
		student: StudentMember,
		attendanceMap: Map[AttendanceMonitoringPoint, AttendanceState],
		usercode: String,
		autocreated: Boolean = false
	): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])
	def updateCheckpointTotal(student: StudentMember, department: Department, academicYear: AcademicYear): AttendanceMonitoringCheckpointTotal
	def getCheckpointTotal(student: StudentMember, departmentOption: Option[Department], academicYear: AcademicYear): AttendanceMonitoringCheckpointTotal
	def generatePointsFromTemplateScheme(templateScheme: AttendanceMonitoringTemplate, academicYear: AcademicYear): Seq[AttendanceMonitoringPoint]
	def findUnrecordedPoints(department: Department, academicYear: AcademicYear, endDate: LocalDate): Seq[AttendanceMonitoringPoint]
	def findUnrecordedStudents(department: Department, academicYear: AcademicYear, endDate: LocalDate): Seq[AttendanceMonitoringStudentData]
	def findSchemesLinkedToSITSByDepartment(academicYear: AcademicYear): Map[Department, Seq[AttendanceMonitoringScheme]]
	def setCheckpointTotalsForUpdate(students: Seq[StudentMember], department: Department, academicYear: AcademicYear): Unit
	def listCheckpointTotalsForUpdate: Seq[AttendanceMonitoringCheckpointTotal]
	def getAttendanceMonitoringDataForStudents(universityIds: Seq[String], academicYear: AcademicYear): Seq[AttendanceMonitoringStudentData]
}

abstract class AbstractAttendanceMonitoringService extends AttendanceMonitoringService with TaskBenchmarking {

	self: AttendanceMonitoringDaoComponent with AttendanceMonitoringMembershipHelpers with UserLookupComponent =>

	var queue: Queue = Wire.named[Queue]("settingsSyncTopic")

	def getSchemeById(id: String): Option[AttendanceMonitoringScheme] =
		attendanceMonitoringDao.getSchemeById(id)

	def getPointById(id: String): Option[AttendanceMonitoringPoint] =
		attendanceMonitoringDao.getPointById(id)

	def saveOrUpdate(scheme: AttendanceMonitoringScheme): Unit =
		attendanceMonitoringDao.saveOrUpdate(scheme)

	def saveOrUpdate(point: AttendanceMonitoringPoint): Unit =
		attendanceMonitoringDao.saveOrUpdate(point)

	def saveOrUpdate(template: AttendanceMonitoringTemplate): Unit =
		attendanceMonitoringDao.saveOrUpdate(template)

	def saveOrUpdate(templatePoint: AttendanceMonitoringTemplatePoint): Unit =
		attendanceMonitoringDao.saveOrUpdate(templatePoint)

	def saveOrUpdate(note: AttendanceMonitoringNote): Unit =
		attendanceMonitoringDao.saveOrUpdate(note)

	def saveOrUpdate(report: MonitoringPointReport): Unit =
		attendanceMonitoringDao.saveOrUpdate(report)

	def saveOrUpdateDangerously(checkpoint: AttendanceMonitoringCheckpoint): Unit =
		attendanceMonitoringDao.saveOrUpdateCheckpoints(Seq(checkpoint))

	def updateCheckpointTotalLow(total: AttendanceMonitoringCheckpointTotal): Unit = {
		total.unauthorisedLowLevelNotified = DateTime.now
		attendanceMonitoringDao.saveOrUpdate(total)
	}
	def updateCheckpointTotalMedium(total: AttendanceMonitoringCheckpointTotal): Unit = {
		total.unauthorisedMediumLevelNotified = DateTime.now
		attendanceMonitoringDao.saveOrUpdate(total)
	}

	def updateCheckpointTotalHigh(total: AttendanceMonitoringCheckpointTotal): Unit = {
		total.unauthorisedHighLevelNotified = DateTime.now
		attendanceMonitoringDao.saveOrUpdate(total)
	}

	def deleteScheme(scheme: AttendanceMonitoringScheme): Unit =
		attendanceMonitoringDao.delete(scheme)

	def deletePoint(point: AttendanceMonitoringPoint): Unit =
		attendanceMonitoringDao.delete(point)

	def deleteTemplate(template: AttendanceMonitoringTemplate): Unit =
		attendanceMonitoringDao.delete(template)

	def deleteTemplatePoint(point: AttendanceMonitoringTemplatePoint): Unit =
		attendanceMonitoringDao.delete(point)

	def getTemplateSchemeById(id: String): Option[AttendanceMonitoringTemplate] =
		attendanceMonitoringDao.getTemplateSchemeById(id)

	def getTemplatePointById(id: String): Option[AttendanceMonitoringTemplatePoint] =
		attendanceMonitoringDao.getTemplatePointById(id)
	def listAllSchemes(department: Department): Seq[AttendanceMonitoringScheme] =
		attendanceMonitoringDao.listAllSchemes(department)

	def listSchemes(department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringScheme] =
		attendanceMonitoringDao.listSchemes(department, academicYear)

	def listSchemesForMembershipUpdate: Seq[AttendanceMonitoringScheme] =
		attendanceMonitoringDao.listSchemesForMembershipUpdate

	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String] =
		attendanceMonitoringDao.findNonReportedTerms(students, academicYear)

	def studentAlreadyReportedThisTerm(student: StudentMember, point: AttendanceMonitoringPoint): Boolean =
		!findNonReportedTerms(Seq(student), point.scheme.academicYear).contains(
			point.scheme.academicYear.termOrVacationForDate(point.startDate).periodType.toString
		)

	def findReports(studentIds: Seq[String], year: AcademicYear, period: String): Seq[MonitoringPointReport] =
		attendanceMonitoringDao.findReports(studentIds, year, period)

	def listUnreportedReports: Seq[MonitoringPointReport] = {
		attendanceMonitoringDao.listUnreportedReports
	}

	def markReportAsPushed(report: MonitoringPointReport): Unit = {
		report.pushedDate = DateTime.now
		saveOrUpdate(report)
	}

	def findSchemeMembershipItems(universityIds: Seq[String], itemType: SchemeMembershipItemType, academicYear: AcademicYear): Seq[SchemeMembershipItem] = {
		val items = attendanceMonitoringDao.findSchemeMembershipItems(universityIds, itemType)
		items.map{ item => {
			val user = userLookup.getUserByWarwickUniId(item.universityId)
			SchemeMembershipItem(
				item.itemType,
				item.firstName,
				item.lastName,
				item.universityId,
				item.userId,
				membersHelper.findBy(user).filter(_.academicYear == academicYear)
			)
		}}
	}

	def findPoints(
		department: Department,
		academicYear: AcademicYear,
		schemes: Seq[AttendanceMonitoringScheme],
		types: Seq[AttendanceMonitoringPointType],
		styles: Seq[AttendanceMonitoringPointStyle]
	): Seq[AttendanceMonitoringPoint] = {
		attendanceMonitoringDao.findPoints(department, academicYear, schemes, types, styles)
	}

	def listAllTemplateSchemes: Seq[AttendanceMonitoringTemplate] = {
		attendanceMonitoringDao.listAllTemplateSchemes
	}

	def listTemplateSchemesByStyle(style: AttendanceMonitoringPointStyle): Seq[AttendanceMonitoringTemplate] = {
		attendanceMonitoringDao.listTemplateSchemesByStyle(style)
	}

	def listStudentsPoints(student: StudentMember, departmentOption: Option[Department], academicYear: AcademicYear): Seq[AttendanceMonitoringPoint] = {
		val validCourses = student.freshStudentCourseDetails.filter(_.freshStudentCourseYearDetails.exists(_.academicYear == academicYear))
		val beginDates = validCourses.map(_.beginDate)
		val endDates = validCourses.map(c => Option(c.endDate))
		if (beginDates.nonEmpty) {
			import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
			val beginDate = beginDates.min
			val endDate = {
				if (endDates.exists(_.isEmpty)) {
					None
				} else {
					Option(endDates.flatten.max)
				}
			}
			val schemes = findSchemesForStudent(student.universityId, student.userId, departmentOption, academicYear)
			schemes.flatMap(_.points.asScala).filter(p =>
				p.applies(beginDate, endDate)
			)
		} else {
			Seq()
		}
	}

	def listStudentsPoints(studentData: AttendanceMonitoringStudentData, department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringPoint] = {
		val schemes = findSchemesForStudent(studentData.universityId, studentData.userId, Option(department), academicYear)
		schemes.flatMap(_.points.asScala).filter(p =>
			p.applies(studentData.scdBeginDate, studentData.scdEndDate)
		)
	}

	def listStudentsPointsForDate(student: StudentMember, departmentOption: Option[Department], date: LocalDate): Seq[AttendanceMonitoringPoint] = {
		AcademicYear.allForDate(date).flatMap { academicYear =>
			listStudentsPoints(student, departmentOption, academicYear)
		}
	}

	def listStudentsPointsForDate(student: StudentMember, departmentOption: Option[Department], date: DateTime): Seq[AttendanceMonitoringPoint] = {
		listStudentsPointsForDate(student, departmentOption, date.toLocalDate)
	}

	private def findSchemesForStudent(universityId: String, userId: String, departmentOption: Option[Department], academicYear: AcademicYear): Seq[AttendanceMonitoringScheme] = {
		val user = new User(userId)
		user.setWarwickId(universityId)
		val schemes = benchmarkTask(s"membersHelper.findBy $universityId") {
			membersHelper.findBy(user)
		}
		departmentOption match {
			case Some(department) => schemes.filter(s => s.department == department && s.academicYear == academicYear)
			case None => schemes.filter(_.academicYear == academicYear)
		}
	}

	def getAllCheckpoints(point: AttendanceMonitoringPoint): Seq[AttendanceMonitoringCheckpoint] = {
		attendanceMonitoringDao.getAllCheckpoints(point)
	}

	def getAllCheckpointData(points: Seq[AttendanceMonitoringPoint]): Seq[AttendanceMonitoringCheckpointData] = {
		attendanceMonitoringDao.getAllCheckpointData(points)
	}

	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], student: StudentMember, withFlush: Boolean = false): Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint] = {
		attendanceMonitoringDao.getCheckpoints(points, student, withFlush)
	}

	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], students: Seq[StudentMember]): Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint]] =
		attendanceMonitoringDao.getCheckpoints(points, students)

	def countCheckpointsForPoint(point: AttendanceMonitoringPoint): Int =
		attendanceMonitoringDao.countCheckpointsForPoint(point)

	def getNonActiveCheckpoints(
		student: StudentMember,
		departmentOption: Option[Department],
		academicYear: AcademicYear,
		activeCheckpoints: Seq[AttendanceMonitoringCheckpoint]
	): Seq[AttendanceMonitoringCheckpoint] =
		attendanceMonitoringDao.getNonActiveCheckpoints(student, departmentOption, academicYear, activeCheckpoints)

	def hasRecordedCheckpoints(points: Seq[AttendanceMonitoringPoint]): Boolean =
		attendanceMonitoringDao.hasRecordedCheckpoints(points)

	def getAllAttendance(studentId: String): Seq[AttendanceMonitoringCheckpoint] =
		attendanceMonitoringDao.getAllAttendance(studentId)

	override def getAllAttendanceInAcademicYear(student: StudentMember, academicYear: AcademicYear): Seq[AttendanceMonitoringCheckpoint] =
		attendanceMonitoringDao.getAllAttendanceInAcademicYear(student, academicYear)

	def getAttendanceNote(student: StudentMember, point: AttendanceMonitoringPoint): Option[AttendanceMonitoringNote] = {
		attendanceMonitoringDao.getAttendanceNote(student, point)
	}

	def getAttendanceNoteMap(student: StudentMember): Map[AttendanceMonitoringPoint, AttendanceMonitoringNote] = {
		attendanceMonitoringDao.getAttendanceNoteMap(student)
	}

	def setAttendance(
		student: StudentMember,
		attendanceMap: Map[AttendanceMonitoringPoint, AttendanceState],
		user: CurrentUser
	): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal]) = {
		setAttendance(student, attendanceMap, user.apparentId)
	}

	def setAttendance(
		student: StudentMember,
		attendanceMap: Map[AttendanceMonitoringPoint, AttendanceState],
		usercode: String,
		autocreated: Boolean = false
	): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal]) = {
		val existingCheckpoints = getCheckpoints(attendanceMap.keys.toSeq, student)
		val checkpointsToDelete: Seq[AttendanceMonitoringCheckpoint] = attendanceMap.filter(_._2 == null).keys.flatMap(existingCheckpoints.get).toSeq
		val checkpointsToUpdate: Seq[AttendanceMonitoringCheckpoint] = attendanceMap.filter(_._2 != null).flatMap{case(point, state) =>
			val checkpoint = existingCheckpoints.getOrElse(point, {
				val checkpoint = new AttendanceMonitoringCheckpoint
				checkpoint.student = student
				checkpoint.point = point
				checkpoint.autoCreated = autocreated
				checkpoint
			})
			if (checkpoint.state != state) {
				checkpoint.state = state
				checkpoint.updatedBy = usercode
				checkpoint.updatedDate = DateTime.now
				Option(checkpoint)
			} else {
				None
			}
		}.toSeq
		attendanceMonitoringDao.removeCheckpoints(checkpointsToDelete)
		attendanceMonitoringDao.saveOrUpdateCheckpoints(checkpointsToUpdate)

		val totals = attendanceMap.keys.toSeq.map(point => (point.scheme.department, point.scheme.academicYear)).distinct.map { case (department, academicYear) =>
			updateCheckpointTotal(student, department, academicYear)
		}

		(checkpointsToUpdate, totals)
	}

	def updateCheckpointTotal(student: StudentMember, department: Department, academicYear: AcademicYear): AttendanceMonitoringCheckpointTotal = {
		val points = benchmarkTask("listStudentsPoints") {
			listStudentsPoints(student, Option(department), academicYear)
		}
		val checkpointMap = getCheckpoints(points, student, withFlush = true)
		val allCheckpoints = checkpointMap.values

		val unrecorded = points.diff(checkpointMap.keys.toSeq).count(_.endDate.isBefore(DateTime.now.toLocalDate))
		val missedUnauthorised = allCheckpoints.count(_.state == AttendanceState.MissedUnauthorised)
		val missedAuthorised = allCheckpoints.count(_.state == AttendanceState.MissedAuthorised)
		val attended = allCheckpoints.count(_.state == AttendanceState.Attended)

		val totals = attendanceMonitoringDao.getCheckpointTotal(student, Option(department), academicYear).getOrElse {
			val total = new AttendanceMonitoringCheckpointTotal
			total.student = student
			total.department = department
			total.academicYear = academicYear
			total
		}

		totals.unrecorded = unrecorded
		totals.unauthorised = missedUnauthorised
		totals.authorised = missedAuthorised
		totals.attended = attended
		totals.updatedDate = DateTime.now
		attendanceMonitoringDao.saveOrUpdate(totals)
		totals
	}

	def getCheckpointTotal(student: StudentMember, departmentOption: Option[Department], academicYear: AcademicYear): AttendanceMonitoringCheckpointTotal = {
		attendanceMonitoringDao.getCheckpointTotal(student, departmentOption, academicYear).getOrElse {
			val total = new AttendanceMonitoringCheckpointTotal
			total.student = student
			total.department = departmentOption.orNull
			total.academicYear = academicYear
			total
		}
	}

	def generatePointsFromTemplateScheme(templateScheme: AttendanceMonitoringTemplate, academicYear: AcademicYear): Seq[AttendanceMonitoringPoint] = {
		val weeksForYear = academicYear.weeks
		val stubScheme = new AttendanceMonitoringScheme
		stubScheme.pointStyle = templateScheme.pointStyle
		stubScheme.academicYear = academicYear

		val attendanceMonitoringPoints =
			templateScheme.points.asScala.map { templatePoint =>
				val point = templatePoint.toPoint
				templateScheme.pointStyle match {
					case AttendanceMonitoringPointStyle.Date =>
						def getYear(templateDate: LocalDate) =
							if (templateDate.getMonthOfYear >= DateTimeConstants.AUGUST) academicYear.startYear
							else academicYear.startYear + 1

						point.startDate = templatePoint.startDate.withYear(getYear(templatePoint.startDate))
						point.endDate = templatePoint.endDate.withYear(getYear(templatePoint.endDate))
					case AttendanceMonitoringPointStyle.Week =>
						point.startWeek = templatePoint.startWeek
						point.endWeek = templatePoint.endWeek
						point.startDate = weeksForYear(templatePoint.startWeek).firstDay
						point.endDate = weeksForYear(templatePoint.endWeek).lastDay
				}
				point.scheme = stubScheme
				point
			}
		attendanceMonitoringPoints
	}

	private def getApplicableStudentsForPoints(points: Seq[AttendanceMonitoringPoint], academicYear: AcademicYear): Map[AttendanceMonitoringPoint, Seq[AttendanceMonitoringStudentData]] = {
		if (points.isEmpty) {
			Map()
		} else {
			// Fetch student details for every student linked to one of the related points
			val allStudents: Map[UniversityId, AttendanceMonitoringStudentData] =
				attendanceMonitoringDao.getAttendanceMonitoringDataForStudents(
					points.flatMap { _.scheme.members.members }.distinct,
					academicYear
				).map { data => data.universityId -> data }.toMap

			// Map schemes to student lists
			val studentsForScheme: Map[AttendanceMonitoringScheme, Seq[AttendanceMonitoringStudentData]] =
				points.map { _.scheme }.distinct.map { scheme =>
					scheme -> scheme.members.members.filter(allStudents.contains).map(allStudents.apply)
				}.toMap

			points.map { point =>
				val students = studentsForScheme(point.scheme)

				point -> students.filter { student => point.applies(student.scdBeginDate, student.scdEndDate) }
			}.toMap
		}
	}

	def findUnrecordedPoints(department: Department, academicYear: AcademicYear, endDate: LocalDate): Seq[AttendanceMonitoringPoint] = {
		val relevantPoints = attendanceMonitoringDao.findRelevantPoints(department, academicYear, endDate)

		if (relevantPoints.isEmpty) {
			Seq()
		} else {
			val applicableStudents = getApplicableStudentsForPoints(relevantPoints, academicYear)
			val checkpointCounts: Map[AttendanceMonitoringPoint, Int] = attendanceMonitoringDao.countCheckpointsForPoints(relevantPoints)

			relevantPoints.filter { point =>
				// every student that should have a checkpoint for this point
				val students = applicableStudents(point).size

				checkpointCounts(point) < students
			}
		}
	}

	def findUnrecordedStudents(department: Department, academicYear: AcademicYear, endDate: LocalDate): Seq[AttendanceMonitoringStudentData] = {
		val relevantPoints = attendanceMonitoringDao.findRelevantPoints(department, academicYear, endDate)

		if (relevantPoints.isEmpty) {
			Seq()
		} else {
			val applicableStudents = getApplicableStudentsForPoints(relevantPoints, academicYear)
			val checkpointsByPoint = attendanceMonitoringDao.getAllCheckpoints(relevantPoints)

			relevantPoints.filterNot { _.scheme.members.isEmpty }.flatMap(point => {
				// every student that should have a checkpoint for this point
				val students = applicableStudents(point)

				// filter to users that don't have a checkpoint for this point
				students.filter { student =>
					!checkpointsByPoint(point).exists(_.student.universityId == student.universityId)
				}
			}).distinct
		}
	}

	def findSchemesLinkedToSITSByDepartment(academicYear: AcademicYear): Map[Department, Seq[AttendanceMonitoringScheme]] =
		attendanceMonitoringDao.findSchemesLinkedToSITSByDepartment(academicYear)

	def setCheckpointTotalsForUpdate(students: Seq[StudentMember], department: Department, academicYear: AcademicYear): Unit = {
		attendanceMonitoringDao.setCheckpointTotalsForUpdate(students, department, academicYear)
	}

	def listCheckpointTotalsForUpdate: Seq[AttendanceMonitoringCheckpointTotal] = {
		attendanceMonitoringDao.listCheckpointTotalsForUpdate
	}

	def getAttendanceMonitoringDataForStudents(universityIds: Seq[String], academicYear: AcademicYear): Seq[AttendanceMonitoringStudentData] = {
		attendanceMonitoringDao.getAttendanceMonitoringDataForStudents(universityIds, academicYear)
	}

}

trait AttendanceMonitoringMembershipHelpers {
	val membersHelper: UserGroupMembershipHelper[AttendanceMonitoringScheme]
}

// new up UGMHs which will Wire.auto() their dependencies
trait AttendanceMonitoringMembershipHelpersImpl extends AttendanceMonitoringMembershipHelpers {
	val membersHelper = new UserGroupMembershipHelper[AttendanceMonitoringScheme]("_members")
}

@Service("attendanceMonitoringService")
class AttendanceMonitoringServiceImpl
	extends AbstractAttendanceMonitoringService
	with AttendanceMonitoringMembershipHelpersImpl
	with AutowiringAttendanceMonitoringDaoComponent
	with AutowiringUserLookupComponent