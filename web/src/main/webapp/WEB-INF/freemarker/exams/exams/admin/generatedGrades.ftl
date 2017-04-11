<div><#list result?keys as universityId>
	<select data-universityid="${universityId}" class="form-control">
		<#assign grades = result[universityId] />
		<#if defaults[universityId]??><#assign default = defaults[universityId] /></#if>
		<#include "_generatedGrades.ftl" />
	</select>
</#list></div>