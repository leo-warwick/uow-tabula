<#ftl strip_text=true />
<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.

TODO grab values from the Routes object in code, as that's pretty equivalent and
	we're repeating ourselves here. OR expose Routes directly.

-->

<#macro _u page context=component.context!('/exams')>
	<@url context=context page=page />
</#macro>

<#macro home><@_u page="/" /></#macro>

<#-- Shared -->
<#macro modulePermissions module><@_u context="/admin" page="/module/${module.code}/permissions"  /></#macro>
<#macro departmentDisplaySettings department><@_u context="/admin" page="/department/${department.code}/settings/display" /></#macro>
<#macro departmentNotificationSettings department><@_u context="/admin" page="/department/${department.code}/settings/notification" /></#macro>
<#macro assignMarkersSmallGroups exam><@_u context="/groups" page="/admin/marker-allocation/exam/${exam.id}" /></#macro>

<#-- Exams -->
<#macro examsHome><@_u page="/exams" /></#macro>
<#macro addMarks exam><@_u page="/exams/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/marks" /></#macro>
<#macro markerAddMarks exam marker><@_u page="/exams/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/marker/${marker.warwickId}/marks" /></#macro>
<#macro generateGradesForMarks exam><@_u page="/exams/admin/module/${exam.module.code}/exams/${exam.id}/generate-grade"/></#macro>
<#macro markstemplate exam><@_u page="/exams/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/marks-template" /></#macro>
<#macro markerMarksTemplate exam marker><@_u page="/exams/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/marker/${marker.warwickId}/marks-template" /></#macro>
<#macro createExam module academicYear><@_u page="/exams/admin/module/${module.code}/${academicYear.startYear?c}/exams/new" /></#macro>
<#macro editExam exam><@_u page="/exams/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/edit" /></#macro>
<#macro moduleHomeWithYear module academicYear><@_u page="/exams/admin/module/${module.code}/${academicYear.startYear?c}" /></#macro>
<#macro departmentHomeWithYear department academicYear><@_u page="/exams/admin/department/${department.code}/${academicYear.startYear?c}" /></#macro>
<#macro viewExam exam><@_u page="/exams/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}" /></#macro>
<#macro deleteExam exam><@_u page="/exams/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/delete" /></#macro>
<#macro uploadToSits exam><@_u page="/exams/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/upload-to-sits"/></#macro>
<#macro checkSitsUpload feedback><@_u page="/exams/admin/module/${feedback.exam.module.code}/${feedback.exam.academicYear.startYear?c}/exams/${feedback.exam.id}/feedback/${feedback.id}/check-sits"/></#macro>
<#macro feedbackAdjustment exam><@_u page="/exams/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/feedback/adjustments"/></#macro>
<#macro feedbackAdjustmentForm exam studentid><@_u page="/exams/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/feedback/adjustments/${studentid}"/></#macro>
<#macro bulkAdjustment exam><@_u page="/exams/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/feedback/bulk-adjustment"/></#macro>
<#macro bulkAdjustmentTemplate exam><@_u page="/exams/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/feedback/bulk-adjustment/template"/></#macro>
<#macro markingWorkflowList department><@_u page="/exams/admin/department/${department.code}/markingworkflows" /></#macro>
<#macro markingWorkflowAdd department><@_u page="/exams/admin/department/${department.code}/markingworkflows/add" /></#macro>
<#macro markingWorkflowEdit department markingWorkflow><@_u page="/exams/admin/department/${department.code}/markingworkflows/edit/${markingWorkflow.id}" /></#macro>
<#macro markingWorkflowDelete department markingWorkflow><@_u page="/exams/admin/department/${department.code}/markingworkflows/delete/${markingWorkflow.id}" /></#macro>
<#macro markingWorkflowReplace department markingWorkflow><@_u page="/exams/admin/department/${department.code}/markingworkflows/edit/${markingWorkflow.id}/replace" /></#macro>
<#macro assignMarkers exam><@_u page="/exams/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/assign-markers" /></#macro>
<#macro assignMarkersTemplate exam><@_u page="/exams/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/assign-markers/template" /></#macro>
<#macro releaseForMarking exam><@_u page="/exams/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/release-for-marking" /></#macro>
<#macro exportExcel module exam><@_u page="/exams/admin/module/${module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/export.xlsx" /></#macro>
<#macro exportCSV module exam><@_u page="/exams/admin/module/${module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/export.csv" /></#macro>
<#macro exportXML module exam><@_u page="/exams/admin/module/${module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/export.xml" /></#macro>

<#-- Grids -->
<#macro gridsHome><@_u page="/grids" /></#macro>
<#macro gridsDepartmentHomeForYear department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}" /></#macro>
<#macro generateGrid department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/generate" /></#macro>
<#macro generateGridSkipImport department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/generate/import/skip" /></#macro>
<#macro generateGridPreview department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/generate/preview" /></#macro>
<#macro generateGridOptions department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/generate/options" /></#macro>
<#macro generateGridCoreRequired department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/generate/corerequired" /></#macro>
<#macro generateModuleGrid department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/module/generate" /></#macro>
<#macro generateModuleGridSkipImport department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/module/generate/import/skip" /></#macro>
<#macro generateModuleGridPreview department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/module/generate/preview" /></#macro>
<#macro generateGridProgress department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/generate/progress" /></#macro>
<#macro generateModuleGridProgress department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/module/generate/progress" /></#macro>
<#macro generateGridOvercatting department academicYear scyd><@_u page="/grids/${department.code}/${academicYear.startYear?c}/generate/overcatting/${scyd.id}" /></#macro>
<#macro uploadYearMarks department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/upload"/></#macro>
<#macro manageNormalLoads department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/normalload"/></#macro>
<#macro manageWeightings department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/weightings"/></#macro>
<#macro downloadGridDocument department academicYear jobInstance><@_u page="/grids/${department.code}/${academicYear.startYear?c}/generate/documents/${jobInstance.id}/download" /></#macro>
