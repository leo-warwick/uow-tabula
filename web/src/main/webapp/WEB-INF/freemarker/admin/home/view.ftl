<#escape x as x?html>

<#macro link_to_department department>
	<a href="<@routes.admin.departmenthome department />">
		Go to the ${department.name} admin page
	</a>
</#macro>

<#if user.loggedIn && user.firstName??>
	<h1>Hello, ${user.firstName}</h1>
<#else>
	<h1>Hello</h1>
</#if>

<#if nonempty(ownedDepartments) || nonempty(ownedModuleDepartments) || nonempty(ownedRouteDepartments)>
	<div><h2 class="section">Administration</h2></div>

	<#if nonempty(ownedModuleDepartments)>
		<h6>My managed <@fmt.p number=ownedModuleDepartments?size singular="module" shownumber=false /></h6>

		<ul class="links">
			<#list ownedModuleDepartments as department>
				<li>
					<@link_to_department department />
				</li>
			</#list>
		</ul>
	</#if>

	<#if nonempty(ownedRouteDepartments)>
		<h6>My managed <@fmt.p number=ownedRouteDepartments?size singular="route" shownumber=false /></h6>

		<ul class="links">
			<#list ownedRouteDepartments as department>
				<li>
					<@link_to_department department />
				</li>
			</#list>
		</ul>
	</#if>

	<#if nonempty(ownedDepartments)>
		<h6>My department-wide <@fmt.p number=ownedDepartments?size singular="responsibility" plural="responsibilities" shownumber=false /></h6>

		<ul class="links">
			<#list ownedDepartments as department>
				<li>
					<@link_to_department department />
				</li>
			</#list>
		</ul>
	</#if>
</#if>

</#escape>
