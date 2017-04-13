${students?size} <#if students?size == 1>student<#else>students</#if> in ${department.name} need attendance recording:

<#list truncatedStudents as user>
- ${user.fullName}
</#list>
<#if truncatedStudents?size < students?size>...and ${students?size - truncatedStudents?size} more students.</#if>