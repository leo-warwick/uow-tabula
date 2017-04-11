<#escape x as x?html>
<#import "*/group_components.ftl" as components />

<#function route_function dept>
	<#local result><@routes.groups.departmentAttendance dept adminCommand.academicYear /></#local>
	<#return result />
</#function>
<@fmt.id7_deptheader title="Attendance" route_function=route_function preposition="for"/>

<#if !modules?has_content>
	<p class="alert alert-info empty-hint">This department doesn't have any groups set up.</p>
<#else>
	<@components.department_attendance department modules adminCommand.academicYear />

	<#-- List of students modal -->
	<div id="students-list-modal" class="modal fade"></div>
</#if>

<div id="profile-modal" class="modal fade profile-subset"></div>
</#escape>