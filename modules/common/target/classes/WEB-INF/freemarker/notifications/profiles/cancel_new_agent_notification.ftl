<#assign formattedDate><@fmt.date date=scheduledDate stripHtml=true /></#assign>
<#assign formattedDate = formattedDate?replace('&#8194;',' ') />
The scheduled change to your ${relationshipType.studentRole}s at ${formattedDate} has been cancelled.

<@compress single_line=true>
	You will no longer be assigned as ${relationshipType.agentRole} to ${student.officialName}.
	<#if cancelledRemovals?has_content>The current ${relationshipType.agentRole}, ${cancelledRemovals?first.officialName}, will no longer be removed.</#if>
</@compress>