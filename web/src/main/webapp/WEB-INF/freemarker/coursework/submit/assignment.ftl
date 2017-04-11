<#escape x as x?html>
<#compress>
<h1>${assignment.name}</h1>
<h5><span class="muted">for</span> ${module.name} (${module.code?upper_case})</h5>

<#if can.do("Assignment.Update", assignment)>
<div class="alert alert-info">
  <button type="button" class="close" data-dismiss="alert">×</button>
	<h4>Information for module managers</h4>

	<p>This box is only shown to module managers. Click the &times; button to see the page as a student sees it.</p>

	<p>You can give students a link to this page to
	<#if assignment.collectSubmissions>submit their work and to</#if>
	receive their feedback<#if assignment.collectMarks> and/or marks</#if>.</p>

	<p><a class="btn" href="<@routes.coursework.depthome module/>">Return to module management for ${assignment.module.code?upper_case}</a></p>
</div>
</#if>

<a id="submittop"></a>

<#if feedback??>
	<h2>Feedback for ${feedback.studentIdentifier}</h2>
	<#include "_assignment_feedbackdownload.ftl" />
	<#if features.submissions>
		<#if submission??>
			<#include "assignment_submissionthanks.ftl" />
	    </#if>
	</#if>

<#else>

	<#if features.submissions>
		<#if submission??>
			<#include "assignment_submissionthanks.ftl" />
		</#if>

		<#-- At some point, also check if resubmission is allowed for this assignment -->
		<#include "assignment_submissionform.ftl" />

		<#if isSelf && submission?? && !canReSubmit>
			<#if assignment.allowResubmission>
				<p>It is not possible to resubmit your assignment because the deadline has passed.</p>
			<#else>
				<p>This assignment does not allow you to resubmit.</p>
			</#if>
		</#if>
	<#elseif isSelf>
		<h2>${user.fullName} (${user.studentIdentifier})</h2>
		<p>
			If you've submitted your assignment, you should be able to access your
			feedback here once it's ready.
		</p>
	</#if>

</#if>

</#compress>
</#escape>
