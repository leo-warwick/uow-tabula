<#macro feedbackSummary feedback isModerated showDates=false>

	<#if isModerated?? && isModerated && feedback.feedbackPosition.toString == "SecondFeedback">
		<div class="feedback-summary-heading">
			<h3>Moderation</h3>
			<h5>${feedback.markerUser.fullName} <#if showDates><small>- <@fmt.date feedback.uploadedDate /></small></#if></h5>
			<div class="clearfix"></div>
		</div>
		<div class="${feedback.feedbackPosition.toString} feedback-summary" >
			<div class="feedback-details">
				<#if feedback.rejectionComments?has_content>
					<div class="feedback-comments">
						<h5>Feedback Comments</h5>

						<p>${feedback.rejectionComments}</p>
					</div>
				<#else>
					<div class="feedback-comments"><h5>No feedback comments added.</h5></div>
				</#if>
			</div>
		</div>
	<#else>
		<div class="feedback-summary-heading">
			<h3>${feedback.feedbackPosition.description}</h3>
			<h5>${feedback.markerUser.fullName} <#if showDates><small>- <@fmt.date feedback.uploadedDate /></small></#if></h5>
			<div class="clearfix"></div>
		</div>
		<div class="${feedback.feedbackPosition.toString} feedback-summary" >
			<div class="feedback-details">
				<#if feedback.mark?has_content || feedback.grade?has_content>
					<div class="mark-grade" >
						<div>
							<div class="mg-label" >
								Mark:</div>
							<div>
								<span class="mark">${feedback.mark!""}</span>
								<span>%</span>
							</div>
							<div class="mg-label" >
								Grade:</div>
							<div class="grade">${feedback.grade!""}</div>
						</div>
					</div>
				<#else>
					<h5>No mark or grade added.</h5>
				</#if>

				<#list feedback.customFormValues as formValue>
					<#if formValue.value?has_content>
						<div class="feedback-comments">
							<h5>Feedback comments</h5>
							<p>${formValue.value!""}</p>
						</div>
					<#else>
						<h5>No feedback comments added.</h5>
					</#if>
				</#list>
			</div>

			<#if feedback.attachments?has_content >
				<div class="feedback-attachments attachments">
					<h5>Attachments</h5>
					<div>
						<#assign downloadMFUrl><@routes.coursework.markerFeedbackFilesDownload feedback/></#assign>
						<@fmt.download_attachments feedback.attachments downloadMFUrl "for ${feedback.feedbackPosition.description?uncap_first}" "feedback-${feedback.feedback.studentIdentifier}" />
						<#list feedback.attachments as attachment>
							<input value="${attachment.id}" name="${attachment.name}" type="hidden"/>
						</#list>
					</div>
				</div>
			</#if>
			<div style="clear: both;"></div>
		</div>
	</#if>
</#macro>

<#macro secondMarkerNotes feedback isModerated>
	<#if isModerated?? && !isModerated && feedback.rejectionComments?? && feedback.feedbackPosition.toString == "SecondFeedback">
	<div class="feedback-notes alert alert-info">
		<h3>Notes from Second Marker</h3>
		<p>${feedback.rejectionComments!""}</p>
	</div>
	</#if>
</#macro>