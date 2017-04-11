<#escape x as x?html>
<#import "../reports_macros.ftl" as reports_macros />

<h1>Unrecorded event attendance by module</h1>

<#assign reportUrl><@routes.reports.unrecordedSmallGroupsByModule department academicYear /></#assign>
<@reports_macros.reportLoader reportUrl "filteredAttendanceCommand">
	<ul class="dropdown-menu">
		<li><a href="#" data-href="<@routes.reports.unrecordedSmallGroupsByModuleDownloadCsv department academicYear />">CSV</a></li>
		<li><a href="#" data-href="<@routes.reports.unrecordedSmallGroupsByModuleDownloadXlsx department academicYear />">Excel</a></li>
		<li><a href="#" data-href="<@routes.reports.unrecordedSmallGroupsByModuleDownloadXml department academicYear />">XML</a></li>
	</ul>
</@reports_macros.reportLoader>
<@reports_macros.smallGroupByModuleReportScript />

</#escape>