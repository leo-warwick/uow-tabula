<#compress>
<#assign formattedDate><@fmt.date date=scheduledDate stripHtml=true /></#assign>
<#assign formattedDate = formattedDate?replace('&#8194;',' ') />
The scheduled change to your ${relationshipType.studentRole}s at ${formattedDate} has been cancelled.

<#if cancelledAdditions?has_content>
	You will no longer be assigned as ${relationshipType.agentRole} to:
	<#list cancelledAdditions as student>
	* ${student.officialName}
	</#list>
</#if>

<#if cancelledRemovals?has_content>
	You will no longer be removed as ${relationshipType.agentRole} from:
	<#list cancelledRemovals as student>
	* ${student.officialName}
	</#list>
</#if>
</#compress>