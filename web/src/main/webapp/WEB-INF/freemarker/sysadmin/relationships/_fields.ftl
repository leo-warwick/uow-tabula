<#escape x as x?html>

<#if newRecord>
	<#assign labelText>ID <@fmt.help_popover id="help-popover-startDate" content="A unique identifier. Set this to something descriptive, using alphanumeric chars, - and _ only, so it's easy to see in the database. e.g. 'personalTutor'" /></#assign>
	<@bs3form.labelled_form_group path="id" labelText=labelText>
		<@f.input path="id" cssClass="text form-control" />
	</@bs3form.labelled_form_group>
<#else>
	<@bs3form.labelled_form_group path="id" labelText="ID">
		<p class="form-control-static">${status.actualValue} <span class="hint">(can't be changed)<span></p>
	</@bs3form.labelled_form_group>
</#if>

<#if newRecord || relationshipType.empty>
	<#assign labelText>URL string <@fmt.help_popover id="help-popover-urlPart" content="A string that can be used as part of the URL. e.g. 'tutor'" /></#assign>
	<@bs3form.labelled_form_group path="urlPart" labelText=labelText >
		<@f.input path="urlPart" cssClass="text form-control" />
	</@bs3form.labelled_form_group>
<#else>
	<@bs3form.labelled_form_group path="urlPart" labelText="URL string">
		<p class="form-control-static">${status.actualValue} <span class="hint">(can't be changed)<span></p>
	</@bs3form.labelled_form_group>
</#if>

<#assign labelText>Description <@fmt.help_popover id="help-popover-description" content="A descriptive name for this type of relationship. Capitalise accordingly; e.g. 'Personal Tutor'" /></#assign>
<@bs3form.labelled_form_group path="description" labelText=labelText >
	<@f.input path="description" cssClass="text form-control" />
</@bs3form.labelled_form_group>

<#assign labelText>Agent Role <@fmt.help_popover id="help-popover-startDate" content="How you'd refer to a single agent in this relationship. Input as if used in the middle of a sentence; it will be capitalised accordingly. e.g. 'personal tutor'" /></#assign>
<@bs3form.labelled_form_group path="agentRole" labelText=labelText >
	<@f.input path="agentRole" cssClass="text form-control" />
</@bs3form.labelled_form_group>

<#assign labelText>Student Role <@fmt.help_popover id="help-popover-studentRole" content="How you'd refer to a single student in this relationship. Input as if used in the middle of a sentence; it will be capitalised accordingly. e.g. 'personal tutee'" /></#assign>
<@bs3form.labelled_form_group path="studentRole" labelText=labelText >
	<@f.input path="studentRole" cssClass="text form-control" />
</@bs3form.labelled_form_group>

<@bs3form.labelled_form_group path="defaultSource" labelText="Default source">
	<@bs3form.radio>
		<@f.radiobutton path="defaultSource" value="local" />
		Tabula (Local)
		<@fmt.help_popover id="help-popover-defaultSource-local" content="By default, information will be input directly into Tabula." />
	</@bs3form.radio>

	<@bs3form.radio>
		<@f.radiobutton path="defaultSource" value="sits" />
		SITS
		<@fmt.help_popover id="help-popover-defaultSource-sits" content="By default, information will be imported from SITS. Relies on code existing to know how to import this type." />
	</@bs3form.radio>
</@bs3form.labelled_form_group>

<@bs3form.checkbox path="defaultDisplay">
	<@f.checkbox path="defaultDisplay" id="defaultDisplay" />
	Display this relationship type by default for departments
</@bs3form.checkbox>

<@bs3form.checkbox path="expectedUG">
	<@f.checkbox path="expectedUG" id="expectedUG" />
	Expect Undergraduates to always have a relationship of this type
	<@fmt.help_popover id="help-popover-expectedUG" content="If enabled, on undergraduate student profiles that don't have this type of relationship the box will be empty rather than hidden" />
</@bs3form.checkbox>

<@bs3form.checkbox path="expectedPGT">
	<@f.checkbox path="expectedPGT" id="expectedPGT" />
	Expect Taught Postgraduates to always have a relationship of this type
	<@fmt.help_popover id="help-popover-expectedPGT" content="If enabled, on taught postgraduate student profiles that don't have this type of relationship the box will be empty rather than hidden" />
</@bs3form.checkbox>

<@bs3form.checkbox path="expectedPGR">
	<@f.checkbox path="expectedPGR" id="expectedPGR" />
	Expect Research Postgraduates to always have a relationship of this type
	<@fmt.help_popover id="help-popover-expectedPGR" content="If enabled, on taught postgraduate student profiles that don't have this type of relationship the box will be empty rather than hidden" />
</@bs3form.checkbox>

<#assign labelText>Sort order <@fmt.help_popover id="help-popover-sortOrder" content="Sort order when viewing multiple relationships. Lower numbers appear first. Identical numbers are ordered alphabetically by ID." /></#assign>
<@bs3form.labelled_form_group path="sortOrder" labelText=labelText >
	<@f.input path="sortOrder" cssClass="text form-control" />
</@bs3form.labelled_form_group>

</#escape>