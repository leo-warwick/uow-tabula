${student.fullName} (${student.warwickId!student.userId}) has made changes to their extension request for the assignment '${assignment.name}' for ${assignment.module.code?upper_case} ${assignment.module.name}.

They have requested an extension until ${requestedExpiryDate}.

<#include "_extension_request_further_details.ftl" />