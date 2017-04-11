<#compress><#escape x as x?html>

<#import "/WEB-INF/freemarker/permissions_macros.ftl" as pm />
<#import "/WEB-INF/freemarker/formatters.ftl" as fmt />
<#assign perms_url><@routes.admin.routeperms route /></#assign>
<#assign route_name><@fmt.route_name route /></#assign>

<div id="route-permissions-page">
	<div class="pull-right btn-toolbar">
		<a class="btn btn-default" href="<@routes.admin.rolesDepartment route.adminDepartment />">About roles</a>
		<a class="btn btn-default" href="<@routes.admin.permissions route />">Advanced</a>
	</div>

	<div class="deptheader">
		<h1 class="with-settings">Route permissions</h1>
		<h5 class="with-related"><span class="muted">for</span> <#noescape>${route_name}</#noescape></h5>
	</div>

	<@pm.alerts "addCommand" route_name users role />

	<#assign scope=route />
	<#include "_roles.ftl" />
</div>

</#escape></#compress>