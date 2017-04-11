<#escape x as x?html>

<#if user.loggedIn && user.firstName??>
	<h1 class="with-settings">Hello, ${user.firstName}</h1>
<#else>
	<h1 class="with-settings">Hello</h1>
</#if>

<p class="lead muted">
	This is a service for managing exams and exam board grids
</p>

<#if !user.loggedIn>
	<#if IS_SSO_PROTECTED!true>
		<p class="alert alert-info">
			You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
			to see a personalised view.
		</p>
	</#if>
<#else>

	<#if examsEnabled>
		<h2><a href="<@routes.exams.examsHome />">Manage and mark exams</a></h2>
	</#if>

	<#if examGridsEnabled>
		<h2><a href="<@routes.exams.gridsHome />">Manage exam board grids</a></h2>
	</#if>

	<#if !examsEnabled && !examGridsEnabled>
		Managing exams and exam grids is not enabled.
	</#if>
</#if>

</#escape>