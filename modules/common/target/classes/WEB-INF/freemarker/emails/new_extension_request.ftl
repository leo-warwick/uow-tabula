${student.fullName} (${student.warwickId!student.userId}) has requested an extension for the assignment '${assignment.name}' for ${assignment.module.code?upper_case} ${assignment.module.name}.

They have requested an extension until ${requestedExpiryDate} and gave the following reasons:

${reasonForRequest}

<#include "_extension_request_further_details.ftl" />