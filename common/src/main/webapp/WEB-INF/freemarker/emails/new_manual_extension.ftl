You have been granted an extension for the assignment  '${assignment.name}' for ${module.code?upper_case} ${module.name}.

Your deadline for this assignment is now ${newExpiryDate}. Any submissions made after this date will be subject to the usual late penalties.

<#if extension.reviewerComments?has_content>
The administrator left the following comments:

${extension.reviewerComments}

</#if>