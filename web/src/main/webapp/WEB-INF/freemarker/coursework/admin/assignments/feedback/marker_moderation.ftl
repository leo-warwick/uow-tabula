<#import "_feedback_summary.ftl" as fs />
<#import "*/courses_macros.ftl" as courses_macros />

<div class="well">
	<@fs.feedbackSummary firstMarkerFeedback false />
</div>

<div class="content online-feedback">
	<#if command.submission??>
		<#assign submission = command.submission />
		<#include "_submission_summary.ftl">
	<#else>
		<#include "_unsubmitted_notice.ftl">
	</#if>

	<#if isCompleted>
		<#include "_moderation_summary.ftl">
	<#else>
		<#assign submit_url>
			<@routes.coursework.markerModerationform assignment=assignment studentid=command.student.userId marker=marker />
		</#assign>
		<@f.form cssClass="form-horizontal double-submit-protection" method="post" enctype="multipart/form-data"
			commandName="command" action="${submit_url}">

			<@form.row>
				<@form.field>
					<label class="radio inline">
						<@f.radiobutton path="approved" value="true" />
						Approve
					</label>
					<label class="radio inline">
						<@f.radiobutton cssClass="reject" path="approved" value="false" />
						Request changes
					</label>
				</@form.field>
			</@form.row>

			<div class="rejection-fields" style="display:none;">

				<@form.labelled_row "rejectionComments" "Comments">
					<@f.textarea path="rejectionComments" cssClass="big-textarea" />
					<div class="help-block">
						Please add any comments about the changes here.
					</div>
				</@form.labelled_row>

				<#if assignment.collectMarks>
					<@form.row>
						<@form.label path="mark">Suggested Mark</@form.label>
						<@form.field>
							<div class="input-append">
								<@f.input path="mark" cssClass="input-small" />
								<span class="add-on">%</span>
							</div>
							<@f.errors path="mark" cssClass="error" />
						</@form.field>
					</@form.row>

					<@form.row>
						<#if isGradeValidation>
							<#assign generateUrl><@routes.coursework.generateGradesForMarks command.assignment /></#assign>
							<@courses_macros.autoGradeOnline "grade" "Suggested Grade" "mark" command.student.userId!"" generateUrl />
						<#else>
							<@form.label path="grade">Suggested Grade</@form.label>
							<@form.field>
								<@f.input path="grade" cssClass="input-small" />
								<@f.errors path="grade" cssClass="error" />
							</@form.field>
						</#if>
					</@form.row>
					<div class="control-group">
						<div class="controls">
							<span class="help-block">
								If you feel that a mark or grade other than the mark given is more appropriate you can include
								it here.
								<br />
								The first marker, ${firstMarkerFeedback.markerUser.fullName}
								(<i class="icon-envelope-alt"></i> <a href="mailto:${firstMarkerFeedback.markerUser.email}">${firstMarkerFeedback.markerUser.email}</a>),
								will be notified of the requested changes.
							</span>
						</div>
					</div>
				</#if>
			</div>

			<div class="submit-buttons">
				<input class="btn btn-primary" type="submit" value="Save">
				<a class="btn discard-changes" href="">Cancel</a>
			</div>
		</@f.form>
	</#if>
</div>
