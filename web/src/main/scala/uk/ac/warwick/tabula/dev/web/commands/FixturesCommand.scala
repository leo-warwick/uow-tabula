package uk.ac.warwick.tabula.dev.web.commands

import org.hibernate.criterion.Restrictions
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.permissions.GrantRoleCommand
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportAcademicInformationCommand
import uk.ac.warwick.tabula.commands.{Command, Description}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.{DepartmentalAdministratorRoleDefinition, UserAccessMgrRoleDefinition}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.services.scheduling.{DepartmentInfo, ModuleInfo}
import uk.ac.warwick.tabula.system.permissions.Public

import scala.collection.JavaConverters._
import scala.reflect._

/** This command is intentionally Public. It only exists on dev and is designed,
  * in essence, to blitz a department and set up some sample data in it.
  */
class FixturesCommand extends Command[Unit] with Public with Daoisms {
	import ImportAcademicInformationCommand._

	var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]
	var routeDao: RouteDao = Wire[RouteDao]
	var relationshipService: RelationshipService = Wire[RelationshipService]
	var scdDao: StudentCourseDetailsDao = Wire[StudentCourseDetailsDao]
	var memberDao: MemberDao = Wire[MemberDao]
	var attendanceMonitoringDao: AttendanceMonitoringDao = Wire[AttendanceMonitoringDao]
	var smallGroupService: SmallGroupService = Wire[SmallGroupService]
	var permissionsService: PermissionsService = Wire[PermissionsService]
	var feedbackForSitsService: FeedbackForSitsService = Wire[FeedbackForSitsService]
	var memberNoteService: MemberNoteService = Wire[MemberNoteService]

	def applyInternal() {
		benchmarkTask("setupDepartmentAndModules") {
			setupDepartmentAndModules()
		}

		val department = moduleAndDepartmentService.getDepartmentByCode(Fixtures.TestDepartment.code).get
		val subDept = moduleAndDepartmentService.getDepartmentByCode(Fixtures.TestSubDepartment.code).get
		val subSubDept = moduleAndDepartmentService.getDepartmentByCode(Fixtures.TestSubSubDepartment.code).get

		val module1 = moduleAndDepartmentService.getModuleByCode(Fixtures.TestModule1.code).get
		val module2 = moduleAndDepartmentService.getModuleByCode(Fixtures.TestModule2.code).get
		val module3 = moduleAndDepartmentService.getModuleByCode(Fixtures.TestModule3.code).get
		val module4 = moduleAndDepartmentService.getModuleByCode(Fixtures.TestModule4.code).get

		// Two department admins, first is a UserAccessManager
		val cmd = GrantRoleCommand(department)

		cmd.roleDefinition = UserAccessMgrRoleDefinition
		cmd.usercodes.add(Fixtures.TestAdmin1)
		cmd.apply()


		cmd.roleDefinition = DepartmentalAdministratorRoleDefinition
		cmd.usercodes.clear()
		cmd.usercodes.add(Fixtures.TestAdmin2)
		cmd.apply()

		// admin on the sub-department
		val subDepartmentAdminCommand = GrantRoleCommand(subDept)
		subDepartmentAdminCommand.roleDefinition = DepartmentalAdministratorRoleDefinition
		subDepartmentAdminCommand.usercodes.addAll(Seq(Fixtures.TestAdmin3).asJava)
		subDepartmentAdminCommand.apply()

		// admin on the sub-department;
		val subSubDepartmentAdminCommand = GrantRoleCommand(subSubDept)
		subSubDepartmentAdminCommand.roleDefinition = DepartmentalAdministratorRoleDefinition
		subSubDepartmentAdminCommand.usercodes.addAll(Seq(Fixtures.TestAdmin4).asJava)
		subSubDepartmentAdminCommand.apply()


		val upstreamAssignment = new AssessmentComponent
		upstreamAssignment.assessmentGroup = "A"
		upstreamAssignment.sequence = "A"
		upstreamAssignment.moduleCode = "XXX01-30"
		upstreamAssignment.module = module1
		upstreamAssignment.name = "Assignment from SITS"
		upstreamAssignment.assessmentType = AssessmentType.Assignment
		upstreamAssignment.inUse = true
		session.save(upstreamAssignment)

		val upstreamAssessmentGroup = new UpstreamAssessmentGroup
		upstreamAssessmentGroup.academicYear = new AcademicYear(new DateTime().getYear)
		upstreamAssessmentGroup.moduleCode = "XXX01-30"
		upstreamAssessmentGroup.assessmentGroup = "A"
		upstreamAssessmentGroup.occurrence = "A"
		session.save(upstreamAssessmentGroup)
	}

	private def setupDepartmentAndModules() {
		// Blitz members
		transactional() {
			sessionWithoutFreshFilters.newQuery("delete from StudentCourseYearDetails where studentCourseDetails.scjCode like '3000%'").executeUpdate()
			sessionWithoutFreshFilters.newQuery("delete from ModuleRegistration where studentCourseDetails.scjCode like '3000%'").executeUpdate()
			sessionWithoutFreshFilters.newQuery("delete from MemberStudentRelationship where studentCourseDetails.scjCode like '3000%'").executeUpdate()
			sessionWithoutFreshFilters.newQuery("delete from StudentCourseDetails where scjCode like '3000%'").executeUpdate()
			sessionWithoutFreshFilters.newQuery("delete from FileAttachment where member_note_id in (select id from MemberNote where memberId like '3000%')").executeUpdate()
			sessionWithoutFreshFilters.newQuery("delete from MemberNote where memberId like '3000%'").executeUpdate()
			sessionWithoutFreshFilters.newQuery("delete from StudentMember where universityId like '3000%'").executeUpdate()
			sessionWithoutFreshFilters.newQuery("delete from StaffMember where universityId like '3000%'").executeUpdate()
		}

		// Blitz the test department
		transactional() {
			moduleAndDepartmentService.getDepartmentByCode(Fixtures.TestDepartment.code) foreach { dept =>
				val routes: Seq[Route] = routeDao.findByDepartment(dept)

				val schemes = attendanceMonitoringDao.listAllSchemes(dept)

				for (scheme <- schemes) {
					for (point <- scheme.points.asScala){
						for (checkpoint <- attendanceMonitoringDao.getAllCheckpoints(point)){
							session.delete(checkpoint)
						}
					}
					// the points will also be deleted by the cascade
					session.delete(scheme)
				}

				for (total <- attendanceMonitoringDao.getAllCheckpointTotals(dept)) {
					session.delete(total)
				}
				session.flush()
				assert(attendanceMonitoringDao.listAllSchemes(dept).isEmpty)

				for (set <- smallGroupService.getAllSmallGroupSets(dept)) {
					session.delete(set)
				}
				session.flush()
				assert(smallGroupService.getAllSmallGroupSets(dept).isEmpty)

				for (staff <- memberDao.getStaffByDepartment(dept)) {
					session.delete(staff)
				}

				def invalidateAndDeletePermissions[A <: PermissionsTarget : ClassTag](scope: A) {
					// Invalidate any permissions or roles set
					val usersToInvalidate =
						permissionsService.getAllGrantedRolesFor(scope).flatMap { role => role.users.users } ++
						permissionsService.getAllGrantedPermissionsFor(scope).flatMap { perm => perm.users.users }

					usersToInvalidate.foreach { user =>
						permissionsService.clearCachesForUser((user.getUserId, classTag[A]))
					}

					permissionsService.getAllGrantedRolesFor(scope).foreach(session.delete)
					permissionsService.getAllGrantedPermissionsFor(scope).foreach(session.delete)
				}

				val modules = dept.modules

				for (module <- modules.asScala) {
					for (assignment <- module.assignments.asScala) {
						for (feedback <- assignment.feedbacks.asScala) {
							for (feedbackForSits <- feedbackForSitsService.getByFeedback(feedback)) {
								session.delete(feedbackForSits)
							}
						}
						triggerService.removeExistingTriggers(assignment)
						assignment.feedbacks.clear()
					}
				}
				session.flush()

				for (module <- modules.asScala) {
					for (assignment <- module.assignments.asScala) {
						session.delete(assignment)
					}
					module.assignments.clear()
				}
				session.flush()

				modules.asScala.foreach(invalidateAndDeletePermissions[Module])
				session.flush()
				assert(modules.asScala.forall(m => permissionsService.getAllGrantedRolesFor(m).isEmpty))

				modules.asScala.foreach(session.delete)

				dept.modules.clear()

				for (feedbackTemplate <- dept.feedbackTemplates.asScala) session.delete(feedbackTemplate)
				dept.feedbackTemplates.clear()

				for (markingWorkflow <- dept.markingWorkflows) {
					dept.removeMarkingWorkflow(markingWorkflow)
					session.delete(markingWorkflow)
				}

				routes.foreach(invalidateAndDeletePermissions[Route])
				routes.foreach(session.delete)
				dept.routes.clear()

				val children = recursivelyGetChildren(dept)
				children.foreach(invalidateAndDeletePermissions[Department])
				children.foreach(session.delete)
				dept.children.clear()

				invalidateAndDeletePermissions[Department](dept)
				session.delete(dept)

				session.flush()
				assert(moduleAndDepartmentService.getDepartmentByCode(Fixtures.TestDepartment.code).isEmpty)
				assert(sessionWithoutFreshFilters.createSQLQuery("select id from module where department_id not in (select id from department)").list.size() == 0)
				assert(sessionWithoutFreshFilters.createSQLQuery("select id from route where department_id not in (select id from department)").list.size() == 0)
				assert(sessionWithoutFreshFilters.createSQLQuery("select id from assignment where module_id not in (select id from module)").list.size() == 0)
				assert(sessionWithoutFreshFilters.createSQLQuery("select id from grantedrole where SCOPE_TYPE = 'Department' and SCOPE_ID not in (select id from department)").list.size() == 0)
				assert(sessionWithoutFreshFilters.createSQLQuery("select id from grantedrole where SCOPE_TYPE = 'Module' and SCOPE_ID not in (select id from module)").list.size() == 0)
				assert(sessionWithoutFreshFilters.createSQLQuery("select id from smallgroupset where module_id not in (select id from module)").list.size() == 0)
				assert(sessionWithoutFreshFilters.createSQLQuery("select id from attendancemonitoringtotal where department_id not in (select id from department)").list.size() == 0)
				assert(sessionWithoutFreshFilters.createSQLQuery(
					"""
						select scheduledtrigger.id from scheduledtrigger
						join entityreference on target_id = entityreference.id
						where scheduledtrigger.trigger_type = 'AssignmentClosed'
						and entityreference.entity_id not in (select id from assignment)
						and scheduledtrigger.completed_date is null
					""").list.size() == 0)
			}
		}

		session.flush()
		session.clear()

		def recursivelyGetChildren(department:Department): Set[Department] = {
			val descendents = department.children.asScala flatMap { recursivelyGetChildren }
			descendents.toSet ++ department.children.asScala
		}

		val department = newDepartmentFrom(Fixtures.TestDepartment, moduleAndDepartmentService)

		// Exam grids stuff
		department.uploadCourseworkMarksToSits = true

		// make sure we can see names, as uni ids are not exposed in the fixtures
		department.showStudentName = true
		transactional() {
			session.newQuery("delete from AssessmentComponent where moduleCode like :codePrefix")
				.setString("codePrefix", "XXX%")
				.executeUpdate()

			session.newCriteria[UpstreamAssessmentGroup]
				.add(Restrictions.like("moduleCode", "XXX%"))
				.seq
				.foreach { uag => session.delete(uag) }
		}

		// Import a new, better department
		transactional() {
			session.save(department)
		}
		// make sure the new parent department is flushed to the DB before we fetch it to create the child
		session.flush()

		val subDepartment = newDepartmentFrom(Fixtures.TestSubDepartment, moduleAndDepartmentService)
		transactional() {
			session.save(subDepartment)
		}
		val subSubDepartment = newDepartmentFrom(Fixtures.TestSubSubDepartment, moduleAndDepartmentService)
			transactional() {
				session.save(subSubDepartment)
		}

		// Setup some modules in the department, deleting anything existing
		val moduleInfos = Seq(Fixtures.TestModule1, Fixtures.TestModule2, Fixtures.TestModule3)

		transactional() {
			for (modInfo <- moduleInfos; module <- moduleAndDepartmentService.getModuleByCode(modInfo.code)) {
				 session.delete(module)
			}
			val module4 = moduleAndDepartmentService.getModuleByCode(Fixtures.TestModule4.code)
			module4 foreach session.delete
		}

		transactional() {
			for (modInfo <- moduleInfos)
				session.save(newModuleFrom(modInfo, department))
			session.save(newModuleFrom(Fixtures.TestModule4, subDepartment))
		}

		session.flush()
		session.clear()
	}

	def describe(d: Description) {}

}

object Fixtures {
	val TestDepartment = DepartmentInfo("Test Services", "Test Services", "xxx", null)
	val TestSubDepartment = DepartmentInfo("Test Services - Undergraduates", "Test Services - UG", "xxx-ug", null, Some("xxx"), Some("UG"))
	val TestSubSubDepartment = DepartmentInfo("Test Services - Freshers", "Test Services - Freshers", "xxx-ug1", null, Some("xxx-ug"), Some("UG,Y1"))

	val TestModule1 = ModuleInfo("Test Module 1", "xxx01", "xxx-xxx01", DegreeType.Undergraduate)
	val TestModule2 = ModuleInfo("Test Module 2", "xxx02", "xxx-xxx02", DegreeType.Undergraduate)
	val TestModule3 = ModuleInfo("Test Module 3", "xxx03", "xxx-xxx03", DegreeType.Undergraduate)
	val TestModule4 = ModuleInfo("Test Module 3","xxx04","xxx-ug-xxx-ug-104", DegreeType.Undergraduate)


	val TestAdmin1 = "tabula-functest-admin1"
	val TestAdmin2 = "tabula-functest-admin2"
	val TestAdmin3 = "tabula-functest-admin3"
	val TestAdmin4 = "tabula-functest-admin4"
}
