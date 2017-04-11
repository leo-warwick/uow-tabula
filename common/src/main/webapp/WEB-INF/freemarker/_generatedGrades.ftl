<option value="" <#if !default??>selected</#if>></option>
<#list grades as grade>
	<option value="${grade.grade}" <#if default?? && grade.grade == default.grade>selected</#if>>${grade.grade}</option>
</#list>