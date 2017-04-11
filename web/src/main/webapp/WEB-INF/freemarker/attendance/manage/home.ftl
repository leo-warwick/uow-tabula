<#escape x as x?html>

<h1>Manage monitoring points</h1>

<#if managePermissions?size == 0>
	<p><em>You do not have permission to manage the monitoring points for any department.</em></p>
<#else>
	<p><em>Choose the department to manage:</em></p>
	<ul class="links">
		<#list managePermissions as department>
			<li>
				<h3><a id="manage-department-${department.code}" href="<@routes.attendance.manageHomeForYear department academicYear />">${department.name}</a></h3>
			</li>
		</#list>
	</ul>
</#if>

</#escape>