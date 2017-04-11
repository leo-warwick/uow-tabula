Your extension for the assignment '${assignment.name}' for ${module.code?upper_case} ${module.name} has changed.

Your deadline for this assignment is now ${newExpiryDate}. Please ignore any previous messages regarding this extension.

<#if extension.reviewerComments?has_content>
The administrator left the following comments:

${extension.reviewerComments}

</#if>