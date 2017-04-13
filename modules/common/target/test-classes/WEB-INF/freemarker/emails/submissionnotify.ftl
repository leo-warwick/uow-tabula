A submission for the assignment '${assignment.name}' for ${module.code?upper_case}, ${module.name} has been received.

- Submission date: ${submissionDate}
- Submission ID: ${submission.id}

<#if submission.allAttachments??>

Uploaded attachments:

<#list submission.allAttachments as attachment>
- ${attachment.name}
</#list>
</#if>

<#-- Only show these links if it's an email. -->
<#if isEmail!false>
You can download all the attachments for this submission here:

<@url context='/coursework' page='/admin/module/${module.code}/assignments/${assignment.id}/submissions/download/${submission.id}/submission-${submission.studentIdentifier}.zip'/>


To unsubscribe from these messages, go here and select "No alerts".

<@url context='/' page='/settings'/>
</#if>