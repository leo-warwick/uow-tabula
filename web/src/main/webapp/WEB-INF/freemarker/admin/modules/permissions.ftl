<#compress><#escape x as x?html>

<#import "/WEB-INF/freemarker/permissions_macros.ftl" as pm />
<#import "/WEB-INF/freemarker/formatters.ftl" as fmt />
<#assign perms_url><@routes.admin.moduleperms module /></#assign>
<#assign module_name><@fmt.module_name module /></#assign>

<div class="permissions-page">
	<div class="pull-right">
		<a class="btn btn-default" href="<@routes.admin.rolesDepartment module.adminDepartment />">About roles</a>
		<a class="btn btn-default" href="<@routes.admin.permissions module />">Advanced</a>
	</div>

	<div class="deptheader">
		<h1 class="with-settings">Module permissions</h1>
		<h5 class="with-related"><span class="muted">for</span> <#noescape>${module_name}</#noescape></h5>
	</div>
	<@pm.alerts "addCommand" module_name users role />

	<#assign scope=module />
	<#include "_roles.ftl" />
</div>

</#escape></#compress>