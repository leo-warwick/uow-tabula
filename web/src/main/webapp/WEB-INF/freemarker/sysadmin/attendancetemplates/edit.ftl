<#escape x as x?html>

<h1>Edit ${template.templateName}</h1>

<#assign action><@url page="/sysadmin/attendancetemplates/${template.id}/edit"/></#assign>
<@f.form id="newScheme" method="POST" commandName="command" action="${action}">

	<@bs3form.labelled_form_group path="name" labelText="Name">
		<@f.input path="name" cssClass="form-control" />
	</@bs3form.labelled_form_group>

	<#if template.points?size == 0>
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
	<#else>
		<@bs3form.labelled_form_group path="pointStyle" labelText="Date format">
			<@f.hidden path="pointStyle" />
			<@bs3form.radio>
				<@f.radiobutton path="pointStyle" value="week" disabled=true />
			term weeks
				<@fmt.help_popover id="pointStyle-week" content="Create points which cover term weeks e.g. Personal tutor meeting weeks 2-3" />
			</@bs3form.radio>
			<@bs3form.radio>
				<@f.radiobutton path="pointStyle" value="date" disabled=true />
			calendar dates
				<@fmt.help_popover id="pointStyle-date" content="Create points which use calendar dates e.g. Supervision 1st-31st October" />
			</@bs3form.radio>
			<span class="help-block">You cannot change the type of points once some points have been added to a scheme</span>
		</@bs3form.labelled_form_group>
	</#if>

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

<hr />

<div class="monitoring-points">

	<div class="striped-section">
		<div class="pull-right">
			<a href="<@url page="/sysadmin/attendancetemplates/${template.id}/points/add" />" class="btn btn-primary new-point">Create new point</a>
		</div>
		<h2 class="section-title">Monitoring points</h2>
		<div class="striped-section-contents">
			<#if template.points?size == 0>
				<div class="item-info">
					<em>No points exist for this template</em>
				</div>
			<#else>
				<#if template.pointStyle.dbValue == "week">
					<#assign points = template.points?sort_by("startWeek") />
				<#else>
					<#assign points = template.points?sort_by("startDate") />
				</#if>
				<#list points as point>
					<div class="item-info row point">
						<div class="col-md-12">
							<div class="pull-right">
								<a class="btn btn-primary edit-point" href="<@url page="/sysadmin/attendancetemplates/${template.id}/points/${point.id}/edit"/>">Edit</a>
								<a class="btn btn-danger delete-point" title="Delete" href="<@url page="/sysadmin/attendancetemplates/${template.id}/points/${point.id}/delete"/>"><i class="fa fa-times"></i></a>
							</div>
							<#if template.pointStyle.dbValue == "week">
								${point.name} (week ${point.startWeek} - ${point.endWeek})
							<#else>
								${point.name} (<#noescape>${command.dateString(point.startDate)} - ${command.dateString(point.endDate)}</#noescape>)
							</#if>
						</div>
					</div>
				</#list>
			</#if>
		</div>
	</div>

</div>

</#escape>