package uk.ac.warwick.tabula.web.controllers.exams

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/exams"))
class ExamsComponentHomeController extends ExamsController {

	@Autowired var moduleAndDepartmentService: ModuleAndDepartmentService = _
	@Autowired var features: Features = _

	@RequestMapping
	def home: Mav = {
		val homeDepartment = moduleAndDepartmentService.getDepartmentByCode(user.apparentUser.getDepartmentCode)
		val examsEnabled = features.exams && user.isStaff && homeDepartment.exists(_.uploadExamMarksToSits)
		val examGridsEnabled = features.examGrids && user.isStaff

		if (examsEnabled && !examGridsEnabled) {
			Redirect(Routes.Exams.home)
		} else if (!examsEnabled && examGridsEnabled) {
			Redirect(Routes.Grids.home)
		} else {
			Mav("exams/home",
				"examsEnabled" -> examsEnabled,
				"examGridsEnabled" -> examGridsEnabled
			).secondCrumbs(
				(examsEnabled match {
					case true => Seq(ExamsBreadcrumbs.Exams.Home)
					case false => Nil
				}) ++
				(examGridsEnabled match {
					case true => Seq(ExamsBreadcrumbs.Grids.Home)
					case false => Nil
				}): _*
			)
		}
	}

}
