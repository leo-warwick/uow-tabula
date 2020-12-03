package uk.ac.warwick.tabula

import java.math

import org.joda.time.{DateTime, DateTimeConstants, LocalDate}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.groups.EventDeliveryMethod.Hybrid
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesPanel, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.MitigatingCircumstancesPanelMemberRoleDefinition
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.services.{LevelService, UserLookupService}
import uk.ac.warwick.userlookup.{AnonymousUser, User}
import uk.ac.warwick.util.termdates.AcademicYearPeriod.PeriodType

import scala.jdk.CollectionConverters._

// scalastyle:off magic.number
object Fixtures extends Mockito {

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

  def assignmentFeedback(universityId: String = "0123456", userId: String = null): Feedback = {
    val f = new Feedback
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
    parent.markerFeedback.add(mf)
    mf
  }

  def department(code: String, name: String = null, isImportDepartment: Boolean = true): Department = {
    val d = new Department
    d.code = code
    d.fullName = Option(name).getOrElse("Department " + code)
    d.isImportDepartment = isImportDepartment
    d
  }

  def departmentWithId(code: String, name: String = null, id: String, isImportDepartment: Boolean = true): Department = {
    val d = new Department
    d.code = code
    d.fullName = Option(name).getOrElse("Department " + code)
    d.id = id
    d.isImportDepartment = isImportDepartment
    d
  }

  def module(code: String, name: String = null): Module = {
    val m = new Module
    m.code = code.toLowerCase
    m.permissionsParents
    m.name = Option(name).getOrElse("Module " + code)
    m
  }

  def route(code: String, name: String = null): Route = {
    val r = new Route
    r.code = code.toLowerCase
    r.name = Option(name).getOrElse("Route " + code)
    r.active = true
    r
  }

  def course(code: String, name: String = null): Course = {
    val c = new Course
    c.code = code
    c.name = Option(name).getOrElse("Course " + code)
    c
  }

  def award(code: String, name: String = null): Award = {
    val a = new Award
    a.code = code
    a.name = Option(name).getOrElse("Award " + code)
    a
  }

  def classification(code: String, name: String = null): Classification = {
    val c = new Classification()
    c.code = code
    c.name = Option(name).getOrElse("Classification " + code)
    c
  }


  def yearWeighting(course: Course, weighting: JBigDecimal, academicYear: AcademicYear, yearOfStudy: YearOfStudy): CourseYearWeighting = {
    new CourseYearWeighting(course, academicYear, yearOfStudy, BigDecimal(weighting))
  }

  def assignment(name: String): Assignment = {
    val a = new Assignment
    a.name = name
    a.setDefaultBooleanProperties()
    a
  }

  def smallGroupSet(name: String): SmallGroupSet = {
    val s = new SmallGroupSet
    s.smallGroupService = None
    s.name = name
    s
  }

  def smallGroup(name: String): SmallGroup = {
    val s = new SmallGroup
    s.smallGroupService = None
    s.name = name
    s
  }

  def smallGroupEvent(title: String): SmallGroupEvent = {
    val s = new SmallGroupEvent
    s.smallGroupService = None
    s.title = title
    s.deliveryMethod = Hybrid
    s
  }

  def smallGroupEventOccurrence(event: SmallGroupEvent, week: Int): SmallGroupEventOccurrence = {
    val s = new SmallGroupEventOccurrence
    s.event = event
    s.week = week
    s
  }

  def departmentSmallGroupSet(name: String): DepartmentSmallGroupSet = {
    val s = new DepartmentSmallGroupSet
    s.smallGroupService = None
    s.name = name
    s
  }

  def departmentSmallGroup(name: String): DepartmentSmallGroup = {
    val s = new DepartmentSmallGroup
    s.smallGroupService = None
    s.name = name
    s
  }

  def assessmentComponent(module: Module, number: Int, assessmentType: AssessmentType = AssessmentType.Essay, weighting: Int = 100, assessmentGroup: String = "A", marksCode: String = null): AssessmentComponent = {
    val a = new AssessmentComponent
    a.name = "Assignment %d" format number
    a.module = module
    a.moduleCode = "%s-30" format module.code.toUpperCase
    a.assessmentGroup = assessmentGroup
    a.sequence = "%s%02d".format(assessmentType.subtype.code, number)
    a.assessmentType = assessmentType
    a.inUse = true
    a.rawWeighting = weighting
    a.marksCode = marksCode
    a
  }

  def assessmentGroup(academicYear: AcademicYear, code: String, module: String, occurrence: String, sequence: String): UpstreamAssessmentGroup = {
    val group = new UpstreamAssessmentGroup
    group.academicYear = academicYear
    group.assessmentGroup = code
    group.moduleCode = module
    group.occurrence = occurrence
    group.sequence = sequence
    group.members.addAll(Seq(
      new UpstreamAssessmentGroupMember(group, "0123456", UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment),
      new UpstreamAssessmentGroupMember(group, "0123457", UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment),
      new UpstreamAssessmentGroupMember(group, "0123458", UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment)
    ).asJava)
    group
  }

  def upstreamAssessmentGroupInfo(academicYear: AcademicYear, code: String, module: String, occurrence: String, sequence: String): UpstreamAssessmentGroupInfo = {
    val uag = assessmentGroup(academicYear, code, module, occurrence, sequence)
    val activeMembers = uag.members
    //add one PWD
    val uagm = new UpstreamAssessmentGroupMember(uag, "1000006", UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment)
    uag.members.add(uagm)
    UpstreamAssessmentGroupInfo(uag, activeMembers.asScala.toSeq)
  }

  def assessmentGroup(assignment: AssessmentComponent, academicYear: AcademicYear = AcademicYear(2012)): UpstreamAssessmentGroup = {
    val group = assessmentGroup(
      academicYear = academicYear,
      code = assignment.assessmentGroup,
      module = assignment.moduleCode,
      occurrence = "A",
      sequence = assignment.sequence
    )
    group.assessmentComponent = assignment
    group
  }

  def assessmentGroupAndMember(
    assignment: AssessmentComponent,
    actualMark: Int,
    academicYear: AcademicYear,
    deadline: LocalDate = AcademicYear(2019).termOrVacation(PeriodType.springTerm).lastDay
  ): UpstreamAssessmentGroup = {
    val group = assessmentGroup(assignment, academicYear)
    group.deadline = Option(deadline)
    val groupMember = new UpstreamAssessmentGroupMember(group, "0123456", UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment)
    groupMember.actualMark = Option(actualMark)
    group.members.clear()
    group.members.addAll(Seq(groupMember).asJava)
    group
  }

  def variableAssessmentWeightingRule(module: Module, number: Int, assessmentType: AssessmentType = AssessmentType.Essay, weighting: Int = 100, assessmentGroup: String = "A"): VariableAssessmentWeightingRule = {
    val rule = new VariableAssessmentWeightingRule
    rule.moduleCode = "%s-30" format module.code.toUpperCase
    rule.assessmentGroup = assessmentGroup
    rule.ruleSequence = "%03d".format(number)
    rule.assessmentType = assessmentType
    rule.rawWeighting = weighting
    rule
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

  def currentUser(universityId: String = "0123456", userId: String = "cuspxp") = {
    val u = user(universityId, userId)
    new CurrentUser(u, u)
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
    scjCode: String = null,
    academicYear: Option[AcademicYear] = None
  ): StudentCourseDetails = {
    val scjCodeToUse = scjCode match {
      case null => member.universityId + "/1"
      case _ => scjCode
    }

    val scd = new StudentCourseDetails(member, scjCodeToUse)
    scd.course = Fixtures.course("UCSA-G500")
    scd.student = member
    scd.sprCode = member.universityId + "/2"
    scd.department = courseDepartment
    scd.mostSignificant = true

    scd.statusOnRoute = sprStatus

    val scyd = studentCourseYearDetails(academicYear.getOrElse(AcademicYear.now()))
    scyd.enrolmentDepartment = courseDepartment
    scyd.yearOfStudy = 1
    scyd.studyLevel = "1"
    scyd.studentCourseDetails = scd
    scd.addStudentCourseYearDetails(scyd)
    scd.latestStudentCourseYearDetails = scyd

    member.attachStudentCourseDetails(scd)
    member.mostSignificantCourse = scd

    val mockLevelService = smartMock[LevelService]
    scd.levelService = mockLevelService
    scyd.levelService = mockLevelService

    mockLevelService.levelFromCode(any[String]) answers { arg: Any =>
      val levelCode = arg.asInstanceOf[String]
      Some(new Level(levelCode, levelCode))
    }

    scd
  }

  def studentCourseYearDetails(
    academicYear: AcademicYear = AcademicYear.now(),
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
    scyd.levelService = smartMock[LevelService]
    scyd.levelService.levelFromCode(any[String]) answers { arg: Any =>
      val levelCode = arg.asInstanceOf[String]
      Some(new Level(levelCode, levelCode))
    }
    scyd
  }

  def memberNoteWithId(note: String, student: Member, id: String): MemberNote = {
    val memberNote = new MemberNote
    memberNote.note = note
    memberNote.member = student
    memberNote.id = id
    memberNote
  }

  def memberNote(note: String, student: Member, creatorId: String): MemberNote = {
    val memberNote = new MemberNote
    memberNote.note = note
    memberNote.member = student
    memberNote.creatorId = creatorId
    memberNote
  }

  def moduleRegistration(
    scd: StudentCourseDetails,
    mod: Module,
    cats: JBigDecimal,
    year: AcademicYear,
    occurrence: String = "",
    agreedMark: Option[Int] = None,
    status: ModuleSelectionStatus = ModuleSelectionStatus.Core,
    marksCode: String = null,
  ): ModuleRegistration = {
    val sprCode = Option(scd).map(_.sprCode).orNull
    val mr = new ModuleRegistration(sprCode, mod, cats, if (cats != null) "%s-%s".format(mod.code.toUpperCase, cats.stripTrailingZeros().toPlainString) else mod.code.toUpperCase, year, occurrence, marksCode)
    mr._allStudentCourseDetails.add(scd)
    mr.agreedMark = agreedMark
    mr.selectionStatus = status
    mr
  }

  def meetingRecordApproval(state: MeetingApprovalState): MeetingRecordApproval = {
    val approval = new MeetingRecordApproval
    approval.state = state
    approval
  }

  def notification(agent: User, recipient: User): HeronWarningNotification = {
    val staff = Fixtures.staff("1234567")
    val student = Fixtures.student("9876543")
    val relType = StudentRelationshipType("tutor", "tutor", "tutor", "tutor")

    val meeting = new MeetingRecord
    meeting.creator = staff

    val relationship = StudentRelationship(staff, relType, student, DateTime.now)
    meeting.relationships = Seq(relationship)
    Notification.init(new HeronWarningNotification, agent, meeting)
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
    point.startDate = new LocalDate(academicYear.startYear, DateTimeConstants.NOVEMBER, 1)
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

  def disability(description: String, code: String = ""): Disability = {
    val d = new Disability
    d.sitsDefinition = description
    d.tabulaDefinition = description
    d.code = if (code.isEmpty) description.charAt(0).toString else code
    d
  }

  def withParents(target: PermissionsTarget): LazyList[PermissionsTarget] = {
    target #:: target.permissionsParents.flatMap(withParents)
  }

  def userLookupService(users: User*): UserLookupService = {
    val userLookup = smartMock[UserLookupService]
    for (user <- users) {
      userLookup.getUserByUserId(user.getUserId) returns user
    }
    userLookup.getUserByUserId(null) returns new AnonymousUser

    userLookup.getUsersByUserIds(any[JList[String]]) answers { ids: Any =>
      val u = ids.asInstanceOf[JList[String]].asScala.toSeq.map(id => (id, users.find(_.getUserId == id).getOrElse(new AnonymousUser())))
      JHashMap(u: _*)
    }

    userLookup.usersByUserIds(any[Seq[String]]) answers { ids: Any =>
      val u = ids.asInstanceOf[Seq[String]].map(id => (id, users.find(_.getUserId == id).getOrElse(new AnonymousUser())))
      Map(u: _*)
    }

    userLookup
  }

  def mitigatingCircumstancesSubmission(creator: String, studentId: String): MitigatingCircumstancesSubmission = {
    val dept = department("HPS", "Heron Purging Services")
    val st = student(studentId, courseDepartment = dept)

    //val sprFullyEnrolledStatus: SitsStatus = Fixtures.sitsStatus("F", "Fully Enrolled", "Fully Enrolled for this Session")
    val s = new MitigatingCircumstancesSubmission(
      st,
      user(creator, creator),
      dept
    )
    s.startDate = LocalDate.now()
    s.endDate = LocalDate.now().plusWeeks(2)
    s.saveAsDraft()
    s
  }

  def mitigatingCircumstancesPanel(viewers: User*): MitigatingCircumstancesPanel = {
    val dept = department("HPS", "Heron Purging Services")
    val s = new MitigatingCircumstancesPanel(dept, AcademicYear(2019))
    val mockPermissionsService: PermissionsService = smartMock[PermissionsService]
    val ug = UserGroup.ofUsercodes
    viewers.foreach(v => ug.knownType.addUserId(v.getUserId))
    ug.userLookup = Fixtures.userLookupService(viewers:_*)
    mockPermissionsService.ensureUserGroupFor(s, MitigatingCircumstancesPanelMemberRoleDefinition) returns ug
    s.permissionsService = mockPermissionsService
    s
  }

  def exam(name: String = "Summer exam", department: Department = Fixtures.department("in"), academicYear: AcademicYear = AcademicYear(2019)): Exam = {
    new Exam(name, department, academicYear)
  }

  def examQuestion(exam:Exam, name: String, score:Int): ExamQuestion = {
    val q = new ExamQuestion(exam, name)
    q.score = score
    q
  }

  def examQuestion(exam:Exam, parent: ExamQuestion, name: String): ExamQuestion = {
    val q = new ExamQuestion(exam, name)
    q.parent = parent
    parent.children.add(q)
    q
  }

  def examQuestion(exam:Exam, parent: ExamQuestion, name: String, score: Int): ExamQuestion = {
    val q = examQuestion(exam, parent, name)
    q.score = score
    q
  }

  def gradeBoundary(
    marksCode: String,
    process: GradeBoundaryProcess = GradeBoundaryProcess.StudentAssessment,
    attempt: Int = 1,
    rank: Int = 1,
    grade: String,
    minimumMark: Option[Int] = None,
    maximumMark: Option[Int] = None,
    signalStatus: GradeBoundarySignalStatus = GradeBoundarySignalStatus.NoSignal,
    result: Option[ModuleResult] = None,
    agreedStatus: GradeBoundaryAgreedStatus = GradeBoundaryAgreedStatus.Agreed,
    incrementsAttempt: Boolean = false,
  ): GradeBoundary = GradeBoundary(
    marksCode = marksCode,
    process = process,
    attempt = attempt,
    rank = rank,
    grade = grade,
    minimumMark = minimumMark,
    maximumMark = maximumMark,
    signalStatus = signalStatus,
    result = result,
    agreedStatus = agreedStatus,
    incrementsAttempt = incrementsAttempt,
  )

}
