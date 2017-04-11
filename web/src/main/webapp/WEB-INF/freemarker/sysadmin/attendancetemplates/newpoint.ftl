<#escape x as x?html>

<h1>Add monitoring point to ${template.templateName}</h1>

<#assign action><@url page="/sysadmin/attendancetemplates/${template.id}/points/add"/></#assign>
<@f.form action="${action}" method="POST" commandName="command">

	<@bs3form.labelled_form_group path="name" labelText="Name">
		<@f.input path="name" cssClass="form-control"/>
	</@bs3form.labelled_form_group>

	<div class="dateTimePair">
		<#if template.pointStyle.dbValue == "week">

			<@bs3form.labelled_form_group path="startWeek" labelText="Start">
				<@f.select path="startWeek" cssClass="startDateTime selectOffset form-control">
					<#list 1..52 as week>
						<@f.option value="${week}">Week ${week}</@f.option>
					</#list>
				</@f.select>
			</@bs3form.labelled_form_group>

			<@bs3form.labelled_form_group path="endWeek" labelText="End">
				<@f.select path="endWeek" cssClass="endDateTime selectOffset form-control">
					<#list 1..52 as week>
						<@f.option value="${week}">Week ${week}</@f.option>
					</#list>
				</@f.select>
			</@bs3form.labelled_form_group>

		<#else>

			<#assign labelText>Start <@fmt.help_popover id="help-popover-startDate" content="You cannot mark a point as attended or missed (unauthorised) before its start date" /></#assign>
			<@bs3form.labelled_form_group path="startDate" labelText=labelText>
				<@f.input type="text" path="startDate" cssClass="form-control date-picker startDateTime" placeholder="Pick the start date" />
				<input class="endoffset" type="hidden" data-end-offset="0" />
			</@bs3form.labelled_form_group>

			<#assign labelText>End <@fmt.help_popover id="help-popover-endDate" content="A warning will appear for unrecorded attendance after its end date" /></#assign>
			<@bs3form.labelled_form_group path="endDate" labelText=labelText>
				<@f.input type="text" path="endDate" cssClass="form-control date-picker endDateTime" placeholder="Pick the end date" />
			</@bs3form.labelled_form_group>

		</#if>
	</div>

	<@bs3form.form_group>
		<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Adding&hellip;">
			Add
		</button>
		<button class="btn" type="submit" name="cancel">Cancel</button>
	</@bs3form.form_group>
</@f.form>


</#escape>