<#import "*/attendance_variables.ftl" as attendance_variables />
<#import "*/attendance_macros.ftl" as attendance_macros />

<#if findResult.termGroupedPoints?keys?has_content >
	<#list attendance_variables.monitoringPointTermNames as term>
		<#if findResult.termGroupedPoints[term]??>
			<@attendance_macros.groupedPointsBySection findResult.termGroupedPoints term ; groupedPoint >
				<div class="col-md-12">
					${groupedPoint.templatePoint.name}
						(<a class="use-tooltip" data-html="true" title="
							<@fmt.wholeWeekDateFormat
								groupedPoint.templatePoint.startWeek
								groupedPoint.templatePoint.endWeek
								groupedPoint.templatePoint.scheme.academicYear
								false,
								true
							/>
						 ">	<@fmt.monitoringPointWeeksFormat
								groupedPoint.templatePoint.startWeek
								groupedPoint.templatePoint.endWeek
								groupedPoint.templatePoint.scheme.academicYear
								command.department/>
						</a>)
					<#if !templateScheme??><@attendance_macros.groupedPointSchemePopover groupedPoint /></#if>
				</div>
			</@attendance_macros.groupedPointsBySection>
		</#if>
	</#list>
</#if>

<#if findResult.monthGroupedPoints?keys?has_content>
	<#list monthNames as month>
		<#if findResult.monthGroupedPoints[month]??>
			<@attendance_macros.groupedPointsBySection findResult.monthGroupedPoints month ; groupedPoint >
				<div class="col-md-12">
					${groupedPoint.templatePoint.name}
					(<@fmt.interval groupedPoint.templatePoint.startDate groupedPoint.templatePoint.endDate />)
					<#if !templateScheme??><@attendance_macros.groupedPointSchemePopover groupedPoint /></#if>
				</div>
			</@attendance_macros.groupedPointsBySection>
		</#if>
	</#list>
</#if>
