package uk.ac.warwick.tabula.commands.groups.admin

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.admin.ListAllSmallGroupSetsCommand._
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{ViewGroup, ViewSet}
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel

import scala.collection.JavaConverters._

case class ListAllSmallGroupSetsResult(
	total: Int,
	results: Int,
	maxResults: Int,
	firstResult: Int,
	academicYear: AcademicYear,
	sets: Seq[ViewSet]
)

object ListAllSmallGroupSetsResult {
	def apply(total: Int, sets: Seq[SmallGroupSet], request: ListAllSmallGroupSetsState): ListAllSmallGroupSetsResult =
		ListAllSmallGroupSetsResult(
			total = total,
			results = sets.length,
			maxResults = request.num,
			firstResult = request.skip,
			academicYear = request.academicYear,
			sets = sets.map { set => ViewSet(set, ViewGroup.fromGroups(set.groups.asScala.sorted), GroupsViewModel.Tutor) }
		)
}

object ListAllSmallGroupSetsCommand {
	type Result = ListAllSmallGroupSetsResult
	type Command = Appliable[Result] with SelfValidating

	def apply(): Command =
		new ListAllSmallGroupSetsCommandInternal()
			with ListAllSmallGroupSetsRequest
			with ComposableCommand[Result]
			with ListAllSmallGroupSetsPermissions
			with ListAllSmallGroupSetsValidation
			with ReadOnly with Unaudited
			with AutowiringSmallGroupServiceComponent
}

trait ListAllSmallGroupSetsState {
	def num: JInteger
	def skip: JInteger
	def academicYear: AcademicYear
}

trait ListAllSmallGroupSetsRequest extends ListAllSmallGroupSetsState {
	var num: JInteger = 100
	var skip: JInteger = 0
	var academicYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
}

abstract class ListAllSmallGroupSetsCommandInternal extends CommandInternal[Result] {
	self: SmallGroupServiceComponent with ListAllSmallGroupSetsState =>

	override def applyInternal(): Result =
		ListAllSmallGroupSetsResult(
			total = smallGroupService.countAllSmallGroupSets(academicYear),
			sets = smallGroupService.getAllSmallGroupSets(academicYear, num, skip),
			this
		)

}

trait ListAllSmallGroupSetsValidation extends SelfValidating {
	self: ListAllSmallGroupSetsState =>

	override def validate(errors: Errors): Unit = {
		if (num == null || num < 1) errors.rejectValue("num", "NotEmpty")
		if (skip == null || skip < 0) errors.rejectValue("skip", "NotEmpty")
		if (academicYear == null) errors.rejectValue("academicYear", "NotEmpty")
	}
}

trait ListAllSmallGroupSetsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.Module.ManageSmallGroups, PermissionsTarget.Global)
	}
}