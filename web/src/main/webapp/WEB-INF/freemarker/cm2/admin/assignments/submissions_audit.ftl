<#import "../assignments/feedback/feedback_macros.ftl" as fs>
<#import "../assignments/submissionsandfeedback/_submission_details.ftl" as sd />
<#import "submission_macros.ftl" as sub />

<#escape x as x?html>
	<h1>${assignment.module.code?upper_case} - (${assignment.name}) </h1>
	<h4>View audit </h4>
	<ul id="marks-tabs" class="nav nav-tabs">
		<li class="active"><a href="#submissions">Submissions</a></li>
		<li class="webform-tab"><a href="#summary">Summary</a></li>
	</ul>
	<div class="tab-content">
		<div class="tab-pane active" id="submissions">
			<table class="table table-striped expanding-row-pairs">
				<thead>
					<tr>
						<th>First name</th>
						<th>Last name</th>
					</tr>
				</thead>
			<tbody>
				<#assign students=auditData.students>
				<#if students?size gt 0>
					<#list students as student>
						<tr>
							<td>${student.user.firstName}-${student.user.warwickId}</td>
							<td>${student.user.lastName}</td>
						</tr>
						<tr>
							<td colspan="2">
								<div calss="audit-info">
									<h2>Submission</h2>
									<#if !assignment.openEnded>
										<strong>Assignment closed: </strong><@fmt.date date=assignment.closeDate />
									<#else>
										<strong>Open-ended assignment (no close date)</strong>
									</#if>
								</div>
								<#if student.coursework.enhancedSubmission??>
									<#assign submission=student.coursework.enhancedSubmission.submission>
									<div>
										<strong>Submission received: </strong><@fmt.date date=submission.submittedDate />
										<#if submission.late>
											- <span class="label label-danger use-tooltip" data-title="<@sd.lateness submission />" data-container="body">Late</span>
										<#elseif submission.authorisedLate>
											- <span class="label label-info use-tooltip" title="<@sd.lateness submission />" data-container="body">Within Extension</span>
										<#else>
											<span> - On time</span>
										</#if>
									</div>
									<#if assignment.module.department.plagiarismDetectionEnabled && assignment.collectSubmissions>
										<#if student.stages?keys?seq_contains('CheckForPlagiarism')>
											<div class="stage-group clearfix">
												<strong>Plagiarism check:</strong>
												<#if !student.stages['CheckForPlagiarism'].completed>
													Not checked for plagiarism
												<#else>
													<#assign attachments = submission.allAttachments />
													<#if attachments?size gt 0>
														${attachments?size}
														<#if attachments?size == 1> file
														<#else> files
														</#if>
													</#if>
													<div>
														<#list submission.allAttachments as attachment>
															<!-- Checking originality report for ${attachment.name} ... -->
															<#if attachment.originalityReportReceived>
																<@sub.originalityReport attachment />
															</#if>
														</#list>
													</div>
												</#if>
											</div>
										</#if>
									</#if>
								<#else>
									<span>Not submitted</span>
								</#if>
								<#if assignment.allowExtensions &&  student.coursework.enhancedExtension??>
									<#assign extension = student.coursework.enhancedExtension.extension />
									<#assign duration = extension.duration + extension.requestedExtraDuration />
									<div class="audit-info">
										<h2>Extension</h2>
										<div><strong>Extension request: </strong>${(extension.state.description)!""}</div>
										<div><strong>Extension date: </strong>
											<#if extension.expiryDate??>
												<@fmt.date date=extension.expiryDate /> - ${extension.duration}
												day<#if extension.duration gt 1>s</#if>
											<#elseif extension.requestedExpiryDate??>
												<@fmt.date date=extension.requestedExpiryDate />
												- ${extension.requestedExtraDuration} day<#if extension.requestedExtraDuration gt 1>
												s</#if>
											</#if>
										</div>
									</div>
									<div>
										<strong>Student's extension request </strong>
										<div class="muted">
											${extension.reason!}
										</div>
									</div>
									<#if extension.attachments??>
										<div><span class="muted"><strong>Supporting documents</strong></span>
												<#list extension.attachments as attachment>
													<div>
														<a href="<@routes.coursework.extensionreviewattachment assignment=assignment userid=student.user.warwickId filename=attachment.name />">
															${attachment.name}
														</a>
													</div>
												</#list>
											</div>
										</div>
									</#if>
								</#if>
								<#if student.stages?keys?seq_contains('ReleaseForMarking') && student.coursework.enhancedFeedback??>
									<#assign feedback = student.coursework.enhancedFeedback.feedback />
									<#if ((assignment.markingWorkflow.markingMethod.toString)!"") = "ModeratedMarking">
										<#assign isModerated = true />
									<#else>
										<#assign isModerated = false />
									</#if>

									<div class="marking-details audit-info">
										<h2>Marking</h2>
										<#list feedback.allMarkerFeedback as markerFeedback>
											<#if markerFeedback??>
												<@fs.feedbackSummary markerFeedback isModerated/>
												<@fs.secondMarkerNotes markerFeedback isModerated />
											</#if>
										</#list>
									</div>

									<#if feedback.released>
										<h3>Published</h3>
										<div class="feedback-summary-heading">
											<#if feedback.releasedDate??><strong>Feedback released:</strong> <@fmt.date feedback.releasedDate /></#if>
										</div>
									</#if>
									<#if feedback.hasPrivateOrNonPrivateAdjustments>
										<h2>Post publish adjustment</h2>
										<#list feedback.adminViewableAdjustments as adminViewableFeedback>
											<div class="mg-label"><strong>Adjustment date:</strong>
												<span><@fmt.date adminViewableFeedback.uploadedDate /></span>
											</div>
											<#if adminViewableFeedback.mark?has_content>
												<div class="mg-label"><strong>Mark:</strong>
													<span>${adminViewableFeedback.mark!""}%</span>
												</div>
											</#if>
											<#if adminViewableFeedback.grade?has_content>
												<div class="mg-label"><strong>Grade:</strong>
													<span>${adminViewableFeedback.grade!""}</span>
												</div>
											</#if>

											<#if adminViewableFeedback.reason?has_content>
												<div class="mg-label"><strong>Reason for adjustment:</strong>
													<span>${adminViewableFeedback.reason!""}</span>
												</div>
											</#if>
											<#if adminViewableFeedback.comments?has_content>
												<div class="mg-label"><strong>Feedback:</strong>
													<p>${adminViewableFeedback.comments!""}</p>
												</div>
											</#if>
										</#list>
									</#if>
								<#elseif !assignment.markingWorkflow??>
									<div class="marking-details audit-info">
										<h2>Marking</h2>
										<div>No marking workflow</div>
										<@fs.feedbackComments student.coursework.enhancedFeedback.feedback />
									</div>
								</#if>
							</td>
						</tr>
					</#list>
				</tbody>
				</#if>
			</table>
		</div>
		<div class="tab-pane " id="summary">
			<div class="audit-info">
				<h2>Submission</h2>
				<div>
					<#if !assignment.openEnded>
						<strong>Assignment closed: </strong><@fmt.date date=assignment.closeDate />
					<#else>
						<strong>Open-ended assignment (no close date)</strong>
					</#if>
				</div>
			</div>
			<!--TODO- Might need to see what should be displayed for formative type assignment as there could be 0 ExpectedSubmissions -->
			<#assign totalExpectedSubmissions = assignment.membershipInfo.totalCount />
			<#assign lateSubmissions = assignment.lateSubmissionCount />
			<div><strong>Total submissions: </strong>${totalExpectedSubmissions} </div>
			<div><strong>Submission received on time: </strong>${assignment.submissions?size - lateSubmissions} </div>
			<div><strong>Submission received late: </strong>${lateSubmissions} </div>
			<div><strong>Submission unreceived: </strong>${totalExpectedSubmissions - assignment.submissions?size} </div>

			<div class="audit-info">
				<h2>Plagiarism check</h2>
				<#if auditData.totalFilesCheckedForPlagiarism gt 0>
					<div><strong>Total files: </strong>${auditData.totalFilesCheckedForPlagiarism}</div>
				<#else>
					<div>Not checked for plagiarism</div>
				</#if>
			</div>

			<div class="audit-info">
				<#assign extensionCount = assignment.extensions?size />
				<h2>Extension</h2>
				<#if extensionCount gt 0>
					<div><strong>Extension requested: </strong>${assignment.extensions?size} </div>
					<div><strong>Extension granted: </strong> ${auditData.extensionInfo.approvedExtensionCount} </div>
					<div><strong>Extension denied: </strong> ${auditData.extensionInfo.rejectedExtensionCount} </div>
				<#else>
					<div>No extensions</div>
				</#if>
			</div>

			<div class="audit-info">
				<h2>Marking</h2>
				<#if assignment.markingWorkflow??>
					<#assign firstMarkerSummaryMap = assignment.firstMarkersWithStudentAllocationCountMap />
					<#assign userCodes = firstMarkerSummaryMap?keys>
					<#list userCodes as usercode>
						<div class="audit-info">
							<div><strong>${usercode.fullName}</strong></div>
							<#assign studentCount = mapGet(firstMarkerSummaryMap, usercode)>
							<div><strong>Allocated: </strong>${studentCount}</div>
						</div>
					</#list>
					<#assign secondMarkerSummaryMap = assignment.secondMarkersWithStudentAllocationCountMap />
					<#assign userCodes = secondMarkerSummaryMap?keys>
					<#list userCodes as usercode>
						<div class="audit-info">
							<div><strong>${usercode.fullName}</strong></div>
							<#assign studentCount = mapGet(secondMarkerSummaryMap, usercode)>
							<div><strong>Allocated: </strong>${studentCount}</div>
						</div>
					</#list>
				<#else>
					<div>No marking workflow</div>
				</#if>
			</div>
			<div class="audit-info">
				<#assign extensionCount = assignment.extensions?size />
				<h2>Published</h2>
				<div><strong>Total feedbacks: </strong>${releasedFeedback} </div>
			</div>
		</div>
	</div>
	<script>
		jQuery(function($) {
			var $table = $('table.member-notes');
			$table.tablesorter({
				headers: {1: {sorter: false}},
				textExtraction: function(node) {
					var $el = $(node);
					if ($el.data('sortby')) {
						return $el.data('sortby');
					} else {
						return $el.text().trim();
					}
				}
			});
			$table.on('sortStart', function() {
				$table.find('tr.expanded + tr').detach();
			}).on('sortEnd', function() {
				$table.find('tr').each(function() {
					var $row = $(this);
					if ($row.hasClass('expanded')) {
						$row.after($row.data('expandRow'));
					}
				});
			});
		});
	</script>
</#escape>
