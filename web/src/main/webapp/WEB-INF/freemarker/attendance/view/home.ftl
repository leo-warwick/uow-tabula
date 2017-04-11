<#escape x as x?html>

<h1>View and record monitoring points</h1>

<#if viewPermissions?size == 0>
	<p><em>You do not have permission to view the monitoring points for any department.</em></p>
<#else>
	<ul class="links">
		<#list viewPermissions as department>
			<li>
				<h3><a id="view-department-${department.code}" href="<@routes.attendance.viewHomeForYear department academicYear />">${department.name}</a></h3>
			</li>
		</#list>
	</ul>
</#if>

</#escape>