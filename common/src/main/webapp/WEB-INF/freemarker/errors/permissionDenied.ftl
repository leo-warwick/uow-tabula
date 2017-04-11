<#escape x as x?html>
<#assign sso=JspTaglibs["/WEB-INF/tld/sso.tld"]>
<h1>Sorry</h1>

<#if (user.loggedIn)!false>
<p>Sorry<#if user??> ${user.firstName?default('')}<#if user.masquerading> (really ${user.realUser.firstName})</#if></#if>, you don't have permission to see that.</p>

<#if !((user.sysadmin)!false) && (user.masquerading)!false>
	<div class="alert alert-warning">
		<h2>Masquerading</h2>

		<p>When you are masquerading as another user, you can only see things in Tabula that <strong>both</strong>
		you <strong>and</strong> the user you're masquerading as are able to see.</p>
	</div>
</#if>

<#if (user.sysadmin)!false>
	<div class="alert alert-info sysadmin-only-content" style="margin-top: 2em;">
		<button type="button" class="close" data-dismiss="alert">&times;</button>

		<h4>Permissions helper</h4>

		<p>This is only shown to Tabula system administrators. Click the &times; button to see the page as a non-administrator sees it.</p>

		<@f.form method="post" action="${url('/sysadmin/permissions-helper')}">
			<input type="hidden" name="user" value="${originalException.user.userId}" />

			<#if originalException.scope??>
				<#if originalException.scope?is_sequence>
					<input type="hidden" name="scopeType" value="${originalException.scope[0].class.name}" />

					<#attempt>
						<input type="hidden" name="scope" value="${originalException.scope[0].id}" />
					<#recover>
						<!-- Exception accessing scope.id - perhaps scope is Global? -->
					</#attempt>
				<#else>
					<input type="hidden" name="scopeType" value="${originalException.scope.class.name}" />

					<#attempt>
						<input type="hidden" name="scope" value="${originalException.scope.id!}" />
					<#recover>
						<!-- Exception accessing scope.id - perhaps scope is Global? -->
					</#attempt>
				</#if>
			</#if>

			<#if originalException.permission??>
				<input type="hidden" name="permission" value="${originalException.permission.name}" />
			<#else>
				<input type="hidden" name="permission" value="[One of many]" />
			</#if>

			<button class="btn btn-default btn-large" type="submit">Why can't I see this?</button>
		</@f.form>

		<p>Alternatively, harness unlimited power by entering God mode:</p>

		<@f.form method="post" action="${url('/sysadmin/god')}">
			<input type="hidden" name="returnTo" value="${info.requestedUri!""}" />
			<button class="btn btn-large btn-warning"><i class="icon-eye-open fa fa-eye"></i> Enable God mode</button>
		</@f.form>
	</div>
</#if>
<#else>
<p>Sorry, you don't have permission to see that.
<#if IS_SSO_PROTECTED!true>Try <a class="sso-link" href="<@sso.loginlink />">signing in</a>.</#if>
</p>
</#if>
</#escape>