Adjustments were made to your ${whatAdjusted} for ${feedback.studentIdentifier} on ${assignment.name} ${assignment.module.code?upper_case}.

<#if feedback.adjustedMark??>- Adjusted mark: ${feedback.adjustedMark}}</#if>
<#if feedback.adjustedGrade??>- Adjusted grade: ${feedback.adjustedGrade}</#if>
<#if feedback.adjustmentReason??>- Reason for adjustment: ${feedback.adjustmentReason}</#if>

${feedback.adjustmentComments!""}