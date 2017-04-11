<#escape x as x?html>

<h1>Small group teaching</h1>

<ul>
	<li><h3><a href="<@routes.reports.allSmallGroups department academicYear />">All event attendance</a></h3></li>
	<li><h3><a href="<@routes.reports.unrecordedSmallGroups department academicYear />">Unrecorded event attendance</a></h3></li>
	<li><h3><a href="<@routes.reports.unrecordedSmallGroupsByModule department academicYear />">Unrecorded event attendance by module</a></h3></li>
	<li><h3><a href="<@routes.reports.missedSmallGroups department academicYear />">Missed event attendance</a></h3></li>
	<li><h3><a href="<@routes.reports.missedSmallGroupsByModule department academicYear />">Missed event attendance by module</a></h3></li>
	<li><h3><a href="<@routes.reports.events department academicYear />">All events</a></h3></li>
</ul>
</#escape>