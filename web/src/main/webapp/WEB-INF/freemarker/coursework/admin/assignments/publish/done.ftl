<#escape x as x?html>

<h1>Published feedback for ${assignment.name}.</h1>

<#assign module=assignment.module />
<#assign department=module.adminDepartment />

<p>
The feedback has been published.
Students will be able to access their feedback by visiting this page:
</p>

<p>
<#assign feedbackUrl><@url page="/coursework/module/${module.code}/${assignment.id}"/></#assign>
<a href="${feedbackUrl}">
${feedbackUrl}
</a>
</p>

<p>
<a href="<@url page="/coursework/admin/department/${department.code}/#module-${module.code}" />">
Return to assignment info
</a>
</p>

</#escape>