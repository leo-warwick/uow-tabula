package uk.ac.warwick.tabula

import java.math

import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.userlookup.{AnonymousUser, User}
import org.junit.Before

import scala.collection.JavaConverters._

// scalastyle:off magic.number
object Fixtures extends Mockito {


	def exam(name: String) : Exam =  {
		val exam = new Exam()
		exam.name = name
		exam
	}

	def submission(universityId: String = "0123456", userId: String = "cuspxp"): Submission = {
		val s = new Submission
		s._universityId = universityId
		s.usercode = userId
		s
	}

	def submissionWithId(universityId: String = "0123456", userId: String = "cuspxp", id: String): Submission = {
		val s = new Submission
		s._universityId = universityId
		s.usercode = userId
		s.id = id
		s
	}

	def assignmentFeedback(universityId: String = "0123456", userId: String = null): AssignmentFeedback = {
		val f = new AssignmentFeedback
		f._universityId = universityId
		f.usercode = if (userId == null) universityId else userId
		f
	}

	def extension(universityId: String = "0123456", userId: String = "cuspxp"): Extension = {
		val e = new Extension
		e._universityId = universityId
		e.usercode = userId
		e
	}

	def markerFeedback(parent: Feedback): MarkerFeedback = {
		val mf = new MarkerFeedback(parent)
		mf.state = MarkingState.ReleasedForMarking
		parent.markerFeedback.add(mf)
		mf
	}

	def department(code:String, name:String = null): Department = {
		val d = new Department
		d.code = code
		d.fullName = Option(name).getOrElse("Department " + code)
		d
	}

	def departmentWithId(code:String, name:String = null, id: String): Department = {
		val d = new Department
		d.code = code
		d.fullName = Option(name).getOrElse("Department " + code)
		d.id = id
		d
	}

	def module(code:String, name: String = null): Module = {
		val m = new Module
		m.code = code.toLowerCase
		m.permissionsParents
		m.name = Option(name).getOrElse("Module " + code)
		m
	}

	def route(code:String, name: String = null): Route = {
		val r = new Route
		r.code = code.toLowerCase
		r.name = Option(name).getOrElse("Route " + code)
		r
	}

	def course(code:String, name: String = null): Course = {
		val c = new Course
		c.code = code
		c.name = Option(name).getOrElse("Course " + code)
		c
	}

	def assignment(name:String): Assignment = {
		val a = new Assignment
		a.name = name
		a.setDefaultBooleanProperties()
		a
	}

	def smallGroupSet(name:String): SmallGroupSet = {
		val s = new SmallGroupSet
		s.smallGroupService = None
		s.name = name
		s
	}

	def smallGroup(name:String): SmallGroup = {
		val s = new SmallGroup
		s.smallGroupService = None
		s.name = name
		s
	}

	def smallGroupEvent(title:String): SmallGroupEvent = {
		val s = new SmallGroupEvent
		s.smallGroupService = None
		s.title = title
		s
	}

	def departmentSmallGroupSet(name:String): DepartmentSmallGroupSet = {
		val s = new DepartmentSmallGroupSet
		s.smallGroupService = None
		s.name = name
		s
	}

	def departmentSmallGroup(name:String): DepartmentSmallGroup = {
		val s = new DepartmentSmallGroup
		s.smallGroupService = None
		s.name = name
		s
	}

	def upstreamAssignment(module: Module, number: Int): AssessmentComponent = {
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

	def assessmentGroup(academicYear: AcademicYear, code: String, module: String, occurrence: String): UpstreamAssessmentGroup = {
		val group = new UpstreamAssessmentGroup
		group.academicYear = academicYear
		group.assessmentGroup = code
		group.moduleCode = module
		group.occurrence = occurrence
		group.members.addAll(Seq(
			new UpstreamAssessmentGroupMember(group, "0123456"),
			new UpstreamAssessmentGroupMember(group, "0123457"),
			new UpstreamAssessmentGroupMember(group, "0123458")
		).asJava)
		group
	}

	def assessmentGroup(assignment:AssessmentComponent): UpstreamAssessmentGroup =
		assessmentGroup(
			academicYear = new AcademicYear(2012),
			code = assignment.assessmentGroup,
			module = assignment.moduleCode + "-30",
			occurrence = "A")


	def seenSecondMarkingLegacyWorkflow(name: String): SeenSecondMarkingLegacyWorkflow = {
		val workflow = new SeenSecondMarkingLegacyWorkflow
		workflow.name = name
		workflow
	}

	def seenSecondMarkingWorkflow(name: String): SeenSecondMarkingWorkflow = {
		val workflow = new SeenSecondMarkingWorkflow
		workflow.name = name
		workflow
	}

	def studentsChooseMarkerWorkflow(name: String): OldStudentsChooseMarkerWorkflow = {
		val workflow = new OldStudentsChooseMarkerWorkflow
		workflow.name = name
		workflow
	}

	def feedbackTemplate(name: String): FeedbackTemplate = {
		val template = new FeedbackTemplate
		template.name = name
		template
	}

	def userSettings(userId: String = "cuspxp"): UserSettings = {
		val settings = new UserSettings
		settings.userId = userId
		settings
	}

	def user(universityId: String = "0123456", userId: String = "cuspxp"): User = {
		val user = new User()
		user.setUserId(userId)
		user.setWarwickId(universityId)
		user.setFoundUser(true)
		user.setVerified(true)
		user
	}

	def member(userType: MemberUserType, universityId: String = "0123456", userId: String = "cuspxp", department: Department = null): Member = {
		val member = userType match {
			case MemberUserType.Student => new StudentMember
			case MemberUserType.Emeritus => new EmeritusMember
			case MemberUserType.Staff => new StaffMember
			case MemberUserType.Applicant => new ApplicantMember
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

	def staff(universityId: String = "0123456", userId: String = "cuspxp", department: Department = null): StaffMember =
		member(MemberUserType.Staff, universityId, userId, department).asInstanceOf[StaffMember]

	def sitsStatus(code: String = "F", shortName: String = "Fully enrolled", fullName: String = "Fully enrolled for this session"): SitsStatus = {
		val status = new SitsStatus(code, shortName, fullName)
		status
	}

	def modeOfAttendance(code: String = "F", shortName: String = "FT", fullName: String = "Full time"): ModeOfAttendance = {
		val moa = new ModeOfAttendance(code, shortName, fullName)
		moa
	}

	def student(universityId: String = "0123456",
							userId: String = "cuspxp",
							department: Department = null,
							courseDepartment: Department = null,
							sprStatus: SitsStatus = null): StudentMember = {
		val m = member(MemberUserType.Student, universityId, userId, department).asInstanceOf[StudentMember]

		studentCourseDetails(m, courseDepartment, sprStatus)
		m
	}

	def studentCourseDetails(member: StudentMember,
													 courseDepartment: Department,
													 sprStatus: SitsStatus = null,
													 scjCode: String = null): StudentCourseDetails = {
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
	): StudentCourseYearDetails = {
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

	def memberNoteWithId(note: String, student: Member, id: String ): MemberNote = {
		val memberNote = new MemberNote
		memberNote.note = note
		memberNote.member = student
		memberNote.id = id
		memberNote
	}

	def memberNote(note: String, student: Member ): MemberNote = {
		val memberNote = new MemberNote
		memberNote.note = note
		memberNote.member = student
		memberNote
	}

	def moduleRegistration(
		scd: StudentCourseDetails,
		mod: Module,
		cats: JBigDecimal,
		year: AcademicYear,
		occurrence: String = "",
		agreedMark: BigDecimal = BigDecimal(0),
		status: ModuleSelectionStatus = ModuleSelectionStatus.Core
	): ModuleRegistration = {
		val mr = new ModuleRegistration(scd, mod, cats, year, occurrence)
		mr.agreedMark = Option(agreedMark).map(_.underlying).orNull
		mr.selectionStatus = status
		mr
	}

	def meetingRecordApproval(state: MeetingApprovalState): MeetingRecordApproval = {
		val approval = new MeetingRecordApproval
		approval.state = state
		approval
	}

	def notification(agent:User, recipient: User): HeronWarningNotification = {
		val heron = new Heron(recipient)
		Notification.init(new HeronWarningNotification, agent, heron)
	}

	def attendanceMonitoringPoint(
		scheme: AttendanceMonitoringScheme,
		name: String = "name",
		startWeek: Int = 0,
		endWeek: Int = 0,
		academicYear: AcademicYear = AcademicYear(2014)
	): AttendanceMonitoringPoint = {
		val point = new AttendanceMonitoringPoint
		point.scheme = scheme
		point.name = name
		point.startWeek = startWeek
		point.endWeek = endWeek
		point.startDate = academicYear.dateInTermOne.toLocalDate
		point.endDate = point.startDate.plusWeeks(endWeek - startWeek).plusDays(6)
		point
	}

	def attendanceMonitoringCheckpoint(point: AttendanceMonitoringPoint, student: StudentMember, state: AttendanceState): AttendanceMonitoringCheckpoint = {
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
	): AttendanceMonitoringCheckpointTotal = {
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

	def userGroup(users: User*): UserGroup = {
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

	def disability(description: String, code: String = ""): Disability = {
		val d = new Disability
		d.sitsDefinition = description
		d.tabulaDefinition = description
		d.code = if (code.isEmpty) description.charAt(0).toString else code
		d
	}

	def firstMarkerMap(assignment: Assignment, marker_id: String, students: Seq[String] = Seq()): FirstMarkersMap = {
		val fmm = new FirstMarkersMap
		fmm.assignment = assignment
		fmm.marker_id = marker_id
		fmm.students = UserGroup.ofUsercodes
		students.foreach(fmm.students.knownType.addUserId)
		fmm
	}

	def secondMarkerMap(assignment: Assignment, marker_id: String, students: Seq[String] = Seq()): SecondMarkersMap = {
		val smm = new SecondMarkersMap
		smm.assignment = assignment
		smm.marker_id = marker_id
		smm.students = UserGroup.ofUsercodes
		students.foreach(smm.students.knownType.addUserId)
		smm
	}

	def withParents(target: PermissionsTarget): Stream[PermissionsTarget] = {
		  target #:: target.permissionsParents.flatMap(withParents)
	}

	def userLookupService(users: User*) = {
		val userLookup = smartMock[UserLookupService]
		for (user <- users) {
			userLookup.getUserByUserId(user.getUserId) returns user
		}
		userLookup.getUserByUserId(null) returns new AnonymousUser

		userLookup.getUsersByUserIds(any[JList[String]]) answers { ids =>
			val u = ids.asInstanceOf[JList[String]].asScala.map(id=>(id, users.find(_.getUserId == id).getOrElse(new AnonymousUser())))
			JHashMap(u:_*)
		}

		userLookup.getUsersByUserIds(any[Seq[String]]) answers { ids =>
			val u = ids.asInstanceOf[Seq[String]].map(id=>(id, users.find(_.getUserId == id).getOrElse(new AnonymousUser())))
			Map(u:_*)
		}

		userLookup
	}

}
