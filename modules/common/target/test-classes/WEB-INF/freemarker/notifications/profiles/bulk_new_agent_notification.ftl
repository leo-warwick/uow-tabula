<#compress>
<#if scheduledDate?has_content>
	<#assign formattedDate><@fmt.date date=scheduledDate stripHtml=true /></#assign>
	<#assign formattedDate =  formattedDate?replace('&#8194;',' ') />
</#if>
<#if previouslyScheduledDate?has_content>
	<#assign formattedPreviousDate><@fmt.date date=previouslyScheduledDate stripHtml=true /></#assign>
	<#assign formattedPreviousDate = formattedPreviousDate?replace('&#8194;',' ') />
</#if>
<#if scheduledDate?has_content>As of ${formattedDate} you will be<#else>You have been</#if> assigned as ${relationshipType.agentRole} to the following students:

<#list modifiedRelationships as rel>
* ${rel.studentMember.officialName}<#--
--><#if rel.replacesRelationships?has_content><#--
--> (previous <@fmt.p number=rel.replacesRelationships?size singular=relationshipType.agentRole shownumber=false /> <#list rel.replacesRelationships as replaced>${replaced.agentName}<#if replaced_has_next>, </#if></#list>)<#--
-->	</#if>
</#list>

<#if previouslyScheduledDate?has_content>
This change was originally scheduled to happen at ${formattedPreviousDate}.
</#if>
</#compress>