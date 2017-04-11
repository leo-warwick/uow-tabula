<#escape x as x?html>
<#import "../reports_macros.ftl" as reports_macros />

<h1>All event attendance</h1>

<#assign reportUrl><@routes.reports.allSmallGroups department academicYear /></#assign>
<@reports_macros.reportLoader reportUrl>
	<ul class="dropdown-menu">
		<li><a href="#" data-href="<@routes.reports.allSmallGroupsDownloadCsv department academicYear />">CSV</a></li>
		<li><a href="#" data-href="<@routes.reports.allSmallGroupsDownloadXlsx department academicYear />">Excel</a></li>
		<li><a href="#" data-href="<@routes.reports.allSmallGroupsDownloadXml department academicYear />">XML</a></li>
	</ul>
</@reports_macros.reportLoader>
<@reports_macros.smallGroupReportScript />

</#escape>