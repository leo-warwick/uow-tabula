This email confirms that you have been granted an extension for the assignment '${assignment.name}' for ${module.code?upper_case}, ${module.name}.
Your new submission date for this assignment is ${newExpiryDate}. Any submissions made after this date will be subject to the usual late penalties.

<#if extension.reviewerComments?has_content>
The administrator left the following comments:

${extension.reviewerComments}

</#if>
Your new deadline is now displayed at the top of the submission page:

<@url page=path context="/coursework" />