You recently sent submissions for the assignment ${assignmentTitle} to Turnitin for a plagiarism check. The check has now completed and a report is ready for you to view. Anyone with permission to view the assignment submissions can also view the report.

<#if failureCount gt 0>
Turnitin could not check the following <@fmt.p number=failureCount singular="submission"/>:

<#list failedReports as report>
${report.attachment.name} -- ${report.lastTurnitinError}
</#list>
</#if>
