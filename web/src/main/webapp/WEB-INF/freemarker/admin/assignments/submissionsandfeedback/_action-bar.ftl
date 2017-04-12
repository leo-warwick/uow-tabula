<#-- FIXME: implemented as part of CM2 migration but will require further reworking due to CM2 workflow changes -->

<#if students??>
	<div class="btn-toolbar">
		<div class="pull-right view-selector">
			<form class="form-inline">
				<label class="radio">View as:</label>
				<label class="radio">
					<input type="radio" name="view" value="summary" data-href="<@routes.coursework.assignmentsubmissionsandfeedbacksummary assignment />" <#if currentView == 'summary'>checked="checked"</#if> />
					Summary
				</label>
				<label class="radio">
					<input type="radio" name="view" value="table" data-href="<@routes.coursework.assignmentsubmissionsandfeedbacktable assignment />" <#if currentView == 'table'>checked="checked"</#if> />
					Table
				</label>
			</form>
		</div>

		<script type="text/javascript">
			jQuery(function($) {
				$('.view-selector input[name="view"]').on('change', function() {
					var $this = $(this);

					if ($this.is(':checked')) {
						var $form = $('<form></form>').attr({method:'POST',action:$this.data('href')}).hide();

						var $inputs = $this.closest(".btn-toolbar").parent().find(".filter-form :input");
						$form.append($inputs.clone());

						$(document.body).append($form);
						$form.submit();
					}
				});
			});
		</script>

		<div class="btn-group-group">
			<div class="btn-group">
				<a class="btn hover"><i class="icon-cog"></i> Actions:</a>
			</div>

			<#if assignment.collectSubmissions>
				<div class="btn-group">
					<a class="btn dropdown-toggle" data-toggle="dropdown">
						Submission
						<span class="caret"></span>
					</a>
					<ul class="dropdown-menu">
						<li class="must-have-selected">
							<a class="long-running use-tooltip form-post"
								 data-href="<@url page='/coursework/admin/module/${module.code}/assignments/${assignment.id}/submissions.zip'/>"
								 href=""
								 title="Download the submission files for the selected students as a ZIP file."
								 data-container="body"><i class="icon-download icon-fixed-width"></i> Download submissions
							</a>
						</li>
						<li class="must-have-selected">
							<a class="long-running use-tooltip download-pdf"
							   data-href="<@url page='/coursework/admin/module/${module.code}/assignments/${assignment.id}/submissions.pdf'/>"
							   href=""
							   title="Download the submission files for the selected students as a PDF for printing."
							   data-container="body"><i class="icon-download icon-fixed-width"></i> Download submissions as PDF
							</a>
						</li>
						<li class="must-have-selected">
							<#assign deletesubmissionurl><@url page='/coursework/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/delete' /></#assign>
							<@fmt.permission_button permission='Submission.Delete' scope=module action_descr='delete submission' classes="form-post" href=deletesubmissionurl tooltip='Delete submissions' >
								<i class="icon-remove icon-fixed-width"></i> Delete submissions
							</@fmt.permission_button>
						</li>
					</ul>
				</div>
			<#else>
				<div class="btn-group">
					<a class="btn dropdown-toggle disabled use-tooltip" title="This assignment does not collect submissions" data-container="body">
						Submission
						<span class="caret"></span>
					</a>
				</div>
			</#if>

			<#if department.plagiarismDetectionEnabled && assignment.collectSubmissions>
				<div class="btn-group">
					<a class="btn dropdown-toggle" data-toggle="dropdown">
						Plagiarism
						<span class="caret"></span>
					</a>
					<ul class="dropdown-menu">
						<#if features.turnitin>
							<#if features.turnitinSubmissions>
								<li>
									<#assign checkplagiarism_url><@url page='/coursework/admin/module/${module.code}/assignments/${assignment.id}/turnitin' /></#assign>
									<@fmt.permission_button permission='Submission.CheckForPlagiarism' scope=assignment action_descr='check for plagiarism' href=checkplagiarism_url tooltip='Check for plagiarism'>
										<i class="icon-book icon-fixed-width"></i> Check for plagiarism
									</@fmt.permission_button>
								</li>
							<#else>
							<li class="disabled">
								<@fmt.permission_button permission='Submission.CheckForPlagiarism' scope=assignment action_descr='check for plagiarism - temporarily disabled' tooltip='Check for plagiarism - temporarily disabled'>
									<i class="icon-book icon-fixed-width"></i> Check for plagiarism
								</@fmt.permission_button>
							</li>
							</#if>
						</#if>

						<li class="must-have-selected">
							<#assign markplagiarised_url><@url page='/coursework/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/mark-plagiarised' /></#assign>
							<@fmt.permission_button permission='Submission.ManagePlagiarismStatus' scope=assignment action_descr='mark plagiarised' href=markplagiarised_url id="mark-plagiarised-selected-button" tooltip="Toggle whether the selected student submissions are possibly plagiarised" data_attr='data-container=body'>
								<i class="icon-exclamation-sign icon-fixed-width"></i> Mark plagiarised
							</@fmt.permission_button>
						</li>
					</ul>
				</div>
			<#elseif assignment.collectSubmissions>
				<div class="btn-group">
					<a class="btn dropdown-toggle disabled use-tooltip" title="Your department does not use plagiarism detection in Tabula" data-container="body">
						Plagiarism
						<span class="caret"></span>
					</a>
				</div>
			<#else>
				<div class="btn-group">
					<a class="btn dropdown-toggle disabled use-tooltip" title="This assignment does not collect submissions" data-container="body">
						Plagiarism
						<span class="caret"></span>
					</a>
				</div>
			</#if>
			<#if assignment.collectSubmissions && features.markingWorkflows>
				<#if mustReleaseForMarking?default(false)>
					<div class="btn-group">
						<a class="btn dropdown-toggle" data-toggle="dropdown">
							Marking
							<span class="caret"></span>
						</a>
						<ul class="dropdown-menu">
							<#if assignment.markingWorkflow?? && !assignment.markingWorkflow.studentsChooseMarker>
								<li>
									<#assign markers_url><@routes.coursework.assignMarkers assignment /></#assign>
									<@fmt.permission_button
										permission='Assignment.Update'
										scope=assignment
										action_descr='assign markers'
										href=markers_url>
							<i class="icon-user icon-fixed-width"></i> Assign markers
			            </@fmt.permission_button>
								</li>
							<#else>
								<li class="disabled"><a><i class="icon-user icon-fixed-width"></i> Assign markers </a></li>
							</#if>
							<li class="must-have-selected">
								<#assign releaseForMarking_url><@routes.coursework.releaseForMarking assignment /></#assign>
								<@fmt.permission_button
									permission='Submission.ReleaseForMarking'
									scope=assignment
									action_descr='release for marking'
									classes='form-post'
									href=releaseForMarking_url
									id="release-submissions-button"
									tooltip="Release the submissions for marking. First markers will be able to download their submissions."
									data_attr='data-container=body'>
									<i class="icon-inbox icon-fixed-width"></i> Release selected for marking
								</@fmt.permission_button>
							</li>
							<li class="must-have-selected">
								<#assign returnForMarking_url><@routes.coursework.returnForMarking assignment /></#assign>
								<@fmt.permission_button
								permission='Submission.ReleaseForMarking'
								scope=assignment
								action_descr='return for marking'
								classes='form-post'
								href=returnForMarking_url
								id="return-submissions-button"
								tooltip="Return the submissions for marking. The last marker in the workflow will be able to update their feedback. You can only return feedback that has not been published."
								data_attr='data-container=body'>
									<i class="icon-arrow-left icon-fixed-width"></i> Return selected for marking
								</@fmt.permission_button>
							</li>
						</ul>
					</div>
				<#else>
					<div class="btn-group">
						<a class="btn dropdown-toggle disabled use-tooltip" title="This assignment does not use a marking workflow that requires assignments to be released for marking" data-container="body">
							Marking
							<span class="caret"></span>
						</a>
					</div>
				</#if>
			</#if>

			<div class="btn-group">
				<a class="btn dropdown-toggle" data-toggle="dropdown">
					Feedback
					<span class="caret"></span>
				</a>
				<ul class="dropdown-menu">

					<#if assignment.hasWorkflow>
						<li>
							<#assign onlinefeedback_url><@routes.coursework.genericfeedback assignment /></#assign>
							<@fmt.permission_button
							permission='AssignmentFeedback.Manage'
							scope=assignment
							action_descr='add general feedback for all students'
							tooltip='Add general feedback that will be sent to all students'
							href=onlinefeedback_url>
								<i class="icon-edit icon-fixed-width"></i> Generic feedback
							</@fmt.permission_button>
						</li>
					<#else>
						<#if features.feedbackTemplates && assignment.hasFeedbackTemplate>
							<li>
								<a class="long-running use-tooltip"
								   href="<@url page='/coursework/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback-templates.zip'/>"
								   title="Download feedback templates for all students as a ZIP file."
								   data-container="body"><i class="icon-download icon-fixed-width"></i> Download templates
								</a>
							</li>
							<li class="divider"></li>
						</#if>
						<li>
							<#assign marks_url><@routes.coursework.addMarks assignment /></#assign>
							<@fmt.permission_button
							permission='AssignmentFeedback.Manage'
							scope=assignment
							action_descr='add marks'
							href=marks_url>
								<i class="icon-check icon-fixed-width"></i> Add marks
							</@fmt.permission_button>
						</li>
						<li>
							<#assign onlinefeedback_url><@routes.coursework.onlinefeedback assignment /></#assign>
							<@fmt.permission_button
							permission='AssignmentFeedback.Read'
							scope=assignment
							action_descr='manage online feedback'
							href=onlinefeedback_url>
								<i class="icon-edit icon-fixed-width"></i> Online feedback
							</@fmt.permission_button>
						</li>
						<li>
							<#assign feedback_url><@routes.coursework.addFeedback assignment /></#assign>
							<@fmt.permission_button
							permission='AssignmentFeedback.Manage'
							scope=assignment
							action_descr='upload feedback'
							classes='feedback-link'
							href=feedback_url>
								<i class="icon-upload icon-fixed-width"></i> Upload feedback
							</@fmt.permission_button>
						</li>
					</#if>
					<#if assignment.collectMarks>
						<li class="must-have-selected">
							<#assign onlinefeedback_url><@routes.coursework.feedbackAdjustment assignment /></#assign>
							<@fmt.permission_button
								permission='AssignmentFeedback.Manage'
								scope=assignment
								action_descr='make adjustments to feedback'
								classes='form-post'
								tooltip='Apply penalties or make adjustments to mark and grade'
								href=onlinefeedback_url>
								<i class="icon-sort icon-fixed-width"></i> Adjustments
							</@fmt.permission_button>
						</li>
					<#else>
						<li class="disabled"><a class="use-tooltip" data-container="body" title="You cannot adjust marks on an assignment that does not collect marks"><i class="icon-sort icon-fixed-width"></i> Adjustments</a></li>
					</#if>

					<#-- Download / Publish / Delete always available -->
					<li class="must-have-selected">
						<a class="long-running use-tooltip form-post"
							 href="<@url page='/coursework/admin/module/${module.code}/assignments/${assignment.id}/feedback.zip'/>"
							 title="Download the feedback files for the selected students as a ZIP file."
							 data-container="body"><i class="icon-download icon-fixed-width"></i> Download feedback
						</a>
					</li>
					<#if assignment.canPublishFeedback>
						<li>
							<#assign publishfeedbackurl><@url page='/coursework/admin/module/${module.code}/assignments/${assignment.id}/publish'/></#assign>
							<@fmt.permission_button permission='AssignmentFeedback.Publish' scope=assignment type='a' action_descr='release feedback to students' tooltip="Release feedback to students" href=publishfeedbackurl>
								<i class="icon-share icon-fixed-width"></i> Publish feedback
							</@fmt.permission_button>
						</li>
					<#else>
						<li class="disabled"><a class="use-tooltip" data-container="body" title="No current feedback to publish, or the assignment is not yet closed."><i class="icon-share icon-fixed-width"></i> Publish feedback</a></li>
					</#if>
					<li class="must-have-selected">
						<#assign deletefeedback_url><@url page='/coursework/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/delete' /></#assign>
						<@fmt.permission_button permission='AssignmentFeedback.Manage' scope=assignment action_descr='delete feedback' classes="form-post" href=deletefeedback_url tooltip='Delete feedback'>
							<i class="icon-remove icon-fixed-width"></i> Delete feedback
						</@fmt.permission_button>
					</li>

					<#if features.queueFeedbackForSits && department.uploadCourseworkMarksToSits>
						<li>
							<#assign uploadToSitsUrl><@routes.coursework.uploadToSits assignment /></#assign>
							<@fmt.permission_button
								permission='AssignmentFeedback.Publish'
								scope=assignment
								action_descr='upload feedback to SITS'
								tooltip='Upload mark and grade to SITS'
								classes='form-post'
								href=uploadToSitsUrl
							>
								<i class="icon-upload icon-fixed-width"></i> Upload to SITS
							</@fmt.permission_button>
						</li>
					</#if>
				</ul>
			</div>

		</div>

		<div class="btn-group">
			<a class="btn dropdown-toggle" data-toggle="dropdown">
				Save As
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu">
				<li>
					<a class="long-running form-post include-filter" title="Export submissions info as XLSX, for advanced users." href="<@url page='/coursework/admin/module/${module.code}/assignments/${assignment.id}/export.xlsx'/>">Excel</a>
				</li>
				<li>
					<a class="long-running form-post include-filter" title="Export submissions info as CSV, for advanced users." href="<@url page='/coursework/admin/module/${module.code}/assignments/${assignment.id}/export.csv'/>">Text (CSV)</a>
				</li>
				<li>
					<a class="long-running form-post include-filter" title="Export submissions info as XML, for advanced users." href="<@url page='/coursework/admin/module/${module.code}/assignments/${assignment.id}/export.xml'/>">Text (XML)</a>
				</li>
			</ul>
		</div>

	</div>

	<div id="download-pdf-modal" class="modal hide fade">
		<div class="modal-header">
			<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
			<h3>Download submissions as PDF</h3>
		</div>
		<div class="modal-body">
			<p>There are <span class="count"></span> submissions that have files that are not PDFs (shown below). The download will not include these files.</p>
			<p><a class="long-running form-post btn btn-primary"
				  data-href="<@url page='/coursework/admin/module/${module.code}/assignments/${assignment.id}/submissions.pdf?download'/>"
				  href=""
			><i class="icon-download"></i> Download submissions as PDF
			</a></p>
			<ul class="submissions"></ul>
		</div>
	</div>
</#if>