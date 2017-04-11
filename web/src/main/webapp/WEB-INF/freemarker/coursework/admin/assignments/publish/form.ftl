<#escape x as x?html>

<#assign module=assignment.module />
<#assign department=module.adminDepartment />
<#import "*/courses_macros.ftl" as courses_macros />

<script type="text/javascript">
jQuery(function($){ "use strict";
	var submitButton = $('#publish-submit'),
		checkbox = $('#confirmCheck');
	function updateCheckbox() {
	  submitButton.attr('disabled', !checkbox.is(':checked'));
	}
	checkbox.change(updateCheckbox);
	updateCheckbox();

	$('#feedback-check-recipient-results')
		.html('<div class="alert"><p>Checking for potential problems with students\' email addresses&hellip;</p></div>')
		.load('${url('/coursework/admin/module/${module.code}/assignments/${assignment.id}/check-recipients')}');

	$('#submissions-report-results')
		.html('<div class="alert"><p>Comparing feedback list against submission list&hellip;</p></div>')
		.load('${url('/coursework/admin/module/${module.code}/assignments/${assignment.id}/submissions-report')}');
});
</script>

<@f.form method="post" action="${url('/coursework/admin/module/${module.code}/assignments/${assignment.id}/publish')}" commandName="publishFeedbackCommand">

<h1>Publish feedback for ${assignment.name}</h1>

<@f.errors cssClass="error" />

<#assign feedbackCount=assignment.fullFeedback?size />
<#assign unreleasedFeedbackCount=assignment.unreleasedFeedback?size />

<p>This will publish feedback for <strong><@fmt.p unreleasedFeedbackCount "student"/></strong>.
<#if feedbackCount != unreleasedFeedbackCount>
There are ${feedbackCount} students in total but some have already had
their feedback published. Those students won't be emailed again.
</#if>
</p>

<#if features.queueFeedbackForSits && department.uploadCourseworkMarksToSits>
	<@courses_macros.uploadToSits assignment=assignment verb="Publishing" withValidation=true isGradeValidation=isGradeValidation gradeValidation=gradeValidation/>
</#if>

<p>
Publishing feedback will make all currently uploaded feedback for this assignment available for students to download.
If more feedback is added later, it won't be published automatically.
</p>

<#if features.emailStudents>
<p>
Each student will receive an email containing the link to the feedback. They will sign in
and be shown the feedback specific to them.
</p>
<#else>
<p>
Note: notifications are not currently sent to students - you will need to distribute the
link yourself, by email or by posting it on your module web pages.
</p>
</#if>

<div id="feedback-check-recipient-results"></div>
<#if features.submissions && assignment.submissions?size gt 0>
<div id="submissions-report-results"></div>
</#if>

<@f.errors path="confirm" cssClass="error" />
<label class="checkbox">
	<@f.checkbox path="confirm" id="confirmCheck" />
	<strong> I have read the above and am ready to release feedback to students.</strong>
</label>

<div class="submit-buttons">
<input class="btn btn-primary" type="submit" id="publish-submit" value="Publish">
</div>
</@f.form>

</#escape>