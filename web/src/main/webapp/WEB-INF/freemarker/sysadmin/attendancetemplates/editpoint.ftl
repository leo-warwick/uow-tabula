<#escape x as x?html>

<h1>Edit monitoring point in ${point.scheme.templateName}</h1>

<#assign action><@url page="/sysadmin/attendancetemplates/${point.scheme.id}/points/${point.id}/edit"/></#assign>
<@f.form action="${action}" method="POST" commandName="command">

	<@bs3form.labelled_form_group path="name" labelText="Name">
		<@f.input path="name" cssClass="form-control"/>
	</@bs3form.labelled_form_group>

	<div class="dateTimePair">
		<#if point.scheme.pointStyle.dbValue == "week">

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
			<#assign labelText>Start <@fmt.help_popover id="help-after-startDate" content="You cannot mark a point as attended or missed (unauthorised) before its start date" /></#assign>
			<@bs3form.labelled_form_group path="startDate" labelText=labelText>
				<@f.input type="text" path="startDate" cssClass="form-control date-picker startDateTime" placeholder="Pick the start date" />
				<input class="endoffset" type="hidden" data-end-offset="0" />
			</@bs3form.labelled_form_group>

			<#assign labelText>End <@fmt.help_popover id="help-after-endDate" content="A warning will appear for unrecorded attendance after its end date" /></#assign>
			<@bs3form.labelled_form_group path="endDate" labelText=labelText>
				<@f.input type="text" path="endDate" cssClass="form-control date-picker endDateTime" placeholder="Pick the end date" />
			</@bs3form.labelled_form_group>

		</#if>
	</div>

	<@bs3form.form_group>
		<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Editing&hellip;">Edit</button>
		<button class="btn btn-default" type="submit" name="cancel">Cancel</button>
	</@bs3form.form_group>
</@f.form>


</#escape>