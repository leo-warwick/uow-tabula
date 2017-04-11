<#escape x as x?html>

<h1>My students</h1>

<ul>
	<#list relationshipTypesMap?keys as relationshipType>
		<#if mapGet(relationshipTypesMap, relationshipType)>
			<li><h3><a id="relationship-${relationshipType.urlPart}" href="<@routes.attendance.agentHomeYears relationshipType />">${relationshipType.studentRole?cap_first}s</a></h3></li>
		</#if>
	</#list>
</ul>

</#escape>