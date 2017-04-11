<#escape x as x?html>
<#compress>

<h1>Edit ${department.name}</h1>
<#assign commandName="editDepartmentCommand" />
<#assign command=editDepartmentCommand />
<#assign submitUrl><@routes.admin.editdepartment department /></#assign>
<@f.form method="post" action="${submitUrl}" commandName=commandName cssClass="double-submit-protection">

	<@f.errors cssClass="error form-errors" />

	<#if department.hasParent>
		<@bs3form.labelled_form_group path="code" labelText="Department code">
			<@f.input path="code" cssClass="form-control" />
		</@bs3form.labelled_form_group>
	</#if>

	<@bs3form.labelled_form_group path="fullName" labelText="Department full name">
		<@f.input path="fullName" cssClass="form-control" />
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="shortName" labelText="Department short name">
		<@f.input path="shortName" cssClass="form-control" />
	</@bs3form.labelled_form_group>

	<#if department.hasParent>
		<@bs3form.labelled_form_group path="filterRule" labelText="Filter rule">
			<@f.select path="filterRule" id="filterRule" cssClass="form-control">
				<@f.options items=allFilterRules itemLabel="name" itemValue="name" />
			</@f.select>
		</@bs3form.labelled_form_group>
	</#if>

	<@bs3form.form_group>
		<input type="submit" value="Edit" class="btn btn-primary">
		<a class="btn btn-default" href="<@routes.admin.home />">Cancel</a>
	</@bs3form.form_group>

</@f.form>

</#compress>
</#escape>