package uk.ac.warwick.tabula.web.controllers.admin

import javax.servlet.http.HttpServletResponse
import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.admin.MasqueradeCommand
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Member, Route}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.tabula.services.{CourseAndRouteService, ModuleAndDepartmentService, ProfileService}
import uk.ac.warwick.tabula.web.Cookies._
import uk.ac.warwick.tabula.web.{Cookie, Mav, Routes}

import scala.collection.immutable.Iterable

@Controller
@RequestMapping(Array("/admin/masquerade"))
class MasqueradeController extends AdminController {

	var sandbox: Boolean = Wire.property("${spring.profiles.active}") == "sandbox"
	var departmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]
	var profileService: ProfileService = Wire[ProfileService]
	var routeService: CourseAndRouteService = Wire[CourseAndRouteService]

	validatesSelf[SelfValidating]

	type MasqueradeCommand = Appliable[Option[Cookie]]

	@ModelAttribute("masqueradeCommand") def command(): MasqueradeCommand = MasqueradeCommand(user)

	@RequestMapping(method = Array(HEAD, GET))
	def form(@ModelAttribute("masqueradeCommand") cmd: MasqueradeCommand): Mav = Mav("admin/masquerade/form").crumbs(Breadcrumbs.Current("Masquerade"))

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("masqueradeCommand") cmd: MasqueradeCommand, errors: Errors, response: HttpServletResponse): Mav = {
		if (errors.hasErrors) form(cmd)
		else {
			for (cookie <- cmd.apply()) response.addCookie(cookie)
			Redirect(Routes.admin.masquerade)
		}
	}

	@ModelAttribute("masqueradeDepartments") def masqueradeDepartments: Iterable[MasqueradeDepartment] with (Int with MasqueradeDepartment) => Boolean =
		if (!sandbox) Nil
		else {
			val realUser = new CurrentUser(user.realUser, user.realUser)

			departmentService.departmentsWithPermission(realUser, Permissions.Masquerade).map { department =>
				val sandboxDept = SandboxData.Departments(department.code)
				val students = sandboxDept.routes.values.toSeq.flatMap { route =>
					routeService.getRouteByCode(route.code).map { r =>
						(r,(route.studentsStartId until math.min(route.studentsStartId + 3, route.studentsEndId)).flatMap { id =>
								profileService.getMemberByUniversityId(id.toString, disableFilter = true).map { MasqueradableUser(_) }
							})
					}
				}
				val staff = (sandboxDept.staffStartId until math.min(sandboxDept.staffStartId + 10, sandboxDept.staffEndId)).flatMap { id =>
					profileService.getMemberByUniversityId(id.toString, disableFilter = true).map { MasqueradableUser(_) }
				}

				MasqueradeDepartment(department.name, department.code, students, staff)
			}
		}

	@ModelAttribute("returnTo") def returnTo: String = getReturnTo("/")

}

case class MasqueradeDepartment(
	name: String,
	code: String,
	students: Seq[(Route, Seq[MasqueradableUser])],
	staff: Seq[MasqueradableUser]
)

case class MasqueradableUser(
	userId: String,
	fullName: String
)
object MasqueradableUser {
	def apply(m: Member): MasqueradableUser = MasqueradableUser(m.userId, m.fullName.getOrElse(""))
}