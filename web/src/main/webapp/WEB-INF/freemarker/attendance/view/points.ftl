<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<#function route_function dept>
	<#local result><@routes.attendance.viewPoints dept academicYear /></#local>
	<#return result />
</#function>
<@fmt.id7_deptheader title="View monitoring points" route_function=route_function preposition="for" />

<#if updatedMonitoringPoint??>
<div class="alert alert-info">
	<button type="button" class="close" data-dismiss="alert">&times;</button>
	Attendance recorded for '${updatedMonitoringPoint.name}'
</div>
</#if>

<#assign submitUrl><@routes.attendance.viewPoints filterCommand.department filterCommand.academicYear /></#assign>

<#assign filterCommand = filterCommand />
<#assign filterCommandName = "filterCommand" />
<#assign filterResultsPath = "/WEB-INF/freemarker/attendance/view/_points_results.ftl" />
<#include "/WEB-INF/freemarker/filter_bar.ftl" />
</#escape>