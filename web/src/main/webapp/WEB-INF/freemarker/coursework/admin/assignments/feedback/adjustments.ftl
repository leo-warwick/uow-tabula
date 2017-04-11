<#import "*/courses_macros.ftl" as courses_macros />
<#assign finalMarkingStage = (allCompletedMarkerFeedback?? && allCompletedMarkerFeedback?size > 1)>

<#macro lateness submission=""><#compress>
	${durationFormatter(submission.deadline, submission.submittedDate)}
</#compress></#macro>

<#function markingId user>
	<#if !user.warwickId?has_content || user.getExtraProperty("urn:websignon:usersource")! == 'WarwickExtUsers'>
		<#return user.userId />
	<#else>
		<#return user.warwickId! />
	</#if>
</#function>

<div class="content feedback-adjustment feedback-summary">

<#if command.submission??>
	<#assign submission = command.submission />
	<#include "_submission_summary.ftl">
</#if>

<#if command.feedback?? && (command.feedback.actualGrade?has_content || command.feedback.actualMark?has_content)>
	<div class="well">
		<h3>Feedback</h3>
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
				<#if command.feedback.latestPrivateOrNonPrivateAdjustment.markType.code == "private">
					This is a private adjustment that is not visible to the student
				</#if>
			</p>
		</div>
	</#if>
</#if>

<#assign submit_url>
	<@routes.coursework.feedbackAdjustmentForm assignment markingId(command.student) />
</#assign>

<@f.form cssClass="form-horizontal double-submit-protection"
		 method="post"
		 commandName="command"
		 action="${submit_url}">

	<@form.row>
		<@form.label path="reason">Reason for adjustment</@form.label>
		<@form.field>
			<@f.select path="reason">
				<@f.option></@f.option>
				<@f.option value="Late submission penalty">Late submission penalty</@f.option>
				<@f.option value="Plagarism penalty">Plagarism penalty</@f.option>
				<@f.option value="Other">Other</@f.option>
			</@f.select>
			<@f.input type="text" path="reason" cssClass="hide other-input" placeholder="Enter your reason" disabled=true/>
			<@f.errors path="reason" cssClass="error" />
		</@form.field>
	</@form.row>

	<@form.row>
		<@form.label path="comments">Adjustment comments</@form.label>
		<@form.field>
			<@f.textarea path="comments" cssClass="big-textarea" />
			<@f.errors path="comments" cssClass="error" />
		</@form.field>
	</@form.row>

	<@form.row>
		<@form.label path="adjustedMark">Adjusted mark</@form.label>
		<@form.field>
			<div class="input-append">
				<@f.input path="adjustedMark" cssClass="input-small" />
				<span class="add-on">%</span>
			</div>
			<#if proposedAdjustment??>
				<div class="late-penalty">
					<button class="btn btn-mini use-suggested-mark"
							data-mark="${proposedAdjustment!""}"
							data-comment="Your submission was <@lateness command.submission /> late. ${marksSubtracted} marks have been subtracted (${latePenalty} for each working day late).">
						Use suggested mark - ${proposedAdjustment!""}
					</button>
					<a class="use-popover" id="popover-${markingId(command.student)}" data-html="true"
					   data-original-title="Late penalty calculation"
					   data-content="The submission was <@fmt.p daysLate "working day" /> late. The suggested penalty
					   was derived by subtracting ${latePenalty} marks from the actual mark for each day the submission
					   was late.">
						<i class="icon-question-sign"></i>
					</a>
				</div>
			</#if>
			<@f.errors path="adjustedMark" cssClass="error" />
		</@form.field>
	</@form.row>

	<@form.row>
		<#if isGradeValidation>
			<#assign generateUrl><@routes.coursework.generateGradesForMarks command.assignment /></#assign>
			<@courses_macros.autoGradeOnline "adjustedGrade" "Adjusted grade" "adjustedMark" markingId(command.student) generateUrl />
		<#else>
			<@form.label path="adjustedGrade">Adjusted grade</@form.label>
			<@form.field>
				<@f.input path="adjustedGrade" cssClass="input-small" />
				<@f.errors path="adjustedGrade" cssClass="error" />
			</@form.field>
		</#if>
	</@form.row>

	<div class="alert alert-info">
		The reason for adjustment and any comments will be made available to students when their feedback is published.
	</div>

	<#if features.queueFeedbackForSits && assignment.module.adminDepartment.uploadCourseworkMarksToSits && command.canBeUploadedToSits>
		<@courses_macros.uploadToSits assignment=assignment verb="Adjusting" withValidation=false/>
	</#if>

	<div class="submit-buttons">
		<input class="btn btn-primary" type="submit" value="Save">
		<a class="btn discard-changes" href="">Cancel</a>
	</div>

</@f.form>
</div>
