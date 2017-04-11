<#escape x as x?html>
<#import "*/group_components.ftl" as components />

<h1>Attendance for <@fmt.module_name module /></h1>

<#if nonempty(sets?keys)>
	<#list sets?keys as set>
		<#assign groups = mapGet(sets, set) />

		<@components.single_groupset_attendance set groups />
	</#list>

	<#-- List of students modal -->
	<div id="students-list-modal" class="modal fade"></div>
<#else>
	<p>There are no small group events for <@fmt.module_name module false /></p>
</#if>

<div id="profile-modal" class="modal fade profile-subset"></div>
</#escape>