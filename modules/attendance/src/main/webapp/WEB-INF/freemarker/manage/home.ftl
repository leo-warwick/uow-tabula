<#escape x as x?html>

<h1>Manage monitoring points</h1>

<#if managePermissions?size == 0>
	<p><em>You do not have permission to manage the monitoring points for any department.</em></p>
<#else>
	<p><em>Choose the department to manage:</em></p>
	<ul class="links">
		<#list managePermissions as department>
			<li>
				<h3><a id="manage-department-${department.code}" href="<@routes.manageDepartment department />">${department.name} 13/14</a></h3>
				<#if features.attendanceMonitoringAcademicYear2014>
					<h3><a id="manage-department-${department.code}" href="<@routes.manageHomeForYear department '2014'/>">${department.name} 14/15</a></h3>
				</#if>
			</li>
		</#list>
	</ul>
</#if>

</#escape>