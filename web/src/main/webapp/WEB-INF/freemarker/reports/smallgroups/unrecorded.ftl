<#escape x as x?html>
<#import "../reports_macros.ftl" as reports_macros />

<h1>Unrecorded event attendance</h1>

<#assign reportUrl><@routes.reports.unrecordedSmallGroups department academicYear /></#assign>
<@reports_macros.reportLoader reportUrl>
	<ul class="dropdown-menu">
		<li><a href="#" data-href="<@routes.reports.unrecordedSmallGroupsDownloadCsv department academicYear />">CSV</a></li>
		<li><a href="#" data-href="<@routes.reports.unrecordedSmallGroupsDownloadXlsx department academicYear />">Excel</a></li>
		<li><a href="#" data-href="<@routes.reports.unrecordedSmallGroupsDownloadXml department academicYear />">XML</a></li>
	</ul>
</@reports_macros.reportLoader>
<@reports_macros.smallGroupReportScript />

</#escape>