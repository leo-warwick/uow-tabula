<#assign department=command.department />
<#assign markingWorkflow=command.markingWorkflow />
<#assign form_url><@routes.coursework.markingworkflowdelete markingWorkflow /></#assign>
<#escape x as x?html>
<#compress>

<p>Are you sure you want to delete <strong>"${markingWorkflow.name}"</strong>?</p>

<@f.form method="post" action="${form_url}" commandName="command" cssClass="form-horizontal">
<@f.errors cssClass="error form-errors" />

<input type="submit" class="btn btn-danger" value="Delete" />

<a class="btn" href="<@routes.coursework.markingworkflowlist department />">Cancel</a>

</@f.form>

</#compress>
</#escape>