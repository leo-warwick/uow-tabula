<#escape x as x?html>

<#-- Field to support redirection post-submit -->
<input type="hidden" name="action" value="submit" id="action-submit">

	<@bs3form.labelled_form_group path="name" labelText="Assignment Title:">
		<@f.input path="name" cssClass="form-control" />
	</@bs3form.labelled_form_group>
	<#if newRecord>
		<#if command.prefillAssignment??>
			<#assign pHolder = "${command.prefillAssignment.name} - ${command.prefillAssignment.module.code}">
		</#if>
    <span class ="assignment-picker-input"  data-target="<@routes.cm2.assignemnts_json module/>">
		<@bs3form.labelled_form_group path="prefillAssignment" labelText="Copy assignment options (optional):">
            <input id="prefillAssignment" name="prefillAssignment" type="hidden" value=""/>
			<input name="query" type="text" class="form-control"  value="${pHolder!''}"/>
		</@bs3form.labelled_form_group>
	</span>
    <div>To find an assignment to pre-populate from, just start typing its name.</div>
    <div>Assignments within your department will be matched. Click on an assignment to choose it.</div>
	</#if>

	<@bs3form.labelled_form_group path="openDate" labelText="Open date">
    <div class="input-group">
		<@f.input type="text" path="openDate" cssClass="form-control date-time-minute-picker" placeholder="Pick the date" />
        <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
    </div>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="openEnded" labelText="">
		<@bs3form.checkbox path="openEnded">
			<@f.checkbox path="openEnded" id="openEnded" />Open ended
			<#assign popoverText>
            <p>
                Check this box to mark the assignment as open-ended.
            </p>

            <ul>
                <li>Any close date previously entered will have no effect.</li>
                <li>Allowing extensions and submission after the close date will have no effect.</li>
                <li>No close date will be shown to students.</li>
                <li>There will be no warnings for lateness, and no automatic deductions to marks.</li>
                <li>You will be able to publish feedback individually at any time.</li>
            </ul>
			</#assign>
			<@fmt.help_popover id="openEndedInfo" content="${popoverText}" html=true/>
		</@bs3form.checkbox>
	</@bs3form.labelled_form_group>

	<#assign openEnd =  command.openEnded?string('true','false') />
<fieldset id="open-reminder-dt" <#if openEnd == 'false'>disabled</#if>>
	<@bs3form.labelled_form_group path="openEndedReminderDate" labelText="Open ended reminder date">
        <div class="input-group disabled">
			<@f.input type="text" path="openEndedReminderDate" cssClass="disabled form-control date-time-minute-picker" placeholder="Pick the date" />
            <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
        </div>
	</@bs3form.labelled_form_group>
</fieldset>

<fieldset id="close-dt" <#if openEnd == 'true'>disabled</#if>>
	<@bs3form.labelled_form_group path="closeDate" labelText="Closing date">
        <div class="input-group">
			<@f.input type="text" path="closeDate" cssClass="form-control date-time-minute-picker" placeholder="Pick the date" />
            <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
        </div>
	</@bs3form.labelled_form_group>
</fieldset>

	<@bs3form.labelled_form_group path="academicYear" labelText="Academic year">
		<#if newRecord>
			<@f.select path="academicYear" id="academicYearSelect" cssClass="form-control">
				<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
			</@f.select>
		<#else>
			<@spring.bind path="academicYear">
            <p class="form-control-static">${status.actualValue.label} <span class="very-subtle">(can't be changed)</span></p>
			</@spring.bind>
		</#if>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="workflowCategory" labelText="Workflow">
		<@f.select path="workflowCategory" id="workflowCategory" cssClass="form-control">
			<@f.options items=command.workflowCategories itemLabel="displayName" itemValue="code" />
		</@f.select>
	</@bs3form.labelled_form_group>

<div>Marking workflows define how and by whom the assignment will be marked. You can use an existing workflow, no
    workflow or create a single use workflow.
</div>
</#escape>
