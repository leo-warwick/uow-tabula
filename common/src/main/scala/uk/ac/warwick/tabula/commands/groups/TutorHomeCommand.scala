package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

object TutorHomeCommand {
	def apply(tutor: CurrentUser, academicYear: AcademicYear) =
		new TutorHomeCommandInternal(tutor, academicYear)
			with AutowiringSmallGroupServiceComponent
			with Command[Map[Module, Map[SmallGroupSet, Seq[SmallGroup]]]]
			with ReadOnly with Unaudited
			with Public
}


class TutorHomeCommandInternal(tutor: CurrentUser, academicYear: AcademicYear)
	extends CommandInternal[Map[Module, Map[SmallGroupSet, Seq[SmallGroup]]]] {

	self: SmallGroupServiceComponent =>

	override def applyInternal(): Map[Module, Map[SmallGroupSet, Seq[SmallGroup]]] = {
		smallGroupService.findReleasedSmallGroupsByTutor(tutor)
			.groupBy { group => group.groupSet }
			.filterKeys(_.academicYear == academicYear)
			.groupBy { case (set, groups) => set.module }
	}

}

