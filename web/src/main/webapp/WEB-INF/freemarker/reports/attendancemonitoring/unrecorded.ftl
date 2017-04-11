<#escape x as x?html>
<#import "../reports_macros.ftl" as reports_macros />

<#function route_function dept>
	<#local result><@routes.reports.unrecordedAttendance dept academicYear /></#local>
	<#return result />
</#function>
<@fmt.id7_deptheader title="Unrecorded monitoring points for ${department.name}" route_function=route_function />

<#assign reportUrl><@routes.reports.unrecordedAttendance department academicYear /></#assign>
<@reports_macros.reportLoader reportUrl>
	<ul class="dropdown-menu">
		<li><a href="#" data-href="<@routes.reports.unrecordedAttendanceDownloadCsv department academicYear />">CSV</a></li>
		<li><a href="#" data-href="<@routes.reports.unrecordedAttendanceDownloadXlsx department academicYear />">Excel</a></li>
		<li><a href="#" data-href="<@routes.reports.unrecordedAttendanceDownloadXml department academicYear />">XML</a></li>
	</ul>
</@reports_macros.reportLoader>
<@reports_macros.attendanceMonitoringReportScript />

</#escape>