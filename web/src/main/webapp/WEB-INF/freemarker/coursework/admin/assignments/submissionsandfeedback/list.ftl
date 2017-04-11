<#import "_submission_details.ftl" as sd />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/submission_components.ftl" as components />
<#escape x as x?html>

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

	<#if students??>
		<#function hasSubmissionOrFeedback students>
			<#local result = [] />
			<#list students as student>
				<#if student.coursework.enhancedSubmission?? || student.coursework.enhancedFeedback??>
					<#local result = result + [student] />
				</#if>
			</#list>
			<#return result />
		</#function>

		<#if hasSubmissionOrFeedback(students)?size = 0>
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

				<#if hasOriginalityReport>
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

						<#if hasOriginalityReport>
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

						<#if hasOriginalityReport>
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
						<#if student.coursework.enhancedSubmission??>
							<#local enhancedSubmission=student.coursework.enhancedSubmission>
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
											<#local downloadUrl><@routes.coursework.downloadSubmission submission filename/>?single=true</#local>
										<#else>
											<#local filename = "submission-${submission.studentIdentifier}.zip">
											<#local downloadUrl><@routes.coursework.downloadSubmission submission filename/></#local>
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
								<@sd.submission_status submission student.coursework.enhancedExtension student.coursework.enhancedFeedback student />
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

							<#if hasOriginalityReport>
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
								<#if student.coursework.enhancedFeedback??>
									<#local attachments=student.coursework.enhancedFeedback.feedback.attachments />
									<#if attachments?size gt 0>
									<#if attachments?size == 1>
										<#local attachmentExtension = student.coursework.enhancedFeedback.feedback.attachments[0].fileExt>
									<#else>
										<#local attachmentExtension = "zip">
									</#if>
									<a class="long-running" href="<@url page='/coursework/admin/module/${module.code}/assignments/${assignment.id}/feedback/download/${student.coursework.enhancedFeedback.feedback.id}/feedback-${student.coursework.enhancedFeedback.feedback.studentIdentifier}.${attachmentExtension}'/>">
										${attachments?size}
										<#if attachments?size == 1> file
										<#else> files
										</#if>
									</a>
									</#if>
								</#if>
							</td>
							<td class="uploaded">
								<#if student.coursework.enhancedFeedback?? && !student.coursework.enhancedFeedback.feedback.placeholder>
									<@fmt.date date=student.coursework.enhancedFeedback.feedback.updatedDate seconds=true capitalise=true shortMonth=true split=true />
								</#if>
							</td>

							 <#if assignment.collectMarks>
								<td class="mark">
								 <#if student.coursework.enhancedFeedback??>
									${(student.coursework.enhancedFeedback.feedback.actualMark)!''}%
									<#if student.coursework.enhancedFeedback.feedback.hasPrivateOrNonPrivateAdjustments>
										 (Adjusted to - ${student.coursework.enhancedFeedback.feedback.latestMark}%)
									</#if>
								 </#if>
								</td>
								<td class="grade">
									<#if student.coursework.enhancedFeedback??>
										${(student.coursework.enhancedFeedback.feedback.actualGrade)!''}
										<#if student.coursework.enhancedFeedback.feedback.hasPrivateOrNonPrivateAdjustments && student.coursework.enhancedFeedback.feedback.latestGrade??>
											(Adjusted to - ${student.coursework.enhancedFeedback.feedback.latestGrade})
										</#if>
									</#if>
								</td>
							</#if>

							<td>
								<#if student.coursework.enhancedFeedback??>
									<a href="<@routes.coursework.feedbackSummary assignment student.user.userId/>"
									   class="ajax-modal"
									   data-target="#feedback-modal">
										View
									</a>
								</#if>
							</td>

							<td class="feedbackReleased">
								<#if student.coursework.enhancedFeedback??>
									<#if student.coursework.enhancedFeedback.feedback.released>
										<#if student.coursework.enhancedFeedback.downloaded><span class="label label-success">Downloaded</span>
										<#else><span class="label label-info">Published</span>
										</#if>
									<#else>
										<span class="label label-warning">Not yet published</span>
									</#if>
									<#if queueSitsUploadEnabled>
										<#if student.coursework.enhancedFeedback.feedbackForSits??>
											<#assign feedbackSitsStatus=student.coursework.enhancedFeedback.feedbackForSits.status />
											<#assign sitsWarning = feedbackSitsStatus.dateOfUpload?has_content && feedbackSitsStatus.status.code != "uploadNotAttempted" && (
												(feedbackSitsStatus.actualMarkLastUploaded!0) != (student.coursework.enhancedFeedback.feedback.latestMark!0) || (feedbackSitsStatus.actualGradeLastUploaded!"") != (student.coursework.enhancedFeedback.feedback.latestGrade!"")
											) />
											<#if feedbackSitsStatus.code == "failed">
												<a href="<@routes.coursework.checkSitsUpload student.coursework.enhancedFeedback.feedback />" target="_blank">
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

					<#list students as student>
						<@row student />
					</#list>
				</tbody>
			</table>

			<#assign users=[] />
			<#list students as student>
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
