<#import "*/permissions_macros.ftl" as pm />

<h1>Permissions helper - results</h1>

<#if results.scopeMismatch>
	<div class="alert <#if permissionsHelperCommand.permission.scoped>alert-block<#else>alert-danger</#if>">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		<h4>Warning!</h4>

		<p>There was a scope mismatch between the permission requested and the settings for the helper.</p>

		<#if permissionsHelperCommand.permission.scoped>
			<p><span class="permission"><i class="fa fa-lock"></i> ${permissionsHelperCommand.permission.name}</span> is a scoped permission and can only be resolved against a provided scope.</p>
		</#if>
	</div>
</#if>

<div class="well">
	<table class="table">
		<tbody>
			<tr>
				<th>User</th>
				<td>${permissionsHelperCommand.user.fullName} (${permissionsHelperCommand.user.userId})</td>
			</tr>
			<#if permissionsHelperCommand.scope?? && permissionsHelperCommand.scopeType??>
				<tr>
					<th>Scope</th>
					<td>${permissionsHelperCommand.scopeType.simpleName} - <span class="scope"><i class="fa fa-bookmark"></i> ${permissionsHelperCommand.scope}</span></td>
				</tr>
			</#if>
			<#if permissionsHelperCommand.permission??>
				<tr>
					<th>Permission</th>
					<td><span class="permission"><i class="fa fa-lock"></i> ${permissionsHelperCommand.permission.name}</span></td>
				</tr>
			</#if>
		</tbody>
		<tbody>
			<#if permissionsHelperCommand.permission??>
				<tr>
					<th>Result</th>
					<td class="<#if results.canDo>text-success<#else>text-error</#if>">
						<i class="fa fa-<#if results.canDo>check<#else>times</#if>"></i>
						${permissionsHelperCommand.user.fullName}
						<strong><#if results.canDo>CAN<#else>CANNOT</#if></strong>
						perform <span class="permission"><i class="fa fa-lock"></i> ${permissionsHelperCommand.permission.name}</span>
						<#if results.scopeMismatch && permissionsHelperCommand.permission.scoped>
							<span class="label label-warning">No scope!</span>
						</#if>
						<#if permissionsHelperCommand.scope?? && permissionsHelperCommand.scopeType??>
							on <span class="scope"><i class="fa fa-bookmark"></i> ${permissionsHelperCommand.scope}</span> (${permissionsHelperCommand.scopeType.simpleName})
						</#if>
					</td>
				</tr>
			</#if>
			<tr>
				<th>Roles<br><small>(relevant to scope)</small></th>
				<td>
					<#if results.roles?size gt 0>
						<ul>
							<#list results.roles as role>
								<li><@pm.debugRole role=role /></li>
							</#list>
						</ul>
					<#else>
						None
					</#if>
				</td>
			</tr>
			<tr>
				<th>Explicit granted permissions<br><small>(relevant to scope)</small></th>
				<td>
					<#if results.permissions?size gt 0>
						<ul>
							<#list results.permissions as permission>
								<li><#if permission.permissionType>[ALLOW]<#else>[DENY]</#if> <@pm.debugPermission permission=permission.permission scope=permission.scope /></li>
							</#list>
						</ul>
					<#else>
						None
					</#if>
				</td>
			</tr>
		</tbody>
	</table>
</div>

<h2>Search again</h2>

<#include "_form.ftl" />