<#assign formattedDate><@fmt.date date=scheduledDate stripHtml=true /></#assign>
<#assign formattedDate = formattedDate?replace('&#8194;',' ') />
The scheduled change to your ${relationshipType.studentRole}s at ${formattedDate} has been cancelled.

<@compress single_line=true>
	You will no longer be removed as ${relationshipType.agentRole} from ${student.officialName}.
	<#if cancelledAdditions?has_content>The pending ${relationshipType.agentRole}, ${cancelledAdditions?first.officialName}, will no longer be assigned.</#if>
</@compress>