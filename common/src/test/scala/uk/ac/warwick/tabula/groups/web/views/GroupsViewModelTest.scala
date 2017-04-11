package uk.ac.warwick.tabula.groups.web.views

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.{Manual, StudentSignUp}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{StudentAssignedToGroup, StudentNotAssignedToGroup, ViewGroup, ViewSet}

class GroupsViewModelTest extends TestBase{

	@Test
	def viewSetReportsWhenViewerMustSignUp(){
		val set = new SmallGroupSet()
		set.allocationMethod = StudentSignUp
		set.openForSignups = true
		val group  =new SmallGroup

		val view = ViewSet(set, Seq(ViewGroup(group, Seq())), StudentNotAssignedToGroup)
		view.viewerMustSignUp should be (true)
	}

	@Test
	def studentsAlreadySignedUpNeedNotSignUp(){
		val set = new SmallGroupSet()
		set.allocationMethod = StudentSignUp
		set.openForSignups = true
		val group  =new SmallGroup

		val view = ViewSet(set, Seq(ViewGroup(group, Seq())), StudentAssignedToGroup)
		view.viewerMustSignUp should be (false)
	}

	@Test
	def studentsNeedNotSignToNonOpenGroup(){
		val set = new SmallGroupSet()
		set.allocationMethod = StudentSignUp
		set.openForSignups = false
		val group  =new SmallGroup

		val view = ViewSet(set, Seq(ViewGroup(group, Seq())), StudentNotAssignedToGroup)
		view.viewerMustSignUp should be (false)
	}

	@Test
	def studentsNeedNotSignToNonSelfSignUpGroup(){
		val set = new SmallGroupSet()
		set.allocationMethod = Manual
		set.openForSignups = true
		val group  =new SmallGroup

		val view = ViewSet(set, Seq(ViewGroup(group, Seq())), StudentNotAssignedToGroup)
		view.viewerMustSignUp should be (false)

	}

}
