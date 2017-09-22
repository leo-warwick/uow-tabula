The following departments have manually added students to assignments and/or small group sets in Tabula:

<#list departments as department>
 <#assign totalAssignMents><#if mapGet(numAssignments, department.code)??>${mapGet(numAssignments, department.code)}<#else>0</#if></#assign>
 <#assign totalSmallGroupSets><#if mapGet(numSmallGroupSets, department.code)??>${mapGet(numSmallGroupSets, department.code)}<#else>0</#if></#assign>
 * ${department.name} - (<@fmt.p totalAssignMents?number "assignment"/> and <@fmt.p totalSmallGroupSets?number "small group set"/>)
</#list>