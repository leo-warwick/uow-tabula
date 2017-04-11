<#import "../attendance_variables.ftl" as attendance_variables />
<#import "../attendance_macros.ftl" as attendance_macros />

<#if findResult.termGroupedPoints?keys?has_content>
	<#list attendance_variables.monitoringPointTermNames as term>
		<#if findResult.termGroupedPoints[term]??>
			<@attendance_macros.groupedPointsBySection findResult.termGroupedPoints term; groupedPoint>
			<div class="col-md-12">
				<div class="pull-right">
					<a class="btn btn-primary" href="<@routes.attendance.manageEditPoint groupedPoint.templatePoint filterQuery returnTo />">Edit</a>
					<#assign hasRecordedCheckpoints = groupedPoint.hasRecordedCheckpoints/>
					<a class="btn btn-danger <#if hasRecordedCheckpoints> disabled use-tooltip</#if>" <#if hasRecordedCheckpoints>title="This point cannot be removed as it has attendance marks against it."</#if> href="<@routes.attendance.manageDeletePoint groupedPoint.templatePoint filterQuery returnTo />">Delete</a>
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
					findCommand.department
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
</#if>

<#if findResult.monthGroupedPoints?keys?has_content>
	<#list monthNames as month>
		<#if findResult.monthGroupedPoints[month]??>
			<@attendance_macros.groupedPointsBySection findResult.monthGroupedPoints month; groupedPoint>
			<div class="col-md-12">
				<div class="pull-right">
					<a class="btn btn-primary" href="<@routes.attendance.manageEditPoint groupedPoint.templatePoint filterQuery returnTo />">Edit</a>
					<#assign hasRecordedCheckpoints = groupedPoint.hasRecordedCheckpoints/>
					<a class="btn btn-danger<#if hasRecordedCheckpoints> disabled use-tooltip</#if>" <#if hasRecordedCheckpoints>title="This point cannot be removed as it has attendance marks against it."</#if> href="<@routes.attendance.manageDeletePoint groupedPoint.templatePoint filterQuery returnTo />">Delete</a>
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
</#if>

<#if !findResult.termGroupedPoints?keys?has_content	&& !findResult.monthGroupedPoints?keys?has_content>
	<div class="alert alert-info">
		No points found
	</div>
</#if>