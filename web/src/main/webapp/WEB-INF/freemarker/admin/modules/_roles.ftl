<#import "/WEB-INF/freemarker/permissions_macros.ftl" as pm />
<#import "/WEB-INF/freemarker/formatters.ftl" as fmt />

<#escape x as x?html>
	<div class="row">
		<div class="col-md-6">
			<#assign popover>
				<p>A module manager can create and delete assignments, download submissions and publish feedback.</p>
			</#assign>

			<h3 class="permissionTitle">Module Managers <@fmt.help_popover id="modulemanagers" title="Module Managers" content="${popover}" html=true /></h3>

			<@pm.roleTable perms_url "modulemanager-table" scope "ModuleManagerRoleDefinition" "Module Manager" />
		</div>

		<div class="col-md-6">
			<#assign popover>
				<p>A module assistant can create assignments and download submissions, but cannot delete assignments or submissions, or publish feedback.</p>
			</#assign>

			<h3 class="permissionTitle">Module Assistants <@fmt.help_popover id="moduleassistants" title="Module Assistants" content="${popover}" html=true /></h3>

			<@pm.roleTable perms_url "moduleassistant-table" scope "ModuleAssistantRoleDefinition" "Module Assistant" />
		</div>
	</div>

	<div class="row">
		<div class="col-md-6">
			<#assign popover>
				<p>A module auditor can view submissions, feedback and marks, but cannot change any information in Tabula.</p>
			</#assign>

			<h3 class="permissionTitle">Module Auditors <@fmt.help_popover id="moduleauditors" title="Module Auditors" content="${popover}" html=true /></h3>

			<@pm.roleTable perms_url "moduleauditor-table" scope "ModuleAuditorRoleDefinition" "Module Auditor" />
		</div>
	</div>
</#escape>