Permissions
===========

Most entities in Tabula implement the `PermissionsTarget` trait. This identifies that permissions can be granted
and checked against them. Part of the `PermissionsTarget` contract includes the `permissionsParents` method,
which returns the direct parents of this entity - the implication is that if you have permissions on a parent,
you will always have those same permissions on any entity which declares that parent, and this is called
iteratively.

For example:

```
  +--------------+
  |              |
  | Assignment 1 +----+
  |              |    |    +---------------+
  +--------------+    |    |               |
                      +---->  Module 1     +----+
                      |    |               |    |
  +--------------+    |    +---------------+    |    +-----------------+
  |              |    |                         |    |                 |
  | Assignment 2 +----+                         +---->   Department    |
  |              |    |                         |    |                 |
  +--------------+    |    +---------------+    |    +-----------------+
                      |    |               |    |
                      +---->  Module 2     +----+
  +--------------+    |    |               |
  |              |    |    +---------------+
  | Assignment 3 +----+
  |              |
  +--------------+
```

When Tabula wants to check whether you have a permission (say, `Assignment.Update`) on a particular assignment,
it'll ask this of the `SecurityService`. This generates all the permissions that the current user has on that
particular `Assignment`, which will go through each of the `RoleProvider`s registered centrally. Each of these
`Role`s have a set of `Permission`, each with a `PermissionsTarget` as its scope. The `DatabaseBackedPermissionsProvider`
is also checked for any explicitly granted (or revoked) permissions.

If this set of `Permission` contains `Assignment.Update` with this `Assignment` as the scope or with the `__GLOBAL__`
scope, then the permissions are granted. If not, the `permissionsParents` of the `Assignment` are evaluated
(returning just the `Module` in this case) and then the process is repeated with the new scope, now looking for
`Assignment.Update` on the `Module`. Eventually, the `permissionsParents` are exhausted (e.g. the `Department`
returns an empty set of parents) and the permissions check fails.

There are some optimisations here; for example, some `RoleProvider`s are `ScopelessRoleProvider`s, which are only
evaluated once as their results don't change based on the scope that's passed in. The most important `RoleProvider`s
are the `DatabaseBackedRoleProvider`, which return `GrantedRole`s, and `UserTypeAndDepartmentRoleProvider` which returns
`StaffRole` if you're a member of staff, for example.

DatabaseBackedRoleProvider
--------------------------

Explicitly granted individual permissions are stored in the `GrantedPermission` table
(see `uk.ac.warwick.tabula.data.model.permissions.GrantedPermission`).

Tabula has its own concept of `UserGroup`s with which granted permissions are associated. Group members can either be
inherited from a base WebGroup (in which case they will update) or defined within Tabula's database (via `UserGroupInclude`/`UserGroupExclude`).

Explicit permissions are normally manually maintained via the DB for e.g. external users used by other web applications. Some of these external users
require wide-ranging (i.e. `PermissionsTarget.Global` target) permissions to perform their function.

Granted roles work in much the same way as described, but they are most commonly assigned via the Tabula UI.

Permissions helper
------------------

A "Permissions helper" tool is provided at the top of the Sysadmin screen which shows a breakdown of why a user is
(or isn't) allowed to perform an action. 
