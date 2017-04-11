package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, AssessmentType}
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class AssessmentComponentCreationFixtureCommandInternal extends CommandInternal[AssessmentComponent] {
	self: AssessmentMembershipServiceComponent with ModuleAndDepartmentServiceComponent with TransactionalComponent =>

	var moduleCode: String = _
	var assessmentGroup = "A"
	var sequence = "A01"
	var departmentCode: String = _
	var assessmentType = AssessmentType.Assignment
	var name: String = _
	var inUse: Boolean = true

	def applyInternal(): AssessmentComponent = transactional() {
		val ac = new AssessmentComponent
		ac.moduleCode = moduleCode
		ac.module = moduleAndDepartmentService.getModuleByCode(ac.moduleCodeBasic.toLowerCase()).get
		ac.assessmentGroup = assessmentGroup
		ac.sequence = sequence
		ac.assessmentType = assessmentType
		ac.name = name
		ac.inUse = inUse

		assessmentMembershipService.save(ac)
	}

}

object AssessmentComponentCreationFixtureCommand {
	def apply(): AssessmentComponentCreationFixtureCommandInternal with ComposableCommand[AssessmentComponent] with AutowiringAssessmentMembershipServiceComponent with AutowiringModuleAndDepartmentServiceComponent with AutowiringTransactionalComponent with Unaudited with PubliclyVisiblePermissions ={
		new AssessmentComponentCreationFixtureCommandInternal
			with ComposableCommand[AssessmentComponent]
			with AutowiringAssessmentMembershipServiceComponent
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringTransactionalComponent
			with Unaudited
			with PubliclyVisiblePermissions
	}
}