<#if scheduledDate?has_content>
	<#assign formattedDate><@fmt.date date=scheduledDate stripHtml=true /></#assign>
	<#assign formattedDate = formattedDate?replace('&#8194;',' ') />
</#if>
<#if previouslyScheduledDate?has_content>
	<#assign formattedPreviousDate><@fmt.date date=previouslyScheduledDate stripHtml=true /></#assign>
	<#assign formattedPreviousDate = formattedPreviousDate?replace('&#8194;',' ') />
</#if>
<@compress single_line=true>
	<#if newAgents?has_content>
		<#if scheduledDate?has_content>
			As of ${formattedDate}
		<#else>
			You have been assigned
		</#if>
		<@fmt.format_list_of_members newAgents/>
		<#if scheduledDate?has_content>
			will be assigned as your
		<#else>
			as your
		</#if>
		${relationshipType.agentRole}<#if (newAgents?size > 1)>s</#if>.
		<#if previouslyScheduledDate?has_content>
			This change was originally scheduled to happen at ${formattedPreviousDate}.
		</#if>
	</#if>
</@compress>


<@compress single_line=true>
	<#if oldAgents?size == 1>
		<#if scheduledDate?has_content>
			As of ${formattedDate} <@fmt.format_list_of_members oldAgents/> will no longer be your ${relationshipType.agentRole}.
		<#else>
			<@fmt.format_list_of_members oldAgents/> is no longer your ${relationshipType.agentRole}.
		</#if>
		<#if previouslyScheduledDate?has_content>
			This change was originally scheduled to happen at ${formattedPreviousDate}.
		</#if>
	<#elseif oldAgents?has_content>
		<#if scheduledDate?has_content>
			As of ${formattedDate} <@fmt.format_list_of_members oldAgents/> will no longer be your ${relationshipType.agentRole}s.
		<#else>
			<@fmt.format_list_of_members oldAgents/> are no longer your ${relationshipType.agentRole}s.
		</#if>
		<#if previouslyScheduledDate?has_content>
			This change was originally scheduled to happen at ${formattedPreviousDate}.
		</#if>
	</#if>
</@compress>