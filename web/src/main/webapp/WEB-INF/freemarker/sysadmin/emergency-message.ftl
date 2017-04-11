<h1>Emergency message</h1>

<@f.form action="?" commandName="emergencyMessageCommand">

	<@bs3form.checkbox path="enable">
		<@f.checkbox path="enable" /> Enabled
	</@bs3form.checkbox>

	<@bs3form.labelled_form_group path="message" labelText="Custom message">
		<@f.textarea path="message" cssClass="form-control" />
	</@bs3form.labelled_form_group>

	<@bs3form.form_group>
		<input class="btn btn-danger" type="submit" value="Update" />
	</@bs3form.form_group>

</@f.form>