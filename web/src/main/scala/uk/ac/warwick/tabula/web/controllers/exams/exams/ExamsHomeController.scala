package uk.ac.warwick.tabula.web.controllers.exams.exams

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{CurrentSITSAcademicYear, TaskBenchmarking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, AssessmentService, FeedbackService, ModuleAndDepartmentService}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController

@Controller
@RequestMapping(Array("/exams/exams"))
class ExamsHomeController extends ExamsController with CurrentSITSAcademicYear with TaskBenchmarking {

	@Autowired var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]
	@Autowired var assessmentService: AssessmentService = Wire[AssessmentService]
	@Autowired var examMembershipService: AssessmentMembershipService = _
	@Autowired var feedbackService: FeedbackService = _

	@RequestMapping
	def examsHome(@RequestParam(required = false) marked: Integer): Mav = {
		val ownedDepartments = benchmarkTask("Get owned departments") {
			moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Module.ManageAssignments)
		}

		val ownedModules = benchmarkTask("Get owned modules") {
			moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.ManageAssignments)
		}

		val examsForMarking = benchmarkTask("Get exams for marking") {
			assessmentService.getExamsWhereMarker(user.apparentUser).sortBy(_.module.code).map(exam => {
				val requiresMarking = examMembershipService.determineMembershipUsersWithOrderForMarker(exam, user.apparentUser)
				val feedbacks = feedbackService.getExamFeedbackMap(exam, requiresMarking.map(_._1)).values.toSeq.filter(_.latestMark.isDefined)
				exam -> (feedbacks, requiresMarking)
			})
		}.toMap

		Mav("exams/exams/home/view",
			"currentAcademicYear" -> academicYear,
			"examsForMarking" -> examsForMarking,
			"ownedDepartments" -> ownedDepartments,
			"ownedModuleDepartments" -> ownedModules.map { _.adminDepartment },
			"marked" -> marked
		)
	}
}
