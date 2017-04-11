<#escape x as x?html>

<h1>Turnitin LTI - submit an assignment</h1>

<@f.form method="post" action="${url('/sysadmin/turnitinlti/submitassignment')}" commandName="turnitinLtiSubmitAssignmentCommand">

	<@f.errors cssClass="error form-errors" />

	<@bs3form.labelled_form_group path="assignment" labelText="Tabula assignment id">
		<@f.input path="assignment" cssClass="form-control" />
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group>
		<input type="submit" value="Save" class="btn btn-primary">
		<a class="btn btn-default" href="<@url page="/sysadmin/turnitinlti" />">Cancel</a>
	</@bs3form.labelled_form_group>

</@f.form>

</#escape>