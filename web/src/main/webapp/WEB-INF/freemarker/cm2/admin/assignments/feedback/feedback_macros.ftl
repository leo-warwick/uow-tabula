<#macro feedbackSummary feedback isModerated=false>
	<#if isModerated && ((feedback.feedbackPosition.toString)!"") == "SecondFeedback">
		<div class="feedback-summary" >
			<h3>Moderation</h3>
			<h6>${feedback.markerUser.fullName}</h6>

			<#if feedback.mark?has_content || feedback.grade?has_content>
				<div class="markgrade"><strong>Mark: </strong>${feedback.mark!''}</div>
				<div class="markgrade"><strong>Grade: </strong>${feedback.grade!''}</div>
			<#else>
				<h5>No mark or grade added.</h5>
			</#if>
			<strong>Feedback</strong>
			<div class="feedback-comments muted">
				<#if feedback.rejectionComments?has_content>
					${feedback.rejectionComments}
				<#else>
					No feedback comments added.
				</#if>
			</div>
		</div>
	<#elseif feedback.feedbackPosition??>
		<div class="feedback-summary" >
			<h5>${feedback.feedbackPosition.description} [${(feedback.markerUser.fullName)!''}]</h5>
			<#if feedback.mark?has_content || feedback.grade?has_content>
				<div class="markgrade"><strong>Mark: </strong>${feedback.mark!''}</div>
				<div class="markgrade"><strong>Grade: </strong>${feedback.grade!''}</div>
			<#else>
				<div>No mark or grade added.</div>
			</#if>
			<@feedbackComments feedback />
		</div>
	</#if>
</#macro>

<#macro secondMarkerNotes feedback isModerated>
	<#if isModerated?? && !isModerated && feedback.rejectionComments?? && feedback.feedbackPosition.toString == "SecondFeedback">
	<div class="feedback-notes alert alert-info">
		<h2>Notes from Second Marker</h2>
		<p>${feedback.rejectionComments!""}</p>
	</div>
	</#if>
</#macro>

<#macro feedbackComments feedback>
	<strong>Feedback</strong>
	<#assign formValues = feedback.customFormValues>
	<#if formValues?size == 0>
	<div>No feedback comments added<div>
	<#else>
		<#list formValues as formValue>
			<#if formValue.value?has_content>
				<div class="feedback-comments">
					<p>${formValue.value!""}</p>
				</div>
			<#else>
				<h5>No feedback comments added.</h5>
			</#if>
		</#list>
	</#if>
</#macro>