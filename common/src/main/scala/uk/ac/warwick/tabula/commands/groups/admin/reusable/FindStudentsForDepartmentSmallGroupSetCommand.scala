package uk.ac.warwick.tabula.commands.groups.admin.reusable

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringUserLookupComponent, ProfileServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

case class FindStudentsForDepartmentSmallGroupSetCommandResult(
	staticStudentIds: JList[String],
	membershipItems: Seq[DepartmentSmallGroupSetMembershipItem]
)

object FindStudentsForDepartmentSmallGroupSetCommand {
	def apply(department: Department, set: DepartmentSmallGroupSet) =
		new FindStudentsForDepartmentSmallGroupSetCommandInternal(department, set)
			with AutowiringProfileServiceComponent
			with AutowiringDeserializesFilterImpl
			with AutowiringUserLookupComponent
			with ComposableCommand[FindStudentsForDepartmentSmallGroupSetCommandResult]
			with PopulateFindStudentsForDepartmentSmallGroupSetCommand
			with UpdatesFindStudentsForDepartmentSmallGroupSetCommand
			with FindStudentsForDepartmentSmallGroupSetPermissions
			with FindStudentsForDepartmentSmallGroupSetCommandState
			with FiltersStudents
			with DeserializesFilter
			with Unaudited with ReadOnly
}

trait FindStudentsForDepartmentSmallGroupSetCommandFactory {
	def apply(department: Department, set: DepartmentSmallGroupSet): Appliable[FindStudentsForDepartmentSmallGroupSetCommandResult]
		with FindStudentsForDepartmentSmallGroupSetCommandState
		with PopulateOnForm
}

object FindStudentsForDepartmentSmallGroupSetCommandFactoryImpl
	extends FindStudentsForDepartmentSmallGroupSetCommandFactory {

	def apply(department: Department, set: DepartmentSmallGroupSet) =
		FindStudentsForDepartmentSmallGroupSetCommand(department, set)
}

class FindStudentsForDepartmentSmallGroupSetCommandInternal(val department: Department, val set: DepartmentSmallGroupSet)
	extends CommandInternal[FindStudentsForDepartmentSmallGroupSetCommandResult]
		with FindStudentsForDepartmentSmallGroupSetCommandState
		with TaskBenchmarking {

	self: FiltersStudents with ProfileServiceComponent with UserLookupComponent =>

	override def applyInternal(): FindStudentsForDepartmentSmallGroupSetCommandResult = {
		if (!doFind && (serializeFilter.isEmpty || findStudents.isEmpty)) {
			FindStudentsForDepartmentSmallGroupSetCommandResult(staticStudentIds, Seq())
		} else {
			doFind = true
			val year = set.academicYear

			staticStudentIds = benchmarkTask("profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments") {
				profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(
					department = department,
					restrictions = buildRestrictions(year),
					orders = buildOrders()
				).filter(userLookup.getUserByWarwickUniId(_).isFoundUser)
			}.asJava

			def toMembershipItem(universityId: String, itemType: DepartmentSmallGroupSetMembershipItemType) = {
				val user = userLookup.getUserByWarwickUniId(universityId)
				DepartmentSmallGroupSetMembershipItem(itemType, user.getFirstName, user.getLastName, user.getWarwickId, user.getUserId)
			}

			val startResult = studentsPerPage * (page-1)
			val staticMembershipItemsToDisplay =
				staticStudentIds.asScala.slice(startResult, startResult + studentsPerPage).map(toMembershipItem(_, DepartmentSmallGroupSetMembershipStaticType))

			val membershipItems: Seq[DepartmentSmallGroupSetMembershipItem] = {
				staticMembershipItemsToDisplay.map { item =>
					if (excludedStudentIds.asScala.contains(item.universityId))
						DepartmentSmallGroupSetMembershipItem(DepartmentSmallGroupSetMembershipExcludeType, item.firstName, item.lastName, item.universityId, item.userId)
					else if (includedStudentIds.asScala.contains(item.universityId))
						DepartmentSmallGroupSetMembershipItem(DepartmentSmallGroupSetMembershipIncludeType, item.firstName, item.lastName, item.universityId, item.userId)
					else
						item
				}
			}

			FindStudentsForDepartmentSmallGroupSetCommandResult(staticStudentIds, membershipItems)
		}
	}

}

trait PopulateFindStudentsForDepartmentSmallGroupSetCommand extends PopulateOnForm {

	self: FindStudentsForDepartmentSmallGroupSetCommandState with FiltersStudents with DeserializesFilter =>

	override def populate(): Unit = {
		staticStudentIds = set.members.knownType.staticUserIds.asJava
		includedStudentIds = set.members.knownType.includedUserIds.asJava
		excludedStudentIds = set.members.knownType.excludedUserIds.asJava
		filterQueryString = Option(set.memberQuery).getOrElse("")
		linkToSits = set.members.isEmpty || (set.memberQuery != null && set.memberQuery.nonEmpty)

		// Should only be true when editing
		if (linkToSits && set.members.knownType.members.nonEmpty) {
			doFind = true
		}

		// Default to current students
		if (!filterQueryString.hasText)
			allSprStatuses.find(_.code == "C").map(sprStatuses.add)
		else
			deserializeFilter(filterQueryString)
	}

}

trait UpdatesFindStudentsForDepartmentSmallGroupSetCommand {

	self: FindStudentsForDepartmentSmallGroupSetCommandState with FiltersStudents with DeserializesFilter =>

	def update(editSchemeMembershipCommandResult: EditDepartmentSmallGroupSetMembershipCommandResult): Any = {
		includedStudentIds = editSchemeMembershipCommandResult.includedStudentIds
		excludedStudentIds = editSchemeMembershipCommandResult.excludedStudentIds
		// Default to current students
		if (!filterQueryString.hasText)
			allSprStatuses.find(_.code == "C").map(sprStatuses.add)
		else
			deserializeFilter(filterQueryString)
	}

}

trait FindStudentsForDepartmentSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: FindStudentsForDepartmentSmallGroupSetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, department)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}
}

trait FindStudentsForDepartmentSmallGroupSetCommandState {
	def department: Department
	def set: DepartmentSmallGroupSet

	// Bind variables

	// Store original students for reset
	var includedStudentIds: JList[String] = LazyLists.create()
	var excludedStudentIds: JList[String] = LazyLists.create()
	var staticStudentIds: JList[String] = LazyLists.create()
	var filterQueryString: String = ""
	var linkToSits = true
	var findStudents: String = ""
	var doFind: Boolean = false

	// Filter properties
	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
	var sortOrder: JList[Order] = JArrayList()
	var page = 1
	def totalResults: Int = staticStudentIds.size
	val studentsPerPage = FiltersStudents.DefaultStudentsPerPage

	// Filter binds
	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()
}
