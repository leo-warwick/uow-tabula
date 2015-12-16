package uk.ac.warwick.tabula.commands.profiles.relationships

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.events.EventHandling
import uk.ac.warwick.tabula.services.{MaintenanceModeService, ProfileService, RelationshipService}

import scala.collection.JavaConverters._

class OldAllocateStudentsToRelationshipCommandTest extends TestBase with Mockito {

	EventHandling.enabled = false

	trait Environment {
		val relationshipService = smartMock[RelationshipService]
		val profileService = smartMock[ProfileService]

		val department = Fixtures.department("in", "IT Services")
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

		val student1 = Fixtures.student("0000001", "student1", department)
		val student2 = Fixtures.student("0000002", "student2", department)
		val student3 = Fixtures.student("0000003", "student3", department)
		val student4 = Fixtures.student("0000004", "student4", department)
		val student5 = Fixtures.student("0000005", "student5", department)
		val student6 = Fixtures.student("0000006", "student6", department)
		val student7 = Fixtures.student("0000007", "student7", department)

		val staff1 = Fixtures.staff("1000001", "staff1", department)
		val staff2 = Fixtures.staff("1000002", "staff2", department)
		val staff3 = Fixtures.staff("1000003", "staff3", department)
		val staff4 = Fixtures.staff("1000004", "staff4", department)

		val rel11 = StudentRelationship(staff1, relationshipType, student1)
		val rel12 = StudentRelationship(staff1, relationshipType, student2)
		val rel23 = StudentRelationship(staff2, relationshipType, student3)

		Seq(rel11, rel12, rel23).foreach { rel =>
			relationshipService.getCurrentRelationship(relationshipType, rel.studentMember.get, rel.agentMember.get) returns Some(rel)
		}

		Seq(staff1, staff2, staff3, staff4).foreach { staff =>
			profileService.getMemberByUniversityId(staff.universityId) returns Some(staff)
			profileService.getAllMembersWithUserId(staff.userId) returns Seq(staff)
		}

		Seq(student1, student2, student3, student4, student5, student6, student7).foreach { student =>
			profileService.getStudentBySprCode(student.universityId + "/1") returns Some(student)
			profileService.getMemberByUniversityId(student.universityId) returns Some(student)
			student.mostSignificantCourseDetails.get.department = department
		}

		relationshipService.listStudentRelationshipsByDepartment(relationshipType, department) returns Seq(rel11, rel12, rel23)
		relationshipService.listStudentsWithoutRelationship(relationshipType, department) returns Seq(student4, student5, student6, student7)

		relationshipService.findCurrentRelationships(relationshipType, student1.mostSignificantCourseDetails.get)	returns Seq(rel11)
		relationshipService.findCurrentRelationships(relationshipType, student2.mostSignificantCourseDetails.get)	returns Seq(rel12)
		relationshipService.findCurrentRelationships(relationshipType, student3.mostSignificantCourseDetails.get)	returns Seq(rel23)
		relationshipService.findCurrentRelationships(relationshipType, student4.mostSignificantCourseDetails.get)	returns Seq()
		relationshipService.findCurrentRelationships(relationshipType, student5.mostSignificantCourseDetails.get)	returns Seq()
		relationshipService.findCurrentRelationships(relationshipType, student6.mostSignificantCourseDetails.get)	returns Seq()
		relationshipService.findCurrentRelationships(relationshipType, student7.mostSignificantCourseDetails.get)	returns Seq()


		val maintenanceModeService = mock[MaintenanceModeService]
		maintenanceModeService.enabled returns false

		val cmd = new OldAllocateStudentsToRelationshipCommand(department, relationshipType, currentUser)
		cmd.relationshipService = relationshipService
		cmd.profileService = profileService
		cmd.maintenanceModeService = maintenanceModeService
		cmd.file.maintenanceModeService = maintenanceModeService
	}

	trait NoChangeScenario extends Environment {
		cmd.populate()
	}

	trait AddScenario extends Environment {
		cmd.populate()
		cmd.mapping.put(staff4, JList(student4))
		cmd.mapping.put(staff3, JList(student5))
	}

	trait RemoveScenario extends Environment {
		cmd.populate()
		cmd.mapping.get(staff2).removeAll(Seq(student3).asJavaCollection)
	}

	trait SwapScenario extends Environment {
		cmd.populate()
		cmd.mapping.get(staff2).removeAll(Seq(student3).asJavaCollection)
		cmd.mapping.get(staff1).addAll(Seq(student3).asJavaCollection)
	}

	trait MultipleRelationshipScenario extends Environment {
		val rel21 = StudentRelationship(staff2, relationshipType, student1)
		val rel41 = StudentRelationship(staff4, relationshipType, student1)

		relationshipService.listStudentRelationshipsByDepartment(relationshipType, department) returns Seq(rel11, rel12, rel23, rel21, rel41)
		relationshipService.findCurrentRelationships(relationshipType, student1.mostSignificantCourseDetails.get)	returns Seq(rel11, rel21, rel41)

		Seq(rel21, rel41).foreach { rel =>
			relationshipService.getCurrentRelationship(relationshipType, rel.studentMember.get, rel.agentMember.get) returns Some(rel)
		}

		cmd.populate()
		cmd.sort()
	}

	trait RemoveFromMultipleScenario extends MultipleRelationshipScenario {
		cmd.mapping.get(staff1).removeAll(Seq(student1).asJavaCollection)
	}

	trait AddToMultipleScenario extends MultipleRelationshipScenario {
		cmd.mapping.get(staff3).addAll(Seq(student1).asJavaCollection)
	}

	@Test def populatesData() = withUser("boombastic") {
		new Environment {
			cmd.unallocated should be(JList())
			cmd.mapping should be(JMap())

			cmd.populate()
			cmd.sort()

			cmd.unallocated should be(JList(student4, student5, student6, student7))
			cmd.mapping should be(JMap(staff1 -> JArrayList(student1, student2), staff2 -> JArrayList(student3)))

			cmd.additionalAgents = JArrayList(staff3.userId)
			cmd.onBind(new BindException(cmd, "cmd"))

			cmd.unallocated should be(JList(student4, student5, student6, student7))
			cmd.mapping should be(JMap(staff1 -> JArrayList(student1, student2), staff2 -> JArrayList(student3), staff3 -> JArrayList()))

			cmd.mapping.get(staff1).addAll(Seq(student4).asJavaCollection)
			cmd.mapping.get(staff3).addAll(Seq(student5, student7).asJavaCollection)

			cmd.sort()

			cmd.mapping should be(JMap(staff1 -> JArrayList(student1, student2, student4), staff2 -> JArrayList(student3), staff3 -> JArrayList(student5, student7)))
		}
	}

	@Test def validateStudentTutorMappingNoChanges() = withUser("boombastic") {
		new Environment {
			cmd.populate()
			cmd.studentTutorMapping.isEmpty should be {true}
		}
	}

	@Test def validateStateAddRel() = withUser("boombastic") {
		new AddScenario {

			cmd.studentTutorMapping.size should be (2)
			cmd.studentTutorMapping(student4).tutorsBefore should be (Set())
			cmd.studentTutorMapping(student4).tutorsAfter should be (Set(staff4))
			cmd.studentTutorMapping(student4).oldTutors should be (Set())
			cmd.studentTutorMapping(student4).newTutors should be (Set(staff4))

			cmd.studentTutorMapping(student5).tutorsBefore should be (Set())
			cmd.studentTutorMapping(student5).tutorsAfter should be (Set(staff3))
			cmd.studentTutorMapping(student5).oldTutors should be (Set())
			cmd.studentTutorMapping(student5).newTutors should be (Set(staff3))

			cmd.studentsWithTutorRemoved.size should be (0)
			cmd.studentsWithTutorAdded.size should be (2)
			cmd.studentsWithTutorChanged.size should be (0)
		}
	}

	@Test def validateStateRemoveRel() = withUser("boombastic") {
		new RemoveScenario {

			cmd.studentTutorMapping.size should be (1)
			cmd.studentTutorMapping(student3).tutorsBefore should be (Set(staff2))
			cmd.studentTutorMapping(student3).tutorsAfter should be (Set())
			cmd.studentTutorMapping(student3).oldTutors should be (Set(staff2))
			cmd.studentTutorMapping(student3).newTutors should be (Set())

			cmd.studentsWithTutorRemoved.size should be (1)
			cmd.studentsWithTutorAdded.size should be (0)
			cmd.studentsWithTutorChanged.size should be (0)

		}
	}

	@Test def validateStateSwapTutor() = withUser("boombastic") {
		new SwapScenario {
			cmd.studentTutorMapping.size should be (1)
			cmd.studentTutorMapping(student3).tutorsBefore should be (Set(staff2))
			cmd.studentTutorMapping(student3).tutorsAfter should be (Set(staff1))
			cmd.studentTutorMapping(student3).oldTutors should be (Set(staff2))
			cmd.studentTutorMapping(student3).newTutors should be (Set(staff1))

			cmd.studentsWithTutorRemoved.size should be (0)
			cmd.studentsWithTutorAdded.size should be (0)
			cmd.studentsWithTutorChanged.size should be (1)
		}
	}

	// student 1 has three tutors to start - we remove one and expect to find two left
	@Test def validateStateRemoveFromMultiple() = withUser("boombastic") {
		new RemoveFromMultipleScenario {
			cmd.studentTutorMapping.size should be (1)
			cmd.studentTutorMapping(student1).tutorsBefore should be (Set(staff1, staff2, staff4))
			cmd.studentTutorMapping(student1).tutorsAfter should be (Set(staff2, staff4))
			cmd.studentTutorMapping(student1).oldTutors should be (Set(staff1))
			cmd.studentTutorMapping(student1).newTutors should be (Set())

			cmd.studentsWithTutorRemoved.size should be (1)
			cmd.studentsWithTutorAdded.size should be (0)
			cmd.studentsWithTutorChanged.size should be (0)
		}
	}

	// student 1 has three tutors to start - we add a fourth
	@Test def validateStateAddToMultiple() = withUser("boombastic") {
		new AddToMultipleScenario {
			cmd.studentTutorMapping.size should be (1)
			cmd.studentTutorMapping(student1).tutorsBefore should be (Set(staff1, staff2, staff4))
			cmd.studentTutorMapping(student1).tutorsAfter should be (Set(staff1, staff2, staff3, staff4))
			cmd.studentTutorMapping(student1).oldTutors should be (Set())
			cmd.studentTutorMapping(student1).newTutors should be (Set(staff3))

			cmd.studentsWithTutorRemoved.size should be (0)
			cmd.studentsWithTutorAdded.size should be (1)
			cmd.studentsWithTutorChanged.size should be (0)
		}
	}

	@Test def validateEndCommandsNoChange() = withUser("boombastic") {
		new NoChangeScenario {
			cmd.getEndStudentRelationshipCommands.size should be (0)
		}
	}

	@Test def validateEndCommandsAdd() = withUser("boombastic") {
		new AddScenario {
			cmd.getEndStudentRelationshipCommands.size should be (0)
		}
	}

	@Test def validateEndCommandsRemove() = withUser("boombastic") {
		new RemoveScenario {
			val endCommands = cmd.getEndStudentRelationshipCommands
			endCommands.size should be (1)
			endCommands.head.relationship should be (rel23)
		}
	}

	@Test def validateEndCommandsSwap() = withUser("boombastic") {
		new SwapScenario {
			val endCommands = cmd.getEndStudentRelationshipCommands
			endCommands.size should be (0)
		}
	}

	@Test def validateEndCommandsRemoveMulti() = withUser("boombastic") {
		new RemoveFromMultipleScenario {
			val endCommands = cmd.getEndStudentRelationshipCommands
			endCommands.size should be (1)
			endCommands.head.relationship should be (rel11)

		}
	}

	@Test def validateEndCommandsAddMulti() = withUser("boombastic") {
		new AddToMultipleScenario  {
			cmd.getEndStudentRelationshipCommands.size should be (0)
		}
	}

	@Test def validateEditCommandsNoChange() = withUser("boombastic") {
		new NoChangeScenario {
			cmd.getEditStudentRelationshipCommands.size should be (0)
		}
	}

	@Test def validateEditCommandsAdd() = withUser("boombastic") {
		new AddScenario {
			val editCommands = cmd.getEditStudentRelationshipCommands
			editCommands.size should be (2)

			editCommands.exists(editCommand =>
				editCommand.agent == staff4 &&
				editCommand.studentCourseDetails == student4.mostSignificantCourseDetails.get &&
				editCommand.oldAgents.isEmpty
			) should be { true }

			editCommands.exists(editCommand =>
				editCommand.agent == staff3 &&
					editCommand.studentCourseDetails == student5.mostSignificantCourseDetails.get &&
					editCommand.oldAgents.isEmpty
			) should be { true }
		}
	}

	@Test def validateEditCommandsRemove() = withUser("boombastic") {
		new RemoveScenario {
			cmd.getEditStudentRelationshipCommands.size should be (0)
		}
	}

	@Test def validateEditCommandsSwap() = withUser("boombastic") {
		new SwapScenario {
			val editCommand = cmd.getEditStudentRelationshipCommands.head

			editCommand.agent should be(staff1)
			editCommand.studentCourseDetails should be(student3.mostSignificantCourseDetails.get)
			editCommand.oldAgents should be (Seq(staff2))
		}
	}

	@Test def validateEditCommandsRemoveMulti() = withUser("boombastic") {
		new RemoveFromMultipleScenario {
			cmd.getEditStudentRelationshipCommands.size should be (0)
		}
	}

	@Test def validateEditCommandsAddMulti() = withUser("boombastic") {
		new AddToMultipleScenario  {
			val editCommand = cmd.getEditStudentRelationshipCommands.head

			editCommand.agent should be (staff3)
			editCommand.studentCourseDetails should be (student1.mostSignificantCourseDetails.get)
			editCommand.oldAgents.isEmpty should be {true}

		}
	}

}
