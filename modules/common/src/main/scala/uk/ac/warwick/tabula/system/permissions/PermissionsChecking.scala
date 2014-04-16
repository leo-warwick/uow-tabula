package uk.ac.warwick.tabula.system.permissions

import org.springframework.util.Assert
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{CurrentUser, PermissionDeniedException, ItemNotFoundException}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.permissions.Permission
import scala.reflect.ClassTag
import uk.ac.warwick.tabula.services.SecurityService
import scala.collection.mutable

/**
 * Trait that allows classes to call ActionCheck() in their inline definitions
 * (i.e. on construction). These are then evaluated on bind.
 */
trait PermissionsChecking extends PermissionsCheckingMethods  {
	
	type PermissionsCheckMultiMap = mutable.HashMap[Permission, mutable.Set[Option[PermissionsTarget]]]
		with mutable.MultiMap[Permission, Option[PermissionsTarget]]
	
	private def newMap(): PermissionsCheckMultiMap = new mutable.HashMap[Permission, mutable.Set[Option[PermissionsTarget]]]
		with mutable.MultiMap[Permission, Option[PermissionsTarget]]
	
	var permissionsAnyChecks: PermissionsCheckMultiMap = newMap()
	var permissionsAllChecks: PermissionsCheckMultiMap = newMap()

	def PermissionCheckAny(checkablePermissions: Iterable[CheckablePermission]) {
		for (p <- checkablePermissions) checkAny(p.permission, p.scope)
	}

	def PermissionCheckAll(permission: Permission, scopes: Iterable[PermissionsTarget]) {
		for (scope <- scopes) checkAll(permission, Some(scope))
	}

	def PermissionCheck(scopelessPermission: ScopelessPermission) {
		checkAll(scopelessPermission, None)
	}

	def PermissionCheck(permission: Permission, scope: PermissionsTarget) {
		checkAll(permission, Some(scope))
	}

	private def checkAny(permission: Permission, scope: Option[PermissionsTarget]) {
		permissionsAnyChecks.addBinding(permission, scope)
	}

	private def checkAll(permission: Permission, scope: Option[PermissionsTarget]) {
		permissionsAllChecks.addBinding(permission, scope)
	}
}

trait Public extends PermissionsChecking

trait PermissionsCheckingMethods extends Logging {
	def mustBeLinked(module: Module, department: Department) =
		if (mandatory(module).department.id != mandatory(department).id) {
			logger.info("Not displaying module as it doesn't belong to specified department")
			throw new ItemNotFoundException(module)
		}
	
	def mustBeLinked(assignment: Assignment, module: Module) =
		if (mandatory(assignment).module.id != mandatory(module).id) {
			logger.info("Not displaying assignment as it doesn't belong to specified module")
			throw new ItemNotFoundException(assignment)
		}
	
	def mustBeLinked(set: SmallGroupSet, module: Module) =
		if (mandatory(set).module.id != mandatory(module).id) {
			logger.info("Not displaying small group set as it doesn't belong to specified module")
			throw new ItemNotFoundException(set)
		}

	def mustBeLinked(feedback: Feedback, assignment: Assignment) =
		if (mandatory(feedback).assignment.id != mandatory(assignment).id) {
			logger.info("Not displaying feedback as it doesn't belong to specified assignment")
			throw new ItemNotFoundException(feedback)
		}

	def mustBeLinked(markingWorkflow: MarkingWorkflow, department: Department) =
		if (mandatory(markingWorkflow).department.id != mandatory(department.id)) {
			logger.info("Not displaying marking workflow as it doesn't belong to specified department")
			throw new ItemNotFoundException(markingWorkflow)
		}

	def mustBeLinked(template: FeedbackTemplate, department: Department) =
		if (mandatory(template).department.id != mandatory(department.id)) {
			logger.info("Not displaying feedback template as it doesn't belong to specified department")
			throw new ItemNotFoundException(template)
		}

  def mustBeLinked(submission: Submission, assignment: Assignment) =
    if (mandatory(submission).assignment.id != mandatory(assignment).id) {
      logger.info("Not displaying submission as it doesn't belong to specified assignment")
      throw new ItemNotFoundException(submission)
    }

	def mustBeLinked(memberNote: MemberNote, member: Member) =
		if (mandatory(memberNote).member.id != mandatory(member).id) {
			logger.info("Not displaying member note as it doesn't belong to specified member")
			throw new ItemNotFoundException(memberNote)
		}

	/**
	 * Returns an object if it is non-null and not None. Otherwise
	 * it throws an ItemNotFoundException, which should get picked
	 * up by an exception handler to display a 404 page.
	 */
	def mandatory[A : ClassTag](something: A): A = something match {
		case thing: A => thing
		case _ => throw new ItemNotFoundException()
	}
	/**
	 * Pass in an Option and receive either the actual value, or
	 * an ItemNotFoundException is thrown.
	 */
	def mandatory[A : ClassTag](option: Option[A]): A = option match {
		case Some(thing: A) => thing
		case _ => throw new ItemNotFoundException()
	}

	def notDeleted[A <: CanBeDeleted](entity: A): A =
		if (entity.deleted) throw new ItemNotFoundException()
		else entity

	/**
	 * Checks target.permissionsAllChecks for ANDed permission, then target.permissionsAnyChecks for ORed permissions.
	 * Throws PermissionDeniedException if permissions are unmet or ItemNotFoundException (-> 404) if scope is missing.
	 */
	def permittedByChecks(securityService: SecurityService, user: CurrentUser, target: PermissionsChecking) {
		Assert.isTrue(
			!target.permissionsAnyChecks.isEmpty || !target.permissionsAllChecks.isEmpty || target.isInstanceOf[Public],
			"Bind target " + target.getClass + " must specify permissions or extend Public"
		)

		// securityService.check() throws on *any* missing permission
		for (check <- target.permissionsAllChecks; scope <- check._2) (check._1, scope) match {
			case (permission: Permission, Some(scope)) => securityService.check(user, permission, scope)
			case (permission: ScopelessPermission, _) => securityService.check(user, permission)
			case _ =>
				logger.warn("Permissions check throwing item not found - this should be caught in command (" + target + ")")
				throw new ItemNotFoundException()
		}

		// securityService.can() wrapped in exists() only throws if no perms match
		if (!target.permissionsAnyChecks.isEmpty && !target.permissionsAnyChecks.exists { check => check._2 exists { scope => (check._1, scope) match {
			case (permission: Permission, Some(scope)) => securityService.can(user, permission, scope)
			case (permission: ScopelessPermission, _) => securityService.can(user, permission)
			case _ => {
				logger.warn("Permissions check throwing item not found - this should be caught in command (" + target + ")")
				throw new ItemNotFoundException()
			}
		}}}) throw new PermissionDeniedException(user, target.permissionsAnyChecks.head._1, target.permissionsAnyChecks.head._2)
	}
}
trait RequiresPermissionsChecking{
	def permissionsCheck(p:PermissionsChecking):Unit
}
trait PubliclyVisiblePermissions extends RequiresPermissionsChecking with Public{
	def permissionsCheck(p:PermissionsChecking){}
}
trait PerformsPermissionsChecking extends PermissionsChecking{
	this: RequiresPermissionsChecking=>
	permissionsCheck(this)
}