<#import "*/cm2_macros.ftl" as cm2 />
<#import "*/coursework_components.ftl" as components />
<#import "*/marking_macros.ftl" as marking />
<#escape x as x?html>
	<@cm2.assignmentHeader "Publish feedback" assignment "for" />
	<#assign module = assignment.module />
	<#assign department = module.adminDepartment />

	<#macro recipientCheckReportResults report>
		<#if recipientCheckReport.hasProblems>
			<#local problems = recipientCheckReport.problems />
			<div class="bad-recipients alert alert-danger">
				<p>
					<@fmt.p problems?size "problem" /> found with students' and/or their email addresses.
					You can continue to publish the feedback, but you may want to post the feedback link to
					a web page that the students can access even if they didn't get the email. You'll be given
					this link after you publish.
				</p>
				<ul>
					<#list problems as problem>
						<li>
							<b>${problem.universityId}</b> :
							<#if problem.user?? >
								Bad email address: (${problem.user.email!})
							<#else>
								No such user found.
							</#if>
						</li>
					</#list>
				</ul>
			</div>
		<#else>
			<div class="alert alert-success">
				<p>No problems found with students' email addresses.</p>
			</div>
		</#if>
	</#macro>

	<#macro userList usercodes>
		<ul class="user-list">
			<@userlookup ids=usercodes>
				<#list returned_users?keys?sort as id>
					<#local returned_user=returned_users[id] />
					<#if returned_user.foundUser>
						<li>${returned_user.fullName} (${returned_user.warwickId!id})</li>
					<#else>
						<li>${id}</li>
					</#if>
				</#list>
			</@userlookup>
		</ul>
	</#macro>

	<#macro submissionsReportResults report>
		<#if report.hasProblems>
			<#if report.submissionOnly?size gt 0>
				<div class="alert alert-danger">
					<p>These users have submitted to the assignment, but no feedback has been uploaded for them.</p>

					<@userList report.submissionOnly />
				</div>
			</#if>

			<#if report.feedbackOnly?size gt 0>
				<div class="alert alert-danger">
					<p>There is feedback or marks for these users but they did not submit an assignment to this system.</p>

					<@userList report.feedbackOnly />
				</div>
			</#if>

			<#if report.withoutAttachments?size == command.feedbackToRelease?size>
				<div class="alert alert-warn">
					<p>None of the submissions have any feedback.</p>
				</div>
			<#elseif report.withoutAttachments?size gt 0>
				<div class="alert alert-danger">
					<p>Submissions received from the following students do not have any feedback.</p>

					<@userList report.withoutAttachments />
				</div>
			</#if>

			<#if report.withoutMarks?size == command.feedbackToRelease?size>
				<div class="alert alert-warn">
					<p>None of the submissions have had marks assigned.</p>
				</div>
			<#elseif report.withoutMarks?size gt 0>
				<div class="alert alert-danger">
					<p>Submissions received from the following students do not have any marks assigned.</p>

					<@userList report.withoutMarks />
				</div>
			</#if>

			<#if report.plagiarised?size == 0>
				<div class="alert alert-success">
					<p>No submissions have been marked as suspected of being plagiarised.</p>
				</div>
			<#else>
				<div class="alert alert-warn">
					<p>
						<#if report.plagiarised?size == 1>
							It is suspected that the submission received from the following student is plagiarised. Feedback for this student will not be published.
						<#else>
							It is suspected that submissions received from the following students are plagiarised. Feedback for these students will not be published.
						</#if>
					</p>

					<@userList report.plagiarised />
				</div>
			</#if>

			<p>
				The above discrepancies are provided for information.
				It is up to you to decide whether to continue publishing.
			</p>
		<#else>
			<div class="alert alert-success">
				<p>The submissions and the feedback items appear to match up for this assignment.</p>
			</div>
		</#if>
	</#macro>

	<script type="text/javascript">
		jQuery(function($){ "use strict";
			var submitButton = $('#publish-submit'),
				checkbox = $('#confirmCheck');
			function updateCheckbox() {
				submitButton.prop('disabled', !checkbox.is(':checked'));
			}
			checkbox.change(updateCheckbox);
			updateCheckbox();
		});
	</script>

	<#assign submitUrl><@routes.cm2.publishFeedback assignment /></#assign>
	<@f.form method="post" action=submitUrl commandName="command">
		<@bs3form.errors path="" />

		<p>This will publish feedback for <strong><@fmt.p command.feedbackToRelease?size "student" /></strong>:</p>

		<ul>
			<#list command.feedbackToRelease as feedback>
				<li>
					<@userlookup id=feedback.usercode>
						<#if returned_user.foundUser>
							${returned_user.fullName} (<#if returned_user.warwickId??>${returned_user.warwickId}<#else>${feedback.usercode}</#if>)
						<#else>
							${feedback.usercode}
						</#if>
					</@userlookup>
					<@f.hidden name="students" value=feedback.usercode />
				</li>
			</#list>
		</ul>

		<p>
			<#if submissionsReport.alreadyReleased?has_content>
				<@fmt.p submissionsReport.alreadyReleased?size "student" /> have already had their feedback published.
				Those students won't be emailed again.
			</#if>
		</p>

		<#if features.queueFeedbackForSits && department.uploadCourseworkMarksToSits>
			<@marking.uploadToSits assignment=assignment verb="Publishing" withValidation=true isGradeValidation=isGradeValidation gradeValidation=gradeValidation />
		</#if>

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

		<div id="feedback-check-recipient-results">
			<@recipientCheckReportResults recipientCheckReport />
		</div>

		<#if features.submissions && assignment.submissions?size gt 0>
			<div id="submissions-report-results">
				<@submissionsReportResults submissionsReport />
			</div>
		</#if>

		<@bs3form.form_group path="confirm">
			<@bs3form.checkbox path="confirm">
				<@f.checkbox path="confirm" id="confirmCheck" /> I have read the above and am ready to release feedback to students.
			</@bs3form.checkbox>
		</@bs3form.form_group>

		<div class="submit-buttons">
			<button id="publish-submit" type="submit" class="btn btn-primary">Publish</button>
			<a class="btn btn-default" href="<@routes.cm2.assignmentsubmissionsandfeedback assignment />">Cancel</a>
		</div>

	</@f.form>
</#escape>