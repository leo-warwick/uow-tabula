package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{CourseType, StudentRelationshipType, Department}
import uk.ac.warwick.tabula.services.{ AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent }
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod
import uk.ac.warwick.tabula.system.permissions.{ PermissionsChecking, RequiresPermissionsChecking }
import org.springframework.validation.{ BindingResult, Errors }
import uk.ac.warwick.tabula.system.BindListener
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.RelationshipServiceComponent
import uk.ac.warwick.tabula.services.AutowiringRelationshipServiceComponent

object DisplaySettingsCommand {
	def apply(department: Department) =
		new DisplaySettingsCommandInternal(department)
			with ComposableCommand[Department]
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringRelationshipServiceComponent
			with DisplaySettingsCommandDescription
			with DisplaySettingsCommandPermissions
}

trait DisplaySettingsCommandState {
	val department: Department
}

class DisplaySettingsCommandInternal(val department: Department) extends CommandInternal[Department] with PopulateOnForm
	with SelfValidating with BindListener with DisplaySettingsCommandState {

	this: ModuleAndDepartmentServiceComponent with RelationshipServiceComponent =>

	var showStudentName: Boolean = department.showStudentName
	var plagiarismDetection: Boolean = department.plagiarismDetectionEnabled
	var assignmentGradeValidation: Boolean = department.assignmentGradeValidation
	var turnitinExcludeBibliography: Boolean = department.turnitinExcludeBibliography
	var turnitinExcludeQuotations: Boolean = department.turnitinExcludeQuotations
	var turnitinExcludeSmallMatches: Boolean = _ // not saved as part of the settings - just used in the UI
	var turnitinSmallMatchWordLimit: Int = department.turnitinSmallMatchWordLimit
	var turnitinSmallMatchPercentageLimit: Int = department.turnitinSmallMatchPercentageLimit
	var assignmentInfoView: String = department.assignmentInfoView
	var weekNumberingSystem: String = department.weekNumberingSystem
	var autoGroupDeregistration: Boolean = department.autoGroupDeregistration
	var studentsCanScheduleMeetings: Boolean = department.studentsCanScheduleMeetings
	var defaultGroupAllocationMethod: String = department.defaultGroupAllocationMethod.dbValue
	var studentRelationshipDisplayed: JMap[String, JBoolean] =
		JHashMap(department.studentRelationshipDisplayed.map {
			case (id, bString) => id -> java.lang.Boolean.valueOf(bString)
		})
	var studentRelationshipExpected: JMap[StudentRelationshipType, JMap[CourseType, JBoolean]] =
		LazyMaps.create{_: StudentRelationshipType => JMap[CourseType, JBoolean]() }.asJava
	var autoMarkMissedMonitoringPoints: Boolean = department.autoMarkMissedMonitoringPoints

	def populate() {
		relationshipService.allStudentRelationshipTypes.foreach { relationshipType => {
			if (!studentRelationshipDisplayed.containsKey(relationshipType.id))
				studentRelationshipDisplayed.put(relationshipType.id, relationshipType.defaultDisplay)

			studentRelationshipExpected.put(relationshipType, JHashMap(
				Seq(CourseType.UG, CourseType.PGT, CourseType.PGR, CourseType.Foundation, CourseType.PreSessional).map(courseType =>
					courseType -> JBoolean(Option(department.getStudentRelationshipExpected(relationshipType, courseType)
						.getOrElse(relationshipType.isDefaultExpected(courseType))))
				):_*
			))
		}}
	}

	override def applyInternal(): Department = transactional() {
		department.showStudentName = showStudentName
		department.plagiarismDetectionEnabled = plagiarismDetection
		department.assignmentGradeValidation = assignmentGradeValidation
		department.turnitinExcludeBibliography = turnitinExcludeBibliography
		department.turnitinExcludeQuotations = turnitinExcludeQuotations
		department.turnitinSmallMatchWordLimit = turnitinSmallMatchWordLimit
		department.turnitinSmallMatchPercentageLimit = turnitinSmallMatchPercentageLimit
		department.assignmentInfoView = assignmentInfoView
		department.autoGroupDeregistration = autoGroupDeregistration
		department.studentsCanScheduleMeetings = studentsCanScheduleMeetings
		department.defaultGroupAllocationMethod = SmallGroupAllocationMethod(defaultGroupAllocationMethod)
		department.weekNumberingSystem = weekNumberingSystem
		department.studentRelationshipDisplayed = studentRelationshipDisplayed.asScala.map {
			case (id, bool) => id -> Option(bool).getOrElse(false).toString
		}.toMap
		studentRelationshipExpected.asScala.foreach{ case(relationshipType, courseTypeMap) =>
			courseTypeMap.asScala.foreach{ case(courseType, isExpected) =>
				department.setStudentRelationshipExpected(relationshipType, courseType, Option(isExpected).exists(_.booleanValue))
			}
		}
		department.autoMarkMissedMonitoringPoints = autoMarkMissedMonitoringPoints

		moduleAndDepartmentService.saveOrUpdate(department)
		department
	}

	override def onBind(result: BindingResult) {
		turnitinExcludeSmallMatches = turnitinSmallMatchWordLimit != 0 || turnitinSmallMatchPercentageLimit != 0
	}

	override def validate(errors: Errors) {
		if (turnitinSmallMatchWordLimit < 0) {
			errors.rejectValue("turnitinSmallMatchWordLimit", "department.settings.turnitinSmallMatchWordLimit")
		}

		if (turnitinSmallMatchPercentageLimit < 0 || turnitinSmallMatchPercentageLimit > 100) {
			errors.rejectValue("turnitinSmallMatchPercentageLimit", "department.settings.turnitinSmallMatchPercentageLimit")
		}

		if (turnitinSmallMatchWordLimit != 0 && turnitinSmallMatchPercentageLimit != 0) {
			errors.rejectValue("turnitinExcludeSmallMatches", "department.settings.turnitinSmallMatchSingle")
		}
	}
}

trait DisplaySettingsCommandPermissions extends RequiresPermissionsChecking {
	this: DisplaySettingsCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ManageDisplaySettings, department)
	}
}

trait DisplaySettingsCommandDescription extends Describable[Department] {
	this: DisplaySettingsCommandState =>
	// describe the thing that's happening.
	override def describe(d: Description): Unit =
		d.department(department)
}

