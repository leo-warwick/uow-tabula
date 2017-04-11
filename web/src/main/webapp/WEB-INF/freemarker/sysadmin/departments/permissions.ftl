<#compress><#escape x as x?html>

<#import "/WEB-INF/freemarker/permissions_macros.ftl" as pm />
<#assign deptperms_url><@url page="/sysadmin/departments/${department.code}/permissions" /></#assign>

<div class="permissions-page">
	<div class="deptheader">
		<h1>Departmental permissions</h1>
		<h5 class="with-related">for ${department.name}</h5>
	</div>

	<@pm.alerts "addCommand" department.name users role />

	<div class="row">
		<div class="col-md-6">
			<#assign popover>
				<p>A departmental administrator has overarching responsibility for a department and everything in it.</p>
				<p>It includes most rights except for some PDP functions, which are restricted to senior tutors and supervisors.</p>
				<p>Note also that departmental admins for sub-departments get <b>no</b> automatic rights over the parent department.</p>
			</#assign>

			<h3 class="permissionTitle">Departmental admins <@fmt.help_popover id="deptadmins" title="Departmental admins" content="${popover}" html=true /></h3>

			<@pm.roleTable deptperms_url "deptadmin-table" department "DepartmentalAdministratorRoleDefinition" "Departmental Administrator" />
		</div>
		<div class="col-md-6">
			<#assign popover>
				<p>A User Access Manager is the person responsible for delegating access rights over a department in Tabula.</p>
				<p>The User Access Manager role can view all data in Tabula and assign all levels of permissions to staff in the department.
				   This includes assigning the role of Senior Tutor/Supervisor. User Access Managers can assign Departmental Administrators as well as nominate other User Access Managers.</p>
			</#assign>

			<h3 class="permissionTitle">Departmental User Access Manager <@fmt.help_popover id="uam" title="Departmental admins" content="${popover}" html=true /></h3>

			<@pm.roleTable deptperms_url "deptuam-table" department "UserAccessMgrRoleDefinition" "User Access Manager" />
		</div>

	</div>
</div>

</#escape></#compress>
