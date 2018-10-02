Your extension request has been<#if extension.expiryDateAdjusted> adjusted and</#if> approved for the assignment '${assignment.name}' in ${module.code?upper_case} ${module.name}.

Your deadline for this assignment is now **${newExpiryDate}**. Any submissions made after this deadline are subject to the usual late penalties.

<#if extension.reviewerComments?has_content>
The administrator commented:

${extension.reviewerComments}

</#if>