<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#assign formAction>
	<#if nextStageRole?has_content><@routes.coursework.markingCompleted assignment marker nextStageRole/>
	<#else><@routes.coursework.markingCompleted assignment marker />
	</#if>
</#assign>

<@f.form method="post" action="${formAction}" commandName="markingCompletedCommand">

<#assign title><#if nextStageRole?has_content>Send to ${nextStageRole}<#else>Marking completed</#if></#assign>

<h1>Send to ${nextStageRole}</h1>

<@form.errors path="" />

<input type="hidden" name="onlineMarking" value="${onlineMarking?string}" />
<input type="hidden" name="confirmScreen" value="true" />

<@spring.bind path="markerFeedback">
	<@form.errors path="markerFeedback" />
	<#assign markerFeedback=status.actualValue />
	<#assign noMarks = markingCompletedCommand.noMarks />
	<#assign noFeedback = markingCompletedCommand.noFeedback />
	<#assign releasedFeedback = markingCompletedCommand.releasedFeedback />

	<#if releasedFeedback?has_content>
		<div class="alert">
			<a href="" class="released-feedback"><@fmt.p (releasedFeedback?size ) "submission" /></a>
			<#if markingCompletedCommand.releasedFeedback?size == 1>
				has already been marked as completed. This will be ignored.
			<#else>
				have already been marked as completed. These will be ignored.
			</#if>
			<div class="hidden released-feedback-list">
				<ul><#list releasedFeedback as markerFeedback>
					<li>${markerFeedback.feedback.studentIdentifier}</li>
				</#list></ul>
			</div>
			<script>
				var listHtml = jQuery(".released-feedback-list").html();
				jQuery(".released-feedback")
					.on('click',function(e){e.preventDefault()})
					.tooltip({
						html: true,
						placement: 'right',
						title: listHtml
					});
			</script>
		</div>
	</#if>

	<#if (assignment.collectMarks && noMarks?size > 0) >
		<#assign cantDo><#if isUserALaterMarker>change this before these submissions are marked by others<#else>add a mark to these submissions later</#if></#assign>
		<#assign count><#if (noMarks?size > 1)>	${noMarks?size} submissions do<#else>One submission does</#if></#assign>
		<#assign noMarksIds>
			<ul><#list noMarks as markerFeedback><li>${markerFeedback.feedback.studentIdentifier}</li></#list></ul>
		</#assign>
		<div class="alert">
			${count} not have a mark. You will not be able to ${cantDo}.
			<a class="use-popover" id="popover-marks" data-html="true"
			   data-original-title="<span class='text-info'><strong>No marks</strong></span>"
			   data-content="${noMarksIds}">
				<i class="icon-question-sign"></i>
			</a>
		</div>
	</#if>

	<#if (noFeedback?size > 0) >
		<#assign cantDo>
			<#if isUserALaterMarker>
				You will not be able to change this before these submissions are marked by others
			<#else>
				You will not be able to add feedback files to this submission later
			</#if>
		</#assign>
		<#assign count><#if (noFeedback?size > 1)>${noFeedback?size} submissions do<#else>One submission does</#if></#assign>
		<#assign noFilesIds>
		<ul><#list noFeedback as markerFeedback><li>${markerFeedback.feedback.studentIdentifier}</li></#list></ul>
		</#assign>
		<div class="alert">
			${count} not have any feedback files attached. ${cantDo}.
			<a class="use-popover" id="popover-files" data-html="true"
			   data-original-title="<span class='text-info'><strong>No feedback files</strong></span>"
			   data-content="${noFilesIds}">
				<i class="icon-question-sign"></i>
			</a>
		</div>
	</#if>

	<#if isUserALaterMarker>
		<p>
			<strong><@fmt.p (markerFeedback?size - releasedFeedback?size) "student" /></strong> submissions will be listed as completed.
			You will not be able to make further changes to the marks or feedback associated with these submissions until they have been marked by others.
			If there are still changes that have to be made for these submissions then click cancel to return to the feedback list.
		</p>
	<#else>
		<p>
			<strong><@fmt.p (markerFeedback?size - releasedFeedback?size) "student" /></strong> submissions will be listed as completed.
			Note that you will not be able to make any further changes to the marks or feedback associated with these submissions after this point.
			If there are still changes that have to be made for these submissions then click cancel to return to the feedback list.
		</p>
	</#if>

	<#list markerFeedback as mf>
		<input type="hidden" name="markerFeedback" value="${mf.id}" />
	</#list>
</@spring.bind>
<p>
	<@form.errors path="confirm" />
	<@form.label checkbox=true><@f.checkbox path="confirm" />
		I confirm that I have finished marking <@fmt.p markerFeedback?size "this" "these" "1" "0" false /> student's submissions.
	</@form.label>
</p>

<div class="submit-buttons">
	<input class="btn btn-primary" type="submit" value="Confirm">
	<a class="btn" href="<@routes.coursework.listmarkersubmissions assignment marker />">Cancel</a>
</div>
</@f.form>
</#escape>