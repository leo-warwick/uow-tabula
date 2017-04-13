${moderatorName} has requested changes to your feedback for student ${studentId} on ${assignment.name} for ${assignment.module.code?upper_case} ${assignment.module.name}.

<#if rejectionComments??>
The moderator left the following comments ...

${rejectionComments}
</#if>
<#if adjustedMark??>Adjusted mark: ${adjustedMark}</#if>
<#if adjustedGrade??>Adjusted grade: ${adjustedGrade}</#if>