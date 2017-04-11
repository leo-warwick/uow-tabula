<#import "*/marking_macros.ftl" as marking_macros />

<#function markingId user>
	<#if !user.warwickId?has_content || user.getExtraProperty("urn:websignon:usersource")! == 'WarwickExtUsers'>
		<#return user.userId />
	<#else>
		<#return user.warwickId! />
	</#if>
</#function>

<div class="content feedback-adjustment feedback-summary">

	<#if command.feedback??>
		<div class="well">
			<h3>Mark</h3>
			<p>
				<#if command.feedback.actualMark??>
					Original mark - ${command.feedback.actualMark}%<br>
				</#if>
				<#if command.feedback.actualGrade??>
					Original grade - ${command.feedback.actualGrade}<br>
				</#if>
			</p>
		</div>
		<#if command.feedback.latestPrivateOrNonPrivateAdjustment?has_content>
			<div class="well">
				<h3>Latest adjustment</h3>
				<p>
					Adjusted mark - ${command.feedback.latestPrivateOrNonPrivateAdjustment.mark!}<br>
					Adjusted grade - ${command.feedback.latestPrivateOrNonPrivateAdjustment.grade!}<br>
					Reason - ${command.feedback.latestPrivateOrNonPrivateAdjustment.reason!}<br>
					Comments - ${command.feedback.latestPrivateOrNonPrivateAdjustment.comments!}<br>
				</p>
			</div>
		</#if>
	</#if>

	<#assign submit_url>
		<@routes.exams.feedbackAdjustmentForm exam markingId(command.student) />
	</#assign>

	<@f.form id="adjust-form-${markingId(command.student)}"
		cssClass="double-submit-protection"
		method="post"
		commandName="command"
		action="${submit_url}"
	>

		<@bs3form.labelled_form_group path="reason" labelText="Reason for adjustment">
			<@f.input type="text" path="reason" placeholder="Enter your reason" cssClass="form-control" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="comments" labelText="Adjustment comments">
			<@f.textarea path="comments" cssClass="form-control" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="adjustedMark" labelText="Adjusted mark">
			<div class="input-group">
				<@f.input path="adjustedMark" cssClass="form-control" />
				<span class="input-group-addon">%</span>
			</div>
		</@bs3form.labelled_form_group>

		<#if isGradeValidation>
			<#assign generateUrl><@routes.exams.generateGradesForMarks exam /></#assign>
			<@marking_macros.autoGradeOnline "adjustedGrade" "Adjusted grade" "adjustedMark" markingId(command.student) generateUrl />
		<#else>
			<@bs3form.labelled_form_group path="adjustedGrade" labelText="Adjusted grade">
				<@f.input path="adjustedGrade" cssClass="form-control" />
			</@bs3form.labelled_form_group>
		</#if>

		<div>
			<input class="btn btn-primary" type="submit" value="Save">
			<a class="btn btn-default discard-changes" href="">Cancel</a>
		</div>

	</@f.form>

	<script>
		jQuery(function($){
			Exams.prepareAjaxForm($('#adjust-form-${markingId(command.student)}'), function(resp) {
				var $resp = $(resp);
				// there should be an ajax-response class somewhere in the response text
				var $response = $resp.find('.ajax-response').andSelf().filter('.ajax-response');
				var success = $response.length && $response.data('status') == 'success';

				if (success) {
					return "";
				} else {
					return resp;
				}
			});
		});
	</script>
</div>
