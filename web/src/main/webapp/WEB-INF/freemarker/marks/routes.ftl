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
