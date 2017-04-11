<#escape x as x?html>

<h1>Turnitin LTI - submission details</h1>

	<@f.form method="post" action="${url('/sysadmin/turnitinlti/submissiondetails')}" commandName="turnitinLtiSubmissionDetailsCommand">

		<@f.errors cssClass="error form-errors" />

		<@bs3form.labelled_form_group path="turnitinSubmissionId" labelText="Turnitin Submission ID">
			<@f.input path="turnitinSubmissionId" cssClass="form-control" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group>
			<input type="submit" value="Save" class="btn btn-primary">
			<a class="btn btn-default" href="<@url page="/sysadmin/turnitinlti" />">Cancel</a>
		</@bs3form.labelled_form_group>

	</@f.form>

</#escape>