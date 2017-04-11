<@fmt.p number=finalisedFeedbacks?size singular="submission" /> for ${assignment.module.code?upper_case} ${assignment.name} <@fmt.p number=finalisedFeedbacks?size singular="has" plural="have" shownumber=false /> been marked and <@fmt.p number=finalisedFeedbacks?size singular="is" plural="are" shownumber=false /> ready to be published to students:

<#list finalisedFeedbacks as feedback>
* ${feedback.studentIdentifier!}<#if feedback_has_next>
</#if></#list>