<#import "_feedback_summary.ftl" as fs>
<#if (allCompletedMarkerFeedback?size > 1) && !isCompleted><h2>Finalise feedback</h2></#if>
<#if command.submission?? && (allCompletedMarkerFeedback?size > 0)>
	<div class="content">
		<#assign submission = command.submission />
		<#include "_submission_summary.ftl">
	</div>
</#if>

<#assign isMarking=false />

<div class="marker-feedback">
	<#list allCompletedMarkerFeedback as feedback>


		<#assign widthClass><#if allCompletedMarkerFeedback?size &gt; 1 && feedback_index &lt; 2>half-width</#if></#assign>

		<div class="well ${widthClass}">

			<@fs.feedbackSummary feedback isModerated/>

			<#if (allCompletedMarkerFeedback?size > 1 && (feedback.hasComments || feedback.hasContent) && !isCompleted)>
				<div class="copy-comments">
					<a data-feedback="${feedback.feedbackPosition.toString}" class="copyFeedback btn btn-small btn-primary"><i class="icon-arrow-down"></i> Copy Comments</a>
				</div>
				<div class="clearfix"></div>
			</#if>

			<@fs.secondMarkerNotes feedback isModerated />
		</div>
	</#list>
</div>

<#assign isMarking=true />
<#if isCurrentUserFeedbackEntry>
	<#if isFinalMarking>
		<div class="well">
			<h3>Final feedback</h3>
			<#include "online_feedback.ftl">
		</div>
	<#else>
		<#include "online_feedback.ftl">
	</#if>
</#if>