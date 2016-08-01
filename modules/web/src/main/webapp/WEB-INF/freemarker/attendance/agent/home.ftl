<#escape x as x?html>

<h1>My students</h1>

<ul>
	<#list relationshipTypesMap?keys as relationshipType>
		<#if mapGet(relationshipTypesMap, relationshipType)>
			<li><h3><a id="relationship-${relationshipType.urlPart}" href="<@routes.attendance.agentHomeForYear relationshipType '2013'/>">${relationshipType.studentRole?cap_first}s 13/14</a></h3></li>
			<#if features.academicYear2014>
				<li><h3><a id="relationship-${relationshipType.urlPart}-2014" href="<@routes.attendance.agentHomeForYear relationshipType '2014'/>">${relationshipType.studentRole?cap_first}s 14/15</a></h3></li>
			</#if>
			<#if features.academicYear2015>
				<li><h3><a id="relationship-${relationshipType.urlPart}-2015" href="<@routes.attendance.agentHomeForYear relationshipType '2015'/>">${relationshipType.studentRole?cap_first}s 15/16</a></h3></li>
			</#if>
			<#if features.academicYear2016>
				<li><h3><a id="relationship-${relationshipType.urlPart}-2016" href="<@routes.attendance.agentHomeForYear relationshipType '2016'/>">${relationshipType.studentRole?cap_first}s 16/17</a></h3></li>
			</#if>
		</#if>
	</#list>
</ul>

</#escape>