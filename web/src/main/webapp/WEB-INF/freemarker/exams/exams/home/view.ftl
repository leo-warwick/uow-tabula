<#if user.loggedIn && user.firstName??>
	<h1 class="with-settings">Hello, ${user.firstName}</h1>
<#else>
	<h1 class="with-settings">Hello</h1>
</#if>

<p class="lead muted">
	This is a service for managing exams and feedback
</p>

<#if !user.loggedIn>
	<#if IS_SSO_PROTECTED!true>
		<p class="alert alert-info">
			You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
			to see a personalised view.
		</p>
	</#if>
<#else>
	<#if ((marked!0) > 0)>
		<div class="alert alert-info">
			<@fmt.p marked "exam mark" /> successfully received.
		</div>
	</#if>

	<#macro link_to_department department>
		<a href="<@routes.exams.departmentHomeWithYear department currentAcademicYear />">
			Go to the ${department.name} admin page
		</a>
	</#macro>

	<#if nonempty(ownedModuleDepartments)>
		<h2>My managed <@fmt.p number=ownedModuleDepartments?size singular="module" shownumber=false /></h2>

		<ul class="links">
			<#list ownedModuleDepartments as department>
				<li>
					<@link_to_department department />
				</li>
			</#list>
		</ul>
	</#if>

	<#if nonempty(examsForMarking)>
		<#include "_markers.ftl" />
	</#if>

	<#if nonempty(ownedDepartments)>
		<h2>My department-wide <@fmt.p number=ownedDepartments?size singular="responsibility" plural="responsibilities" shownumber=false /></h2>

		<ul class="links">
			<#list ownedDepartments as department>
				<li>
					<@link_to_department department />
				</li>
			</#list>
		</ul>
	</#if>

	<#if !ownedModuleDepartments?has_content && !examsForMarking?has_content && !ownedDepartments?has_content >
		<p>
			You do not currently have permission to manage or mark any exams.
		</p>
	</#if>
</#if>