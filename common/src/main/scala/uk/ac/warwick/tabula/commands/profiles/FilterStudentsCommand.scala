package uk.ac.warwick.tabula.commands.profiles

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order._
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{CourseType, Department, ModeOfAttendance, Module, Route, SitsStatus, StudentMember}
import uk.ac.warwick.tabula.data.{AliasAndJoinType, ScalaRestriction}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

case class FilterStudentsResults(
	students: Seq[StudentMember],
	totalResults: Int
)

object FilterStudentsCommand {
	def apply(department: Department, year: AcademicYear) =
		new FilterStudentsCommand(department, year)
			with ComposableCommand[FilterStudentsResults]
			with FilterStudentsPermissions
			with AutowiringProfileServiceComponent
			with ReadOnly with Unaudited
}

abstract class FilterStudentsCommand(val department: Department, val year: AcademicYear)
	extends CommandInternal[FilterStudentsResults] with FilterStudentsState with BindListener with TaskBenchmarking {
	self: ProfileServiceComponent =>

	def applyInternal(): FilterStudentsResults = {
		val restrictions = buildRestrictions(year)

		val totalResults = benchmarkTask("countStudentsByRestrictions") { profileService.countStudentsByRestrictions(
			department = department,
			academicYear = year,
			restrictions = restrictions
		)}

		val (offset, students) = benchmarkTask("findStudentsByRestrictions") { profileService.findStudentsByRestrictions(
			department = department,
			academicYear = year,
			restrictions = restrictions,
			orders = buildOrders(),
			maxResults = studentsPerPage,
			startResult = studentsPerPage * (page-1)
		)}

		if (offset == 0) page = 1

		FilterStudentsResults(students, totalResults)
	}

	def onBind(result: BindingResult) {
		// Add all non-withdrawn codes to SPR statuses by default
		if (!hasBeenFiltered) {
			allSprStatuses.filter { status => !status.code.startsWith("P") && !status.code.startsWith("T") }.foreach { sprStatuses.add }
		}
	}
}

trait FilterStudentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: FilterStudentsState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.Search, department)
	}
}

trait FilterStudentsState extends ProfileFilterExtras {
	override def department: Department

	var studentsPerPage = FiltersStudents.DefaultStudentsPerPage
	var page = 1

	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
	var sortOrder: JList[Order] = JArrayList()

	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()

	var hasBeenFiltered = false
}

trait ProfileFilterExtras extends FiltersStudents {

	final val HAS_ADMIN_NOTE = "Has administrative note"

	override lazy val allOtherCriteria: Seq[String] = Seq(
		"Tier 4 only",
		"Visiting",
		"Enrolled for year or course completed",
		HAS_ADMIN_NOTE
	)

	override def getAliasPaths(table: String): Seq[(String, AliasAndJoinType)] = {
		(FiltersStudents.AliasPaths ++ Map(
			"memberNotes" -> Seq(
				"memberNotes" -> AliasAndJoinType("memberNotes")
			)
		))(table)
	}

	override def buildRestrictions(year: AcademicYear): Seq[ScalaRestriction] = {
		super.buildRestrictions(year) ++ Seq(hasAdminNoteRestriction).flatten
	}

	def hasAdminNoteRestriction: Option[ScalaRestriction] = otherCriteria.contains(HAS_ADMIN_NOTE) match {
		case false => None
		case true => ScalaRestriction.notEmpty(
			"memberNotes",
			getAliasPaths("memberNotes"): _*
		)
	}
}