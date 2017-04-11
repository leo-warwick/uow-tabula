package uk.ac.warwick.tabula.commands.timetables

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.FilterStudentsOrRelationships
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentServiceComponent, ProfileServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.TimetableEventType
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, ItemNotFoundException}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.{Failure, Success}

object DepartmentTimetableEventsHelper {
	val RequiredPermission = Permissions.Module.ViewTimetable
	val DraftPermission = Permissions.Timetabling.ViewDraft
}

trait DepartmentTimetableEventsState {
	def user: CurrentUser
	def department: Department
}

trait DepartmentTimetableEventsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DepartmentTimetableEventsState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(DepartmentTimetableEventsHelper.RequiredPermission, mandatory(department))
	}
}

trait DraftDepartmentTimetableEventsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DepartmentTimetableEventsState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(DepartmentTimetableEventsHelper.DraftPermission, mandatory(department))
	}
}

trait DepartmentTimetableEventsRequest extends PermissionsCheckingMethods with TimetableEventsRequest {

	self: DepartmentTimetableEventsState with ProfileServiceComponent with RelationshipServiceComponent =>

	var modules: JList[Module] = JArrayList()
	var routes: JList[Route] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var students: JList[String] = JArrayList()
	lazy val studentMembers: Seq[StudentMember] = profileService.getAllMembersWithUniversityIds(students.asScala).flatMap {
		case student: StudentMember => Option(student)
		case _ => None
	}
	var staff: JList[String] = JArrayList()
	lazy val staffMembers: Seq[StaffMember] = profileService.getAllMembersWithUniversityIds(staff.asScala).flatMap {
		case staffMember: StaffMember => Option(staffMember)
		case _ => None
	}
	var eventTypes: JList[TimetableEventType] = JArrayList()

	private def modulesForDepartmentAndSubDepartments(department: Department): Seq[Module] =
		(department.modules.asScala ++ department.children.asScala.flatMap { modulesForDepartmentAndSubDepartments }).sorted

	lazy val allModules: Seq[Module] = ((modulesForDepartmentAndSubDepartments(mandatory(department)) match {
		case Nil => modulesForDepartmentAndSubDepartments(mandatory(department.rootDepartment))
		case someModules => someModules
	}) ++ modules.asScala).distinct.sorted

	private def routesForDepartmentAndSubDepartments(department: Department): Seq[Route] =
		(department.routes.asScala ++ department.children.asScala.flatMap { routesForDepartmentAndSubDepartments }).sorted

	lazy val allRoutes: Seq[Route] = ((routesForDepartmentAndSubDepartments(mandatory(department)) match {
		case Nil => routesForDepartmentAndSubDepartments(mandatory(department.rootDepartment))
		case someRoutes => someRoutes
	}) ++ routes.asScala).distinct.sorted(Route.DegreeTypeOrdering)

	lazy val allYearsOfStudy: Seq[Int] = 1 to FilterStudentsOrRelationships.MaxYearsOfStudy

	lazy val suggestedStaff: Seq[StaffMember] = (
		staffMembers ++
		user.profile.collect { case m: StaffMember => m }.toSeq
	).distinct.sortBy { m => (m.lastName, m.firstName) }

	lazy val personalTutorRelationshipType: StudentRelationshipType = relationshipService.getStudentRelationshipTypeByUrlPart("tutor").getOrElse(
		throw new ItemNotFoundException("Could not find personal tutor relationship type")
	)

	lazy val suggestedStudents: Seq[StudentMember] = (
		studentMembers ++
		user.profile.collect { case m: StudentMember => m }.toSeq ++
		relationshipService.listCurrentRelationshipsWithAgent(personalTutorRelationshipType, user.universityId).flatMap { _.studentMember }
	).distinct.sortBy { m => (m.lastName, m.firstName) }

	lazy val allEventTypes: Seq[TimetableEventType] = {
		val r = TimetableEventType.members
		if (r.contains(null)) {
			logger.error(s"Somehow some of TimetableEventType.members are null. The not-null members are: ${r.filter(_ != null).map(_.code).mkString(", ")}. Skipping the null members...")
		}
		r.filter(_ != null)
	}

	var showTimetableEvents: Boolean = true
	var showSmallGroupEvents: Boolean = true

}

trait GetsModuleEvents {

	self: ModuleAndDepartmentServiceComponent with DepartmentTimetableEventsRequest
		with DepartmentTimetableEventsState =>

	def getModuleEvents(
		academicYear: AcademicYear,
		moduleTimetableCommandFactory: ViewModuleTimetableCommandFactory,
		errors: mutable.Buffer[String]
	): TimetableFetchingService.EventList = {
		val routesModules = moduleAndDepartmentService.findModulesByRoutes(routes.asScala, academicYear)
		val yearOfStudyModules = moduleAndDepartmentService.findModulesByYearOfStudy(department, yearsOfStudy.asScala, academicYear)

		val queryModules = (
			modules.asScala ++ routesModules ++ yearOfStudyModules
			).distinct.filter { module =>
			(modules.isEmpty || modules.contains(module)) &&
				(routes.isEmpty || routesModules.contains(module)) &&
				(yearsOfStudy.isEmpty || yearOfStudyModules.contains(module))
		}

		val moduleCommands = queryModules.map(module => module -> moduleTimetableCommandFactory.apply(module, user))
		EventList.combine(moduleCommands.map { case (module, cmd) =>
			cmd.academicYear = academicYear
			cmd.showTimetableEvents = showTimetableEvents
			cmd.showSmallGroupEvents = showSmallGroupEvents
			cmd.apply() match {
				case Success(events) =>
					events
				case Failure(_) =>
					errors.append(s"Unable to load timetable for ${module.code.toUpperCase}")
					EventList.empty
			}
		}).map(_.distinct)
	}
}
