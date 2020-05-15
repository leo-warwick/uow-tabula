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
<#macro assessmentcomponent_marks_progress assessmentComponent upstreamAssessmentGroup><@_u page="/admin/assessment-component/${assessmentComponent.id}/${upstreamAssessmentGroup.id}/marks/progress" /></#macro>
<#macro assessmentcomponent_marks_importComplete assessmentComponent upstreamAssessmentGroup><@_u page="/admin/assessment-component/${assessmentComponent.id}/${upstreamAssessmentGroup.id}/marks/import-complete" /></#macro>
<#macro assessmentcomponent_generateGrades assessmentComponent><@_u page="/admin/assessment-component/${assessmentComponent.id}/generate-grades" /></#macro>
