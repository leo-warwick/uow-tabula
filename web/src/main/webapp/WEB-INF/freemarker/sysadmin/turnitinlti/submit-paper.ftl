<#escape x as x?html>

<h1>Turnitin LTI - submit a test paper</h1>

	<@f.form method="post" action="${url('/sysadmin/turnitinlti/submitpaper')}" commandName="turnitinLtiSubmitPaperCommand">

		<@f.errors cssClass="error form-errors" />

		<@bs3form.labelled_form_group path="assignment" labelText="Assignment">
			<@f.input path="assignment" cssClass="form-control" value=""/>
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="attachment" labelText="Attachment">
			<@f.input path="attachment" cssClass="form-control" value=""/>
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="paperUrl" labelText="URL to paper">
			<@f.input path="paperUrl" cssClass="form-control" value=""/>
		</@bs3form.labelled_form_group>


		<@bs3form.labelled_form_group>
			<input type="submit" value="Save" class="btn btn-primary">
			<a class="btn btn-default" href="<@url page="/sysadmin/turnitinlti" />">Cancel</a>
		</@bs3form.labelled_form_group>

	</@f.form>

</#escape>