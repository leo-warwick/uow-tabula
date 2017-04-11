<#escape x as x?html>

<#function route_function dept>
	<#local result><@routes.routes.department_timetables dept /></#local>
	<#return result />
</#function>
<#assign calendarDownloadUrl><@routes.profiles.department_timetables_calendar_download department /></#assign>
<#function timetableDownloadRoute dept year>
	<#local result><@routes.profiles.department_timetables_download dept year /></#local>
	<#return result />
</#function>
<#if calendarDownloadUrl??>
	<div class = "pull-right">
		<a class="btn btn-default calendar-download hidden-xs" href="${calendarDownloadUrl}" data-href="${calendarDownloadUrl}">
			Download calendar as PDF
		</a>
		<#if academicYears?has_content>
			<#list academicYears as academicYear>
				<a class="btn btn-default timetable-download" href="${timetableDownloadRoute(department, academicYear)}" data-href="${timetableDownloadRoute(department, academicYear)}">
					Download timetable as PDF (${academicYear.toString})
				</a>
			</#list>
		</#if>
	</div>
</#if>

<@fmt.id7_deptheader "Timetables" route_function "for" />

<#assign submitUrl><@routes.profiles.department_timetables department /></#assign>
<#include "_department_timetable.ftl" />
</#escape>