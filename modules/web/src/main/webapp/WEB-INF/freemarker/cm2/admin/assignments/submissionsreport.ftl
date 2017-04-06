<#compress>
<#escape x as x?html>

<#macro studentIdentifier user><#compress>
	<#if user.warwickId??>${user.warwickId}<#else>${user.userId!}</#if>
</#compress></#macro>

<#if !embedded>
<h1>Submissions/feedback comparison for ${assignment.name}</h1>
</#if>

<#if report.hasProblems>

	<#if submissionOnly?size gt 0>
	<div class="alert alert-error">
		<p><i class="icon-remove"></i> These users have submitted to the assignment, but no feedback has been uploaded for them.</p>

		<ul class="user-list">
		<#list submissionOnly as u>
			<li><@studentIdentifier u /></li>
		</#list>
		</ul>
	</div>
	</#if>

	<#if feedbackOnly?size gt 0>
	<div class="alert alert-error">
		<p><i class="icon-remove"></i> There is feedback or marks for these users but they did not submit an assignment to this system.</p>

		<ul class="user-list">
		<#list feedbackOnly as u>
			<li><@studentIdentifier u /></li>
		</#list>
		</ul>
	</div>
	</#if>

	<#if hasNoAttachments?size == assignment.feedbacks?size>
		<div class="alert alert-warn">
			<p><i class="icon-remove"></i> None of the submissions have any feedback.</p>
		</div>
	<#elseif hasNoAttachments?size gt 0>
		<div class="alert alert-error">
			<p><i class="icon-remove"></i> Submissions received from the following students do not have any feedback.</p>
			<ul class="user-list">
			<#list hasNoAttachments as u>
				<li><@studentIdentifier u /></li>
			</#list>
			</ul>
		</div>
	</#if>

	<#if hasNoMarks?size == assignment.feedbacks?size>
		<div class="alert alert-warn">
			<p><i class="icon-remove"></i> None of the submissions have had marks assigned.</p>
		</div>
	<#elseif hasNoMarks?size gt 0>
		<div class="alert alert-error">
			<p><i class="icon-remove"></i> Submissions received from the following students do not have any marks assigned.</p>
			<ul class="user-list">
			<#list hasNoMarks as u>
				<li><@studentIdentifier u /></li>
			</#list>
			</ul>
		</div>
	</#if>

	<#if plagiarised?size == 0>
		<div class="alert alert-success">
			<p><i class="icon-ok"></i> No submissions have been marked as suspected of being plagiarised.</p>
		</div>
	<#else>
		<div class="alert alert-warn">
			<p><i class="icon-remove"></i>
			<#if plagiarised?size == 1>
				It is suspected that the submission received from the following student is plagiarised.  Feedback for this student will not be published.
			<#else>
				It is suspected that submissions received from the following students are plagiarised.  Feedback for these students will not be published.
			</#if>
			</p>
			<ul class="user-list">
			<#list plagiarised as u>
				<li><@studentIdentifier u /></li>
			</#list>
			</ul>
		</div>
	</#if>

	<p>
		The above discrepancies are provided for information.
		It is up to you to decide whether to continue publishing.
	</p>


<#else>
	<div class="alert alert-success">
	<p><i class="icon-ok"></i> The submissions and the feedback items appear to match up for this assignment.</p>
	</div>
</#if>

</#escape>
</#compress>