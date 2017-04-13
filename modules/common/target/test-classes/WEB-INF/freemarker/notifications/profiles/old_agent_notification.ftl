<#if scheduledDate?has_content>
	<#assign formattedDate><@fmt.date date=scheduledDate stripHtml=true /></#assign>
	<#assign formattedDate =  formattedDate?replace('&#8194;',' ') />
</#if>
<@compress single_line=true>
	<#if scheduledDate?has_content>
		As of ${formattedDate} you will no longer be assigned as ${relationshipType.agentRole} to ${student.officialName}.
		<#if newAgents?has_content>The new ${relationshipType.agentRole} will be ${newAgents?first.officialName}.</#if>
	<#else>
		You are no longer assigned as ${relationshipType.agentRole} to ${student.officialName}.
		<#if newAgents?has_content>The new ${relationshipType.agentRole} is now ${newAgents?first.officialName}.</#if>
	</#if>
</@compress>