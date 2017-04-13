<#import "_submission_details.ftl" as sd />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/../../../../../../../../../../modules/web/src/main/webapp/WEB-INF/freemarker/cm2/admin/assignments/submission_components.ftl" as components />
<#escape x as x?html>
<#-- FIXME: implemented as part of CM2 migration but will require further reworking due to CM2 workflow changes -->
<div id="feedback-modal" class="modal fade"></div>
<div id="profile-modal" class="modal fade profile-subset"></div>

<div class="fixed-container form-post-container">
	<h1>${assignment.name} (${assignment.module.code?upper_case})</h1>

	<#if assignment.openEnded>
		<p class="dates">
			<@fmt.interval assignment.openDate />, never closes
			(open-ended)
			<#if !assignment.opened>
				<span class="label label-warning">Not yet open</span>
			</#if>
		</p>
	<#else>
		<p class="dates">
			<@fmt.interval assignment.openDate assignment.closeDate />
			<#if assignment.closed>
				<span class="label label-warning">Closed</span>
			</#if>
			<#if !assignment.opened>
				<span class="label label-warning">Not yet open</span>
			</#if>
		</p>
	</#if>

	<#assign module=assignment.module />
	<#assign department=module.adminDepartment />
	<#assign queueSitsUploadEnabled=(features.queueFeedbackForSits && department.uploadCourseworkMarksToSits) />

	<div class="fix-header pad-when-fixed">
		<#include "_filter.ftl" />
		<#assign currentView = "table" />
		<#include "_action-bar.ftl" />
	</div>

	<#if results.students??>
		<#function hasSubmissionOrFeedback students>
			<#local result = [] />
			<#list results.students as student>
				<#if student.cm2.enhancedSubmission?? || student.cm2.enhancedFeedback??>
					<#local result = result + [student] />
				</#if>
			</#list>
			<#return result />
		</#function>

		<#if hasSubmissionOrFeedback(results.students)?size = 0>
			<p>There are no submissions or feedback yet for this assignment.</p>
		</#if>

		<div class="submission-feedback-list">
			<table class="table table-bordered table-striped table-condensed submission-table">
				<colgroup class="student">
					<col class="checkbox" />
					<col class="student-info" />
				</colgroup>

				<colgroup class="submission">
					<col class="files" />
					<col class="submitted" />
					<col class="status" />
					<#assign submissionColspan=3 />

					<#if assignment.wordCountField??>
						<#assign submissionColspan=submissionColspan+1 />
						<col class="word-count" />
					</#if>
					<#if assignment.markingWorkflow??>
						<#assign submissionColspan=submissionColspan+2 />
						<col class="first-marker" />
						<col class="second-marker" />
					</#if>
				</colgroup>

				<#if results.hasOriginalityReport>
					<colgroup class="plagiarism">
						<col class="report" />
					</colgroup>
				</#if>

				<colgroup class="feedback">
					<#assign feedbackColspan=4 />

					<col class="files" />
					<col class="uploaded" />
					<#if assignment.collectMarks>
						<#assign feedbackColspan=feedbackColspan+2 />
						<col class="mark" />
						<col class="grade" />
					</#if>
					<col class="viewFeedback" />
					<col class="status" />
				</colgroup>


				<thead>
					<tr>
						<th class="check-col student"><div class="check-all checkbox"><input type="checkbox" class="collection-check-all"></div></th>
						<th class="sortable student">Student</th>

						<th class="submission" colspan="${submissionColspan?c}">
							Submission
						</th>

						<#if results.hasOriginalityReport>
							<th class="plagiarism">Plagiarism</th>
						</#if>

						<th class="feedback" colspan="${feedbackColspan?c}">
							Feedback
						</th>
					</tr>
					<tr>
						<th class="student" colspan="2"></th>

						<th class="submission">Files</th>
						<th class="submission">Submitted</th>
						<th class="sortable submission">Status</th>
						<#if assignment.wordCountField??>
							<th class="sortable submission" title="Declared word count">Words</th>
						</#if>
						<#if assignment.markingWorkflow??>
							<th class="sortable submission">First Marker</th>
							<th class="sortable submission">Second Marker</th>
						</#if>

						<#if results.hasOriginalityReport>
							<th class="sortable plagiarism">Report</th>
						</#if>

						<th class="feedback">Files</th>
						<th class="feedback">Updated</th>
						<#if assignment.collectMarks>
							<th class="feedback">Mark</th>
							<th class="feedback">Grade</th>
						</#if>
						<th class="feedback">Summary</th>
						<th class="sortable feedback">Status</th>
					</tr>
				</thead>

				<tbody>
					<#macro row student>
						<#if student.cm2.enhancedSubmission??>
							<#local enhancedSubmission=student.cm2.enhancedSubmission>
							<#local submission=enhancedSubmission.submission>
						</#if>

						<#local lateness><@sd.lateness submission /></#local>

						<tr class="itemContainer<#if !enhancedSubmission??> awaiting-submission</#if>" <#if enhancedSubmission?? && submission.suspectPlagiarised> data-plagiarised="true" </#if> >
							<td><@form.selector_check_row "students" student.user.userId /></td>
							<td class="id">
							<#if module.adminDepartment.showStudentName>
								${student.user.fullName} <#if student.user.warwickId??><@pl.profile_link student.user.warwickId /><#else><@pl.profile_link student.user.userId /></#if>
							<#else>
								<#if student.user.warwickId??>${student.user.warwickId}<#else>${student.user.userId}</#if>
							</#if>
							</td>

							<td class="files">
								<#if submission??>
									<#local attachments=submission.allAttachments />
									<#if attachments?size gt 0>
										<#if attachments?size == 1>
											<#local filename = "${attachments[0].name}">
											<#local downloadUrl><@routes.cm2.downloadSubmission submission filename/>?single=true</#local>
										<#else>
											<#local filename = "submission-${submission.studentIdentifier}.zip">
											<#local downloadUrl><@routes.cm2.downloadSubmission submission filename/></#local>
										</#if>
										<a class="long-running" href="${downloadUrl}">
											${attachments?size}
											<#if attachments?size == 1> file
											<#else> files
											</#if>
										</a>
									</#if>
								</#if>
							</td>
							<td class="submitted">
								<#if submission?? && submission.submittedDate??>
									<span class="date use-tooltip" title="${lateness!''}" data-container="body">
										<@fmt.date date=submission.submittedDate seconds=true capitalise=true shortMonth=true split=true />
									</span>
								</#if>
							</td>
							<td class="submission-status">
								<#-- Markable - ignore placeholder submissions -->
								<#if assignment.isReleasedForMarking(student.user.userId)>
									<span class="label label-success">Markable</span>
								</#if>
								<#if submission??>
									<#-- Downloaded -->
									<#if enhancedSubmission.downloaded>
										<span class="label label-success">Downloaded</span>
									</#if>
									<#-- Plagiarised -->
									<#if submission.suspectPlagiarised>
										<i class="icon-exclamation-sign use-tooltip" title="Suspected of being plagiarised"></i>
									<#elseif submission.investigationCompleted>
										<i class="icon-ok-sign use-tooltip" title="Plagiarism investigation completed"></i>
									</#if>
								</#if>
								<@sd.submission_status submission student.cm2.enhancedExtension student.cm2.enhancedFeedback student />
							</td>
							<#if assignment.wordCountField??>
								<td class="word-count">
									<#if submission?? && submission.valuesByFieldName[assignment.defaultWordCountName]??>
										${submission.valuesByFieldName[assignment.defaultWordCountName]?number}
									</#if>
								</td>
							</#if>
							<#if assignment.markingWorkflow??>
								<td>
									<#if (assignment.getStudentsFirstMarker(student.user.userId)!"")?has_content>
										${assignment.getStudentsFirstMarker(student.user.userId).fullName}
									</#if>
								</td>
								<td>
									<#if (assignment.getStudentsSecondMarker(student.user.userId)!"")?has_content>
										${assignment.getStudentsSecondMarker(student.user.userId).fullName}
									</#if>
								</td>
							</#if>

							<#if results.hasOriginalityReport>
								<td class="originality-report">
									<#if submission??>
										<#list submission.allAttachments as attachment>
											<!-- Checking originality report for ${attachment.name} ... -->
											<#if attachment.originalityReportReceived>
												<@components.originalityReport attachment />
											</#if>
										</#list>
									</#if>
								</td>
							</#if>

							<td class="download">
								<#if student.cm2.enhancedFeedback??>
									<#local attachments=student.cm2.enhancedFeedback.feedback.attachments />
									<#if attachments?size gt 0>
									<#if attachments?size == 1>
										<#local attachmentExtension = student.cm2.enhancedFeedback.feedback.attachments[0].fileExt>
									<#else>
										<#local attachmentExtension = "zip">
									</#if>
									<a class="long-running" href="<@url page='/cm2/admin/assignments/${assignment.id}/feedback/download/${student.cm2.enhancedFeedback.feedback.id}/feedback-${student.cm2.enhancedFeedback.feedback.studentIdentifier}.${attachmentExtension}'/>">
										${attachments?size}
										<#if attachments?size == 1> file
										<#else> files
										</#if>
									</a>
									</#if>
								</#if>
							</td>
							<td class="uploaded">
								<#if student.cm2.enhancedFeedback?? && !student.cm2.enhancedFeedback.feedback.placeholder>
									<@fmt.date date=student.cm2.enhancedFeedback.feedback.updatedDate seconds=true capitalise=true shortMonth=true split=true />
								</#if>
							</td>

							 <#if assignment.collectMarks>
								<td class="mark">
								 <#if student.cm2.enhancedFeedback??>
									${(student.cm2.enhancedFeedback.feedback.actualMark)!''}%
									<#if student.cm2.enhancedFeedback.feedback.hasPrivateOrNonPrivateAdjustments>
										 (Adjusted to - ${student.cm2.enhancedFeedback.feedback.latestMark}%)
									</#if>
								 </#if>
								</td>
								<td class="grade">
									<#if student.cm2.enhancedFeedback??>
										${(student.cm2.enhancedFeedback.feedback.actualGrade)!''}
										<#if student.cm2.enhancedFeedback.feedback.hasPrivateOrNonPrivateAdjustments && student.cm2.enhancedFeedback.feedback.latestGrade??>
											(Adjusted to - ${student.cm2.enhancedFeedback.feedback.latestGrade})
										</#if>
									</#if>
								</td>
							</#if>

							<td>
								<#if student.cm2.enhancedFeedback??>
									<a href="<@routes.cm2.feedbackSummary assignment student.user.userId/>"
									   class="ajax-modal"
									   data-target="#feedback-modal">
										View
									</a>
								</#if>
							</td>

							<td class="feedbackReleased">
								<#if student.cm2.enhancedFeedback??>
									<#if student.cm2.enhancedFeedback.feedback.released>
										<#if student.cm2.enhancedFeedback.downloaded><span class="label label-success">Downloaded</span>
										<#else><span class="label label-info">Published</span>
										</#if>
									<#else>
										<span class="label label-warning">Not yet published</span>
									</#if>
									<#if queueSitsUploadEnabled>
										<#if student.cm2.enhancedFeedback.feedbackForSits??>
											<#assign feedbackSitsStatus=student.cm2.enhancedFeedback.feedbackForSits.status />
											<#assign sitsWarning = feedbackSitsStatus.dateOfUpload?has_content && feedbackSitsStatus.status.code != "uploadNotAttempted" && (
												(feedbackSitsStatus.actualMarkLastUploaded!0) != (student.cm2.enhancedFeedback.feedback.latestMark!0) || (feedbackSitsStatus.actualGradeLastUploaded!"") != (student.cm2.enhancedFeedback.feedback.latestGrade!"")
											) />
											<#if feedbackSitsStatus.code == "failed">
												<a href="<@routes.cm2.checkSitsUpload student.cm2.enhancedFeedback.feedback />" target="_blank">
													<span style="cursor: pointer;" class="label label-important use-tooltip" title="There was a problem uploading to SITS. Click to try and diagnose the problem.">
													${feedbackSitsStatus.description}
													</span><#--
												--></a>
											<#elseif sitsWarning>
												<span class="label label-important use-tooltip" title="The mark or grade uploaded differs from the current mark or grade. You will need to upload the marks to SITS again.">
													${feedbackSitsStatus.description}
												</span>
											<#elseif feedbackSitsStatus.code == "successful">
												<span class="label label-success">${feedbackSitsStatus.description}</span>
											<#else>
												<span class="label label-info">${feedbackSitsStatus.description}</span>
											</#if>
										<#else>
											<span class="label label-info">Not queued for SITS upload</span>
										</#if>
									</#if>
								</#if>
							</td>
						</tr>
					</#macro>

					<#list results.students as student>
						<@row student />
					</#list>
				</tbody>
			</table>

			<#assign users=[] />
			<#list results.students as student>
				<#assign users = users + [student.user] />
			</#list>

			<p><@fmt.bulk_email_students users /></p>

			<script type="text/javascript">
				(function($) {
					$('.fixed-container').fixHeaderFooter();

					$('.submission-table').sortableTable({
						textExtraction: function(node) {
							var $el = $(node);
							if ($el.hasClass('originality-report')) {
								var $tooltip = $el.find('.similarity-tooltip').first();
								if ($tooltip.length) {
									return $tooltip.text().substring(0, $tooltip.text().indexOf('%'));
								} else {
									return '0';
								}
							} else if ($el.hasClass('word-count')) {
								return $el.text().trim().replace(',','');
							} else {
								return $el.text().trim();
							}
						}
					});

					$('#feedback-modal').on('hidden.bs.modal', function() {
						$(this).html('');
					});
				})(jQuery);
			</script>
		</div>
	</#if>
</div><!-- end fixed container -->
</#escape>
