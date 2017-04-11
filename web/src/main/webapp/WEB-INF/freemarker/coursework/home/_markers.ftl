<#if nonempty(assignmentsForMarking)>

<#macro marker_info info>
	<#local assignment = info.assignment />
	<#local marker = info.marker />
	<#local numSubmissions = info.numSubmissions!0 />
	<#assign time_remaining=durationFormatter(assignment.closeDate) />

	<#if numSubmissions==0 || !info.isFeedbacksToManage>
		<#local class="disabled use-tooltip" />
		<#local href="" />
		<#local title>
			You'll be able to download submissions for marking when an administrator releases them.
		</#local>
	<#else>
		<#local class="" />
		<#local title="" />
		<#local href>
			<@routes.coursework.listmarkersubmissions assignment=assignment marker=marker/>
		</#local>
	</#if>
	<#if assignment.closed || assignment.openEnded>
		<div class="alert alert-success deadline">
			<#if !assignment.openEnded>
				Assignment closed: <strong><@fmt.date date=assignment.closeDate /> (${time_remaining})</strong>
			<#else>Open-ended assignment (no close date)
			</#if>
			<div class="pull-right btn-group">
				<a class="btn btn-primary ${class}" href="${href}" data-title="${title}" data-container="body">Mark</a>
			</div>
			<div class="clearfix"></div>
		</div>
	<#else>
		<div class="alert alert-info deadline">
			Assignment closes <strong><@fmt.date date=assignment.closeDate /> (${time_remaining})</strong>
		</div>
	</#if>
</#macro>

<h2 class="section">Assignments for marking</h2>
<p>You're a marker for one or more assignments.</p>
<div class="simple-assignment-list">
	<#list assignmentsForMarking as info>
		<div class="simple-assignment-info">
			<#if info.isAdmin>
				<@fmt.admin_assignment_link info.assignment />
			<#else>
				<@fmt.assignment_name info.assignment />
			</#if>
			<@marker_info info />
		</div>
	</#list>
</div>
<script>
	jQuery("a.disabled").on('click', function(e){e.preventDefault(e)})
</script>
</#if>
