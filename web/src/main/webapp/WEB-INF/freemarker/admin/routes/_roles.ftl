<#import "/WEB-INF/freemarker/permissions_macros.ftl" as pm />

<#escape x as x?html>
	<div class="row">
		<div class="col-md-6">
			<#assign popover>
				<p>A route manager can manage monitoring points for a route and record which students have attended or missed points.</p>
			</#assign>

			<h3 class="permissionTitle">Route Managers <@fmt.help_popover id="routemanager" title="Route Managers" content="${popover}" html=true /></h3>

			<@pm.roleTable perms_url "routemanager-table" scope "RouteManagerRoleDefinition" "Route Manager" />
		</div>

		<div class="col-md-6">
			<#assign popover>
				<p>A route assistant can record whether students have attended or missed a monitoring point, but can't change the monitoring point schemes.</p>
			</#assign>

			<h3 class="permissionTitle">Route Assistants <@fmt.help_popover id="routeassistants" title="Route Assistants" content="${popover}" html=true /></h3>

			<@pm.roleTable perms_url "routeassistant-table" scope "RouteAssistantRoleDefinition" "Route Assistant" />
		</div>
	</div>

	<div class="row">
		<div class="col-md-6">
			<#assign popover>
				<p>A route auditor can view which students have met or missed monitoring points.</p>
			</#assign>

			<h3 class="permissionTitle">Route Auditors <@fmt.help_popover id="routeauditors" title="Route Auditors" content="${popover}" html=true /></h3>

			<@pm.roleTable perms_url "routeauditor-table" scope "RouteAuditorRoleDefinition" "Route Auditor" />
		</div>
	</div>
</#escape>