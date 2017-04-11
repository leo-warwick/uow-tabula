<#assign formattedDate><@fmt.date date=scheduledDate stripHtml=true /></#assign>
<#assign formattedDate = formattedDate?replace('&#8194;',' ') />
A scheduled change to your ${relationshipType.agentRole}s at ${formattedDate} has been cancelled.

<#if cancelledAdditions?has_content><@fmt.format_list_of_members cancelledAdditions /> will no longer be assigned.</#if>

<#if cancelledRemovals?has_content><@fmt.format_list_of_members cancelledRemovals /> will no longer be removed.</#if>