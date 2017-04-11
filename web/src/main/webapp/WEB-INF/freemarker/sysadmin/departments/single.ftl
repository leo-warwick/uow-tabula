<#escape x as x?html>

<h1>${department.name}</h1>

<p><a href="<@url page="/department/${department.code}/edit" context="/admin" />">Edit name / short name</a></p>

<p><a href="permissions/">View department admins</a></p>

<#if department.children?size gt 0>
<p><a href="<@url page="/department/${department.code}/sort-modules" context="/admin" />">Arrange modules among sub-departments</a></p>
<p><a href="<@url page="/department/${department.code}/sort-routes" context="/admin" />">Arrange routes among sub-departments</a></p>

<p><strong>Sub-departments:</strong></p>
<ul>
	<#list department.children as child>
		<li><a href='<@url page="/sysadmin/departments/${child.code}/" />'>${child.name}</a> (${child.modules?size} modules)</li>
	</#list>
</ul>
</#if>


<#if department.hasParent>
	<p>Parent department: <a href='<@url page="/sysadmin/departments/${department.parent.code}/" />'>${department.parent.name}</a></p>
</#if>

<p>
	${department.modules?size} modules
</p>

<ul>
	<#list department.modules as module>
		<li>${module.code} - ${module.name!"Unknown"}</li>
	</#list>
</ul>

<p>
	${department.routes?size} routes
</p>

<ul>
	<#list department.routes as route>
		<li>${route.code} - ${route.name!"Unknown"}</li>
	</#list>
</ul>

</#escape>