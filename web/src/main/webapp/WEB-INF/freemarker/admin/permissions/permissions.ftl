<#compress><#escape x as x?html>

<#import "*/permissions_macros.ftl" as pm />
<#assign perms_url = info.requestedUri />
<#assign display_name = target.humanReadableId />
<#if target.urlCategory == 'assignment'>
	<#assign display_name>${target.name} (${target.module.code?upper_case})</#assign>
<#elseif target.urlCategory == 'smallgroupevent'>
	<#assign display_name>${target.group.name} <@fmt.time target.startTime /> ${target.day.name}</#assign>
</#if>

<div class="permissions-page">
	<#-- Permissions parents -->
	<#if target.permissionsParents?size gt 0 || target.customRoleDefinitions??>
		<div class="pull-right btn-toolbar">
			<#if target.urlCategory == 'department'>
				<a class="btn btn-default" href="<@routes.admin.permissionstree target />">Permissions tree</a>
				<a class="btn btn-default" href="<@routes.admin.rolesDepartment target />">About roles</a>
			<#else>
				<a class="btn btn-default" href="<@routes.admin.roles />">About roles</a>
			</#if>

			<#if adminLinks?has_content>
				<#if adminLinks?size = 1>
					<a class="btn btn-default" href="${adminLinks[0].href}">${adminLinks[0].title}</a>
				<#elseif adminLinks?size gt 1>
					<div class="btn-group">
						<button class="btn btn-default dropdown-toggle" data-toggle="dropdown">
							Manage <span class="caret"></span>
						</button>
						<ul class="dropdown-menu">
							<#list adminLinks as link>
								<li><a href="${link.href}">${link.title}</a></li>
							</#list>
						</ul>
					</div>
				</#if>
			</#if>

			<#if target.customRoleDefinitions??>
				<a class="btn btn-default" href="<@routes.admin.customroles target />">Custom roles</a>
			</#if>

			<#if target.permissionsParents?size gt 0>
				<div class="btn-group">
					<button class="btn btn-default dropdown-toggle" data-toggle="dropdown">
						Permissions parents <span class="caret"></span>
					</button>
					<ul class="dropdown-menu">
						<#list target.permissionsParents as parent>
							<li><a href="<@routes.admin.permissions parent />"><i class="fa fa-lock"></i> ${parent.humanReadableId}</a></li>
						</#list>
					</ul>
				</div>
			</#if>
		</div>
	</#if>

	<div class="deptheader">
		<h1 class="with-settings">Permissions</h1>
		<h5 class="with-related">on ${display_name}</h5>
	</div>

	<#-- Alerts -->

	<@pm.alerts "addCommand" display_name users role />

	<div class="existing-roles">
		<h2>Granted roles</h2>

		<#if existingRoleDefinitions?size gt 0>
			<#list existingRoleDefinitions?keys?chunk(2) as roleDefinitions>
				<div class="row">
					<#list roleDefinitions as roleDefinition>
						<#assign roleDefinitionName><#compress>
							<#if roleDefinition.id??>
								${roleDefinition.id}
							<#elseif roleDefinition.selector??>
								${roleDefinition.name}(${roleDefinition.selector.id})
							<#else>
								${roleDefinition.name}
							</#if>
						</#compress></#assign>

						<div class="col-md-6">
							<#assign roleDescription><@pm.debugRole role=mapGet(existingRoleDefinitions, roleDefinition) showScopes=false /></#assign>
							<h3 class="permissionTitle">${roleDefinition.description} <@fmt.help_popover id="${roleDefinition.name}" title="${roleDefinition.description}" content="${roleDescription}" html=true /></h3>

							<@pm.roleTable perms_url "${roleDefinition.name}-table" target roleDefinitionName roleDefinition.description />
						</div>
					</#list>
				</div>
			</#list>
		<#else>
			<p>There are no granted roles applied against ${display_name}.</p>
		</#if>
	</div>

	<#-- Add a new (pre-existing) role -->
	<div class="new-role">
		<h2>Grant a new role</h2>

		<#if grantableRoleDefinitions?size gt 0>
			<#list grantableRoleDefinitions?keys?chunk(2) as roleDefinitions>
				<div class="row">
					<#list roleDefinitions as roleDefinition>
						<#assign roleDefinitionName><#compress>
							<#if roleDefinition.id??>
								${roleDefinition.id}
							<#elseif roleDefinition.selector??>
								${roleDefinition.name}(${roleDefinition.selector.id})
							<#else>
								${roleDefinition.name}
							</#if>
						</#compress></#assign>

						<div class="col-md-6">
							<#assign roleDescription><@pm.debugRole role=mapGet(grantableRoleDefinitions, roleDefinition) showScopes=false /></#assign>
							<h3 class="permissionTitle">${roleDefinition.description} <@fmt.help_popover id="${roleDefinition.name}" title="${roleDefinition.description}" content="${roleDescription}" html=true /></h3>

							<@pm.roleTable perms_url "${roleDefinition.name}-table" target roleDefinitionName roleDefinition.description />
						</div>
					</#list>
				</div>
			</#list>
		<#else>
			<p>You do not have permission to grant any roles against ${display_name}.</p>
		</#if>
	</div>

	<#-- Existing permissions -->
	<div class="existing-permissions">
		<h2>Explicitly granted/revoked permissions</h2>

		<#if existingPermissions?size gt 0>
			<table class="table permission-list">
				<thead>
					<tr>
						<th>User</th>
						<th>Permission</th>
						<th>Allowed?</th>
						<th>&nbsp;</th>
					</tr>
				</thead>
				<tbody>
					<#list existingPermissions as grantedPermission>
						<#assign permissionName><#compress>
							<#if grantedPermission.permission.selector??>
								${grantedPermission.permission.name}(${grantedPermission.permission.selector.id})
							<#else>
								${grantedPermission.permission.name}
							</#if>
						</#compress></#assign>
						<@manageRole scope=target permissionName=permissionName>
							<#list grantedPermission.users.users as u>
								<tr>
									<td class="user">
										${u.fullName} <span class="very-subtle">${u.userId}</span>
									</td>
									<td><abbr title="${grantedPermission.permission.description}" class="initialism">${permissionName}</abbr></td>
									<td>
										<#if grantedPermission.overrideType>
											<i class="fa fa-check" title="Allowed"></i>
										<#else>
											<i class="fa fa-times" title="Not allowed"></i>
										</#if>
									</td>
									<td class="actions">
										<#if can_delegate>
											<form action="${perms_url}" method="post" class="remove-permissions" onsubmit="return confirm('Are you sure you want to remove permission for this user?');">
												<input type="hidden" name="_command" value="removeSingle">
												<input type="hidden" name="permission" value="${permissionName}">
												<input type="hidden" name="overrideType" value="<#if grantedPermission.overrideType>true<#else>false</#if>">
												<input type="hidden" name="usercodes" value="${u.userId}">
												<button type="submit" class="btn btn-danger btn-xs removeUser"><i class="fa fa-inverse fa-times"></i></button>
											</form>
										<#else>
											<#assign popoverText>
												<p>You can't remove ${permissionName} because you don't have permission to:</p>
												<ul>
													<#list denied_permissions as perm>
														<li>${perm.description}</li>
													</#list>
												</ul>
												<p>on ${target.toString}.</p>
											</#assign>

											<button class="btn btn-danger btn-xs use-tooltip disabled" type="button"
													data-html="true"
													data-title="${popoverText}"><i class="fa fa-inverse fa-times"></i></button>
										</#if>
									</td>
								</tr>
							</#list>
						</@manageRole>
					</#list>
				</tbody>
			</table>
		<#else>
			<p>There are no granted or revoked permissions applied against ${display_name}.</p>
		</#if>
	</div>

	<#-- Add a new permission -->
	<div class="add-permission well">
		<h2>Explicitly grant or revoke permissions</h2>

		<@f.form action="${perms_url}" commandName="addSingleCommand">
			<@f.errors cssClass="error form-errors" />

			<input type="hidden" name="_command" value="addSingle">

			<@bs3form.labelled_form_group path="usercodes" labelText="User ID">
				<@bs3form.flexipicker path="usercodes" placeholder="Type name or usercode" />
			</@bs3form.labelled_form_group>

			<@bs3form.labelled_form_group path="permission" labelText="Permission">
				<@f.select path="permission" cssClass="form-control">
					<#list allPermissions?keys as group>
						<optgroup label="${group}">
							<#list allPermissions[group] as permission>
								<option value="${permission._1()}"<#if status.value?? && permission._1()?? && (status.value!"") == (permission._1()!"")> selected="selected"</#if>>${permission._2()}</option>
							</#list>
						</optgroup>
					</#list>
				</@f.select>
			</@bs3form.labelled_form_group>

			<@bs3form.labelled_form_group path="overrideType" labelText="Allowed?">
				<@f.select path="overrideType" cssClass="form-control">
					<option value="true">Allow</option>
					<option value="false">Disallow</option>
				</@f.select>
			</@bs3form.labelled_form_group>

			<div class="submit-buttons">
				<button type="submit" class="btn btn-default">Add</button>
			</div>
		</@f.form>
	</div>
</div>

</#escape></#compress>