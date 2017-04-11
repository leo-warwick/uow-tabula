package uk.ac.warwick.tabula.commands.coursework.marksmanagement

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{DegreeType, Department}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringTermServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._

sealed abstract class DepartmentState (val value: String)
case object DepartmentStateClosed extends DepartmentState("closed")
case object DepartmentStateThisYearOnly extends DepartmentState("openCurrent")
case object DepartmentStateThisYearAndLastYear extends DepartmentState("openCurrentAndPrevious")

object OpenAndCloseDepartmentsCommand {
	def apply() =
		new OpenAndCloseDepartmentsCommandInternal
		with ComposableCommand[DegreeType]
		with AutowiringTermServiceComponent
		with AutowiringModuleAndDepartmentServiceComponent
		with PopulateOpenAndCloseDepartmentsCommand
		with OpenAndCloseDepartmentsCommandState
		with OpenAndCloseDepartmentsCommandPermissions
		with OpenAndCloseDepartmentsCommandDescription
}

class OpenAndCloseDepartmentsCommandInternal extends CommandInternal[DegreeType] {

	self: OpenAndCloseDepartmentsCommandState with ModuleAndDepartmentServiceComponent =>

	def applyInternal(): DegreeType = {
		if (updatePostgrads){
			updateDepartments(pgMappings, DegreeType.Postgraduate)
			DegreeType.Postgraduate
		} else {
			updateDepartments(ugMappings, DegreeType.Undergraduate)
			DegreeType.Undergraduate
		}
	}

	private def updateDepartments(mapping: JMap[String, String], degreeType: DegreeType ) {
		mapping.asScala.foreach {
			case (key, value) => {
				val dept: Department = moduleAndDepartmentService.getDepartmentByCode(key).get
				dept.setUploadMarksToSitsForYear(currentAcademicYear, degreeType, thisYearOpen(value))
				dept.setUploadMarksToSitsForYear(previousAcademicYear, degreeType, lastYearOpen(value))
			}
		}
	}

	private def thisYearOpen(deptStateValue: String) = deptStateValue != DepartmentStateClosed.value
	private def lastYearOpen(deptStateValue: String) = deptStateValue ==  DepartmentStateThisYearAndLastYear.value

}

trait OpenAndCloseDepartmentsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Marks.MarksManagement)
	}
}

trait OpenAndCloseDepartmentsCommandDescription extends Describable[DegreeType] {

	self: OpenAndCloseDepartmentsCommandState =>

	def describe(d: Description): Unit = d.properties(
		"degreeType" -> {
			if (updatePostgrads) DegreeType.Postgraduate
			else DegreeType.Undergraduate
		}
	)
}

trait OpenAndCloseDepartmentsCommandState {
	self: TermServiceComponent with ModuleAndDepartmentServiceComponent =>

	lazy val currentAcademicYear: AcademicYear = AcademicYear.findAcademicYearContainingDate(DateTime.now)
	lazy val previousAcademicYear: AcademicYear = currentAcademicYear.previous

	lazy val departments: Seq[Department] = moduleAndDepartmentService.allRootDepartments
	var ugMappings: JMap[String, String] = JHashMap()
	var pgMappings: JMap[String, String] = JHashMap()

	var updatePostgrads: Boolean = _
}

trait PopulateOpenAndCloseDepartmentsCommand extends PopulateOnForm {

	self: OpenAndCloseDepartmentsCommandState =>

	override def populate(): Unit = {

		ugMappings = departments.map {d =>
			d.code -> getState(d, DegreeType.Undergraduate).value
		}.toMap.asJava
		pgMappings = departments.map {d =>
			d.code -> getState(d, DegreeType.Postgraduate).value
		}.toMap.asJava

	}

	def getState(department: Department, degreeType: DegreeType): DepartmentState = {

		val canUploadThisYear = department.canUploadMarksToSitsForYear(currentAcademicYear, degreeType)
		val canUploadLastYear = department.canUploadMarksToSitsForYear(previousAcademicYear, degreeType)

		if (canUploadThisYear && canUploadLastYear) DepartmentStateThisYearAndLastYear
		else if (canUploadThisYear) DepartmentStateThisYearOnly
		else DepartmentStateClosed

	}
}
