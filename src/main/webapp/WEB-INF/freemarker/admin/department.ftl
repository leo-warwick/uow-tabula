<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>
 
<#if department??>
<h1>${department.name}</h1>

<#list modules as module>
<div class="module-info"><a id="module-${module.code}"></a>
<h2><span class="code">${module.code?upper_case}</span> <span class="name">(${module.name})</span></h2>
	
	<#if module.assignments!?size = 0>
		<p>This module has no assignments. 
		<a href="<@url page="/admin/module/${module.code}/assignments/new" />">New assignment</a>
		</p>
	<#else>
		<#list module.assignments as assignment>
		<div class="assignment-info">
			<h3><a href="<@url page="/admin/module/${module.code}/assignments/edit/${assignment.id}" />">${assignment.name}</a></h3>
		</div>
		</#list>
		<p><a href="<@url page="/admin/module/${module.code}/assignments/new" />">New assignment</a></p>
	</#if>
	
</div>
</#list>

<#else>
<p>No department.</p>
</#if>

</#escape>