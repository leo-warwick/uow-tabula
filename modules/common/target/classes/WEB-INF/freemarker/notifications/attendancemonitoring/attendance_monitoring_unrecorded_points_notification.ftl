${points?size} monitoring <#if points?size == 1>point<#else>points</#if> in ${department.name} <#if points?size == 1>points<#else>need</#if> attendance recording:

<#list points as point>
- ${point.name} (<#if point.scheme.pointStyle.dbValue == 'week'>${wholeWeekFormatter(point.startWeek, point.endWeek, academicYear, department, false)?replace('<sup>','')?replace('</sup>','')}<#else>${intervalFormatter(point.startDate, point.endDate)?replace('<sup>','')?replace('</sup>','')}</#if>)
</#list>