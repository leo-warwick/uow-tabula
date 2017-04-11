package uk.ac.warwick.tabula.data.model.groups

import org.hibernate.{Session, SessionFactory}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{Module, UpstreamAssessmentGroup, UserGroup}
import uk.ac.warwick.tabula.helpers.Tap.tap
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}

import scala.collection.JavaConverters._

class SmallGroupSetTest extends TestBase with Mockito {

	val sessionFactory: SessionFactory = smartMock[SessionFactory]
	val session: Session = smartMock[Session]
	sessionFactory.getCurrentSession returns session
	sessionFactory.openSession() returns session

  @Test
  def duplicateCopiesAllFields(){
    val source = new SmallGroupSet
		source.smallGroupService = None

    val cloneGroup = new SmallGroup
    val group:SmallGroup  = new SmallGroup(){
      override def duplicateTo(groupSet:SmallGroupSet, transient: Boolean, copyEvents: Boolean = true, copyMembership: Boolean = true): SmallGroup = cloneGroup
    }
    source.id = "testId"
    source.academicYear = AcademicYear(2001)
    source.allocationMethod = SmallGroupAllocationMethod.Manual
    source.allowSelfGroupSwitching = false
    source.archived = true
    source.assessmentGroups =  JArrayList()
    source.format = SmallGroupFormat.Lab
    source.groups  = JArrayList(group)
    source.members = UserGroup.ofUniversityIds.tap(_.addUserId("test user"))
    source.defaultTutors = UserGroup.ofUniversityIds.tap(_.addUserId("test tutor"))
		source.members.asInstanceOf[UserGroup].sessionFactory = sessionFactory
		source.defaultTutors.asInstanceOf[UserGroup].sessionFactory = sessionFactory

    source.membershipService = mock[AssessmentMembershipService]
    source.module = new Module
    source.name = "test name"
    source.permissionsService = mock[PermissionsService]
    source.releasedToStudents = true
    source.releasedToTutors = true
    val cloneModule = new Module
    val assessmentGroups:JList[UpstreamAssessmentGroup] = JArrayList()


    val clone = source.duplicateTo(module = cloneModule, transient = true)

    clone.id should be(null) // Don't duplicate IDs
    clone.academicYear should be (source.academicYear)
    clone.allocationMethod should be (source.allocationMethod)
    clone.allowSelfGroupSwitching.booleanValue should be {false}
    clone.allowSelfGroupSwitching.booleanValue should be (source.allowSelfGroupSwitching.booleanValue)
    clone.archived should be(source.archived)
    clone.assessmentGroups should be(assessmentGroups)
    clone.format should be (source.format)
    clone.groups.size should be(1)
    clone.groups.asScala.head should be(cloneGroup)
		clone.members should not be source.members
    clone.members.hasSameMembersAs(source.members) should be {true}
    clone.defaultTutors should not be source.defaultTutors
    clone.defaultTutors.hasSameMembersAs(source.defaultTutors) should be {true}
    clone.module should be (cloneModule)
    clone.name should be(source.name)
    clone.permissionsService should be(source.permissionsService)
    clone.releasedToStudents should be(source.releasedToStudents)
    clone.releasedToTutors should be (source.releasedToTutors)
  }

	@Test
	def duplicateWithNullDefaultTutors(){
		val source = new SmallGroupSet
		source.defaultTutors = null
		source.members.asInstanceOf[UserGroup].sessionFactory = sessionFactory
		val clone = source.duplicateTo(module = source.module, transient = true)
		clone.defaultTutors.size should be (0)
	}

	@Test
	def duplicateCreatesNewSettingsMap(){
		val source = new SmallGroupSet
		source.studentsCanSeeOtherMembers = true
		source.members.asInstanceOf[UserGroup].sessionFactory = sessionFactory
		source.defaultTutors.asInstanceOf[UserGroup].sessionFactory = sessionFactory
		val clone = source.duplicateTo(transient = true)
		clone.studentsCanSeeOtherMembers should be {true}
		source.studentsCanSeeOtherMembers = false
		clone.studentsCanSeeOtherMembers should be {true}
	}

	@Test
	def canGetAndSetTutorVisibility(){
		val set = new SmallGroupSet()
		set.studentsCanSeeTutorName should be{false}
		set.studentsCanSeeTutorName = true
		set.studentsCanSeeTutorName should be{true}
	}

	@Test
	def canGetAndSetOtherStudentVisibility(){
		val set = new SmallGroupSet()
		set.studentsCanSeeOtherMembers should be {false}
		set.studentsCanSeeOtherMembers  = true
		set.studentsCanSeeOtherMembers should be {true}
	}

	@Test
	def allowSelfGroupSwitchingDefaultsToTrue(){
		val set = new SmallGroupSet()
		set.allowSelfGroupSwitching should be {true}
	}

	@Test
	def nameWithoutModulePrefix() {
		val module = new Module()
		module.code = "LA101"
		val set = new SmallGroupSet()
		set.module = module

		set.name = "LA101 Seminars"
		set.nameWithoutModulePrefix should be ("Seminars")

		set.name = "some other seminars"
		set.nameWithoutModulePrefix should be ("some other seminars")
	}

}
