<#escape x as x?html>

<h1>Create a template</h1>

<#assign action><@url page="/sysadmin/attendancetemplates/add"/></#assign>
<@f.form id="newScheme" method="POST" commandName="command" action="${action}">

	<@bs3form.labelled_form_group path="name" labelText="Name">
		<@f.input path="name" cssClass="form-control" />
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="pointStyle" labelText="Date format">
		<@bs3form.radio>
			<@f.radiobutton path="pointStyle" value="week" />
			term weeks
			<@fmt.help_popover id="pointStyle-week" content="Create points which cover term weeks e.g. Personal tutor meeting weeks 2-3" />
		</@bs3form.radio>
		<@bs3form.radio>
			<@f.radiobutton path="pointStyle" value="date" />
			calendar dates
			<@fmt.help_popover id="pointStyle-date" content="Create points which use calendar dates e.g. Supervision 1st-31st October" />
		</@bs3form.radio>
		<span class="help-block">Select the date format to use for points on this scheme</span>
	</@bs3form.labelled_form_group>

	<@bs3form.form_group>
		<input
			type="submit"
			class="btn btn-primary"
			name="create"
			value="Save"
			data-container="body"
		/>
		<a class="btn btn-default" href="<@url page="/sysadmin/attendancetemplates" />">Cancel</a>
	</@bs3form.form_group>

</@f.form>
</#escape>