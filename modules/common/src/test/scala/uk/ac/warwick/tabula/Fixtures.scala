package uk.ac.warwick.tabula

import java.math

import org.hibernate.{Session, SessionFactory}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.services.MonitoringPointService
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.userlookup.User

// scalastyle:off magic.number
object Fixtures extends Mockito {

	def exam(name: String) : Exam =  {
		val exam = new Exam()
		exam.name = name
		exam
	}

	def submission(universityId: String = "0123456", userId: String = "cuspxp") = {
		val s = new Submission
		s.universityId = universityId
		s.userId = userId
		s
	}

	def submissionWithId(universityId: String = "0123456", userId: String = "cuspxp", id: String) = {
		val s = new Submission
		s.universityId = universityId
		s.userId = userId
		s.id = id
		s
	}

	def assignmentFeedback(universityId: String = "0123456") = {
		val f = new AssignmentFeedback
		f.universityId = universityId
		f
	}

	def extension(universityId: String = "0123456", userId: String = "cuspxp") = {
		val e = new Extension
		e.universityId = universityId
		e.userId = userId
		e
	}

	def markerFeedback(parent: Feedback) = {
		val mf = new MarkerFeedback(parent)
		mf.state = MarkingState.ReleasedForMarking
		mf
	}

	def department(code:String, name:String = null) = {
		val d = new Department
		d.code = code
		d.fullName = Option(name).getOrElse("Department " + code)
		d
	}

	def departmentWithId(code:String, name:String = null, id: String) = {
		val d = new Department
		d.code = code
		d.fullName = Option(name).getOrElse("Department " + code)
		d.id = id
		d
	}

	def module(code:String, name: String = null) = {
		val m = new Module
		m.code = code.toLowerCase
		m.name = Option(name).getOrElse("Module " + code)
		m
	}

	def route(code:String, name: String = null) = {
		val r = new Route
		r.code = code.toLowerCase
		r.name = Option(name).getOrElse("Route " + code)
		r
	}

	def course(code:String, name: String = null) = {
		val c = new Course
		c.code = code
		c.name = Option(name).getOrElse("Course " + code)
		c
	}

	def assignment(name:String) = {
		val a = new Assignment
		a.name = name
		a.setDefaultBooleanProperties()
		a
	}

	def smallGroupSet(name:String) = {
		val s = new SmallGroupSet
		s.smallGroupService = None
		s.name = name
		s
	}

	def smallGroup(name:String) = {
		val s = new SmallGroup
		s.smallGroupService = None
		s.name = name
		s
	}

	def smallGroupEvent(title:String) = {
		val s = new SmallGroupEvent
		s.smallGroupService = None
		s.title = title
		s
	}

	def departmentSmallGroupSet(name:String) = {
		val s = new DepartmentSmallGroupSet
		s.smallGroupService = None
		s.name = name
		s
	}

	def departmentSmallGroup(name:String) = {
		val s = new DepartmentSmallGroup
		s.smallGroupService = None
		s.name = name
		s
	}

	def upstreamAssignment(module: Module, number: Int) = {
		val a = new AssessmentComponent
		a.name = "Assignment %d" format number
		a.module = module
		a.moduleCode = "%s-30" format module.code.toUpperCase
		a.assessmentGroup = "A"
		a.sequence = "A%02d" format number
		a.assessmentType = AssessmentType.Assignment
		a.inUse = true
		a
	}

	def assessmentGroup(academicYear: AcademicYear, code: String, module: String, occurrence: String) = {
		val group = new UpstreamAssessmentGroup
		group.academicYear = academicYear
		group.assessmentGroup = code
		group.moduleCode = module
		group.occurrence = occurrence
		group.members.staticUserIds = Seq(
			"0123456",
			"0123457",
			"0123458"
		)
		group
	}

	def assessmentGroup(assignment:AssessmentComponent): UpstreamAssessmentGroup =
		assessmentGroup(
			academicYear = new AcademicYear(2012),
			code = assignment.assessmentGroup,
			module = assignment.moduleCode + "-30",
			occurrence = "A")


	def seenSecondMarkingLegacyWorkflow(name: String) = {
		val workflow = new SeenSecondMarkingLegacyWorkflow
		workflow.name = name
		workflow
	}

	def seenSecondMarkingWorkflow(name: String) = {
		val workflow = new SeenSecondMarkingWorkflow
		workflow.name = name
		workflow
	}

	def studentsChooseMarkerWorkflow(name: String) = {
		val workflow = new StudentsChooseMarkerWorkflow
		workflow.name = name
		workflow
	}

	def feedbackTemplate(name: String) = {
		val template = new FeedbackTemplate
		template.name = name
		template
	}

	def userSettings(userId: String = "cuspxp") = {
		val settings = new UserSettings
		settings.userId = userId
		settings
	}

	def user(universityId: String = "0123456", userId: String = "cuspxp") = {
		val user = new User()
		user.setUserId(userId)
		user.setWarwickId(universityId)
		user.setFoundUser(true)
		user.setVerified(true)
		user
	}

	def member(userType: MemberUserType, universityId: String = "0123456", userId: String = "cuspxp", department: Department = null) = {
		val member = userType match {
			case MemberUserType.Student => new StudentMember
			case MemberUserType.Emeritus => new EmeritusMember
			case MemberUserType.Staff => new StaffMember
			case MemberUserType.Other => new OtherMember
		}
		member.firstName = universityId
		member.lastName = userType.toString
		member.universityId = universityId
		member.userId = userId
		member.userType = userType
		member.homeDepartment = department
		member.inUseFlag = "Active"
		member
	}

	def staff(universityId: String = "0123456", userId: String = "cuspxp", department: Department = null) =
		member(MemberUserType.Staff, universityId, userId, department).asInstanceOf[StaffMember]

	def sitsStatus(code: String = "F", shortName: String = "Fully enrolled", fullName: String = "Fully enrolled for this session") = {
		val status = new SitsStatus(code, shortName, fullName)
		status
	}

	def modeOfAttendance(code: String = "F", shortName: String = "FT", fullName: String = "Full time") = {
		val moa = new ModeOfAttendance(code, shortName, fullName)
		moa
	}

	def student(universityId: String = "0123456",
							userId: String = "cuspxp",
							department: Department = null,
							courseDepartment: Department = null,
							sprStatus: SitsStatus = null)	= {
		val m = member(MemberUserType.Student, universityId, userId, department).asInstanceOf[StudentMember]

		studentCourseDetails(m, courseDepartment, sprStatus)
		m
	}

	def studentCourseDetails(member: StudentMember,
													 courseDepartment: Department,
													 sprStatus: SitsStatus = null,
													 scjCode: String = null) = {
		val scjCodeToUse = scjCode match {
			case null => member.universityId + "/1"
			case _ => scjCode
		}

		val scd = new StudentCourseDetails(member, scjCodeToUse)
		scd.student = member
		scd.sprCode = member.universityId + "/2"
		scd.department = courseDepartment
		scd.mostSignificant = true

		scd.statusOnRoute = sprStatus

		val scyd = studentCourseYearDetails()
		scyd.enrolmentDepartment = courseDepartment
		scyd.studentCourseDetails = scd
		scd.addStudentCourseYearDetails(scyd)
		scd.latestStudentCourseYearDetails = scyd

		member.attachStudentCourseDetails(scd)
		member.mostSignificantCourse = scd

		scd
	}

	def studentCourseYearDetails(
		academicYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now),
		modeOfAttendance: ModeOfAttendance = null,
		yearOfStudy: Int = 1,
		studentCourseDetails: StudentCourseDetails = null
	) = {
		val scyd = new StudentCourseYearDetails
		scyd.academicYear = academicYear
		scyd.modeOfAttendance = modeOfAttendance
		scyd.yearOfStudy = yearOfStudy
		scyd.studentCourseDetails = studentCourseDetails
		scyd.sceSequenceNumber = 1
		scyd.casUsed = false
		scyd.tier4Visa = false
		scyd
	}

	def monitoringPoint(name: String = "name", validFromWeek: Int = 0, requiredFromWeek: Int = 0) = {
		val point = new MonitoringPoint
		point.name = name
		point.validFromWeek = validFromWeek
		point.requiredFromWeek = requiredFromWeek
		point
	}

	def memberNoteWithId(note: String, student: Member, id: String ) = {
		val memberNote = new MemberNote
		memberNote.note = note
		memberNote.member = student
		memberNote.id = id
		memberNote
	}

	def memberNote(note: String, student: Member ) = {
		val memberNote = new MemberNote
		memberNote.note = note
		memberNote.member = student
		memberNote
	}

	def monitoringCheckpoint(point: MonitoringPoint, student: StudentMember, state: AttendanceState) = {
		val checkpoint = new MonitoringCheckpoint
		val monitoringPointService = smartMock[MonitoringPointService]
		monitoringPointService.studentAlreadyReportedThisTerm(student, point) returns false
		checkpoint.monitoringPointService = monitoringPointService
		checkpoint.point = point
		checkpoint.student = student
		checkpoint.state = state
		checkpoint
	}

	def moduleRegistration(scd: StudentCourseDetails, mod: Module, cats: java.math.BigDecimal, year: AcademicYear, occurrence: String) = {
		new ModuleRegistration(scd, mod, cats, year, occurrence)
	}

	def meetingRecordApproval(state: MeetingApprovalState) = {
		val approval = new MeetingRecordApproval
		approval.state = state
		approval
	}

	def notification(agent:User, recipient: User) = {
		val heron = new Heron(recipient)
		Notification.init(new HeronWarningNotification, agent, heron)
	}

	def attendanceMonitoringPoint(
		scheme: AttendanceMonitoringScheme,
		name: String = "name",
		startWeek: Int = 0,
		endWeek: Int = 0,
		academicYear: AcademicYear = AcademicYear(2014)
	) = {
		val point = new AttendanceMonitoringPoint
		point.scheme = scheme
		point.name = name
		point.startWeek = startWeek
		point.endWeek = endWeek
		point.startDate = academicYear.dateInTermOne.toLocalDate
		point.endDate = point.startDate.plusWeeks(endWeek - startWeek).plusDays(6)
		point
	}

	def attendanceMonitoringCheckpoint(point: AttendanceMonitoringPoint, student: StudentMember, state: AttendanceState) = {
		val checkpoint = new AttendanceMonitoringCheckpoint
		val attendanceMonitoringService = smartMock[AttendanceMonitoringService]
		attendanceMonitoringService.studentAlreadyReportedThisTerm(student, point) returns false
		checkpoint.attendanceMonitoringService = attendanceMonitoringService
		checkpoint.point = point
		checkpoint.student = student
		checkpoint.state = state
		checkpoint
	}

	def attendanceMonitoringCheckpointTotal(
		student: StudentMember,
		department: Department,
		academicYear: AcademicYear,
		attended: Int = 0,
		authorised: Int = 0,
		unauthorised: Int = 0,
		unrecorded: Int = 0
	) = {
		val total = new AttendanceMonitoringCheckpointTotal
		total.student = student
		total.department = department
		total.academicYear = academicYear
		total.attended = attended
		total.authorised = authorised
		total.unauthorised = unauthorised
		total.unrecorded = unrecorded
		total.updatedDate = DateTime.now
		total
	}

	def routeTeachingInformation(route: Route, departments: Seq[Department]): Seq[RouteTeachingInformation] = {
		val percentage = new math.BigDecimal(100 / departments.size)
		departments.map(dept => {
			val rti = new RouteTeachingInformation
			rti.route = route
			rti.department = dept
			rti.percentage = percentage
			rti
		})
	}

	def userGroup(users: User*) = {
		val ug = new UserGroup()
		for (user <- users) {
			ug.add(user)
		}
		ug
	}

	def feedbackForSits(feedback: Feedback, user: User): FeedbackForSits = {
		val fb = new FeedbackForSits
		fb.init(feedback, user)
		fb
	}

	def disability(description: String, code: String = "") = {
		val d = new Disability
		d.sitsDefinition = description
		d.tabulaDefinition = description
		d.code = if (code.isEmpty) description.charAt(0).toString else code
		d
	}

	def firstMarkerMap(assignment: Assignment, marker_id: String, students: Seq[String] = Seq()) = {
		val fmm = new FirstMarkersMap
		fmm.assignment = assignment
		fmm.marker_id = marker_id
		fmm.students = UserGroup.ofUsercodes
		students.foreach(fmm.students.knownType.addUserId)
		fmm
	}

	def secondMarkerMap(assignment: Assignment, marker_id: String, students: Seq[String] = Seq()) = {
		val smm = new SecondMarkersMap
		smm.assignment = assignment
		smm.marker_id = marker_id
		smm.students = UserGroup.ofUsercodes
		students.foreach(smm.students.knownType.addUserId)
		smm
	}

}
