<@fmt.p number=numReleasedFeedbacks singular="submission" /> for ${assignment.name} - ${assignment.module.code?upper_case} <@fmt.p number=numReleasedFeedbacks singular="is" plural="are" /> have been returned to you.

<#if comment?has_content>
	The following reason was given: ${comment}
</#if>