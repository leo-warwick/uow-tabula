package uk.ac.warwick.tabula.data.model.permissions

import org.hibernate.annotations.Type
import javax.persistence._
import javax.persistence.CascadeType._

import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.{BuiltInRoleDefinition, Role, RoleBuilder, RoleDefinition}

import scala.reflect._
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent, SmallGroupSet}
import uk.ac.warwick.tabula.roles.RoleBuilder.GeneratedRole

@Entity
@Access(AccessType.FIELD)
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
		name="scope_type",
		discriminatorType=DiscriminatorType.STRING
)
abstract class GrantedRole[A <: PermissionsTarget] extends GeneratedId with HibernateVersioned with PostLoadBehaviour {

	@OneToOne(cascade=Array(CascadeType.ALL), fetch = FetchType.EAGER)
	@JoinColumn(name="usergroup_id")
	private var _users: UserGroup = UserGroup.ofUsercodes
	def users: UnspecifiedTypeUserGroup = _users

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "custom_role_id")
	var customRoleDefinition: CustomRoleDefinition = _

	@Type(`type` = "uk.ac.warwick.tabula.data.model.permissions.BuiltInRoleDefinitionUserType")
	var builtInRoleDefinition: BuiltInRoleDefinition = _

	def roleDefinition: RoleDefinition = Option(customRoleDefinition) getOrElse builtInRoleDefinition
	def roleDefinition_= (definition: RoleDefinition): Unit = definition match {
		case customDefinition: CustomRoleDefinition =>
			customRoleDefinition = customDefinition
			builtInRoleDefinition = null
		case builtInDefinition: BuiltInRoleDefinition =>
			customRoleDefinition = null
			builtInRoleDefinition = builtInDefinition
		case _ =>
			customRoleDefinition = null
			builtInRoleDefinition = null
	}

	/**
	 * The scope of the GrantedRole is what the permissions contained within are granted against,
	 * which is a PermissionsTarget.
	 */
	var scope: A

	// this ought not to be necessary, but for some reason the compiler fails to see the type bound on scope and won't
	// assume it's a permissions target
	def scopeAsPermissionsTarget:PermissionsTarget = scope

	/**
	 * Build a Role from this definition
	 */
	def build(): GeneratedRole = RoleBuilder.build(replaceableRoleDefinition, Option(scope), replaceableRoleDefinition.getName)
	def mayGrant(target: Permission): Boolean = Option(replaceableRoleDefinition).fold(false) { _.mayGrant(target) }

	/**
	 * Provides a route to Department from the scope, so that we can look for custom definitions.
	 *
	 * In almost all cases, Department will be one of the permissionsParents of the scope (maybe multiple
	 * levels up), but providing a direct link here means we don't have to iterate up the tree.
	 */
	def scopeDepartment: Option[Department]

	def replaceableRoleDefinition: RoleDefinition = scopeDepartment.flatMap { _.replacedRoleDefinitionFor(roleDefinition) }.getOrElse(roleDefinition)

	// If hibernate sets users to null, make a new empty usergroup
	override def postLoad() {
		ensureUsers
	}

	def ensureUsers: UnspecifiedTypeUserGroup = {
		if (users == null) _users = UserGroup.ofUsercodes
		users
	}

}

// TODO DRY this out
object GrantedRole {
	def apply[A <: PermissionsTarget](scope: A, definition: RoleDefinition): GrantedRole[A] =
		(scope match {
			case dept: Department => new DepartmentGrantedRole(dept, definition)
			case module: Module => new ModuleGrantedRole(module, definition)
			case route: Route => new RouteGrantedRole(route, definition)
			case member: Member => new MemberGrantedRole(member, definition)
			case assignment: Assignment => new AssignmentGrantedRole(assignment, definition)
			case group: SmallGroup => new SmallGroupGrantedRole(group, definition)
			case smallGroupSet: SmallGroupSet => new SmallGroupSetGrantedRole(smallGroupSet, definition)
			case event: SmallGroupEvent => new SmallGroupEventGrantedRole(event, definition)
			case _ => throw new IllegalArgumentException("Cannot define new roles for " + scope)
		}).asInstanceOf[GrantedRole[A]]

	def canDefineFor[A <: PermissionsTarget](scope: A): Boolean = scope match {
		case _: Department => true
		case _: Module => true
		case _: Route => true
		case _: Member => true
		case _: Assignment => true
		case _: SmallGroup => true
		case _: SmallGroupSet => true
		case _: SmallGroupEvent => true
		case _ => false
	}

	def classObject[A <: PermissionsTarget : ClassTag] = classTag[A] match {
		case t if isSubtype(t, classTag[Department]) => classOf[DepartmentGrantedRole]
		case t if isSubtype(t, classTag[Module]) => classOf[ModuleGrantedRole]
		case t if isSubtype(t, classTag[Route]) => classOf[RouteGrantedRole]
		case t if isSubtype(t, classTag[Member]) => classOf[MemberGrantedRole]
		case t if isSubtype(t, classTag[Assignment]) => classOf[AssignmentGrantedRole]
		case t if isSubtype(t, classTag[SmallGroup]) => classOf[SmallGroupGrantedRole]
		case t if isSubtype(t, classTag[SmallGroupSet]) => classOf[SmallGroupSetGrantedRole]
		case t if isSubtype(t, classTag[SmallGroupEvent]) => classOf[SmallGroupEventGrantedRole]
		case _ => classOf[GrantedRole[_]]
	}

  private def isSubtype[A,B](self: ClassTag[A], other: ClassTag[B]) = other.runtimeClass.isAssignableFrom(self.runtimeClass)

  def className[A <: PermissionsTarget : ClassTag]: String = classObject[A].getSimpleName
	def discriminator[A <: PermissionsTarget : ClassTag]: Option[String] =
		Option(classObject[A].getAnnotation(classOf[DiscriminatorValue])) map { _.value }
}

/* Ok, this is icky, but I can't find any other way. If you need new targets for GrantedRoles, create them below with a new discriminator */
@Entity @DiscriminatorValue("___GLOBAL___") class GloballyGrantedRole extends GrantedRole[PermissionsTarget] {
	def this(definition: RoleDefinition) = {
		this()
		this.roleDefinition = definition
	}

	@transient var scope: PermissionsTarget = null

	def scopeDepartment = None

	override def build(): GeneratedRole = RoleBuilder.build(GlobalRoleDefinition(replaceableRoleDefinition), None, replaceableRoleDefinition.getName)
}

/**
 * Wrap a normal RoleDefinition to allow us to make permissions that aren't allowed to be global, global.
 */
case class GlobalRoleDefinition(delegate: RoleDefinition) extends RoleDefinition {
	def permissions(scope: Option[PermissionsTarget]): Map[Permission, Option[PermissionsTarget]] =
		delegate.permissions(Some(null)).map {
			case (perm, Some(null)) => (perm, None)
			case (perm, s) => (perm, s)
		}

	def subRoles(scope: Option[PermissionsTarget]): Set[Role] = delegate.subRoles(scope)
	def mayGrant(permission: Permission): Boolean = delegate.mayGrant(permission)
	def allPermissions(scope: Option[PermissionsTarget]): Map[Permission, Option[PermissionsTarget]] = delegate.allPermissions(scope)
	def canDelegateThisRolesPermissions: _root_.uk.ac.warwick.tabula.JavaImports.JBoolean = delegate.canDelegateThisRolesPermissions
	def getName: String = delegate.getName
	def description: String = delegate.description
	def isAssignable: Boolean = delegate.isAssignable
}

@Entity @DiscriminatorValue("Department") class DepartmentGrantedRole extends GrantedRole[Department] {
	def this(department: Department, definition: RoleDefinition) = {
		this()
		this.scope = department
		this.roleDefinition = definition
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: Department = _

	def scopeDepartment = Some(scope)
}
@Entity @DiscriminatorValue("Module") class ModuleGrantedRole extends GrantedRole[Module] {
	def this(module: Module, definition: RoleDefinition) = {
		this()
		this.scope = module
		this.roleDefinition = definition
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: Module = _

	def scopeDepartment = Some(scope.adminDepartment)
}
@Entity @DiscriminatorValue("Route") class RouteGrantedRole extends GrantedRole[Route] {
	def this(route: Route, definition: RoleDefinition) = {
		this()
		this.scope = route
		this.roleDefinition = definition
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: Route = _

	def scopeDepartment = Some(scope.adminDepartment)
}
@Entity @DiscriminatorValue("Member") class MemberGrantedRole extends GrantedRole[Member] {
	def this(member: Member, definition: RoleDefinition) = {
		this()
		this.scope = member
		this.roleDefinition = definition
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: Member = _

	def scopeDepartment: Option[Department] = scope match {
		case student: StudentMember =>
			student.mostSignificantCourseDetails.flatMap {
				_.latestStudentCourseYearDetails.enrolmentDepartment.subDepartmentsContaining(student).lastOption
			}.orElse(Option(student.homeDepartment).flatMap(_.subDepartmentsContaining(student).lastOption))
		case _ => Option(scope.homeDepartment)
	}
}
@Entity @DiscriminatorValue("Assignment") class AssignmentGrantedRole extends GrantedRole[Assignment] {
	def this(assignment: Assignment, definition: RoleDefinition) = {
		this()
		this.scope = assignment
		this.roleDefinition = definition
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: Assignment = _

	def scopeDepartment = Some(scope.module.adminDepartment)
}
@Entity @DiscriminatorValue("SmallGroup") class SmallGroupGrantedRole extends GrantedRole[SmallGroup] {
	def this(group: SmallGroup, definition: RoleDefinition) = {
		this()
		this.scope = group
		this.roleDefinition = definition
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: SmallGroup = _

	def scopeDepartment = Some(scope.groupSet.module.adminDepartment)
}
@Entity @DiscriminatorValue("SmallGroupSet") class SmallGroupSetGrantedRole extends GrantedRole[SmallGroupSet] {
	def this(groupset: SmallGroupSet, definition: RoleDefinition) = {
		this()
		this.scope = groupset
		this.roleDefinition = definition
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: SmallGroupSet = _

	def scopeDepartment = Some(scope.module.adminDepartment)
}
@Entity @DiscriminatorValue("SmallGroupEvent") class SmallGroupEventGrantedRole extends GrantedRole[SmallGroupEvent] {
	def this(event: SmallGroupEvent, definition: RoleDefinition) = {
		this()
		this.scope = event
		this.roleDefinition = definition
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: SmallGroupEvent = _

	def scopeDepartment = Some(scope.group.groupSet.module.adminDepartment)
}