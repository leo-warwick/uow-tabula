<#ftl strip_text=true />
<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.
TODO grab values from the Routes object in code, as that's pretty equivalent and
   we're repeating ourselves here. OR expose Routes directly.
-->
<#macro _u page context='/marks'><@url context=context page=page /></#macro>

<#macro home><@_u page="/" /></#macro>
<#macro adminhome department academicYear="">
  <#if academicYear?has_content>
    <@_u page="/admin/${department.code}/${academicYear.startYear?c}" />
  <#else>
    <@_u page="/admin/${department.code}" />
  </#if>
</#macro>
<#macro assessmentcomponents department academicYear><@_u page="/admin/${department.code}/${academicYear.startYear?c}/assessment-components" /></#macro>
<#macro assessmentcomponent_marks assessmentComponent upstreamAssessmentGroup><@_u page="/admin/assessment-component/${assessmentComponent.id}/${upstreamAssessmentGroup.id}/marks" /></#macro>
<#macro assessmentcomponent_marks_skipImport assessmentComponent upstreamAssessmentGroup><@_u page="/admin/assessment-component/${assessmentComponent.id}/${upstreamAssessmentGroup.id}/marks/skip-import" /></#macro>
<#macro assessmentcomponent_marks_forceImport assessmentComponent upstreamAssessmentGroup><@_u page="/admin/assessment-component/${assessmentComponent.id}/${upstreamAssessmentGroup.id}/marks/import" /></#macro>
<#macro assessmentcomponent_marks_progress assessmentComponent upstreamAssessmentGroup><@_u page="/admin/assessment-component/${assessmentComponent.id}/${upstreamAssessmentGroup.id}/marks/progress" /></#macro>
<#macro assessmentcomponent_marks_importComplete assessmentComponent upstreamAssessmentGroup><@_u page="/admin/assessment-component/${assessmentComponent.id}/${upstreamAssessmentGroup.id}/marks/import-complete" /></#macro>
<#macro assessmentcomponent_marks_template assessmentComponent upstreamAssessmentGroup><@_u page="/admin/assessment-component/${assessmentComponent.id}/${upstreamAssessmentGroup.id}/marks/template.xlsx" /></#macro>
<#macro assessmentcomponent_generateGrades assessmentComponent><@_u page="/admin/assessment-component/${assessmentComponent.id}/generate-grades" /></#macro>
<#macro assessmentcomponent_missingMarks assessmentComponent upstreamAssessmentGroup><@_u page="/admin/assessment-component/${assessmentComponent.id}/${upstreamAssessmentGroup.id}/missing-marks" /></#macro>
<#macro assessmentcomponent_scaling assessmentComponent upstreamAssessmentGroup><@_u page="/admin/assessment-component/${assessmentComponent.id}/${upstreamAssessmentGroup.id}/scaling" /></#macro>

<#macro module_marks sitsModuleCode academicYear occurrence><@_u page="/admin/module/${sitsModuleCode}/${academicYear.startYear?c}/${occurrence?url}/marks" /></#macro>
<#macro module_marks_skipImport sitsModuleCode academicYear occurrence><@_u page="/admin/module/${sitsModuleCode}/${academicYear.startYear?c}/${occurrence?url}/marks/skip-import" /></#macro>
<#macro module_marks_forceImport sitsModuleCode academicYear occurrence><@_u page="/admin/module/${sitsModuleCode}/${academicYear.startYear?c}/${occurrence?url}/marks/import" /></#macro>
<#macro module_marks_progress sitsModuleCode academicYear occurrence><@_u page="/admin/module/${sitsModuleCode}/${academicYear.startYear?c}/${occurrence?url}/marks/progress" /></#macro>
<#macro module_marks_importComplete sitsModuleCode academicYear occurrence><@_u page="/admin/module/${sitsModuleCode}/${academicYear.startYear?c}/${occurrence?url}/marks/import-complete" /></#macro>
<#macro module_marks_template sitsModuleCode academicYear occurrence><@_u page="/admin/module/${sitsModuleCode}/${academicYear.startYear?c}/${occurrence?url}/marks/template.xlsx" /></#macro>
<#macro module_generateGrades sitsModuleCode academicYear occurrence sprCode><@_u page="/admin/module/${sitsModuleCode}/${academicYear.startYear?c}/${occurrence?url}/generate-grades?sprCode=${sprCode?url}" /></#macro>
<#macro module_marks_confirm sitsModuleCode academicYear occurrence><@_u page="/admin/module/${sitsModuleCode}/${academicYear.startYear?c}/${occurrence?url}/confirm" /></#macro>
<#macro module_marks_process sitsModuleCode academicYear occurrence><@_u page="/admin/module/${sitsModuleCode}/${academicYear.startYear?c}/${occurrence?url}/process" /></#macro>
<#macro module_marks_resits sitsModuleCode academicYear occurrence><@_u page="/admin/module/${sitsModuleCode}/${academicYear.startYear?c}/${occurrence?url}/resits" /></#macro>

<#macro cohort_process department academicYear><@_u page="/admin/${department.code}/${academicYear.startYear?c}/cohort/process" /></#macro>
<#macro cohort_outcomes department academicYear><@_u page="/admin/${department.code}/${academicYear.startYear?c}/cohort/outcomes" /></#macro>

<#macro out_of_sync_marks department academicYear><@_u page="/admin/${department.code}/${academicYear.startYear?c}/out-of-sync-marks" /></#macro>
