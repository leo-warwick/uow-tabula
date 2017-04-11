<#ftl strip_text=true />
<#--
		Just a handy place to create macros for generating URLs to various places, to save time
		if we end up changing any of them.

		TODO grab values from the Routes object in code, as that's pretty equivalent and
		we're repeating ourselves here. OR expose Routes directly.

		-->

<#macro _u page context='/reports'><@url context=context page=page /></#macro>

<#macro home><@_u page="/" /></#macro>
<#macro department department><@_u page="/${department.code}" /></#macro>
<#macro departmentWithYear department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}" /></#macro>

<#macro profile profile><@_u page="/view/${profile.universityId}" context="/profiles"/></#macro>

<#macro allAttendance department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/attendance/all" /></#macro>
<#macro allAttendanceDownloadCsv department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/attendance/all/download.csv" /></#macro>
<#macro allAttendanceDownloadXlsx department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/attendance/all/download.xlsx" /></#macro>
<#macro allAttendanceDownloadXml department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/attendance/all/download.xml" /></#macro>

<#macro unrecordedAttendance department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/attendance/unrecorded" /></#macro>
<#macro unrecordedAttendanceDownloadCsv department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/attendance/unrecorded/download.csv" /></#macro>
<#macro unrecordedAttendanceDownloadXlsx department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/attendance/unrecorded/download.xlsx" /></#macro>
<#macro unrecordedAttendanceDownloadXml department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/attendance/unrecorded/download.xml" /></#macro>

<#macro missedAttendance department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/attendance/missed" /></#macro>
<#macro missedAttendanceDownloadCsv department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/attendance/missed/download.csv" /></#macro>
<#macro missedAttendanceDownloadXlsx department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/attendance/missed/download.xlsx" /></#macro>
<#macro missedAttendanceDownloadXml department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/attendance/missed/download.xml" /></#macro>

<#macro allSmallGroups department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/all" /></#macro>
<#macro allSmallGroupsDownloadCsv department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/all/download.csv" /></#macro>
<#macro allSmallGroupsDownloadXlsx department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/all/download.xlsx" /></#macro>
<#macro allSmallGroupsDownloadXml department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/all/download.xml" /></#macro>

<#macro unrecordedSmallGroups department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/unrecorded" /></#macro>
<#macro unrecordedSmallGroupsDownloadCsv department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/unrecorded/download.csv" /></#macro>
<#macro unrecordedSmallGroupsDownloadXlsx department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/unrecorded/download.xlsx" /></#macro>
<#macro unrecordedSmallGroupsDownloadXml department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/unrecorded/download.xml" /></#macro>

<#macro unrecordedSmallGroupsByModule department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/unrecorded/bymodule" /></#macro>
<#macro unrecordedSmallGroupsByModuleDownloadCsv department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/unrecorded/bymodule/download.csv" /></#macro>
<#macro unrecordedSmallGroupsByModuleDownloadXlsx department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/unrecorded/bymodule/download.xlsx" /></#macro>
<#macro unrecordedSmallGroupsByModuleDownloadXml department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/unrecorded/bymodule/download.xml" /></#macro>

<#macro missedSmallGroups department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/missed" /></#macro>
<#macro missedSmallGroupsDownloadCsv department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/missed/download.csv" /></#macro>
<#macro missedSmallGroupsDownloadXlsx department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/missed/download.xlsx" /></#macro>
<#macro missedSmallGroupsDownloadXml department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/missed/download.xml" /></#macro>

<#macro missedSmallGroupsByModule department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/missed/bymodule" /></#macro>
<#macro missedSmallGroupsByModuleDownloadCsv department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/missed/bymodule/download.csv" /></#macro>
<#macro missedSmallGroupsByModuleDownloadXlsx department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/missed/bymodule/download.xlsx" /></#macro>
<#macro missedSmallGroupsByModuleDownloadXml department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/missed/bymodule/download.xml" /></#macro>

<#macro events department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/events" /></#macro>
<#macro eventsDownloadCsv department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/events/download.csv" /></#macro>
<#macro eventsDownloadXlsx department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/events/download.xlsx" /></#macro>
<#macro eventsDownloadXml department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/events/download.xml" /></#macro>

<#macro profileExport department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/profiles/export" /></#macro>
<#macro profileExportReport department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/profiles/export/report" /></#macro>
<#macro profileExportReportProgress department academicYear jobId><@_u page="/${department.code}/${academicYear.startYear?c}/profiles/export/report?jobId=${jobId}" /></#macro>
<#macro profileExportReportZip department academicYear jobId><@_u page="/${department.code}/${academicYear.startYear?c}/profiles/export/report/zip?jobId=${jobId}" /></#macro>