<h1>Published feedback for ${assignment.name}.</h1>

<#assign module=assignment.module />
<#assign department=module.department />

<p>
The feedback has been published.
Students will be able to access their feedback by visiting this page:
</p>

<p>
<a href="<@url page="/module/${module.code}/${assignment.id}"/>">
Assignment page
</a>
</p>

<p>
<a href="<@url page="/admin/department/{department.code}/#module-${module.code}" />">
Return to assignment info
</a>
</p>