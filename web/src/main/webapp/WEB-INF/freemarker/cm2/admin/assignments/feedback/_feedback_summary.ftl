<#macro feedbackSummary stage markerFeedback>
<div class="well">
	<div class="feedback-summary-heading">
		<h3>${stage.description}'s feedback</h3>
		<h5>${markerFeedback.marker.fullName} <small>- <@fmt.date markerFeedback.uploadedDate /></small></h5>
		<div class="clearfix"></div>
	</div>

	<div class="${stage.name} feedback-summary" >

		<#if markerFeedback.hasContent && !hasBeenModified>
			Approved by the moderator
		<#elseif !markerFeedback.hasContent>
			Not moderated
		<#else>
			<div class="feedback-details">
				<#if markerFeedback.mark?has_content || markerFeedback.grade?has_content>
					<div class="mark-grade" >
						<div>
							<div class="mg-label" >
								Mark:</div>
							<div>
								<span class="mark">${markerFeedback.mark!""}</span>
								<span>%</span>
							</div>
							<div class="mg-label" >
								Grade:</div>
							<div class="grade">${markerFeedback.grade!""}</div>
						</div>
					</div>
				<#else>
					<h5>No mark or grade added.</h5>
				</#if>

				<#list markerFeedback.customFormValues as formValue>
					<#if formValue.value?has_content>
						<div class="feedback-comments">
							<h5>Feedback comments</h5>
							<p>${formValue.valueFormattedHtml!""}</p>
						</div>
					<#else>
						<h5>No feedback comments added.</h5>
					</#if>
				</#list>
			</div>

			<#if markerFeedback.attachments?has_content >
				<div class="feedback-attachments attachments">
					<h5>Attachments</h5>
					<div>
						<#assign downloadMFUrl><@routes.cm2.markerFeedbackFilesDownload markerFeedback/></#assign>
						<@fmt.download_attachments markerFeedback.attachments downloadMFUrl "for ${stage.description?uncap_first}" "feedback-${markerFeedback.feedback.studentIdentifier}" />
						<#list markerFeedback.attachments as attachment>
							<input value="${attachment.id}" name="${attachment.name}" type="hidden"/>
						</#list>
					</div>
				</div>
			</#if>
			<div style="clear: both;"></div>
		</#if>
	</div>
</div>
</#macro>