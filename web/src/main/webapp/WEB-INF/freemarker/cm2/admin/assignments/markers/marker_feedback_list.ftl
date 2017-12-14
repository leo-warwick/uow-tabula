<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/coursework_components.ftl" as components />
<#import "*/modal_macros.ftl" as modal />

<#list feedbackByOrderAndStage as order>
	<#assign markingCompleted><@routes.cm2.markingCompleted assignment order.headerStage.order marker /></#assign>
	<#assign finishMarking><@routes.cm2.finishMarking assignment order.headerStage.order marker /></#assign>
	<div class="marking-stage">
		<h3>${order.headerStage.description}</h3>
		<#if order.hasFeedback>
			<#if order.headerStage.nextStagesDescription?has_content>
				<a class="btn btn-primary must-have-selected must-have-ready-next-stage form-post" href="${markingCompleted}">Confirm selected and send to ${order.headerStage.nextStagesDescription?lower_case}</a>
			</#if>
			<#if order.headerStage.canFinish(assignment.cm2MarkingWorkflow)>
				<a class="btn btn-primary must-have-selected must-have-ready-next-stage form-post" href="${finishMarking}">Confirm selected and send to admin</a>
			</#if>
			<div class="btn-group">
				<button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
					Download <span class="caret"></span>
				</button>
				<ul class="dropdown-menu">
					<li class="must-have-selected">
						<a class="form-post" href="<@routes.cm2.downloadMarkerSubmissions assignment marker />">
							Download all selected submissions
						</a>
					</li>
					<li class="must-have-selected">
						<a class="download-pdf" data-target="#download-pdf-modal-${order.headerStage.name}" href="<@routes.cm2.downloadMarkerSubmissionsPdf assignment marker />">
							Download all selected submissions as pdf
						</a>
					</li>
					<#if features.feedbackTemplates && assignment.hasFeedbackTemplate>
						<li>
							<a class="use-tooltip" title="Download feedback templates for all students as a ZIP file." href="<@routes.cm2.markerTemplatesZip assignment marker/>" data-container="body">
								Download feedback templates
							</a>
						</li>
					</#if>
					<#list workflowType.allPreviousStages(order.headerStage) as pStage>
						<li class="must-have-selected">
							<a class="form-post" href="<@routes.cm2.downloadMarkerFeedbackStage assignment marker pStage />">Download feedback from ${pStage.description?lower_case}</a>
						</li>
					</#list>
				</ul>
			</div>
			<div class="btn-group">
				<button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
					Upload <span class="caret"></span>
				</button>
				<ul class="dropdown-menu">
					<li><a href="<@routes.cm2.markerUploadFeedback assignment marker />">Upload attachments</a></li>
					<li><a href="<@routes.cm2.markerUploadMarks assignment marker />">Upload marks & feedback</a></li>
				</ul>
			</div>
			<table class="table table-striped marking-table table-sortable<#if order.headerStage.summarisePreviousFeedback> preview-marks</#if>">
				<thead>
				<tr>
					<th class="check-col"><@bs3form.selector_check_all /></th>
					<#if assignment.anonymity.equals(AssignmentAnonymity.FullyAnonymous)>
						<th class="student-col sortable">ID</th>
					<#elseif assignment.anonymity.equals(AssignmentAnonymity.IDOnly)>
						<th class="student-col sortable">University ID</th>
					<#else>
						<th class="student-col sortable">University ID</th>
						<th class="student-col sortable">First name</th>
						<th class="student-col sortable">Last name</th>
					</#if>
					<#if order.headerStage.summariseCurrentFeedback>
							<th class="sortable">Mark</th>
					</#if>
					<#if order.headerStage.summarisePreviousFeedback>
						<#list order.headerStage.previousStages as prevStage>
							<th class="sortable">${prevStage.allocationName}</th>
							<th class="sortable">${prevStage.allocationName} Mark</th>
						</#list>
					</#if>
					<th colspan="2">Progress</th>
				</tr>
				</thead>
				<tbody>
					<#list order.enhancedFeedbackByStage?keys as stage>
						<#assign enhancedMarkerFeedbacks = mapGet(order.enhancedFeedbackByStage, stage)/>
						<#list enhancedMarkerFeedbacks as emf>
							<#assign mf = emf.markerFeedback />
							<#assign student = mf.student />
							<#assign studentId><#if assignment.anonymity.equals(AssignmentAnonymity.FullyAnonymous)>${mf.feedback.anonymousId}<#else>${student.userId}</#if></#assign>
							<tr
								data-toggle="collapse"
								data-target="#${stage.name}-${studentId}"
								class="clickable collapsed expandable-row <#if mf.readyForNextStage>ready-next-stage</#if>"
							>
								<td class="check-col">
									<@bs3form.selector_check_row name="markerFeedback" value="${mf.id}" />
								</td>
								<#if assignment.anonymity.equals(AssignmentAnonymity.FullyAnonymous)>
									<#assign colspan = 4>
									<td class="toggle-icon-large student-col"><span class=""></span>Student${mf.feedback.anonymousId}</td>
								<#elseif assignment.anonymity.equals(AssignmentAnonymity.IDOnly)>
									<#assign colspan = 4>
									<td class="toggle-icon-large student-col"><span class=""></span>${mf.feedback.studentIdentifier!""}</td>
								<#else>
									<#assign colspan = 6>
									<td class="toggle-icon-large student-col">${mf.feedback.studentIdentifier!""}</td>
									<td class="student-col">${student.firstName}</td>
									<td class="student-col">${student.lastName}&nbsp;<#if student.warwickId??><@pl.profile_link student.warwickId /><#else><@pl.profile_link student.userId /></#if></td>
								</#if>
								<#if order.headerStage.summariseCurrentFeedback>
									<td><#if mf.mark??>${mf.mark}</#if></td>
									<#assign colspan = colspan + 1>
								</#if>
								<#if order.headerStage.summarisePreviousFeedback>
									<#list emf.previousMarkerFeedback as prevMf>
										<td><#if prevMf.marker??>${prevMf.marker.fullName}</#if></td>
										<td><#if prevMf.mark??>${prevMf.mark}</#if></td>
										<#assign colspan = colspan + 2>
									</#list>
								</#if>
								<td class="progress-col">
									<@components.individual_stage_progress_bar emf.workflowStudent.stages assignment student />
								</td>
								<td>
									<#if emf.workflowStudent.nextAction?has_content>
									<@components.workflowMessage emf.workflowStudent.nextAction assignment student />
								</#if>
								</td>
							</tr>
							<#assign detailUrl><@routes.cm2.markerOnlineFeedback assignment stage marker student /></#assign>
							<tr id="${stage.name}-${studentId}" data-detailurl="${detailUrl}" class="collapse detail-row">
								<td colspan="${colspan}" class="detailrow-container">
									<i class="fa fa-spinner fa-spin"></i> Loading
								</td>
							</tr>
						</#list>
					</#list>
				</tbody>
			</table>
			<#if order.headerStage.nextStagesDescription?has_content>
				<a class="btn btn-primary must-have-selected must-have-ready-next-stage form-post" href="${markingCompleted}">Confirm selected and send to ${order.headerStage.nextStagesDescription?lower_case}</a>
			</#if>
			<div id="download-pdf-modal-${order.headerStage.name}" class="modal fade">
				<@modal.wrapper>
					<@modal.header>
						<h3 class="modal-title">Download submissions as PDF</h3>
					</@modal.header>
					<@modal.body>
						<p>There are <span class="count"></span> submissions that have files that are not PDFs (shown below). The download will not include these files.</p>
						<p><a class="form-post btn btn-primary"
									data-href="<@routes.cm2.downloadMarkerSubmissionsPdf assignment marker />?download" href="">Download submissions as PDF</a>
						</p>
						<ul class="submissions"></ul>
					</@modal.body>
				</@modal.wrapper>
			</div>
		<#else>
			No students found
		</#if>
	</div>
</#list>