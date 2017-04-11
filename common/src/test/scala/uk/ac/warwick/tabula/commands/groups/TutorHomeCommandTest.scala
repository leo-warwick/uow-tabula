package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.services.{SmallGroupService, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}

class TutorHomeCommandTest extends TestBase with Mockito {

	@Test def commandWorks() {
		withUser("cusebr") {
			val department = new Department
			val academicYear = AcademicYear(2015)
			val groups = Seq(new SmallGroup, new SmallGroup)
			val set = new SmallGroupSet
			set.academicYear = academicYear
			set.releasedToTutors = true

			val module = new Module
			module.adminDepartment = department
			set.module = module
			for (group <- groups) group.groupSet = set

			val command = new TutorHomeCommandInternal(currentUser, academicYear) with SmallGroupServiceComponent {
				override val smallGroupService: SmallGroupService = smartMock[SmallGroupService]
			}

			command.smallGroupService.findReleasedSmallGroupsByTutor(currentUser) returns groups

			val result = command.applyInternal()
			result should be (Map(module -> Map(set -> groups)))
		}
	}

}
