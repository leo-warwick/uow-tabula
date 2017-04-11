<#escape x as x?html>
<#compress>

<h1>Create module in ${department.name}</h1>
<#assign commandName="addModuleCommand" />
<#assign command=addModuleCommand />
<#assign submitUrl><@routes.admin.createmodule department /></#assign>
<@f.form method="post" action="${submitUrl}" commandName=commandName>
	<@f.errors cssClass="error form-errors" />

	<@f.hidden path="department" />

	<@bs3form.labelled_form_group path="code" labelText="Module code">
		<@f.input path="code" cssClass="form-control" />
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="name" labelText="Module name">
		<@f.input path="name" cssClass="form-control" />
	</@bs3form.labelled_form_group>

	<@bs3form.form_group>
		<input type="submit" value="Create" class="btn btn-primary">
		<a class="btn btn-default" href="<@routes.admin.home />">Cancel</a>
	</@bs3form.form_group>

</@f.form>

</#compress>
</#escape>