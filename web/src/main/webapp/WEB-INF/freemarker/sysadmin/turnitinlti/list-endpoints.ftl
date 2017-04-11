<#escape x as x?html>

<h1>Turnitin LTI - list endpoints for an assignment</h1>

	<@f.form method="post" action="${url('/sysadmin/turnitinlti/listendpoints')}" commandName="turnitinLtiListEndpointsCommand">

		<@f.errors cssClass="error form-errors" />

		<@bs3form.labelled_form_group path="turnitinAssignmentId" labelText="Turnitin Assignment ID">
			<@f.input path="turnitinAssignmentId" cssClass="form-control" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group>
			<input type="submit" value="Save" class="btn btn-primary">
			<a class="btn btn-default" href="<@url page="/sysadmin/turnitinlti" />">Cancel</a>
		</@bs3form.labelled_form_group>

	</@f.form>

</#escape>