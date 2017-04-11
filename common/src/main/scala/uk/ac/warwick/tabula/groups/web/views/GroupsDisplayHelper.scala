package uk.ac.warwick.tabula.groups.web.views

import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel._
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

//
// Stateless functions to munge groupsets and groups
//
object GroupsDisplayHelper {
	// For each of the groupsets of which this user is a member, work out which of the constituent groups
	// should be displayed (which could be None, All, or "All groups the student is allocated to"), based
	// on the type and state of the groupset. Also return whether the student is allocated to the returned
	// groups or not.
	//
	// c.f rules outlined in TAB-922
	//
	// isTutor = true when viewing groups for another user

	def getGroupsToDisplay(set: SmallGroupSet, user:User, isTutor:Boolean = false): (Seq[SmallGroup], ViewerRole) = {
		val allGroupsInSet = set.groups.asScala
		val groupsStudentHasJoined = allGroupsInSet.filter(_.students.users.contains(user))

		def viewerRoleOrTutor(role: ViewerRole) =
			if(isTutor) Tutor
			else role

		groupsStudentHasJoined.toSeq match {
			case Nil if set.allocationMethod == StudentSignUp && !set.openForSignups =>
				(Nil,viewerRoleOrTutor(StudentNotAssignedToGroup))
			case Nil if set.allocationMethod == StudentSignUp =>
				(allGroupsInSet, viewerRoleOrTutor(StudentNotAssignedToGroup))
			case Nil =>
				(Nil,viewerRoleOrTutor(StudentNotAssignedToGroup))
			case x: Seq[SmallGroup] =>
				(x, viewerRoleOrTutor(StudentAssignedToGroup))
		}
	}


	// Given a list of groupsets (which a student is a member of), group them by module and then
	// return all the modules with at least one groupset with at least one group that needs
	// to be displayed to the student (@see getGroupsToDisplay); map the modules and groupsets to
	// ViewModules and ViewSets for rendering.

	def getViewModulesForStudent(
		memberGroupSets: Seq[SmallGroupSet],
		getDisplayGroups: (SmallGroupSet) => (Seq[SmallGroup], ViewerRole)
	): Seq[ViewModule] = {
		val memberGroupSetsWithApplicableGroups = memberGroupSets.map(set => (set, getDisplayGroups(set)))
			.filterNot{ case (s, (groups, role)) => groups.isEmpty}

		val memberViewModules =	memberGroupSetsWithApplicableGroups.groupBy(_._1.module).map{
			case(module,setData)=>
				val viewSets = setData.map{case(set,(groups,role))=>
					ViewSet(set, ViewGroup.fromGroups(groups.sorted), role)
				}
				ViewModule(module, viewSets, false)
		}
		memberViewModules.filterNot(m => m.setItems.isEmpty).toSeq
	}

	def getGroupSetsReleasedToStudents(memberGroupSets:Seq[SmallGroupSet]): Seq[SmallGroupSet] = memberGroupSets.filter(_.visibleToStudents)

}
