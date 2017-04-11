<div class="submission-received">
	<#if justSubmitted!false>
	<div class="alert alert-success">
		<a class="close" data-dismiss="alert">&times;</a>
		Thanks, we've received your submission. We'll send you an email confirming this shortly;
		don't worry if the email doesn't arrive straight away - we've already recorded the official
		time of your submission, and it's <@fmt.date date=submission.submittedDate at=true seconds=false relative=false />,
		regardless of when the email reaches you.
	</div>
	</#if>

	<div class="submission-receipt-container is-stackable">
	<div class="submission-receipt">
	<h3>Submission receipt</h3>
	<p>Submission received <@fmt.date date=submission.submittedDate at=true seconds=true relative=false />.</p>
	<p>Submission ID: ${submission.id}</p>
	<#if submission.allAttachments??>
		<p>
			Uploaded attachments:
			<ul>
				<#list submission.allAttachments as attachment>
					<#assign attachmentUrl><#compress>
						<#if isSelf>
							<@routes.coursework.submissionAttachment submission attachment />
						<#else>
							<@routes.coursework.submissionAttachment_in_profile submission attachment />
						</#if>
					</#compress></#assign>
					<li><a href="${attachmentUrl}">${attachment.name}</a></li>
				</#list>
			</ul>
		</p>
	</#if>
	</div>
	</div>

	<#assign receiptPdfUrl><#compress>
		<#if isSelf>
			<@routes.coursework.submissionReceiptPdf submission />
		<#else>
			<@routes.coursework.submissionReceiptPdf_in_profile submission />
		</#if>
	</#compress></#assign>

	<p><a href="${receiptPdfUrl}">Download submission receipt as a PDF file</a></p>

	<#if isSelf>
		<#if !feedback??>
		<p>You should have been sent an email confirming the submission. Check your spam folders if it doesn't show up in your inbox.
		If it's been a few minutes and it still hasn't reached you, click the button below to send a fresh copy.</p>
		</#if>

		<#assign receiptFormUrl><@routes.coursework.assignmentreceipt assignment=assignment /></#assign>
		<form action="${receiptFormUrl}" method="POST">
			<div class="submit-buttons">
			<button class="btn" name="resend" value=""><i class="icon-envelope-alt"></i> Re-send email receipt</button>
			</div>
		</form>
	</#if>

	<#if assignment.hasReleasedFeedback && !feedback??>
		<h3>Expecting your feedback?</h3>
		<p>
			There is no feedback available for you yet.
			If you've been told to come here to retrieve your feedback
			then you'll need to get in touch directly with your
			course/module convenor to see why it hasn't been published yet.
			<#if !features.emailStudents>
				When it's published you'll receive an automated email.
			</#if>
		</p>
	</#if>

</div>