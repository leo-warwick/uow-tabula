<#escape x as x?html>
<#import "../reports_macros.ftl" as reports_macros />

<#function route_function dept>
	<#local result><@routes.reports.allAttendance dept academicYear /></#local>
	<#return result />
</#function>

<@fmt.id7_deptheader title="All attendance for ${department.name}" route_function=route_function />

<#assign reportUrl><@routes.reports.allAttendance department academicYear /></#assign>
<@reports_macros.reportLoader reportUrl>
	<ul class="dropdown-menu">
		<li><a href="#" data-href="<@routes.reports.allAttendanceDownloadCsv department academicYear />">CSV</a></li>
		<li><a href="#" data-href="<@routes.reports.allAttendanceDownloadXlsx department academicYear />">Excel</a></li>
		<li><a href="#" data-href="<@routes.reports.allAttendanceDownloadXml department academicYear />">XML</a></li>
	</ul>
</@reports_macros.reportLoader>
<@reports_macros.attendanceMonitoringReportScript />

</#escape>