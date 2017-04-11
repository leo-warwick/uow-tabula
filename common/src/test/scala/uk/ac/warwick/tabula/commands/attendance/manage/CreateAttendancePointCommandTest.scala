package uk.ac.warwick.tabula.commands.attendance.manage

import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import org.springframework.validation.BindException
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.util.termdates.{Term, TermImpl}

import scala.collection.mutable
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.JavaImports.JHashSet
import uk.ac.warwick.util.termdates.Term.TermType
import uk.ac.warwick.tabula.data.model.attendance._

import collection.JavaConverters._

class CreateAttendancePointCommandTest extends TestBase with Mockito {

	trait Fixture {
		val thisDepartment: Department = Fixtures.department("its")
		val thisAcademicYear = AcademicYear(2014)
		val thisTermService: TermService = smartMock[TermService]
		val scheme = new AttendanceMonitoringScheme
		scheme.department = thisDepartment
		scheme.academicYear = thisAcademicYear
		scheme.pointStyle = AttendanceMonitoringPointStyle.Date
		val student: StudentMember = Fixtures.student("1234")
		scheme.members.addUserId(student.universityId)
		val command = new CreateAttendancePointCommandInternal(thisDepartment, thisAcademicYear, Seq(scheme))
			with CreateAttendancePointCommandState
			with TermServiceComponent
			with SmallGroupServiceComponent
			with ModuleAndDepartmentServiceComponent
			with AttendanceMonitoringServiceComponent
			with ProfileServiceComponent {

			val termService: TermService = thisTermService
			val smallGroupService = null
			val moduleAndDepartmentService = null
			val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
			val profileService: ProfileService = smartMock[ProfileService]
			thisScheduledNotificationService = smartMock[ScheduledNotificationService]
		}
		val validator = new AttendanceMonitoringPointValidation with TermServiceComponent with AttendanceMonitoringServiceComponent {
			val termService: TermService = thisTermService
			val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
		}
		val errors = new BindException(command, "command")
	}

	@Test
	def validateName() { new Fixture {
		validator.validateName(errors, "Name")
		errors.hasFieldErrors("name") should be {false}
		validator.validateName(errors, "")
		errors.hasFieldErrors("name") should be {true}
	}}

	@Test
	def validateWeek() { new Fixture {
		validator.validateWeek(errors, 1, "startWeek")
		errors.hasFieldErrors("startWeek") should be {false}
		validator.validateWeek(errors, 53, "startWeek")
		errors.hasFieldErrors("startWeek") should be {true}
	}}

	@Test
	def validateWeeks() { new Fixture {
		validator.validateWeeks(errors, 2, 3)
		errors.hasFieldErrors("startWeek") should be {false}
		validator.validateWeeks(errors, 2, 1)
		errors.hasFieldErrors("startWeek") should be {true}
	}}

	@Test
	def validateDate() {
		new Fixture {
			validator.validateDate(errors, null, null, "startDate")
			errors.hasFieldErrors("startDate") should be {true}
		}
		new Fixture {
			val date: LocalDate = new DateTime().withYear(2013).toLocalDate
			validator.termService.getAcademicWeekForAcademicYear(date.toDateTimeAtStartOfDay, AcademicYear(2014)) returns Term.WEEK_NUMBER_BEFORE_START
			validator.validateDate(errors, date, AcademicYear(2014), "startDate")
			errors.hasFieldErrors("startDate") should be {true}
		}
		new Fixture {
			val date: LocalDate = new DateTime().withYear(2016).toLocalDate
			validator.termService.getAcademicWeekForAcademicYear(date.toDateTimeAtStartOfDay, AcademicYear(2014)) returns Term.WEEK_NUMBER_AFTER_END
			validator.validateDate(errors, date, AcademicYear(2014), "startDate")
			errors.hasFieldErrors("startDate") should be {true}
		}
		new Fixture {
			val date: LocalDate = new DateTime().withYear(2015).toLocalDate
			validator.termService.getAcademicWeekForAcademicYear(date.toDateTimeAtStartOfDay, AcademicYear(2014)) returns 10
			validator.validateDate(errors, date, AcademicYear(2014), "startDate")
			errors.hasFieldErrors("startDate") should be {false}
		}
	}

	@Test
	def validateDates() { new Fixture {
		validator.validateDates(errors, new DateTime().toLocalDate, new DateTime().toLocalDate)
		errors.hasFieldErrors("startDate") should be {false}
		validator.validateDates(errors, new DateTime().toLocalDate, new DateTime().toLocalDate.plusDays(1))
		errors.hasFieldErrors("startDate") should be {false}
		validator.validateDates(errors, new DateTime().toLocalDate, new DateTime().toLocalDate.minusDays(1))
		errors.hasFieldErrors("startDate") should be {true}
	}}

	@Test
	def validateTypeForEndDate() { new Fixture {
		validator.validateTypeForEndDate(errors, AttendanceMonitoringPointType.Standard, new DateTime().toLocalDate.minusDays(1))
		errors.hasFieldErrors("pointType") should be {false}
		validator.validateTypeForEndDate(errors, AttendanceMonitoringPointType.Meeting, new DateTime().toLocalDate)
		errors.hasFieldErrors("pointType") should be {false}
		validator.validateTypeForEndDate(errors, AttendanceMonitoringPointType.Meeting, new DateTime().toLocalDate.minusDays(1))
		errors.hasFieldErrors("pointType") should be {true}
	}}

	@Test
	def validateTypeMeeting() {
		new Fixture {
			validator.validateTypeMeeting(errors, mutable.Set(), mutable.Set(), 0, null)
			errors.hasFieldErrors("meetingRelationships") should be {true}
			errors.hasFieldErrors("meetingFormats") should be {true}
		}
		new Fixture {
			val department = new Department
			department.relationshipService = smartMock[RelationshipService]
			department.relationshipService.allStudentRelationshipTypes returns Seq()
			validator.validateTypeMeeting(errors, mutable.Set(StudentRelationshipType("tutor","tutor","tutor","tutee")), mutable.Set(), 0, department)
			errors.hasFieldErrors("meetingRelationships") should be {true}
		}
		new Fixture {
			val validRelationship = StudentRelationshipType("tutor","tutor","tutor","tutee")
			validRelationship.defaultDisplay = true
			val department = new Department
			department.relationshipService = smartMock[RelationshipService]
			department.relationshipService.allStudentRelationshipTypes returns Seq(validRelationship)
			validator.validateTypeMeeting(errors, mutable.Set(validRelationship), mutable.Set(), 0, department)
			errors.hasFieldErrors("meetingRelationships") should be {false}
		}
	}

	@Test
	def validateTypeSmallGroup() {
		new Fixture {
			validator.validateTypeSmallGroup(errors, JHashSet(), isAnySmallGroupEventModules = false, smallGroupEventQuantity = 0)
			errors.hasFieldErrors("smallGroupEventQuantity") should be {true}
			errors.hasFieldErrors("smallGroupEventModules") should be {true}
		}
		new Fixture {
			validator.validateTypeSmallGroup(errors, JHashSet(Fixtures.module("a100")), isAnySmallGroupEventModules = false, smallGroupEventQuantity = 1)
			errors.hasFieldErrors("smallGroupEventQuantity") should be {false}
			errors.hasFieldErrors("smallGroupEventModules") should be {false}
		}
	}

	@Test
	def validateTypeAssignmentSubmission() {
		new Fixture {
			validator.validateTypeAssignmentSubmission(
				errors,
				assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Assignments,
				assignmentSubmissionTypeAnyQuantity = 0,
				assignmentSubmissionTypeModulesQuantity = 0,
				assignmentSubmissionModules = JHashSet(),
				assignmentSubmissionAssignments = JHashSet()
			)
			errors.hasFieldErrors("assignmentSubmissionAssignments") should be {true}
		}
		new Fixture {
			validator.validateTypeAssignmentSubmission(
				errors,
				assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Assignments,
				assignmentSubmissionTypeAnyQuantity = 0,
				assignmentSubmissionTypeModulesQuantity = 0,
				assignmentSubmissionModules = JHashSet(),
				assignmentSubmissionAssignments = JHashSet(Fixtures.assignment("assignment"))
			)
			errors.hasFieldErrors("assignmentSubmissionAssignments") should be {false}
		}
		new Fixture {
			validator.validateTypeAssignmentSubmission(
				errors,
				assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Modules,
				assignmentSubmissionTypeAnyQuantity = 0,
				assignmentSubmissionTypeModulesQuantity = 0,
				assignmentSubmissionModules = JHashSet(),
				assignmentSubmissionAssignments = JHashSet()
			)
			errors.hasFieldErrors("assignmentSubmissionTypeModulesQuantity") should be {true}
			errors.hasFieldErrors("assignmentSubmissionModules") should be {true}
		}
		new Fixture {
			validator.validateTypeAssignmentSubmission(
				errors,
				assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Modules,
				assignmentSubmissionTypeAnyQuantity = 0,
				assignmentSubmissionTypeModulesQuantity = 1,
				assignmentSubmissionModules = JHashSet(Fixtures.module("a100")),
				assignmentSubmissionAssignments = JHashSet()
			)
			errors.hasFieldErrors("assignmentSubmissionTypeModulesQuantity") should be {false}
			errors.hasFieldErrors("assignmentSubmissionModules") should be {false}
		}
		new Fixture {
			validator.validateTypeAssignmentSubmission(
				errors,
				assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Any,
				assignmentSubmissionTypeAnyQuantity = 0,
				assignmentSubmissionTypeModulesQuantity = 0,
				assignmentSubmissionModules = JHashSet(),
				assignmentSubmissionAssignments = JHashSet()
			)
			errors.hasFieldErrors("assignmentSubmissionTypeModulesQuantity") should be {false}
			errors.hasFieldErrors("assignmentSubmissionTypeAnyQuantity") should be {true}
		}
		new Fixture {
			validator.validateTypeAssignmentSubmission(
				errors,
				assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Any,
				assignmentSubmissionTypeAnyQuantity = 1,
				assignmentSubmissionTypeModulesQuantity = 0,
				assignmentSubmissionModules = JHashSet(),
				assignmentSubmissionAssignments = JHashSet()
			)
			errors.hasFieldErrors("assignmentSubmissionTypeModulesQuantity") should be {false}
			errors.hasFieldErrors("assignmentSubmissionTypeAnyQuantity") should be {false}
		}

	}

	@Test
	def validateCanPointBeEditedByDate() {
		val startDate = DateTime.now.toLocalDate
		val autumnTerm = new TermImpl(null, null, null, TermType.autumn)
		val studentId = "1234"
		new Fixture {
			validator.termService.getTermFromDateIncludingVacations(startDate.toDateTimeAtStartOfDay) returns autumnTerm
			validator.attendanceMonitoringService.findReports(Seq(studentId), AcademicYear(2014), autumnTerm.getTermTypeAsString) returns Seq(new MonitoringPointReport)
			validator.validateCanPointBeEditedByDate(errors, startDate, Seq(studentId), AcademicYear(2014))
			errors.hasFieldErrors("startDate") should be {true}
		}
		new Fixture {
			validator.termService.getTermFromDateIncludingVacations(startDate.toDateTimeAtStartOfDay) returns autumnTerm
			validator.attendanceMonitoringService.findReports(Seq(studentId), AcademicYear(2014), autumnTerm.getTermTypeAsString) returns Seq()
			validator.validateCanPointBeEditedByDate(errors, startDate, Seq(studentId), AcademicYear(2014))
			errors.hasFieldErrors("startDate") should be {false}
		}
	}

	@Test
	def validateDuplicateForWeek() {
		val nonDupPoint = new AttendanceMonitoringPoint
		nonDupPoint.id = "1"
		nonDupPoint.name = "Name2"
		nonDupPoint.startWeek = 1
		nonDupPoint.endWeek = 1
		val dupPoint = new AttendanceMonitoringPoint
		dupPoint.id = "2"
		dupPoint.name = "Name"
		dupPoint.startWeek = 1
		dupPoint.endWeek = 1
		val schemeWithNonDupPoint = new AttendanceMonitoringScheme
		dupPoint.scheme = schemeWithNonDupPoint
		schemeWithNonDupPoint.points.add(nonDupPoint)
		val schemeWithDupPoint = new AttendanceMonitoringScheme
		schemeWithDupPoint.points.add(dupPoint)
		new Fixture {
			validator.validateDuplicateForWeek(errors, "Name", 1, 1, Seq(schemeWithNonDupPoint))
			errors.hasFieldErrors("name") should be {false}
			errors.hasFieldErrors("startWeek") should be {false}
		}
		new Fixture {
			validator.validateDuplicateForWeek(errors, "Name", 1, 1, Seq(schemeWithNonDupPoint, schemeWithDupPoint))
			errors.hasFieldErrors("name") should be {true}
			errors.hasFieldErrors("startWeek") should be {true}
		}
	}

	@Test
	def validateDuplicateForDate() {
		val baseDate = DateTime.now.toLocalDate
		val nonDupPoint = new AttendanceMonitoringPoint
		nonDupPoint.id = "1"
		nonDupPoint.name = "Name2"
		nonDupPoint.startDate = baseDate
		nonDupPoint.endDate = baseDate.plusDays(1)
		val dupPoint = new AttendanceMonitoringPoint
		dupPoint.id = "2"
		dupPoint.name = "Name"
		dupPoint.startDate = baseDate
		dupPoint.endDate = baseDate.plusDays(1)
		val schemeWithNonDupPoint = new AttendanceMonitoringScheme
		schemeWithNonDupPoint.points.add(nonDupPoint)
		val schemeWithDupPoint = new AttendanceMonitoringScheme
		schemeWithDupPoint.points.add(dupPoint)
		new Fixture {
			validator.validateDuplicateForDate(errors, "Name", baseDate, baseDate.plusDays(1), Seq(schemeWithNonDupPoint))
			errors.hasFieldErrors("name") should be {false}
			errors.hasFieldErrors("startDate") should be {false}
		}
		new Fixture {
			validator.validateDuplicateForDate(errors, "Name", baseDate, baseDate.plusDays(1), Seq(schemeWithNonDupPoint, schemeWithDupPoint))
			errors.hasFieldErrors("name") should be {true}
			errors.hasFieldErrors("startDate") should be {true}
		}
	}

	@Test
	def validateOverlapMeeting() {
		val baseDate = DateTime.now.toLocalDate
		val existingMeetingPoint = new AttendanceMonitoringPoint
		existingMeetingPoint.id = "1"
		existingMeetingPoint.name = "existingMeetingPoint"
		existingMeetingPoint.startDate = baseDate
		existingMeetingPoint.endDate = baseDate.plusDays(1)
		existingMeetingPoint.pointType = AttendanceMonitoringPointType.Meeting
		existingMeetingPoint.meetingFormats = Seq(MeetingFormat.FaceToFace)
		existingMeetingPoint.meetingRelationships = Seq(StudentRelationshipType("tutor","tutor","tutor","tutee"))
		existingMeetingPoint.relationshipService = smartMock[RelationshipService]
		existingMeetingPoint.relationshipService.getStudentRelationshipTypeById("tutor") returns Option(StudentRelationshipType("tutor","tutor","tutor","tutee"))
		val schemeWithExistingMeetingPoint = new AttendanceMonitoringScheme
		schemeWithExistingMeetingPoint.points.add(existingMeetingPoint)

		new Fixture {
			validator.validateOverlapMeeting(
				errors,
				existingMeetingPoint.startDate,
				existingMeetingPoint.endDate,
				mutable.Set(StudentRelationshipType("tutor","tutor","tutor","tutee")),
				mutable.Set(MeetingFormat.FaceToFace),
				Seq(schemeWithExistingMeetingPoint)
			)
			errors.hasGlobalErrors should be {true}
			errors.getAllErrors.asScala.map(_.getCode).contains("attendanceMonitoringPoint.overlaps") should be {true}
		}

		new Fixture {
			validator.validateOverlapMeeting(
				errors,
				existingMeetingPoint.startDate,
				existingMeetingPoint.endDate,
				mutable.Set(StudentRelationshipType("tutor","tutor","tutor","tutee")),
				mutable.Set(MeetingFormat.Email),
				Seq(schemeWithExistingMeetingPoint)
			)
			errors.hasGlobalErrors should be {false}
			errors.getAllErrors.asScala.map(_.getCode).contains("attendanceMonitoringPoint.overlaps") should be {false}
		}

		new Fixture {
			validator.validateOverlapMeeting(
				errors,
				existingMeetingPoint.startDate,
				existingMeetingPoint.endDate,
				mutable.Set(StudentRelationshipType("nottutor","nottutor","nottutor","nottutor")),
				mutable.Set(MeetingFormat.FaceToFace),
				Seq(schemeWithExistingMeetingPoint)
			)
			errors.hasGlobalErrors should be {false}
			errors.getAllErrors.asScala.map(_.getCode).contains("attendanceMonitoringPoint.overlaps") should be {false}
		}
	}

	@Test
	def validateOverlapSmallGroup() {
		val baseDate = DateTime.now.toLocalDate
		val emptyModuleSet: Set[Module] = Set()
		val module = Fixtures.module("aa101")
		module.id = "1"
		val existingSmallGroupPoint = new AttendanceMonitoringPoint
		existingSmallGroupPoint.id = "1"
		existingSmallGroupPoint.name = "existingSmallGroupPoint"
		existingSmallGroupPoint.startDate = baseDate
		existingSmallGroupPoint.endDate = baseDate.plusDays(1)
		existingSmallGroupPoint.pointType = AttendanceMonitoringPointType.SmallGroup
		existingSmallGroupPoint.smallGroupEventModules = Seq(module)
		val schemeWithExistingSmallGroupPoint = new AttendanceMonitoringScheme
		schemeWithExistingSmallGroupPoint.points.add(existingSmallGroupPoint)
		existingSmallGroupPoint.moduleAndDepartmentService = smartMock[ModuleAndDepartmentService]
		existingSmallGroupPoint.moduleAndDepartmentService.getModuleById("1") returns Option(module)

		new Fixture {
			validator.validateOverlapSmallGroup(
				errors,
				existingSmallGroupPoint.startDate,
				existingSmallGroupPoint.endDate,
				Set(module).asJava,
				isAnySmallGroupEventModules = false,
				schemes = Seq(schemeWithExistingSmallGroupPoint)
			)
			errors.hasGlobalErrors should be {true}
			errors.getAllErrors.asScala.map(_.getCode).contains("attendanceMonitoringPoint.overlaps") should be {true}
		}

		new Fixture {
			validator.validateOverlapSmallGroup(
				errors,
				existingSmallGroupPoint.startDate,
				existingSmallGroupPoint.endDate,
				emptyModuleSet.asJava,
				isAnySmallGroupEventModules = true,
				schemes = Seq(schemeWithExistingSmallGroupPoint)
			)
			errors.hasGlobalErrors should be {true}
			errors.getAllErrors.asScala.map(_.getCode).contains("attendanceMonitoringPoint.overlaps") should be {true}
		}
	}

	@Test
	def validateOverlapAssignment() {
		val baseDate = DateTime.now.toLocalDate
		val assignment = Fixtures.assignment("foo")
		assignment.id = "1"
		val module = Fixtures.module("aa101")
		module.id = "1"
		val existingAssignmentPoint = new AttendanceMonitoringPoint
		existingAssignmentPoint.id = "1"
		existingAssignmentPoint.name = "existingAssignmentPoint"
		existingAssignmentPoint.startDate = baseDate
		existingAssignmentPoint.endDate = baseDate.plusDays(1)
		existingAssignmentPoint.pointType = AttendanceMonitoringPointType.AssignmentSubmission
		existingAssignmentPoint.assignmentSubmissionAssignments = Seq(assignment)
		existingAssignmentPoint.assignmentSubmissionModules = Seq(module)
		existingAssignmentPoint.assignmentSubmissionIsDisjunction = true
		val schemeWithExistingAssignmentPoint = new AttendanceMonitoringScheme
		schemeWithExistingAssignmentPoint.points.add(existingAssignmentPoint)
		existingAssignmentPoint.moduleAndDepartmentService = smartMock[ModuleAndDepartmentService]
		existingAssignmentPoint.moduleAndDepartmentService.getModuleById("1") returns Option(module)
		existingAssignmentPoint.assignmentService = smartMock[AssessmentService]
		existingAssignmentPoint.assignmentService.getAssignmentById("1") returns Option(assignment)

		new Fixture {
			existingAssignmentPoint.assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Assignments
			validator.validateOverlapAssignment(
				errors,
				existingAssignmentPoint.startDate,
				existingAssignmentPoint.endDate,
				assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Assignments,
				assignmentSubmissionModules = Set(module).asJava,
				assignmentSubmissionAssignments = Set(assignment).asJava,
				isAssignmentSubmissionDisjunction = true,
				schemes = Seq(schemeWithExistingAssignmentPoint)
			)
			errors.hasGlobalErrors should be {true}
			errors.getAllErrors.asScala.map(_.getCode).contains("attendanceMonitoringPoint.overlaps") should be {true}
		}

		new Fixture {
			existingAssignmentPoint.assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Modules
			validator.validateOverlapAssignment(
				errors,
				existingAssignmentPoint.startDate,
				existingAssignmentPoint.endDate,
				assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Modules,
				assignmentSubmissionModules = Set(module).asJava,
				assignmentSubmissionAssignments = Set(assignment).asJava,
				isAssignmentSubmissionDisjunction = true,
				schemes = Seq(schemeWithExistingAssignmentPoint)
			)
			errors.hasGlobalErrors should be {true}
			errors.getAllErrors.asScala.map(_.getCode).contains("attendanceMonitoringPoint.overlaps") should be {true}
		}

		new Fixture {
			existingAssignmentPoint.assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Assignments
			validator.validateOverlapAssignment(
				errors,
				existingAssignmentPoint.startDate,
				existingAssignmentPoint.endDate,
				assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Modules,
				assignmentSubmissionModules = Set(module).asJava,
				assignmentSubmissionAssignments = Set(assignment).asJava,
				isAssignmentSubmissionDisjunction = true,
				schemes = Seq(schemeWithExistingAssignmentPoint)
			)
			errors.hasGlobalErrors should be {false}
			errors.getAllErrors.asScala.map(_.getCode).contains("attendanceMonitoringPoint.overlaps") should be {false}
		}
	}

	@Test
	def applyInternal(): Unit = new Fixture {
		command.attendanceMonitoringService.listAllSchemes(thisDepartment) returns Seq()
		command.profileService.getAllMembersWithUniversityIds(Seq(student.universityId)) returns Seq(student)
		command.applyInternal()
		verify(command.attendanceMonitoringService, times(1)).setCheckpointTotalsForUpdate(Seq(student), thisDepartment, thisAcademicYear)
	}

}
