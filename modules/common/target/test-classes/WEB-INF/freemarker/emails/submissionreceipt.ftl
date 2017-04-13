This message confirms that you made a submission for the assignment '${assignment.name}' for ${module.code?upper_case} ${module.name}.

- Submission date: ${submissionDate}
- Submission ID: ${submission.id}
<#if user.warwickId??>
- University ID: ${user.warwickId}
<#else>
- User ID: ${user.userId}
</#if>
<#if submission.allAttachments??>

Uploaded attachments:

<#list submission.allAttachments as attachment>
- ${attachment.name}
</#list>
</#if>