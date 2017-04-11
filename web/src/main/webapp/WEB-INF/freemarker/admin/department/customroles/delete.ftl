<#assign form_url><@routes.admin.deletecustomrole customRoleDefinition /></#assign>
<#escape x as x?html>
<#compress>

<p>Are you sure you want to delete <strong>"${customRoleDefinition.description}"</strong>?</p>

<@f.form method="post" action="${form_url}" commandName="command">
	<@f.errors cssClass="error form-errors" />

	<input type="submit" class="btn btn-danger" value="Delete" />

	<a class="btn btn-default" href="<@routes.admin.customroles department />">Cancel</a>

</@f.form>

</#compress>
</#escape>