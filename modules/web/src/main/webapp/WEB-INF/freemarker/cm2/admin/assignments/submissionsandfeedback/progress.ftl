<#import "_submission_details.ftl" as sd />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/submission_components.ftl" as components />
<#escape x as x?html>

<#macro studentIdentifier user><#compress>
	<#if user.warwickId??>${user.warwickId}<#else>${user.userId!}</#if>
</#compress></#macro>

<div id="profile-modal" class="modal fade profile-subset"></div>
<div id="feedback-modal" class="modal fade"></div>

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
	<#assign department=assignment.module.adminDepartment />
	<#assign queueSitsUploadEnabled=(features.queueFeedbackForSits && department.uploadCourseworkMarksToSits) />

	<div class="fix-header pad-when-fixed">
		<#include "_filter.ftl" />

		<#assign currentView = "summary" />
		<#include "_action-bar.ftl" />

		<#if results.students??>
	</div>

	<table class="coursework-progress-table results.students table table-bordered table-striped tabula-greenLight sticky-table-headers ">
		<thead<#if results.students?size == 0> style="display: none;"</#if> class="fix-header">
			<tr>
				<th class="check-col" style="padding-right: 0px;"><input type="checkbox" class="collection-check-all"></th>
				<#if department.showStudentName>
					<th class="student-col">First name</th>
					<th class="student-col">Last name</th>
				<#else>
					<th class="student-col">University ID</th>
				</#if>

				<th class="progress-col">Progress</th>
				<th class="action-col">Next action</th>
			</tr>
		</thead>

	<#if results.students?size gt 0>
		<tbody>
			<#macro action actionCode student>
				<#local studentDetails><span data-profile="<@studentIdentifier student.user />"><#if department.showStudentName>${student.user.fullName}<#else><@studentIdentifier student.user /></#if></span></#local>
				<#local firstMarker="first marker" />
				<#local secondMarker="second marker" />
				<#assign fm = assignment.getStudentsFirstMarker(student.user.userId)!"" />
				<#if fm?has_content>
					<#local firstMarker><span data-profile="${fm.warwickId!}">${fm.fullName}</span></#local>
				</#if>
				<#assign sm = assignment.getStudentsSecondMarker(student.user.userId)!"" />
				<#if sm?has_content>
					<#local secondMarker><span data-profile="${sm.warwickId!}">${sm.fullName}</span></#local>
				</#if>

				<#local text><@spring.message code=actionCode /></#local>

				<#noescape>${(text!"")?replace("[STUDENT]", studentDetails)?replace("[FIRST_MARKER]", firstMarker)?replace("[SECOND_MARKER]", secondMarker)}</#noescape>
			</#macro>

				<#macro stage stage>
					<#if stage.messageCode?default("")?length gt 0>
						<div class="stage<#if !stage.completed> incomplete<#if !stage.preconditionsMet> preconditions-not-met</#if></#if><#if stage.started && !stage.completed> current</#if>">
							<#if stage.completed>
								<#if stage.health.toString == 'Good'>
									<i class="icon-ok"></i>
								<#else>
									<i class="icon-remove"></i>
								</#if>
							<#else>
								<i class="icon-blank"></i>
							</#if>
							<@spring.message code=stage.messageCode /><#nested/>
						</div>
					</#if>
				</#macro>

				<#macro uniIdSafeMarkerLink marker role>
					<#if marker.warwickId?has_content>
						- <a href="<@routes.cm2.listmarkersubmissions assignment marker />">Proxy as this ${role}</a>
					<#else>
						- Cannot proxy as this marker as they have no University ID
					</#if>
				</#macro>

				<#macro workflow student>
					<#if student.cm2.enhancedSubmission??>
						<#local enhancedSubmission=student.cm2.enhancedSubmission>
						<#local submission=enhancedSubmission.submission>
					</#if>
					<#if student.cm2.enhancedFeedback??>
						<#local enhancedFeedback=student.cm2.enhancedFeedback>
						<#local feedback=enhancedFeedback.feedback>
					</#if>
					<#if student.cm2.enhancedExtension??>
						<#local enhancedExtension=student.cm2.enhancedExtension>
						<#local extension=enhancedExtension.extension>
					</#if>

					<div class="content">
						<#if student.stages?keys?seq_contains('Submission')>
							<div class="stage-group clearfix">
								<h3>Submission</h3>

								<div class="labels">
									<@sd.submission_status submission enhancedExtension enhancedFeedback student />
								</div>

								<#-- If the current action is in this section, then add the next action blowout here -->
								<#if student.nextStage?? && ['Submission','DownloadSubmission']?seq_contains(student.nextStage.toString) && student.progress.messageCode != 'workflow.Submission.unsubmitted.failedToSubmit'>
									<div class="alert pull-right">
										<strong>Next action:</strong> <@action student.nextStage.actionCode student />
									</div>
								</#if>

								<@stage student.stages['Submission']><@sd.submission_details submission /></@stage>

								<#if student.stages?keys?seq_contains('DownloadSubmission')>
									<@stage student.stages['DownloadSubmission'] />
								</#if>
							</div>
						</#if>

						<#if student.stages?keys?seq_contains('CheckForPlagiarism')>
							<div class="stage-group clearfix">
								<h3>Plagiarism</h3>

								<div class="labels">
									<#if submission?? && submission.suspectPlagiarised>
										<i class="icon-exclamation-sign use-tooltip" title="Suspected of being plagiarised" data-container="body"></i>
									</#if>
								</div>

								<#-- If the current action is in this section, then add the next action blowout here -->
								<#if student.nextStage?? && ['CheckForPlagiarism']?seq_contains(student.nextStage.toString)>
									<div class="alert pull-right">
										<strong>Next action:</strong> <@action student.nextStage.actionCode student />
									</div>
								</#if>

								<@stage student.stages['CheckForPlagiarism']><#compress>
									<#if submission??>
										<#list submission.allAttachments as attachment>
											<!-- Checking originality report for ${attachment.name} ... -->
											<#if attachment.originalityReportReceived>
												: <@components.originalityReport attachment />
											</#if>
											<#if can.do("Submission.ViewUrkundPlagiarismStatus", submission) && attachment.urkundResponseReceived>
												: <@components.urkundOriginalityReport attachment />
											</#if>
										</#list>
									</#if>
								</#compress></@stage>
							</div>
						</#if>

						<#if student.stages?keys?seq_contains('ReleaseForMarking')>
							<div class="stage-group clearfix">
								<h3>Marking</h3>

								<div class="labels">
									<#if assignment.isReleasedForMarking(student.user.userId)>
										<span class="label label-success">Markable</span>
									</#if>
								</div>

								<#-- If the current action is in this section, then add the next action blowout here -->
								<#if student.nextStage?? && ['ReleaseForMarking','FirstMarking','SecondMarking']?seq_contains(student.nextStage.toString)>
									<div class="alert pull-right">
										<strong>Next action:</strong> <@action student.nextStage.actionCode student />
									</div>
								</#if>

								<@stage student.stages['ReleaseForMarking'] />

								<#if student.stages?keys?seq_contains('FirstMarking')>

									<#assign fm = assignment.getStudentsFirstMarker(student.user.userId)!"" />
									<#if fm?has_content>
										<#local firstMarker><span data-profile="${fm.warwickId!}">${fm.fullName}</span></#local>
									</#if>

									<@stage student.stages['FirstMarking']> <#compress>
										<#if firstMarker?default("")?length gt 0>
											(<#noescape>${firstMarker}</#noescape>)
											<#if can.do("Assignment.MarkOnBehalf", assignment)>
												<@uniIdSafeMarkerLink fm "marker" />
											</#if>
										</#if>
									</#compress></@stage>
								</#if>

								<#if student.stages?keys?seq_contains('SecondMarking')>

									<#assign sm = assignment.getStudentsSecondMarker(student.user.userId)!"" />
									<#if sm?has_content>
										<#local secondMarker><span data-profile="${sm.warwickId!}">${sm.fullName}</span></#local>
									</#if>

									<@stage student.stages['SecondMarking']> <#compress>
										<#if secondMarker?default("")?length gt 0>
											(<#noescape>${secondMarker}</#noescape>)
											<#if can.do("Assignment.MarkOnBehalf", assignment)>
												<@uniIdSafeMarkerLink sm "marker" />
											</#if>
										</#if>
									</#compress></@stage>
								</#if>

								<#if student.stages?keys?seq_contains('Moderation')>
									<#assign sm = assignment.getStudentsSecondMarker(student.user.userId)!"" />
									<#if sm?has_content>
										<#local secondMarker><span data-profile="${sm.warwickId!}">${sm.fullName}</span></#local>
									</#if>

									<@stage student.stages['Moderation']> <#compress>
										<#if secondMarker?default("")?length gt 0>
											(<#noescape>${secondMarker}</#noescape>)
											<#if can.do("Assignment.MarkOnBehalf", assignment)>
												<@uniIdSafeMarkerLink sm "moderator" />
											</#if>
										</#if>
									</#compress></@stage>
								</#if>

								<#if student.stages?keys?seq_contains('FinaliseSeenSecondMarking')>
									<#assign fm = assignment.getStudentsFirstMarker(student.user.userId)!"" />
									<#if fm?has_content>
										<#local firstMarker><span data-profile="${fm.warwickId!}">${fm.fullName}</span></#local>
									</#if>

									<@stage student.stages['FinaliseSeenSecondMarking']> <#compress>
										<#if firstMarker?default("")?length gt 0>
											(<#noescape>${firstMarker}</#noescape>)
											<#if can.do("Assignment.MarkOnBehalf", assignment)>
												<@uniIdSafeMarkerLink fm "marker" />
											</#if>
										</#if>
									</#compress></@stage>
								</#if>

							</div>
						</#if>

						<#if student.stages?keys?seq_contains('AddMarks') || student.stages?keys?seq_contains('AddFeedback')>
							<div class="stage-group">
								<h3>Feedback</h3>

								<div class="labels">
									<#if enhancedFeedback?? && feedback??>
										<#if feedback.released>
											<#if enhancedFeedback.downloaded><span class="label label-success">Downloaded</span>
											<#else><span class="label label-info">Published</span>
											</#if>
										<#else><span class="label label-warning">Not yet published</span>
										</#if>
										<#if queueSitsUploadEnabled>
											<#if enhancedFeedback.feedbackForSits??>
												<#assign feedbackSitsStatus=enhancedFeedback.feedbackForSits.status />
												<#assign sitsWarning = feedbackSitsStatus.dateOfUpload?has_content && feedbackSitsStatus.status.code != "uploadNotAttempted" && (
													(feedbackSitsStatus.actualMarkLastUploaded!0) != (feedback.latestMark!0) || (feedbackSitsStatus.actualGradeLastUploaded!"") != (feedback.latestGrade!"")
												) />
												<#if feedbackSitsStatus.code == "failed">
													<a href="<@routes.cm2.checkSitsUpload feedback />" target="_blank">
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
									<#if student.nextStage??><strong>Next action:</strong> <@action student.nextStage.actionCode student /></#if>
								</div>
							</div>
						</#if>


						<#if feedback?? && !(feedback.placeholder)>
							<#-- not really a stage but the best place to put a link to the feedback summary -->
							<div class="stage">
								<i class="icon-eye-open"></i>
								<a href="<@routes.cm2.feedbackSummary assignment student.user.userId!''/>"
								   class="ajax-modal"
								   data-target="#feedback-modal">
									View feedback
								</a>
							</div>

							<div class="stage">
								<i class="icon-eye-open"></i>
								<a href="<@routes.cm2.feedbackAudit assignment student.user.userId!''/>">
									View audit
								</a>
							</div>

							<#-- If the current action is in this section, then add the next action blowout here -->
							<#if student.nextStage?? && ['AddMarks','AddFeedback','ReleaseFeedback','DownloadFeedback']?seq_contains(student.nextStage.toString)>
								<div class="alert pull-right">
									<strong>Next action:</strong> <@action student.nextStage.actionCode student />
								</div>
							</#if>

							<#if student.stages?keys?seq_contains('AddMarks')>
								<@stage student.stages['AddMarks']><#compress>
									<#if feedback?? && feedback.hasMarkOrGrade>
										: <#compress>
											<#if feedback.hasMark>
												${feedback.actualMark!''}%<#if feedback.hasGrade>,</#if>
											</#if>
											<#if feedback.hasGrade>
												grade ${feedback.actualGrade!''}
											</#if>
										</#compress>
									</#if>
								</#compress></@stage>
								<#if feedback.hasPrivateOrNonPrivateAdjustments>
									<div>
										<i class="icon-ok"></i> Marks adjusted:
										<#if feedback.latestMark??>${feedback.latestMark}%</#if><#if feedback.latestGrade??>,</#if>
										<#if feedback.latestGrade??> grade ${feedback.latestGrade}</#if>
										<#if feedback.latestPrivateOrNonPrivateAdjustment?? && feedback.latestPrivateOrNonPrivateAdjustment.reason??>
										 - Reason for adjustment: ${feedback.latestPrivateOrNonPrivateAdjustment.reason!''}
										</#if>
									</div>
								</#if>
							</#if>

							<#if student.stages?keys?seq_contains('AddFeedback')>
								<@stage student.stages['AddFeedback']><#compress>
									<#if feedback?? && (feedback.hasAttachments || feedback.hasOnlineFeedback)>: <#compress>
										<#local attachments=feedback.attachments />
										<#if attachments?size gt 0>
											<#if attachments?size == 1>
												<#local attachmentExtension = student.cm2.enhancedFeedback.feedback.attachments[0].fileExt>
											<#else>
												<#local attachmentExtension = "zip">
											</#if>
											<a class="long-running" href="<@url page='/cm2/admin/assignments/${assignment.id}/feedback/download/${student.cm2.enhancedFeedback.feedback.id}/feedback-${student.cm2.enhancedFeedback.feedback.studentIdentifier}.${attachmentExtension}'/>"><#compress>
												${attachments?size}
												<#if attachments?size == 1> file
												<#else> files
												</#if>
											</#compress></a>
										<#-- If the feedback was entered online there may not be attachments  -->
										<#elseif feedback?? && feedback.hasOnlineFeedback>
											Comments entered online
										</#if>
										<#if feedback.updatedDate??><#compress>
												<@fmt.date date=feedback.updatedDate seconds=true capitalise=true shortMonth=true />
										</#compress></#if>
									</#compress></#if>
								</#compress></@stage>
							</#if>

							<#if student.stages?keys?seq_contains('ReleaseFeedback')>
								<@stage student.stages['ReleaseFeedback']><#compress>
									<#if feedback?? && feedback.releasedDate??><#compress>
										: <@fmt.date date=feedback.releasedDate seconds=true capitalise=true shortMonth=true />
									</#compress></#if>
								</#compress>
								</@stage>
							</#if>

							<#if student.stages?keys?seq_contains('DownloadFeedback')>
								<@stage student.stages['DownloadFeedback'] />
							</#if>
						</#if>

					</div>
				</#macro>

				<#macro row student>
					<tr data-contentid="<@studentIdentifier student.user />" class="itemContainer<#if !student.cm2.enhancedSubmission??> awaiting-submission</#if>"<#if student.cm2.enhancedSubmission?? && student.cm2.enhancedSubmission.submission.suspectPlagiarised> data-plagiarised="true"</#if>>
						<td class="check-col"><input type="checkbox" class="collection-checkbox" name="results.students" value="${student.user.userId!}"></td>
						<#if department.showStudentName>
							<td class="student-col toggle-cell">
								<h6 class="toggle-icon" data-profile="<@studentIdentifier student.user />">${student.user.firstName}</h6>
							</td>
							<td class="student-col toggle-cell">
								<h6 data-profile="<@studentIdentifier student.user />">
									${student.user.lastName}&nbsp;<#if student.user.warwickId??><@pl.profile_link student.user.warwickId /><#else><@pl.profile_link student.user.userId /></#if>
								</h6>
							</td>
						<#else>
							<#assign studentId><#if student.user.warwickId??>${student.user.warwickId}<#else>${student.user.userId}</#if></#assign>
							<td class="student-col toggle-cell">
								<h6 class="toggle-icon" data-profile="${studentId}">
									${studentId}
								</h6>
							</td>
						</#if>
						<td class="progress-col content-cell toggle-cell">
							<#if student.stages?keys?seq_contains('Submission') && student.nextStage?? && student.nextStage.toString != 'Submission' && student.stages['Submission'].messageCode != student.progress.messageCode>
								<#local progressTooltip><@spring.message code=student.stages['Submission'].messageCode />. <@spring.message code=student.progress.messageCode /></#local>
							<#else>
								<#local progressTooltip><@spring.message code=student.progress.messageCode /></#local>
							</#if>

							<dl class="progress progress-${student.progress.t} use-tooltip" title="${progressTooltip}" style="margin: 0; border-bottom: 0;" data-container="body">
								<dt class="bar" style="width: ${student.progress.percentage}%;"></dt>
								<dd style="display: none;" class="table-content-container" data-contentid="<@studentIdentifier student.user />">
									<div id="content-<@studentIdentifier student.user />" class="content-container">
										<@workflow student />
									</div>
								</dd>
							</dl>
						</td>
						<td class="action-col">
							<#if student.nextStage??>
								<#if student.progress.messageCode == 'workflow.Submission.unsubmitted.failedToSubmit'>
									Failed to submit
								<#else>
									<@action student.nextStage.actionCode student />
								</#if>
							<#elseif student.progress.percentage == 100>
								Complete
							</#if>
						</td>
					</tr>
				</#macro>

				<#list results.students as student>
					<@row student />
				</#list>
			</tbody>
		</#if>
	</table>

	<#if results.students?size gt 0>
		<#assign users=[] />
		<#list results.students as student>
			<#assign users = users + [student.user] />
		</#list>

		<p><@fmt.bulk_email_students users /></p>

	<script type="text/javascript">
	(function($) {
		$('.fixed-container').fixHeaderFooter();

		var options = {
			sortList: [<#if department.showStudentName>[3, 0], [2, 0]<#else>[2, 0], [1, 0]</#if>],
			headers: { 0: { sorter: false } },
			textExtraction: function(node) {
				var $el = $(node);
				if ($el.hasClass('progress-col')) {
					var width = $el.find('.bar').width();

					// Add differing amounts for info, success, warning and danger so that they are sorted in the right order.
					// info is the default, so == 0
					if ($el.find('.progress').hasClass('progress-success')) {
						width += 3;
					} else if ($el.find('.progress').hasClass('progress-warning')) {
						width += 2;
					} else if ($el.find('.progress').hasClass('progress-danger')) {
						width += 1;
					}

					return width;
				} else {
					return $el.text().trim();
				}
			}
		};

		$('.coursework-progress-table').each(function() { $(this).expandingTable({ tableSorterOptions: options }); });

		$('#feedback-modal').on('hidden.bs.modal', function() {
			$(this).html('');
		});
	})(jQuery);
	</script>
	<#else>
		<#if submissionAndFeedbackCommand.filter.name == 'AllStudents'>
			<p>There are no submissions or feedback yet for this assignment.</p>
		<#else>
			<#macro filterDescription filter filterParameters><#compress>
				${filter.description}
				<#if filter.parameters?size gt 0>(<#compress>
					<#list filter.parameters as param>${param._1()}=${filterParameters[param._1()]}<#if param_has_next>, </#if></#list>
				</#compress>)</#if>
			</#compress></#macro>

			<p>There are no <@filterDescription submissionAndFeedbackCommand.filter submissionAndFeedbackCommand.filterParameters /> for this assignment.</p>
		</#if>
	</#if>

	<#else>
	</div>
	</#if>
</div>
</#escape>
