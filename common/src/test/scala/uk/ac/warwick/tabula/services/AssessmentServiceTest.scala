package uk.ac.warwick.tabula.services

import org.joda.time.DateTime
import org.junit.Before
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.JavaImports.{JArrayList, JBigDecimal}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.PlagiarismInvestigation.SuspectPlagiarised
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.{CommentField, Extension, FormFieldContext, WordCountField}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

// scalastyle:off magic.number
class AssessmentServiceTest extends PersistenceTestBase with Mockito {

	val thisAssignmentDao = new AssessmentDaoImpl
	val thisFirstMarkerHelper: UserGroupMembershipHelper[MarkingWorkflow] = smartMock[UserGroupMembershipHelper[MarkingWorkflow]]
	val thisSecondMarkerHelper: UserGroupMembershipHelper[MarkingWorkflow] = smartMock[UserGroupMembershipHelper[MarkingWorkflow]]
	val thisMarkingWorkflowService: MarkingWorkflowService = smartMock[MarkingWorkflowService]

	val assignmentService = new AbstractAssessmentService with AssessmentDaoComponent
		with AssessmentServiceUserGroupHelpers with MarkingWorkflowServiceComponent {
		val assessmentDao: AssessmentDaoImpl = thisAssignmentDao
		val firstMarkerHelper: UserGroupMembershipHelper[MarkingWorkflow] = thisFirstMarkerHelper
		val secondMarkerHelper: UserGroupMembershipHelper[MarkingWorkflow] = thisSecondMarkerHelper
		val markingWorkflowService: MarkingWorkflowService = thisMarkingWorkflowService
	}
	val assignmentMembershipService = new AssessmentMembershipServiceImpl
	val feedbackService = new FeedbackServiceImpl
	val submissionService = new SubmissionServiceImpl
	val originalityReportService = new OriginalityReportServiceImpl
  val modAndDeptService = new ModuleAndDepartmentService
  var userLookup:MockUserLookup = _
	var extensionService: ExtensionService = _

  @Before def setup() {
		userLookup = new MockUserLookup()
		userLookup.defaultFoundUser = true
		thisAssignmentDao.sessionFactory = sessionFactory
		submissionService.sessionFactory = sessionFactory
		val deptDao = new DepartmentDaoImpl
		deptDao.sessionFactory = sessionFactory
		modAndDeptService.departmentDao = deptDao
		feedbackService.userLookup = userLookup
		feedbackService.sessionFactory = sessionFactory
		val amDao = new AssessmentMembershipDaoImpl
		amDao.sessionFactory = sessionFactory
		val profileService = smartMock[ProfileService]
		profileService.getAllMembersWithUniversityIds(any[Seq[String]]) returns Seq()
		assignmentMembershipService.dao = amDao
		assignmentMembershipService.assignmentManualMembershipHelper.sessionFactory = sessionFactory
		assignmentMembershipService.assignmentManualMembershipHelper.userLookup = userLookup
		assignmentMembershipService.assignmentManualMembershipHelper.cache.foreach { _.clear() }
		assignmentMembershipService.userLookup = userLookup
		assignmentMembershipService.profileService = profileService
		val extDao = new ExtensionDaoImpl
		extDao.sessionFactory = sessionFactory
		extensionService = new AbstractExtensionService with ExtensionDaoComponent {
			val extensionDao: ExtensionDaoImpl = extDao
		}
	}

	@Transactional @Test def recentAssignment() {
		val assignment = newDeepAssignment()
		val department = assignment.module.adminDepartment

		session.save(department)
		session.save(assignment.module)
		assignmentService.save(assignment)

		assignmentService.recentAssignment(department).get should be (assignment)
	}

	/**
	 * The Hibernate filter that adds deleted != 0
	 */
	@Transactional @Test def notDeletedFilter() {
		val module = new Module
		session.save(module)
		val assignment = new Assignment
		assignment.name = "Essay"
		assignment.module = module
		assignment.academicYear = new AcademicYear(2009)
		assignment.markDeleted()
		assignment.addDefaultFields()
		assignmentService.save(assignment)

		assignment.fields.get(1)

		assignmentService.assessmentDao.isFilterEnabled("notDeleted") should be (false)
		assignmentService.getAssignmentById(assignment.id) should be (Some(assignment))
		session.enableFilter("notDeleted")
		assignmentService.getAssignmentById(assignment.id) should be (None)

		assignmentService.getAssignmentByNameYearModule(assignment.name, assignment.academicYear, assignment.module) should be ('empty)
	}

	/** Checks that assignment field positions don't intefere across FormFieldContexts. */
	@Transactional @Test def overlappingFieldPositions() {
		val assignment = newDeepAssignment()

		val wordCountField = new WordCountField()
		wordCountField.name = "wordcount"
		wordCountField.position = 0
		wordCountField.context = FormFieldContext.Submission

		val feedbackTextField = new CommentField()
		feedbackTextField.name = "feedbackText"
		feedbackTextField.position = 0
		feedbackTextField.context = FormFieldContext.Feedback

		session.save(assignment.module.adminDepartment)
		session.save(assignment.module)
		session.save(assignment)

		assignment.addField(feedbackTextField)
		assignment.addField(wordCountField)

		// are added both with position 0 as they are different contexts.
		assignment.fields.asScala foreach { field =>
			field.position should be (0)
		}

		session.save(feedbackTextField)
		session.save(wordCountField)

		session.flush()
		session.clear()

		val fetched = assignmentService.getAssignmentById(assignment.id).get
		fetched.fields.size should be (2)

		fetched.fields.asScala foreach { field =>
			field.position should be (0)
		}
	}

	@Transactional @Test def findDuplicateAssignmentNames() {
		val module = new Module
		session.save(module)

		assignmentService.getAssignmentByNameYearModule("Essay", new AcademicYear(2009), module) should be ('empty)

		val assignment = new Assignment
		assignment.name = "Essay"
		assignment.module = module
		assignment.academicYear = new AcademicYear(2009)
		assignmentService.save(assignment)
		session.flush()

		assignmentService.getAssignmentByNameYearModule("Essay", new AcademicYear(2009), module) should not be 'empty
		assignmentService.getAssignmentByNameYearModule("Essay", new AcademicYear(2008), module) should be ('empty)
		assignmentService.getAssignmentByNameYearModule("Blessay", new AcademicYear(2009), module) should be ('empty)
	}

	@Transactional @Test def assignmentsByNameTest() {
	    val compSciDept = modAndDeptService.getDepartmentByCode("cs")
	    compSciDept should be ('defined)

	    compSciDept.foreach(dept => {
	        assignmentService.getAssignmentsByName("Test", dept) should have size 2
            assignmentService.getAssignmentsByName("Computing", dept) should have size 1
	        assignmentService.getAssignmentsByName("Assignment", dept) should have size 3
            assignmentService.getAssignmentsByName("xxxx", dept) should have size 0
	    })
    }

	/*
	 * getUsersForFeedback gets all the users associated with an assignment who:
	 *     1. have feedback associated with that assignment which has not been released
	 *     2. have a submission associated with that assignment which is not suspected plagiarised.
	 */
	@Transactional @Test def usersForFeedbackTest() {
		val assignment = assignmentService.getAssignmentById("1")
		assignment should be('defined)

		assignment.foreach { assmt =>
			// create a feedback for the assignment, not yet released
			val feedback = new AssignmentFeedback
			feedback._universityId = "0070790"
			feedback.usercode = "abcdef"
			feedback.actualMark = Some(41)
			feedback.released = false
			assmt.addFeedback(feedback)
			session.save(feedback)

			// create a submission for the assignment, not plagiarised
			val submission = new Submission

			submission._universityId = "0070790"
			submission.usercode = "abcdef"
			submission.plagiarismInvestigation = PlagiarismInvestigation.SuspectPlagiarised
			assmt.addSubmission(submission)
			submissionService.saveSubmission(submission)

			// submissions suspected of plagiarism should be ignored
			val userPairs = feedbackService.getUsersForFeedback(assmt)
			userPairs.size should be (0)

			submission.plagiarismInvestigation = PlagiarismInvestigation.InvestigationCompleted

			// now check one user who needs to get feedback for this assignment is returned
			// as the PlagiarismInvestigation has completed the user should be returned
			val userPairs1 = feedbackService.getUsersForFeedback(assmt)
			userPairs1.size should be (1)

			// and check it's the right one
			for (userPair <- userPairs) {
				val studentId = userPair._1
				val user = userPair._2

				studentId should equal ("0070790")
				user.getWarwickId should equal ("0070790")
			}

			// suppose the feedback was already released - would expect to get no users back
			feedback.released = true
			val userPairs2 = feedbackService.getUsersForFeedback(assmt)
			userPairs2.size should be (0)

			// feedback was not released - expect 1
			feedback.released = false
			val userPairs3 = feedbackService.getUsersForFeedback(assmt)
			userPairs3.size should be (1)

			// the only person was suspected of plagiarism - expect 0
			submission.plagiarismInvestigation = SuspectPlagiarised
			val userPairs4 = feedbackService.getUsersForFeedback(assmt)
			userPairs4.size should be (0)
		}

	}

	@Transactional @Test def updateAssessmentComponent() {
		val module = new Module("ch101")
		session.save(module)

		val upstream = new AssessmentComponent
		upstream.module = module
		upstream.moduleCode = "ch101"
		upstream.sequence = "A01"
		upstream.assessmentGroup = "A"
		upstream.assessmentType = AssessmentType.Assignment
		upstream.name = "Egg plants"
		upstream.inUse = true

		assignmentMembershipService.save(upstream)

		val upstream2 = new AssessmentComponent
		upstream2.module = module
		upstream2.moduleCode = "ch101"
		upstream2.sequence = "A01"
		upstream2.assessmentGroup = "A"
		upstream2.assessmentType = AssessmentType.Assignment
		upstream2.name = "Greg's plants"
		upstream2.inUse = true

		assignmentMembershipService.save(upstream2)
	}

	@Transactional @Test def findAssignmentsWithFeedback() {
		val ThisUser = 	"1234567"
		val OtherUser = "1234568"

		val thisStudent = Fixtures.student("1234567", "1234567")

		val myFeedback = new AssignmentFeedback
		myFeedback._universityId = ThisUser
		myFeedback.usercode = ThisUser
		myFeedback.released = true

		val otherFeedback = new AssignmentFeedback
		otherFeedback._universityId = OtherUser
		otherFeedback.usercode = OtherUser
		otherFeedback.released = true

		val unreleasedFeedback = new AssignmentFeedback
		unreleasedFeedback._universityId = ThisUser
		unreleasedFeedback.usercode = ThisUser

		val deletedFeedback = new AssignmentFeedback
		deletedFeedback._universityId = ThisUser
		deletedFeedback.usercode = ThisUser
		deletedFeedback.released = true

		val assignment1 = new Assignment
		val assignment2 = new Assignment
		val assignment3 = new Assignment
		assignment3.markDeleted()

		assignment1.addFeedback(myFeedback)
		assignment1.addFeedback(otherFeedback)
		assignment2.addFeedback(unreleasedFeedback)
		assignment3.addFeedback(deletedFeedback)

		assignmentService.save(assignment1)
		assignmentService.save(assignment2)
		assignmentService.save(assignment3)

		session.save(myFeedback)
		session.save(otherFeedback)
		session.save(unreleasedFeedback)
		session.save(deletedFeedback)

		session.enableFilter("notDeleted")

		val assignments = assignmentService.getAssignmentsWithFeedback(ThisUser)
		assignments.size should be (1)
		assignments.head should be (assignment1)

		// thisStudent doesn't have a module registration so the assignment should be returned for their most significant details
		val assignmentsByCourseAndYear = assignmentService.getAssignmentsWithFeedback(thisStudent.mostSignificantCourse.latestStudentCourseYearDetails)
		assignmentsByCourseAndYear.size should be (1)
		assignmentsByCourseAndYear.head should be (assignment1)

	}

	@Transactional @Test def findAssignmentsWithSubmission() {
		val ThisUser = 	"1234567"
		val OtherUser = "1234568"

		val thisStudent = Fixtures.student("1234567", "1234567")

		val mySubmission = new Submission
		mySubmission._universityId = ThisUser
		mySubmission.usercode = ThisUser

		val otherSubmission = new Submission
		otherSubmission._universityId = OtherUser
		otherSubmission.usercode = OtherUser

		val deletedSubmission = new Submission
		deletedSubmission._universityId = ThisUser
		deletedSubmission.usercode = ThisUser

		val assignment1 = new Assignment
		val assignment2 = new Assignment
		val assignment3 = new Assignment
		assignment3.markDeleted()

		assignment1.addSubmission(mySubmission)
		assignment1.addSubmission(otherSubmission)
		assignment3.addSubmission(deletedSubmission)

		assignmentService.save(assignment1)
		assignmentService.save(assignment2)
		assignmentService.save(assignment3)

		submissionService.saveSubmission(mySubmission)
		submissionService.saveSubmission(otherSubmission)
		submissionService.saveSubmission(deletedSubmission)

		session.enableFilter("notDeleted")

		val assignments = assignmentService.getAssignmentsWithSubmission(ThisUser)
		assignments.size should be (1)
		assignments.head should be (assignment1)

		val assignmentsByCourseAndYear = assignmentService.getAssignmentsWithSubmission(thisStudent.mostSignificantCourse.latestStudentCourseYearDetails)
		assignmentsByCourseAndYear.size should be (1)
		assignmentsByCourseAndYear.head should be (assignment1)
	}

	@Test def upstreamAssessmentGroups() = transactional { tx =>
		val group = new UpstreamAssessmentGroup
		group.moduleCode = "LA155-10"
		group.occurrence = "A"
		group.assessmentGroup = "A"
		group.sequence = "A01"
		group.academicYear = new AcademicYear(2010)
		group.members = JArrayList(
			new UpstreamAssessmentGroupMember(group, "rob"),
			new UpstreamAssessmentGroupMember(group, "kev"),
			new UpstreamAssessmentGroupMember(group, "bib")
		)

		assignmentMembershipService.save(group)
		session.flush()

		val module = new Module("la155")
		session.save(module)

		val ua = new AssessmentComponent
		ua.module = module
		ua.moduleCode = "LA155-10"
		ua.sequence = "A01"
		ua.assessmentGroup = "A"
		ua.assessmentType = AssessmentType.Assignment
		ua.name = "Egg plants"
		ua.inUse = true

		assignmentMembershipService.save(ua) should be (ua)

		assignmentMembershipService.getUpstreamAssessmentGroups(ua, new AcademicYear(2010)) should be (Seq(group))
		assignmentMembershipService.getUpstreamAssessmentGroups(ua, new AcademicYear(2011)) should be (Seq())
		assignmentMembershipService.getUpstreamAssessmentGroups(new AssessmentComponent, new AcademicYear(2010)) should be (Seq())

		session.clear()

		val foundGroup = assignmentMembershipService.find(group)
		foundGroup should be ('defined)
		foundGroup.eq(Some(group)) should be (false)

		foundGroup.get.occurrence = "B"

		assignmentMembershipService.save(foundGroup.get)
		session.flush()
		session.clear()

		assignmentMembershipService.find(group) should be ('empty)
		assignmentMembershipService.find(foundGroup.get) should be ('defined)
	}

	@Test def upstreamAssignments() = transactional { tx =>
		val chemistryDept = Fixtures.department("chem") // dept code irrelephant
		val chemistryModule = Fixtures.module("ch101")
		chemistryDept.modules.add(chemistryModule)

		session.save(chemistryDept)
		session.save(chemistryModule)

		val lawDept = Fixtures.department("law") // dept code irrelephant
		val lawModule1 = Fixtures.module("la101")
		val lawModule2 = Fixtures.module("la102")
		lawDept.modules.add(lawModule1)
		lawDept.modules.add(lawModule2)

		session.save(lawDept)
		session.save(lawModule1)
		session.save(lawModule2)

		val ua1 = new AssessmentComponent
		ua1.module = chemistryModule
		ua1.moduleCode = "CH101-10"
		ua1.sequence = "A01"
		ua1.assessmentGroup = "A"
		ua1.assessmentType = AssessmentType.Assignment
		ua1.name = "Egg plants"
		ua1.inUse = true

		val ua2 = new AssessmentComponent
		ua2.module = chemistryModule
		ua2.moduleCode = "CH101-20"
		ua2.sequence = "A02"
		ua2.assessmentGroup = "A"
		ua2.assessmentType = AssessmentType.Assignment
		ua2.name = "Egg plants"
		ua2.inUse = true

		val ua3 = new AssessmentComponent
		ua3.module = lawModule1
		ua3.moduleCode = "LA101-10"
		ua3.sequence = "A01"
		ua3.assessmentGroup = "A"
		ua3.assessmentType = AssessmentType.Assignment
		ua3.name = "Egg plants"
		ua3.inUse = true

		val ua4 = new AssessmentComponent
		ua4.module = lawModule1
		ua4.moduleCode = "LA101-10"
		ua4.sequence = "A02"
		ua4.assessmentGroup = "B"
		ua4.assessmentType = AssessmentType.Assignment
		ua4.name = "Egg plants NOT IN USE"
		ua4.inUse = false

		assignmentMembershipService.save(ua1) should be (ua1)
		assignmentMembershipService.save(ua2) should be (ua2)
		assignmentMembershipService.save(ua3) should be (ua3)
		assignmentMembershipService.save(ua4) should be (ua4)

		session.flush()

		assignmentMembershipService.getAssessmentComponent(ua1.id) should be (Some(ua1))
		assignmentMembershipService.getAssessmentComponent(ua4.id) should be (Some(ua4))
		assignmentMembershipService.getAssessmentComponent("wibble") should be (None)

		assignmentMembershipService.getAssessmentComponents(Fixtures.module("ch101")) should be (Seq(ua1, ua2))
		assignmentMembershipService.getAssessmentComponents(Fixtures.module("la101")) should be (Seq(ua3))
		assignmentMembershipService.getAssessmentComponents(Fixtures.module("cs101")) should be (Seq())

		assignmentMembershipService.save(ua1) should be (ua1)
		assignmentMembershipService.save(ua2) should be (ua2)
		assignmentMembershipService.save(ua3) should be (ua3)
		assignmentMembershipService.save(ua4) should be (ua4)

		session.flush()

		assignmentMembershipService.getAssessmentComponents(chemistryDept, includeSubDepartments = false) should be (Seq(ua1, ua2))
		assignmentMembershipService.getAssessmentComponents(lawDept, includeSubDepartments = true) should be (Seq(ua3))
		assignmentMembershipService.getAssessmentComponents(Fixtures.department("cs"), includeSubDepartments = true) should be (Seq())

		val chemistrySubDept = Fixtures.department("ch-ug")
		chemistrySubDept.parent = chemistryDept
		chemistryDept.children.add(chemistrySubDept)

		// Move CH101 from parent to child
		chemistryDept.modules.remove(chemistryModule)
		chemistrySubDept.modules.add(chemistryModule)

		assignmentMembershipService.getAssessmentComponents(chemistryDept, includeSubDepartments = false) should be (Seq())
		assignmentMembershipService.getAssessmentComponents(chemistryDept, includeSubDepartments = true) should be (Seq(ua1, ua2))
	}

	@Test def assessmentGroups() = transactional { tx =>
		val assignment = newDeepAssignment("ch101")
		val module = assignment.module
		val department = module.adminDepartment

		session.save(department)
		session.save(module)

		val upstreamGroup = new UpstreamAssessmentGroup
		upstreamGroup.moduleCode = "ch101-10"
		upstreamGroup.occurrence = "A"
		upstreamGroup.assessmentGroup = "A"
		upstreamGroup.academicYear = new AcademicYear(2010)
		upstreamGroup.members = JArrayList(
			new UpstreamAssessmentGroupMember(upstreamGroup, "rob"),
			new UpstreamAssessmentGroupMember(upstreamGroup, "kev"),
			new UpstreamAssessmentGroupMember(upstreamGroup, "bib")
		)

		assignmentMembershipService.save(upstreamGroup)

		val upstreamAssignment = new AssessmentComponent
		upstreamAssignment.module = module
		upstreamAssignment.moduleCode = "ch101-10"
		upstreamAssignment.sequence = "A01"
		upstreamAssignment.assessmentGroup = "A"
		upstreamAssignment.assessmentType = AssessmentType.Assignment
		upstreamAssignment.name = "Egg plants"
		upstreamAssignment.inUse = true

		assignmentMembershipService.save(upstreamAssignment) should be (upstreamAssignment)

		assignmentService.save(assignment)

		val group = new AssessmentGroup
		group.membershipService = assignmentMembershipService
		group.assignment = assignment
		group.assessmentComponent = upstreamAssignment
		group.occurrence = "A"

		session.save(group)

		session.flush()

		assignmentMembershipService.getAssessmentGroup(group.id) should be (Some(group))

		assignmentMembershipService.getAssessmentGroup(group.id) foreach { assignmentMembershipService.delete }
		assignmentMembershipService.getAssessmentGroup(group.id) should be ('empty)
	}

	@Test def submissions() = transactional { tx =>
		val assignment = newDeepAssignment()
		val department = assignment.module.adminDepartment

		session.save(department)
		session.save(assignment.module)
		assignmentService.save(assignment)

		val submission = new Submission
		submission._universityId = "0070790"
		submission.usercode = "abcdef"
		submission.plagiarismInvestigation = SuspectPlagiarised
		assignment.addSubmission(submission)
		submissionService.saveSubmission(submission)

		session.flush()
		session.clear()

		submissionService.getSubmission(submission.id) should be ('defined)
		submissionService.getSubmission(submission.id).eq(Some(submission)) should be (false)

		submissionService.getSubmissionByUsercode(assignment, "abcdef") should be ('defined)
		submissionService.getSubmissionByUsercode(assignment, "abcdef").eq(Some(submission)) should be (false)

		submissionService.getSubmissionByUsercode(assignment, "abcdef") foreach { submissionService.delete }

		session.flush()
		session.clear()

		submissionService.getSubmissionByUsercode(assignment, "abcdef") should be ('empty)
	}

	@Test def extensions() = transactional { tx =>
		val assignment = newDeepAssignment()
		val department = assignment.module.adminDepartment

		session.save(department)
		session.save(assignment.module)
		assignmentService.save(assignment)

		val extension = new Extension
		extension._universityId = "0070790"
		extension.usercode = "abcdef"
		extension.assignment = assignment
		assignment.extensions.add(extension)
		session.saveOrUpdate(extension)

		session.flush()
		session.clear()

		extensionService.getExtensionById(extension.id) should be ('defined)
		extensionService.getExtensionById(extension.id).eq(Some(extension)) should be (false)

		extensionService.getExtensionById(extension.id) foreach { session.delete }

		session.flush()
		session.clear()

		extensionService.getExtensionById(extension.id) should be ('empty)
	}

	trait AssignmentMembershipFixture {
		val student1 = new User("student1") { setWarwickId("0000001"); setFoundUser(true); setVerified(true); }
		val student2 = new User("student2") { setWarwickId("0000002"); setFoundUser(true); setVerified(true); }
		val student3 = new User("student3") { setWarwickId("0000003"); setFoundUser(true); setVerified(true); }
		val student4 = new User("student4") { setWarwickId("0000004"); setFoundUser(true); setVerified(true); }
		val student5 = new User("student5") { setWarwickId("0000005"); setFoundUser(true); setVerified(true); }
		val manual1 = new User("manual1") { setWarwickId("0000006"); setFoundUser(true); setVerified(true); }
		val manual2 = new User("manual2") { setWarwickId("0000007"); setFoundUser(true); setVerified(true); }
		val manual3 = new User("manual3") { setWarwickId("0000008"); setFoundUser(true); setVerified(true); }
		val manual4 = new User("manual4") { setWarwickId("0000009"); setFoundUser(true); setVerified(true); }
		val manual5 = new User("manual5") { setWarwickId("0000010"); setFoundUser(true); setVerified(true); }
		val manual10 = new User("manual10") { setWarwickId("0000015"); setFoundUser(true); setVerified(true); }

		userLookup.registerUserObjects(student1, student2, student3, student4, student5, manual1, manual2, manual3, manual4, manual5, manual10)

		val year: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
			case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
			case ug: UserGroup => ug.userLookup = userLookup
		}

		val assignment1: Assignment = newDeepAssignment("ch101")
		assignment1.academicYear = year
		assignment1.assessmentMembershipService = assignmentMembershipService
		wireUserLookup(assignment1.members)

		val department1: Department = assignment1.module.adminDepartment

		session.save(department1)
		session.save(assignment1.module)
		assignmentService.save(assignment1)

		val assignment2: Assignment = newDeepAssignment("ch101")
		assignment2.module = assignment1.module
		assignment2.academicYear = year
		assignment2.assessmentMembershipService = assignmentMembershipService
		wireUserLookup(assignment2.members)

		val department2: Department = assignment2.module.adminDepartment

		session.save(department2)
		assignmentService.save(assignment2)

		val up1 = new AssessmentComponent
		up1.module = assignment1.module
		up1.moduleCode = "ch101"
		up1.sequence = "A01"
		up1.assessmentGroup = "A"
		up1.assessmentType = AssessmentType.Assignment
		up1.name = "Egg plants"
		up1.inUse = true

		val upstream1: AssessmentComponent = assignmentMembershipService.save(up1)

		val up2 = new AssessmentComponent
		up2.module = assignment1.module
		up2.moduleCode = "ch101"
		up2.sequence = "A02"
		up2.assessmentGroup = "B"
		up2.assessmentType = AssessmentType.Assignment
		up2.name = "Greg's plants"
		up2.inUse = true

		val upstream2: AssessmentComponent = assignmentMembershipService.save(up2)

		val up3 = new AssessmentComponent
		up3.module = assignment1.module
		up3.moduleCode = "ch101"
		up3.sequence = "A03"
		up3.assessmentGroup = "C"
		up3.assessmentType = AssessmentType.Assignment
		up3.name = "Steg's plants"
		up3.inUse = true

    val upstream3: AssessmentComponent = assignmentMembershipService.save(up3)

    session.flush()

    val upstreamAg1 = new UpstreamAssessmentGroup
    upstreamAg1.moduleCode = "ch101"
    upstreamAg1.assessmentGroup = "A"
    upstreamAg1.academicYear = year
    upstreamAg1.occurrence = "A"
		upstreamAg1.sequence = "A01"

    val upstreamAg2 = new UpstreamAssessmentGroup
    upstreamAg2.moduleCode = "ch101"
    upstreamAg2.assessmentGroup = "B"
    upstreamAg2.academicYear = year
    upstreamAg2.occurrence = "B"
		upstreamAg2.sequence = "A02"

    val upstreamAg3 = new UpstreamAssessmentGroup
    upstreamAg3.moduleCode = "ch101"
    upstreamAg3.assessmentGroup = "C"
    upstreamAg3.academicYear = year
    upstreamAg3.occurrence = "C"
		upstreamAg3.sequence = "A03"

		upstreamAg1.members = JArrayList(
			new UpstreamAssessmentGroupMember(upstreamAg1, "0000001"),
			new UpstreamAssessmentGroupMember(upstreamAg1, "0000002")
		)

		upstreamAg2.members = JArrayList(
			new UpstreamAssessmentGroupMember(upstreamAg2, "0000002"),
			new UpstreamAssessmentGroupMember(upstreamAg2, "0000003")
		)

		upstreamAg3.members = JArrayList(
			new UpstreamAssessmentGroupMember(upstreamAg3, "0000001"),
			new UpstreamAssessmentGroupMember(upstreamAg3, "0000002"),
			new UpstreamAssessmentGroupMember(upstreamAg3, "0000003"),
			new UpstreamAssessmentGroupMember(upstreamAg3, "0000004"),
			new UpstreamAssessmentGroupMember(upstreamAg3, "0000005")
		)

		assignmentMembershipService.save(upstreamAg1)
		assignmentMembershipService.save(upstreamAg2)
		assignmentMembershipService.save(upstreamAg3)

		assignment1.members.knownType.addUserId("manual1")
		assignment1.members.knownType.addUserId("manual2")
		assignment1.members.knownType.addUserId("manual3")

		assignment2.members.knownType.addUserId("manual2")
		assignment2.members.knownType.addUserId("manual3")
		assignment2.members.knownType.addUserId("manual4")

		assignment1.members.knownType.excludeUserId("student2")
		assignment1.members.knownType.excludeUserId("student3")
		assignment1.members.knownType.excludeUserId("manual3") // both inc and exc, but exc should take priority

		assignment2.members.knownType.excludeUserId("student4")
		assignment2.members.knownType.excludeUserId("student3")

		val ag1 = new AssessmentGroup
		ag1.membershipService = assignmentMembershipService
		ag1.assignment = assignment1
		ag1.assessmentComponent = upstream1
		ag1.occurrence = "A"

		val ag2 = new AssessmentGroup
		ag2.membershipService = assignmentMembershipService
		ag2.assignment = assignment1
		ag2.assessmentComponent = upstream2
		ag2.occurrence = "B"

		val ag3 = new AssessmentGroup
		ag3.membershipService = assignmentMembershipService
		ag3.assignment = assignment2
		ag3.assessmentComponent = upstream3
		ag3.occurrence = "C"

		assignment1.assessmentGroups.add(ag1)
		assignment1.assessmentGroups.add(ag3)

		assignment2.assessmentGroups.add(ag2)

		assignmentService.save(assignment1)
		assignmentService.save(assignment2)

		session.flush()

		val universityIdGroup: UserGroup = UserGroup.ofUniversityIds
		universityIdGroup.userLookup = userLookup
		universityIdGroup.addUserId("0000001")
		universityIdGroup.addUserId("0000010")
		universityIdGroup.addUserId("0000015")
		universityIdGroup.excludeUserId("0000009")

		val userIdGroup: UserGroup = UserGroup.ofUsercodes
		userIdGroup.userLookup = userLookup
		userIdGroup.addUserId("student1")
		userIdGroup.addUserId("manual5")
		userIdGroup.addUserId("manual10")
		userIdGroup.excludeUserId("manual4")
	}

	@Test def enrolledAssignments() { transactional { tx =>
		new AssignmentMembershipFixture() {
			val ams: AssessmentMembershipServiceImpl = assignmentMembershipService

			withUser("manual1", "0000006") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment1).toSet) }
			withUser("manual2", "0000007") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment1, assignment2).toSet) }
			withUser("manual3", "0000008") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment2).toSet) }
			withUser("manual4", "0000009") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment2).toSet) }

			withUser("student1", "0000001") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment1, assignment2).toSet) }
			withUser("student2", "0000002") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment2).toSet) }
			withUser("student3", "0000003") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq().toSet) }
			withUser("student4", "0000004") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq().toSet) }
			withUser("student5", "0000005") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment2).toSet) }
		}
	}}

	@Test def determineMembership() { transactional { tx =>
		new AssignmentMembershipFixture() {
			// Assignment1:
			// INC: manual1/0000006, manual2/0000007, manual3/0000008 (also excluded, so excluded takes priority)
			// EXC: student2/0000002, student3/0000003, manual3/0000008
			// Ass Groups: ag1 (0000001, 0000002), ag3 (0000001, 0000002, 0000003, 0000004, 0000005)
			// Actual membership: manual1/0000006, manual2/0000007, 0000001, 0000004, 0000005

			assignmentMembershipService.determineMembershipUsers(assignment1).map { _.getWarwickId }.toSet should be (Set(
					"0000001", "0000004", "0000005", "0000006", "0000007"
			))

			// Assignment2:
			// INC: manual2/0000007, manual3/0000008, manual4/0000009
			// EXC: student4/0000004, student3/0000003
			// Ass Groups: ag2 (0000002, 0000003)
			// Actual membership: manual2/0000007, manual3/0000008, manual4/0000009, 0000002

			assignmentMembershipService.determineMembershipUsers(assignment2).map { _.getWarwickId }.toSet should be (Set(
					"0000007", "0000008", "0000009", "0000002"
			))

			assignmentMembershipService.determineMembershipUsers(Seq(upstreamAg1), None).map { _.getWarwickId }.toSet should be (Set(
					"0000001", "0000002"
			))
			assignmentMembershipService.determineMembershipUsers(Seq(upstreamAg2), None).map { _.getWarwickId }.toSet should be (Set(
					"0000002", "0000003"
			))
			assignmentMembershipService.determineMembershipUsers(Seq(upstreamAg3), None).map { _.getWarwickId }.toSet should be (Set(
					"0000001", "0000002", "0000003", "0000004", "0000005"
			))
			assignmentMembershipService.determineMembershipUsers(Seq(upstreamAg1, upstreamAg2, upstreamAg3), None).map { _.getWarwickId }.toSet should be (Set(
					"0000001", "0000002", "0000003", "0000004", "0000005"
			))

			// UniversityID Group: 0000001, 0000010, 0000015

			assignmentMembershipService.determineMembershipUsers(Seq(upstreamAg1, upstreamAg2, upstreamAg3), Some(universityIdGroup)).map { _.getWarwickId }.toSet should be (Set(
					"0000001", "0000002", "0000003", "0000004", "0000005", "0000010", "0000015"
			))

			// UserID Group: student1/0000001, manual5/0000010, manual10/0000015

			assignmentMembershipService.determineMembershipUsers(Seq(upstreamAg1, upstreamAg2, upstreamAg3), Some(userIdGroup)).map { _.getWarwickId }.toSet should be (Set(
					"0000001", "0000002", "0000003", "0000004", "0000005", "0000010", "0000015"
			))
		}
	}}

	@Test def countMembershipWithUniversityIdGroup() { transactional { tx =>
		new AssignmentMembershipFixture() {
			assignmentMembershipService.countMembershipWithUniversityIdGroup(Seq(upstreamAg1), None) should be (2)
			assignmentMembershipService.countMembershipWithUniversityIdGroup(Seq(upstreamAg2), None) should be (2)
			assignmentMembershipService.countMembershipWithUniversityIdGroup(Seq(upstreamAg3), None) should be (5)
			assignmentMembershipService.countMembershipWithUniversityIdGroup(Seq(upstreamAg1, upstreamAg2, upstreamAg3), None) should be (5)

			assignmentMembershipService.countMembershipWithUniversityIdGroup(Seq(upstreamAg1, upstreamAg2, upstreamAg3), Some(universityIdGroup)) should be (7)

			// UserID Group should fall back to using the other strategy, but get the same result
			assignmentMembershipService.countMembershipWithUniversityIdGroup(Seq(upstreamAg1, upstreamAg2, upstreamAg3), Some(userIdGroup)) should be (7)
		}
	}}

	@Transactional
	@Test def submissionsForAssignmentsBetweenDates() {
		val universityId = "1234"

		val startDate = new DateTime(2014, 3, 1, 0, 0, 0)
		val endDate = new DateTime(2014, 3, 8, 0, 0, 0)

		val assignmentBefore = new Assignment
		assignmentBefore.closeDate = startDate.minusDays(1)
		val assignmentInside = new Assignment
		assignmentInside.closeDate = startDate
		val assignmentAfter = new Assignment
		assignmentAfter.closeDate = endDate
		val assignmentNoSubmission = new Assignment
		assignmentNoSubmission.closeDate = startDate.plusDays(1)

		val submissionBefore = new Submission
		submissionBefore._universityId = universityId
		submissionBefore.usercode = universityId
		submissionBefore.assignment = assignmentBefore
		assignmentBefore.submissions.add(submissionBefore)
		val submissionInside = new Submission
		submissionInside._universityId = universityId
		submissionInside.usercode = universityId
		submissionInside.assignment = assignmentInside
		assignmentInside.submissions.add(submissionInside)
		val submissionAfter = new Submission
		submissionAfter._universityId = universityId
		submissionAfter.usercode = universityId
		submissionAfter.assignment = assignmentAfter
		assignmentAfter.submissions.add(submissionAfter)

		session.save(assignmentBefore)
		session.save(assignmentInside)
		session.save(assignmentAfter)
		session.save(assignmentNoSubmission)
		session.save(submissionBefore)
		session.save(submissionInside)
		session.save(submissionAfter)

		val result = assignmentService.getSubmissionsForAssignmentsBetweenDates(universityId, startDate, endDate)
		result.size should be (1)
		result.head should be (submissionInside)
	}

	@Transactional
	@Test def filterAssignments() {

		val assignment1 = Fixtures.assignment("Ass1")
		val assignment2 = Fixtures.assignment("Ass2")

		val module1 = Fixtures.module("FO02", "Flowers in the Mesolithic Diet")
		val module2 = Fixtures.module("FO03", "Distinguishing Umbellifers")
		assignment1.module = module1
		assignment2.module = module2

		session.save(assignment1)
		session.save(assignment2)

		val thisStudent = Fixtures.student("1234567")
		val scd = thisStudent.mostSignificantCourse
		val scyd = scd.latestStudentCourseYearDetails

		// first, the case where the student isn't registered for any modules so the filter won't remove any:
		val result = assignmentService.filterAssignmentsByCourseAndYear(Seq(assignment1, assignment2), scyd)
		result.size should be (2)

		// now mismatch the year:
		scyd.academicYear = new AcademicYear(2012)
		val resultForDifferentYear = assignmentService.filterAssignmentsByCourseAndYear(Seq(assignment1, assignment2), scyd)
		resultForDifferentYear.size should be (0)

		val currentAcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		// now register the student on a module:
		val mr = Fixtures.moduleRegistration(scd, module1, new JBigDecimal("12.0"), currentAcademicYear, "A")

		// this shouldn't affect anything if the assignment is not for that module:
		scyd.academicYear = currentAcademicYear
		val resultWhenSomeMR = assignmentService.filterAssignmentsByCourseAndYear(Seq(assignment1, assignment2), scyd)
		resultWhenSomeMR.size should be (2)

		// one of the modules is for this course and the other for none - should still get both assignments back
		scd.addModuleRegistration(mr)
		val resultWhenOneMR = assignmentService.filterAssignmentsByCourseAndYear(Seq(assignment1, assignment2), scyd)
		resultWhenOneMR.size should be (2)

		// now register the student on a different course
		val dept = Fixtures.department("fo", "Foraging")
		val status = Fixtures.sitsStatus("C", "Current", "Current Student")
		val scd2 = Fixtures.studentCourseDetails(thisStudent, dept, status)

		val resultWhenOneMismatchedMR = assignmentService.filterAssignmentsByCourseAndYear(Seq(assignment1, assignment2), scd2.latestStudentCourseYearDetails)
		resultWhenOneMismatchedMR.size should be (1)

		}



}
