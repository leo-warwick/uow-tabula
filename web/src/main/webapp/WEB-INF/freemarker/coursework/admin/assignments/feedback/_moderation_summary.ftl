<#if command.approved>
	<div class="well alert alert-success">
		<h3 style="color: inherit;">You approved this feedback at <@fmt.date completedDate /></h3>
	</div>
<#else>
	<#assign moderationFeedback = firstMarkerFeedback.feedback.secondMarkerFeedback />
	<div class="well alert alert-danger">
		<h3 style="color: inherit;">You requested changes to this feedback at <@fmt.date completedDate /></h3>
	</div>
    <div class="well">
		<h4>Moderation Summary</h4>
	<div class="${moderationFeedback.feedbackPosition.toString} feedback-summary">
		<div class="feedback-details">
			<#if moderationFeedback.mark?has_content || moderationFeedback.grade?has_content>
				<div class="mark-grade" >
					<div>
						<div class="mg-label" >
							Mark:
						</div>
						<div>
							<span class="mark">${moderationFeedback.mark!""}</span>
							<span>%</span>
						</div>
						<div class="mg-label" >
							Grade:
						</div>
						<div class="grade">
							${moderationFeedback.grade!""}
						</div>
					</div>
				</div>
			<#else>
				<h5>No mark or grade added.</h5>
			</#if>
			<#if moderationFeedback.rejectionComments?has_content>
				<div class="feedback-comments">
					<h5>Feedback Comments</h5>
			<#else>
				<div>
			</#if>
				<p>${moderationFeedback.rejectionComments!"<h5>No feedback comments added.</h5>"}</p>
			</div>
	</div>

		<#if moderationFeedback.attachments?has_content >
			<div class="feedback-attachments">
				<h5>Attachments</h5>
				<div>
					<#assign downloadMFUrl><@routes.coursework.markerFeedbackFilesDownload moderationFeedback/></#assign>
					<@fmt.download_attachments feedback.attachments downloadMFUrl "" "feedback-${feedback.feedback.studentIdentifier}" />
				</div>
			</div>
		</#if>
	  </div>

		<div style="clear: both;"></div>
	</div>

	</div>
</#if>