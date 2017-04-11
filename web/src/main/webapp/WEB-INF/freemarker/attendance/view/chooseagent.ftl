<#escape x as x?html>

<#function route_function dept>
	<#local result><@routes.attendance.viewAgentsHome dept academicYear /></#local>
	<#return result />
</#function>
<@fmt.id7_deptheader title="View and record attendance for ${academicYear.toString}" route_function=route_function preposition="in" />

<#if can.do("MonitoringPoints.View", department)>
	<#list department.displayedStudentRelationshipTypes as relationshipType>
		<h3><a href="<@routes.attendance.viewAgents department academicYear relationshipType />">View by ${relationshipType.agentRole}</a></h3>
	</#list>
</#if>

</#escape>