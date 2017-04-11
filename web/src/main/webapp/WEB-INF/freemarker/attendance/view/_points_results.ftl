<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />
<#import "../attendance_variables.ftl" as attendance_variables />

<#assign filterQuery = filterCommand.serializeFilter />

<#if validationError?has_content>
	<#noescape>${validationError}</#noescape>
<#elseif !filterCommand.hasBeenFiltered>
	<p class="alert alert-info">Find points for students using the filter options above.</p>
<#elseif filterCommand.filterTooVague>
	<p class="alert alert-info">The filter you have chosen includes too many students.</p>
<#elseif filterResult?size == 0>
	<p class="alert alert-info">No students with points exist for the selected options.</p>
<#else>

	<#assign returnTo><@routes.attendance.viewPoints filterCommand.department filterCommand.academicYear filterQuery/></#assign>
	<#function permission_button_function groupedPoint>
		<#local record_url><@routes.attendance.viewRecordPoints filterCommand.department filterCommand.academicYear groupedPoint filterQuery returnTo/></#local>
		<#local result>
			<@fmt.permission_button
				permission='MonitoringPoints.Record'
				scope=(filterCommand.department)
				action_descr='record monitoring points'
				classes='btn btn-primary'
				href=record_url
			>
				Record
			</@fmt.permission_button>
		</#local>
		<#return result>
	</#function>


	<div class="monitoring-points">
		<#list attendance_variables.monitoringPointTermNames as term>
			<#if filterResult[term]??>
				<@attendance_macros.groupedPointsBySection filterResult term; groupedPoint>
					<div class="col-md-12">
						<div class="pull-right">
							<#noescape>
								${permission_button_function(groupedPoint.templatePoint)}
							</#noescape>
						</div>
						${groupedPoint.templatePoint.name}
						(<a class="use-tooltip" data-html="true" title="
							<@fmt.wholeWeekDateFormat
								groupedPoint.templatePoint.startWeek
								groupedPoint.templatePoint.endWeek
								groupedPoint.templatePoint.scheme.academicYear
							/>
						"><@fmt.monitoringPointWeeksFormat
							groupedPoint.templatePoint.startWeek
							groupedPoint.templatePoint.endWeek
							groupedPoint.templatePoint.scheme.academicYear
							filterCommand.department
						/></a>)
						<#assign popoverContent>
							<ul>
								<#list groupedPoint.schemes?sort_by("displayName") as scheme>
									<li>${scheme.displayName}</li>
								</#list>
							</ul>
						</#assign>
						<a href="#" class="use-popover" data-content="${popoverContent}" data-html="true" data-placement="right">
							<@fmt.p groupedPoint.schemes?size "scheme" />
						</a>
					</div>
				</@attendance_macros.groupedPointsBySection>
			</#if>
		</#list>

		<#list monthNames as month>
			<#if filterResult[month]??>
				<@attendance_macros.groupedPointsBySection filterResult month; groupedPoint>
					<div class="col-md-12">
						<div class="pull-right">
							<#noescape>
								${permission_button_function(groupedPoint.templatePoint)}
							</#noescape>
						</div>
						${groupedPoint.templatePoint.name}
						(<@fmt.interval groupedPoint.templatePoint.startDate groupedPoint.templatePoint.endDate />)
						<#assign popoverContent>
							<ul>
								<#list groupedPoint.schemes?sort_by("displayName") as scheme>
									<li>${scheme.displayName}</li>
								</#list>
							</ul>
						</#assign>
						<a href="#" class="use-popover" data-content="${popoverContent}" data-html="true" data-placement="right">
							<@fmt.p groupedPoint.schemes?size "scheme" />
						</a>
					</div>
				</@attendance_macros.groupedPointsBySection>
			</#if>
		</#list>

	</div>
</#if>

<script type="text/javascript">
	// Enable any freshly loaded popovers
	jQuery('.use-popover').tabulaPopover({
		trigger: 'click',
		container: 'body'
	});
</script>

</#escape>