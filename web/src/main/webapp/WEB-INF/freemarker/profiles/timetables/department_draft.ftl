<#escape x as x?html>
	<#function route_function dept>
		<#local result><@routes.profiles.draft_department_timetables dept academicYear endpoint /></#local>
		<#return result />
	</#function>
	<@fmt.id7_deptheader "Draft timetable" route_function "for" />

	<#assign submitUrl><@routes.profiles.draft_department_timetables department academicYear endpoint /></#assign>
	<#include "_department_timetable.ftl" />
</#escape>