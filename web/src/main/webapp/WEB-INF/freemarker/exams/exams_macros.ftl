<#escape x as x?html>
	<#function generateColumnValue perYearColumnValues entity year column>
		<#local columnValue = "" />
		<#local perYearColumnValue = mapGet(perYearColumnValues, column)!"" />
		<#if perYearColumnValue?has_content>
			<#local perYearEntityColumnValue = mapGet(perYearColumnValue, entity)!"" />
			<#if perYearEntityColumnValue?has_content>
				<#local columnValue = mapGet(perYearEntityColumnValue, year)!"" />
			</#if>
		</#if>
		<#return columnValue />
	</#function>
</#escape>