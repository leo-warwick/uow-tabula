package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.JavaImports.JArrayList

import scala.collection.JavaConversions.seqAsJavaList
import uk.ac.warwick.tabula.{AcademicYear, PersistenceTestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.{AssessmentDaoComponent, AssessmentDaoImpl, AssessmentMembershipDaoImpl}

// scalastyle:off magic.number
class UpstreamEntitiesTest extends PersistenceTestBase {

	@Test def associations() {
		transactional { t =>

			val thisAssignmentDao = new AssessmentDaoImpl
			thisAssignmentDao.sessionFactory = sessionFactory

			val assignmentService = new AbstractAssessmentService with AssessmentDaoComponent
				with AssessmentServiceUserGroupHelpers with MarkingWorkflowServiceComponent {
				val assessmentDao: AssessmentDaoImpl = thisAssignmentDao
				val firstMarkerHelper = null
				val secondMarkerHelper = null
				val markingWorkflowService = null
			}

			val dao = new AssessmentMembershipDaoImpl
			dao.sessionFactory = sessionFactory

			val assignmentMembershipService = new AssessmentMembershipServiceImpl
			assignmentMembershipService.dao = dao

			val law = new AssessmentComponent
			law.moduleCode = "la155-10"
			law.assessmentGroup = "A"
			law.sequence = "A02"
			law.assessmentType = AssessmentType.Assignment
			law.name = "Cool Essay"
			law.inUse = true

			val assessmentGroup2010 = new AssessmentGroup
			assessmentGroup2010.membershipService = assignmentMembershipService
			assessmentGroup2010.assessmentComponent = law
			assessmentGroup2010.occurrence = "A"

			val assessmentGroup2011 = new AssessmentGroup
			assessmentGroup2011.membershipService = assignmentMembershipService
			assessmentGroup2011.assessmentComponent = law
			assessmentGroup2011.occurrence = "A"

			val law2010 = new Assignment
			law2010.assignmentService = assignmentService
			law2010.assessmentMembershipService = assignmentMembershipService
			law2010.name = "Cool Essay!"
			law2010.academicYear = new AcademicYear(2010)
			law2010.assessmentGroups = List(assessmentGroup2010)
			assessmentGroup2010.assignment = law2010

			val law2011 = new Assignment
			law2011.name = "Cool Essay?"
			law2011.assignmentService = assignmentService
			law2011.assessmentMembershipService = assignmentMembershipService
			law2011.academicYear = new AcademicYear(2011)
			law2011.assessmentGroups = List(assessmentGroup2011)
			assessmentGroup2011.assignment = law2011

			// Not linked to an upstream assignment
			val law2012 = new Assignment
			law2012.assignmentService = assignmentService
			law2012.assessmentMembershipService = assignmentMembershipService
			law2012.name = "Cool Essay?"
			law2012.academicYear = new AcademicYear(2011)

			val group2010 = new UpstreamAssessmentGroup
			group2010.moduleCode = "la155-10"
			group2010.occurrence = "A"
			group2010.assessmentGroup = "A"
			group2010.academicYear = new AcademicYear(2010)
			group2010.members = JArrayList(
				new UpstreamAssessmentGroupMember(group2010, "rob"),
				new UpstreamAssessmentGroupMember(group2010, "kev"),
				new UpstreamAssessmentGroupMember(group2010, "bib")
			)

			val group2011 = new UpstreamAssessmentGroup
			group2011.moduleCode = "la155-10"
			group2011.occurrence = "A"
			group2011.assessmentGroup = "A"
			group2011.academicYear = new AcademicYear(2011)
			group2011.members = JArrayList(
				new UpstreamAssessmentGroupMember(group2011, "hog"),
				new UpstreamAssessmentGroupMember(group2011, "dod"),
				new UpstreamAssessmentGroupMember(group2011, "han")
			)

			// similar group but doesn't match the occurence of any assignment above, so ignored.
			val otherGroup = new UpstreamAssessmentGroup
			otherGroup.moduleCode = "la155-10"
			otherGroup.occurrence = "B"
			otherGroup.assessmentGroup = "A"
			otherGroup.academicYear = new AcademicYear(2011)
			otherGroup.members = JArrayList(
				new UpstreamAssessmentGroupMember(otherGroup, "hog"),
				new UpstreamAssessmentGroupMember(otherGroup, "dod"),
				new UpstreamAssessmentGroupMember(otherGroup, "han")
			)

			val member = new StaffMember
			member.universityId = "0672089"
			member.userId = "cuscav"
			member.firstName = "Mathew"
			member.lastName = "Mannion"
			member.email = "M.Mannion@warwick.ac.uk"

			val student = new StudentMember
			student.universityId = "0812345"
			student.userId = "studen"
			student.firstName = "My"
			student.lastName = "Student"
			student.email = "S.Tudent@warwick.ac.uk"

			for (entity <- Seq(law, law2010, law2011, law2012, group2010, group2011, otherGroup, member, student))
				session.save(entity)
			session.flush()
			session.clear()

			law2010.upstreamAssessmentGroups.foreach { group =>
				group.id should be (group2010.id)
				group.membersIncludes("bib") should be (true)
			}

			law2011.upstreamAssessmentGroups.foreach { group =>
				group.id should be (group2011.id)
				group.membersIncludes("dod") should be (true)
			}

			law2012.upstreamAssessmentGroups.isEmpty should be (true)

			session.load(classOf[Member], "0672089") match {
				case loadedMember:Member => loadedMember.firstName should be ("Mathew")
				case _ => fail("Member not found")
			}

			session.load(classOf[StudentMember], "0812345") match {
				case loadedMember:StudentMember =>
					loadedMember.firstName should be ("My")
				case _ => fail("Student not found")
			}
		}
	}
}