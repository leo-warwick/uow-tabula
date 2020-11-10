For the assignment ${assignment.module.code?upper_case} ${assignment.module.name} ${assignment.name}:
<#list batches as batch>

${batch.timeStatement}:

<#list batch.items as item>
* ${item.extension.universityId} had an extension until ${item.dateOnlyFormatter.print(item.extension.expiryDate)}. Their feedback is due <#if item.dueToday>today<#elseif item.deadline??>in <@fmt.p item.daysLeft "working day" /> on ${item.dateOnlyFormatter.print(item.deadline)}</#if>.
</#list></#list>
