<#ftl strip_text=true />

<#escape x as x?html>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<#macro alerts commandName scope users="" role="">
	<#local bindingError><@f.errors path="${commandName}.*" /></#local>
	<#if bindingError?has_content>
		<p class="alert alert-danger">
			<button type="button" class="close" data-dismiss="alert">&times;</button>
			<#noescape>${bindingError}</#noescape>
		</p>
	</#if>

	<#if users?has_content && role?has_content>
		<div id="permissionsMessage" class="alert alert-info">
			<button type="button" class="close" data-dismiss="alert">&times;</button>
			<p>
				<#list users?keys as key>
					<strong>${users[key].getFullName()}</strong> <#if users[key].getFullName()!=""> (${key})</#if>
				</#list>
				<#if action = "add">
					<#local actionWords = "now" />
					<#else>
					<#local actionWords = "no longer" />
				</#if>

				<#if users?size gt 1>
				  <br />
					are ${actionWords} <@fmt.role_definition_description role />s for
				<#else>
					is  ${actionWords} a <@fmt.role_definition_description role /> for
				</#if>
			<#noescape>${scope}</#noescape></p>
		</div>
	</#if>
</#macro>

<#macro roleTable permsUrl cssClass scope roleDefinition roleName>
	<@manageRole scope=scope roleName=roleDefinition>
		<div class="permission-list ${cssClass}">
			<div class="row">
				<div class="col-md-12">
					<form action="${permsUrl}" method="post" class="add-permissions">
						<input type="hidden" name="_command" value="add">
						<input type="hidden" name="roleDefinition" value="${roleDefinition}">

						<@bs3form.flexipicker cssClass="pickedUser" name="usercodes" placeholder="Enter name or ID">
							<span class="input-group-btn">
								<#if can_delegate>
									<button class="btn btn-default" type="submit">Add</button>
								<#else>
									<#local popoverText>
										<p>You can't add a new ${roleName} because you don't have permission to:</p>
										<ul>
											<#list denied_permissions as perm>
												<li>${perm.description}</li>
											</#list>
										</ul>
										<p>on ${scope.toString}.</p>
									</#local>

									<button class="btn btn-default use-tooltip disabled" type="button" data-html="true" data-title="${popoverText}" data-container="body">Add</button>
								</#if>
							</span>
						</@bs3form.flexipicker>
					</form>
				</div>
			</div>
			<#local users = usersWithRole('${roleDefinition}', scope) />
			<#if users?size gt 0>
				<#list users as u>
					<div class="row">
						<div class="col-md-12"><div class="col-md-12 <#if u_has_next>user</#if>">
							<div class="pull-right">
								<#if can_delegate>
									<form action="${permsUrl}" method="post" class="remove-permissions" onsubmit="return confirm('Are you sure you want to remove permission for this user?');">
										<input type="hidden" name="_command" value="remove">
										<input type="hidden" name="roleDefinition" value="${roleDefinition}">
										<input type="hidden" name="usercodes" value="${u.userId}">
										<button class="btn btn-danger btn-xs removeUser">Remove</button>
									</form>
								<#else>
									<#local popoverText>
										<p>You can't remove a ${roleName} because you don't have permission to:</p>
										<ul>
											<#list denied_permissions as perm>
												<li>${perm.description}</li>
											</#list>
										</ul>
										<p>on ${scope.toString}.</p>
									</#local>

									<button class="btn btn-danger btn-xs use-tooltip disabled" type="button"
											data-html="true"
											data-title="${popoverText}">Remove</button>
								</#if>
							</div>
							${u.fullName} <span class="very-subtle">${u.userId}</span>
						</div></div>
					</div>
				</#list>
			<#else>
				<div class="row"><div class="col-md-12">
					There is no ${roleName} yet.
				</div></div>
			</#if>
		</div>
	</@manageRole>
</#macro>

<#macro debugPermission permission scope={} showScopes=true>
	<#local isTarget=permissionsHelperCommand?? && permissionsHelperCommand.permission?? && (permission.name == permissionsHelperCommand.permission.name && (scope?size == 0 || (scope.id == results.resolvedScope.id)))>

	<#if isTarget!false><strong class="text-success"></#if>

	<span class="permission"><i class="fa fa-lock use-tooltip" title="${permission.name}"></i> ${permission.description}</span>
	<#if showScopes && scope?? && scope?size != 0>
		on <span class="scope"><i class="fa fa-bookmark"></i> ${scope.toString}</span>
	<#elseif showScopes && permission.scoped>
		<i class="fa fa-globe use-tooltip" title="Granted against any scope" data-placement="right"></i>
	</#if>

	<#if isTarget!false></strong></#if>
</#macro>

<#macro debugRole role showScopes=true>
<span class="role"><i class="fa fa-user"></i> ${role.definition.description}</span><#if showScopes && role.scope??> on <span class="scope"><i class="fa fa-bookmark"></i> ${role.scope.toString}</span></#if>
	<#if role.explicitPermissions?size gt 0 || role.subRoles?size gt 0>
	<ul>
		<#list role.subRoles as subRole>
			<li><@debugRole role=subRole showScopes=showScopes /></li>
		</#list>
		<#list role.viewablePermissionsAsList as permission>
			<li><@debugPermission permission=permission._1() scope=permission._2() showScopes=showScopes /></li>
		</#list>
	</ul>
	</#if>
</#macro>

</#escape>
