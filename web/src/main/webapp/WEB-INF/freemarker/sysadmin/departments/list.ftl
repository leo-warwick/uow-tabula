<#escape x as x?html>

<h1>All ${departments?size} departments</h1>

<#list departments as department>
	<#if !department.hasParent>
		<@department_item department 0 />
	</#if>
</#list>

<#macro department_item department indent>
	<div class="row">
		<div class="col-md-1 col-md-offset-${indent}">
		${(department.code!'?')?upper_case}
		</div>
		<div class="col-md-6">
			<a href="<@url page="/sysadmin/departments/${department.code}/" />"><strong>${department.name}</strong></a>
		</div>
	</div>
	<#if department.children?has_content>
		<#local indent = indent + 1 />
		<#list department.children as child>
			<@department_item child indent />
		</#list>
	</#if>
</#macro>


</#escape>