package uk.ac.warwick.tabula.data.model.permissions

import org.hibernate.annotations.Type
import javax.persistence._
import javax.persistence.CascadeType._
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import scala.reflect._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSet, SmallGroup, SmallGroupEvent}

@Entity
@Access(AccessType.FIELD)
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
		name="scope_type",
		discriminatorType=DiscriminatorType.STRING
)
abstract class GrantedPermission[A <: PermissionsTarget] extends GeneratedId with HibernateVersioned with PostLoadBehaviour {
	import GrantedPermission.OverrideType

	@OneToOne(cascade=Array(CascadeType.ALL), fetch = FetchType.EAGER)
	@JoinColumn(name="usergroup_id")
	private var _users: UserGroup = UserGroup.ofUsercodes
	def users: UnspecifiedTypeUserGroup = _users

	@Type(`type` = "uk.ac.warwick.tabula.data.model.permissions.PermissionUserType")
	var permission: Permission = _

	/*
	 * TODO Deny not currently supported by SecurityService because the value isn't passed through!
	 */
	var overrideType: OverrideType = _

	var scope: A

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
object GrantedPermission {
	type OverrideType = Boolean
	val Allow: OverrideType = true
	val Deny: OverrideType = false

	def apply[A <: PermissionsTarget](scope: A, permission: Permission, overrideType: OverrideType): GrantedPermission[A] =
		(scope match {
			case dept: Department => new DepartmentGrantedPermission(dept, permission, overrideType)
			case module: Module => new ModuleGrantedPermission(module, permission, overrideType)
			case route: Route => new RouteGrantedPermission(route, permission, overrideType)
			case member: Member => new MemberGrantedPermission(member, permission, overrideType)
			case assignment: Assignment => new AssignmentGrantedPermission(assignment, permission, overrideType)
			case group: SmallGroup => new SmallGroupGrantedPermission(group, permission, overrideType)
			case groupset: SmallGroupSet => new SmallGroupSetGrantedPermission(groupset, permission, overrideType)
			case event: SmallGroupEvent => new SmallGroupEventGrantedPermission(event, permission, overrideType)
			case _ => throw new IllegalArgumentException("Cannot define new permissions for " + scope)
		}).asInstanceOf[GrantedPermission[A]]

	def canDefineFor[A <: PermissionsTarget](scope: A): OverrideType = scope match {
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
		case t if isSubtype(t, classTag[Department]) => classOf[DepartmentGrantedPermission]
		case t if isSubtype(t, classTag[Module]) => classOf[ModuleGrantedPermission]
		case t if isSubtype(t, classTag[Route]) => classOf[RouteGrantedPermission]
		case t if isSubtype(t, classTag[Member]) => classOf[MemberGrantedPermission]
		case t if isSubtype(t, classTag[Assignment]) => classOf[AssignmentGrantedPermission]
		case t if isSubtype(t, classTag[SmallGroup]) => classOf[SmallGroupGrantedPermission]
		case t if isSubtype(t, classTag[SmallGroupSet]) => classOf[SmallGroupSetGrantedPermission]
		case t if isSubtype(t, classTag[SmallGroupEvent]) => classOf[SmallGroupEventGrantedPermission]
		case _ => classOf[GrantedPermission[_]]
	}

  private def isSubtype[A,B](self: ClassTag[A], other: ClassTag[B]) = other.runtimeClass.isAssignableFrom(self.runtimeClass)

	def className[A <: PermissionsTarget : ClassTag]: String = classObject[A].getSimpleName
	def discriminator[A <: PermissionsTarget : ClassTag]: Option[String] =
		Option(classObject[A].getAnnotation(classOf[DiscriminatorValue])) map { _.value }
}

/* Ok, this is icky, but I can't find any other way. If you need new targets for GrantedPermissions, create them below with a new discriminator */
@Entity @DiscriminatorValue("___GLOBAL___") class GloballyGrantedPermission extends GrantedPermission[PermissionsTarget] {
	def this(permission: Permission, overrideType: GrantedPermission.OverrideType) = {
		this()
		this.permission = permission
		this.overrideType = overrideType
	}

	@transient var scope: PermissionsTarget = null
}

@Entity @DiscriminatorValue("Department") class DepartmentGrantedPermission extends GrantedPermission[Department] {
	def this(department: Department, permission: Permission, overrideType: GrantedPermission.OverrideType) = {
		this()
		this.scope = department
		this.permission = permission
		this.overrideType = overrideType
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: Department = _
}
@Entity @DiscriminatorValue("Module") class ModuleGrantedPermission extends GrantedPermission[Module] {
	def this(module: Module, permission: Permission, overrideType: GrantedPermission.OverrideType) = {
		this()
		this.scope = module
		this.permission = permission
		this.overrideType = overrideType
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: Module = _
}
@Entity @DiscriminatorValue("Route") class RouteGrantedPermission extends GrantedPermission[Route] {
	def this(route: Route, permission: Permission, overrideType: GrantedPermission.OverrideType) = {
		this()
		this.scope = route
		this.permission = permission
		this.overrideType = overrideType
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: Route = _
}
@Entity @DiscriminatorValue("Member") class MemberGrantedPermission extends GrantedPermission[Member] {
	def this(member: Member, permission: Permission, overrideType: GrantedPermission.OverrideType) = {
		this()
		this.scope = member
		this.permission = permission
		this.overrideType = overrideType
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: Member = _
}
@Entity @DiscriminatorValue("Assignment") class AssignmentGrantedPermission extends GrantedPermission[Assignment] {
	def this(assignment: Assignment, permission: Permission, overrideType: GrantedPermission.OverrideType) = {
		this()
		this.scope = assignment
		this.permission = permission
		this.overrideType = overrideType
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: Assignment = _
}
@Entity @DiscriminatorValue("SmallGroup") class SmallGroupGrantedPermission extends GrantedPermission[SmallGroup] {
	def this(group: SmallGroup, permission: Permission, overrideType: GrantedPermission.OverrideType) = {
		this()
		this.scope = group
		this.permission = permission
		this.overrideType = overrideType
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: SmallGroup = _
}
@Entity @DiscriminatorValue("SmallGroupSet") class SmallGroupSetGrantedPermission extends GrantedPermission[SmallGroupSet] {
	def this(groupset: SmallGroupSet, permission: Permission, overrideType: GrantedPermission.OverrideType) = {
		this()
		this.scope = groupset
		this.permission = permission
		this.overrideType = overrideType
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: SmallGroupSet = _
}
@Entity @DiscriminatorValue("SmallGroupEvent") class SmallGroupEventGrantedPermission extends GrantedPermission[SmallGroupEvent] {
	def this(event: SmallGroupEvent, permission: Permission, overrideType: GrantedPermission.OverrideType) = {
		this()
		this.scope = event
		this.permission = permission
		this.overrideType = overrideType
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: SmallGroupEvent = _
}