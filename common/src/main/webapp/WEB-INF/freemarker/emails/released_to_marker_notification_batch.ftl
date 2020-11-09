<#list notifications as notification><#compress>
<#assign assignment = notification['assignment'] />
<#assign feedbacksCount = notification['feedbacksCount'] />
<#assign workflowVerb = notification['workflowVerb'] />
<#if assignment.collectSubmissions>
  <#if notification['feedbackDeadlineDate']??><#assign feedbackDeadlineDate = notification['feedbackDeadlineDate'] /></#if>
  <#assign allocatedStudentsCount = notification['allocatedStudentsCount'] />
  <#assign studentsAtStagesCount = notification['studentsAtStagesCount'] />
  <#assign submissionsCount = notification['submissionsCount'] />
  <#assign noSubmissionsWithExtensionCount = notification['noSubmissionsWithExtensionCount'] />
  <#assign noSubmissionsWithoutExtensionCount = notification['noSubmissionsWithoutExtensionCount'] />
</#if>
</#compress>**${assignment.module.code?upper_case}: ${assignment.name} has been released for marking:**

<#include "released_to_marker_notification.ftl" /><#if notification_has_next>

---

</#if></#list>
