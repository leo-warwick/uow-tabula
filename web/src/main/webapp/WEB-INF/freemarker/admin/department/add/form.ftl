<#escape x as x?html>
<#compress>

<h1>Create sub-department in ${department.name}</h1>

<#assign commandName="addSubDepartmentCommand" />
<#assign command=addSubDepartmentCommand />
<#assign submitUrl><@routes.admin.createsubdepartment department /></#assign>

<@f.form method="post" action="${submitUrl}" commandName=commandName cssClass="double-submit-protection">
	<@f.errors cssClass="error form-errors" />

	<@bs3form.labelled_form_group path="code" labelText="Department code">
		<@f.input path="code" cssClass="form-control" />
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="name" labelText="Department name">
		<@f.input path="name" cssClass="form-control" />
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="filterRule" labelText="Filter rule">
		<@f.select path="filterRule" id="filterRule" cssClass="form-control">
			<@f.options items=allFilterRules itemLabel="name" itemValue="name" />
		</@f.select>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group>
		<input type="submit" value="Create" class="btn btn-primary">
		<a class="btn btn-default" href="<@routes.admin.home />">Cancel</a>
	</@bs3form.labelled_form_group>

</@f.form>

</#compress>
</#escape>