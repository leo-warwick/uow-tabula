<#escape x as x?html>
<#if nonempty(ownedDepartments) || nonempty(ownedModuleDepartments) || nonempty(taughtGroups)>

	<#if nonempty(ownedDepartments) || nonempty(ownedModuleDepartments)>
		<h2>Administration</h2>
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

	<#if nonempty(taughtGroups)>
		<h2>Teaching</h2>

		<ul class="links">
			<li><a href="<@routes.groups.tutor academicYear />">My small groups in ${academicYear.toString}</a></li>
		</ul>
	</#if>

</#if>
</#escape>