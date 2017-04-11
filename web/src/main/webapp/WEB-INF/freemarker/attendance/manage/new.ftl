<#escape x as x?html>

<h1>Create a scheme</h1>

<@f.form id="newScheme" method="POST" commandName="command">

	<p class="progress-arrows">
		<span class="arrow-right active">Properties</span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and edit students"><button type="submit" class="btn btn-link" name="${ManageSchemeMappingParameters.createAndAddStudents}">Students</button></span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and edit points"><button type="submit" class="btn btn-link" name="${ManageSchemeMappingParameters.createAndAddPoints}">Points</button></span>
	</p>

	<#assign label>
		Scheme name
		<@fmt.help_popover id="name" content="Give the scheme an optional name to distinguish it from other schemes in your department e.g. 1st Year Undergrads (part-time)" />
	</#assign>
	<@bs3form.labelled_form_group path="name" labelText="${label}">
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

	<input
		type="submit"
		class="btn btn-primary use-tooltip"
		name="${ManageSchemeMappingParameters.createAndAddStudents}"
		value="Save and add students"
		title="Select which students this scheme should apply to"
		data-container="body"
	/>
	<input
		type="submit"
		class="btn btn-primary use-tooltip"
		name="create"
		value="Save and exit"
		title="Save your blank scheme and add students and points to it later"
		data-container="body"
	/>
	<a class="btn btn-default" href="<@routes.attendance.manageHomeForYear command.department command.academicYear />">Cancel</a>
</@f.form>
</#escape>