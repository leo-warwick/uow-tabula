<#import "*/group_components.ftl" as components />
<#escape x as x?html>

<h1>My small groups in ${academicYear.toString}</h1>

<#if updatedOccurrence??>
	<div class="alert alert-info">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		Attendance at
			<#if updatedOccurrence.event.title?has_content>${updatedOccurrence.event.title},</#if>
			${updatedOccurrence.event.group.groupSet.name},
			${updatedOccurrence.event.group.name}:
			${updatedOccurrence.event.day.name} <@fmt.time updatedOccurrence.event.startTime /> - <@fmt.time updatedOccurrence.event.endTime />
			in <strong><@fmt.singleWeekFormat week=updatedOccurrence.week academicYear=updatedOccurrence.event.group.groupSet.academicYear dept=updatedOccurrence.event.group.groupSet.module.adminDepartment /></strong> has been recorded.
	</div>
</#if>

<#if data.moduleItems?has_content>
	<@components.module_info data />
<#else>
	<div class="alert alert-block alert-info">
		There are no groups to show you right now for ${academicYear.toString}
	</div>
</#if>


</#escape>