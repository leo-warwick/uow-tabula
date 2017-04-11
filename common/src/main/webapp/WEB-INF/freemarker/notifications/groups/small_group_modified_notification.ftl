Your small teaching group allocation for ${groupSet.module.code?upper_case} - "${groupSet.module.name?trim}" has been updated

<#if groupsWithOldSizeInfo??>
<#list groupsWithOldSizeInfo as changedGroupInfo>
<#assign newGroup = changedGroupInfo._1() />
<#assign oldGroupSize = changedGroupInfo._2() />
${newGroup.name} has changed from ${oldGroupSize} to ${newGroup.students.members?size}
</#list>
</#if>

${groupSet.nameWithoutModulePrefix} ${groupSet.format.description} for ${groupSet.module.code?upper_case} - ${groupSet.module.name}

<#list groups as group>
${group.name} - <@fmt.p number=group.students.members?size singular="student"/>
<#list group.events as event><#if !event.unscheduled>
   <@fmt.time time=event.startTime /> ${event.day.name}, ${(event.location.name)!}, <@fmt.weekRanges event />
</#if></#list>

</#list>