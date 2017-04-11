<#escape x as x?html>

<h1>LTI Conformance tester (basic-lti-launch-request)</h1>

	<@f.form method="post" action="${url('/sysadmin/turnitinlti/conformancetester-generate')}" commandName="ltiConformanceTesterPopulateFormCommand">

		<@f.errors cssClass="error form-errors" />

		<@bs3form.labelled_form_group path="endpoint" labelText="Endpoint for testing">
			<@f.input path="endpoint" cssClass="form-control" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="key" labelText="Consumer key for testing">
			<@f.input path="key" cssClass="form-control" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="secret" labelText="Consumer secret for testing">
			<@f.input path="secret" cssClass="form-control" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="givenName" labelText="Given name">
			<@f.input path="givenName" cssClass="form-control" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="familyName" labelText="Family name">
			<@f.input path="familyName" cssClass="form-control" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="role" labelText="Role">
			<@f.input path="role" value="Instructor" cssClass="form-control" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="mentee" labelText="Mentee">
			<@f.input path="mentee" cssClass="form-control" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="email" labelText="Email">
			<@f.input path="email" value="sian@imscert.org" cssClass="form-control" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="tool_consumer_info_version" labelText="Tabula version">
			<@f.input path="tool_consumer_info_version" value="113" cssClass="form-control" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="assignment" labelText="Tabula assignment id">
			<@f.input path="assignment" cssClass="form-control" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="customParams" labelText="Custom params">
			<@f.textarea path="customParams" cssClass="form-control" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group>
			<input type="submit" value="Send" class="btn btn-primary">
			<a class="btn btn-default" href="<@url page="/sysadmin/turnitinlti" />">Cancel</a>
		</@bs3form.labelled_form_group>

	</@f.form>

</#escape>