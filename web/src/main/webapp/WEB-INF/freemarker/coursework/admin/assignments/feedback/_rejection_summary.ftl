<#assign secondMarkerFeedback = parentFeedback.secondMarkerFeedback />
<div class="well alert alert-danger">
	<h3 style="color: inherit;">
		The moderator requested changes to this feedback at <@fmt.date secondMarkerFeedback.uploadedDate />
	</h3>
</div>
<div class="well">
	<h4>Summary of requested changes</h4>
	<div class="${secondMarkerFeedback.feedbackPosition.toString} feedback-summary">
		<div class="feedback-details">
		<#if secondMarkerFeedback.mark?has_content || secondMarkerFeedback.grade?has_content>
			<div class="mark-grade" >
				<div>
					<div class="mg-label" >
						Mark:
					</div>
					<div>
						<span class="mark">${secondMarkerFeedback.mark!""}</span>
						<span>%</span>
					</div>
					<div class="mg-label" >
						Grade:
					</div>
					<div class="grade">
					${secondMarkerFeedback.grade!""}
					</div>
				</div>
			</div>
		<#else>
			<h5>No mark or grade added.</h5>
		</#if>
		<#if secondMarkerFeedback.rejectionComments?has_content>
		<div class="feedback-comments">
			<h5>Feedback Comments</h5>
		<#else>
			<div>
		</#if>
			<p>${secondMarkerFeedback.rejectionComments!"<h5>No feedback comments added.</h5>"}</p>
		</div>


	<#if secondMarkerFeedback.attachments?has_content >
		<div class="feedback-attachments attachments" >
			<h5>Attachments</h5>
			<div>
				<#assign downloadMFUrl><@routes.coursework.markerFeedbackFilesDownload secondMarkerFeedback/></#assign>
				<@fmt.download_attachments secondMarkerFeedback.attachments downloadMFUrl "for ${secondMarkerFeedback.feedbackPosition.description?uncap_first}" "feedback-${secondMarkerFeedback.feedback.studentIdentifier}" />
				<#list secondMarkerFeedback.attachments as attachment>
					<input value="${attachment.id}" name="${attachment.name}" type="hidden"/>
				</#list>
			</div>
		</div>
	</#if>
	</div>
	<div style="clear: both;"></div>

	</div>

</div>