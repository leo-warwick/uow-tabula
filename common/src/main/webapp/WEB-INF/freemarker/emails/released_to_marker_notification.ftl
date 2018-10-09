

Note:
<#if assignment.collectSubmissions>
 - <@fmt.p number=numAllocated!0 singular="student" plural="students" /> <@fmt.p number=numAllocated!0 singular="is" plural="are" shownumber=false /> allocated to you for marking
<#if assignment.numAllocatedAsFirstMarker gt 0>
   - First/final marker for <@fmt.p number=numAllocatedAsFirstMarker!0 singular="student" plural="students" /> <@fmt.p number=numAllocated!0 singular="is" plural="are" shownumber=false />
</#if>
<#if assignment.numAllocatedAsSecondMarker gt 0>
   - Second marker for <@fmt.p number=numAllocatedAsSecondMarker!0 singular="student" plural="students" /> <@fmt.p number=numAllocated!0 singular="is" plural="are" shownumber=false />
</#if>
 - <@fmt.p number=numReleasedFeedbacks!0 singular="student" plural="students" /> allocated to you <@fmt.p number=numReleasedFeedbacks!0 singular="has" plural="have" shownumber=false /> been released for marking
 - <@fmt.p number=numReleasedSubmissionsFeedbacks!0 singular="student" plural="students" /> <@fmt.p number=numReleasedSubmissionsFeedbacks!0 singular="has" plural="have" shownumber=false /> submitted work
 - <@fmt.p number=numReleasedNoSubmissionsFeedbacks!0 singular="student" plural="students" /> <@fmt.p number=numReleasedNoSubmissionsFeedbacks!0 singular="has" plural="have" shownumber=false /> not submitted work
<#else>
- <@fmt.p number=numReleasedFeedbacks!0 singular="student" plural="students" /> <@fmt.p number=numReleasedFeedbacks!0 singular="is" plural="are" shownumber=false /> allocated to you for marking
- This assignment does not require students to submit work to Tabula
</#if>