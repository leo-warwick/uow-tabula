<#escape x as x?html>

<h1>Delete template</h1>

<form id="deleteMonitoringPointSet" action="<@url page="/sysadmin/attendancetemplates/${template.id}/delete"/>" method="POST" class="form-inline">

	<p>You are deleting template: ${template.templateName}.</p>

	<p>
		<@bs3form.form_group>
			<@bs3form.checkbox path="command.confirm">
				<@f.checkbox path="command.confirm" /> I confirm that I want to delete this template and all its points.
			</@bs3form.checkbox>
		</@bs3form.form_group>
	</p>

	<input type="submit" value="Delete" class="btn btn-danger"/>
	<a class="btn btn-default" href="<@url page="/sysadmin/attendancetemplates"/>">Cancel</a>

</form>

</#escape>