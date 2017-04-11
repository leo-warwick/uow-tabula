<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#escape x as x?html>

<h1>${exam.name} (${module.code?upper_case})</h1>

<div class="btn-toolbar">

	<#if exam.hasWorkflow>
		<#assign markers_url><@routes.exams.assignMarkers exam /></#assign>
		<@fmt.permission_button
			permission='Assignment.Update'
			scope=exam.module
			action_descr='assign markers'
			href=markers_url
			classes="btn btn-default"
		>
			Assign markers
		</@fmt.permission_button>
	<#else>
		<span class="use-tooltip btn btn-default disabled" data-container="body" title="Marking workflow is not enabled for this exam"><a class="btn-default disabled">Assign markers</a></span>
	</#if>

	<#if !exam.released>
		<#assign releaseForMarking_url><@routes.exams.releaseForMarking exam /></#assign>
		<@fmt.permission_button
			permission='Submission.ReleaseForMarking'
			scope=exam
			action_descr='release for marking'
			classes='btn btn-default'
			href=releaseForMarking_url
			id="release-submissions-button"
		>
			Release for marking
		</@fmt.permission_button>
	<#else>
		<span class="use-tooltip btn btn-default disabled" data-container="body" title="This exam has already been released for marking"><a class="disabled btn-default">Release for marking</a></span>
	</#if>

	<#assign marks_url><@routes.exams.addMarks exam /></#assign>
	<@fmt.permission_button
		permission='AssignmentFeedback.Manage'
		scope=exam
		action_descr='add marks'
		href=marks_url
		classes='btn btn-default'
	>
		Add marks
	</@fmt.permission_button>

	<#assign adjust_url><@routes.exams.feedbackAdjustment exam /></#assign>
	<@fmt.permission_button
		permission='AssignmentFeedback.Manage'
		scope=exam
		action_descr='adjust marks'
		tooltip='Adjust marks'
		href=adjust_url
		classes='btn btn-default'
	>
		Adjustments
	</@fmt.permission_button>

	<#assign upload_url><@routes.exams.uploadToSits exam /></#assign>
	<@fmt.permission_button
		permission='AssignmentFeedback.Publish'
		scope=exam
		action_descr='upload feedback to SITS'
		tooltip='Upload mark and grade to SITS'
		href=upload_url
		classes='btn btn-default'
	>
		Upload to SITS
	</@fmt.permission_button>

	<div class="btn-group">
		<a class="btn btn-default dropdown-toggle" data-toggle="dropdown">
			Save As
			<span class="caret"></span>
		</a>
		<ul class="dropdown-menu">
			<li>
				<a class="long-running form-post include-filter" title="Export exam info as XLSX, for advanced users." href="<@routes.exams.exportExcel module exam />">Excel</a>
			</li>
			<li>
				<a class="long-running form-post include-filter" title="Export exam info as CSV, for advanced users." href="<@routes.exams.exportCSV module exam />">Text (CSV)</a>
			</li>
			<li>
				<a class="long-running form-post include-filter" title="Export exam info as XML, for advanced users." href="<@routes.exams.exportXML module exam />">Text (XML)</a>
			</li>
		</ul>
	</div>
</div>

<table class="table table-striped table-condensed feedback-table">
	<thead>
		<tr>
			<th class="sortable">Seat number</th>
			<th class="sortable">Student</th>
			<th colspan="2">Original</th>
			<th colspan="2">Adjusted</th>
			<th colspan="4">SITS upload</th>
		</tr>
		<tr>
			<th colspan="2"></th>
			<th class="sortable">Mark</th>
			<th class="sortable">Grade</th>
			<th class="sortable">Mark</th>
			<th class="sortable">Grade</th>
			<th class="sortable">Status</th>
			<th class="sortable">Date</th>
			<th class="sortable">Mark</th>
			<th class="sortable">Grade</th>
		</tr>
	</thead>
	<tbody>
		<#list students as student>
			<#assign hasSeatNumber = mapGet(seatNumberMap, student)?? />
			<#assign hasFeedback = mapGet(feedbackMap, student)?? />
			<#assign hasSitsStatus = hasFeedback && mapGet(sitsStatusMap, mapGet(feedbackMap, student))?? />
			<tr>
				<td><#if hasSeatNumber>${mapGet(seatNumberMap, student)}</#if></td>
				<td>
					<#if module.adminDepartment.showStudentName>
						${student.fullName} <#if student.warwickId??><@pl.profile_link student.warwickId /><#else><@pl.profile_link student.userId /></#if>
					<#else>
						<#if student.warwickId??>${student.warwickId}<#else>${student.userId!}</#if>
					</#if>
				</td>
				<#if hasFeedback>
					<#assign feedback = mapGet(feedbackMap, student) />
					<td>${feedback.actualMark!""}%</td>
					<td>${feedback.actualGrade!""}</td>
					<td>${(feedback.latestPrivateOrNonPrivateAdjustment.mark)!""}</td>
					<td>${(feedback.latestPrivateOrNonPrivateAdjustment.grade)!""}</td>
					<#if hasSitsStatus>
						<#assign sitsStatus = mapGet(sitsStatusMap, feedback) />
						<#assign sitsWarning = sitsStatus.dateOfUpload?has_content && sitsStatus.status.code != "uploadNotAttempted" && (
							(sitsStatus.actualMarkLastUploaded!0) != (feedback.latestMark!0) || (sitsStatus.actualGradeLastUploaded!"") != (feedback.latestGrade!"")
						) />
						<#assign sitsClass>
							<#if sitsStatus.status.code == "failed" || sitsWarning >
								label-danger
							<#elseif sitsStatus.status.code == "successful">
								label-primary
							<#else>
								label-default
							</#if>
						</#assign>
						<td>
							<#if sitsStatus.status.code == "failed">
								<a href="<@routes.exams.checkSitsUpload feedback />" target="_blank" style="text-decoration: none;">
									<span class="label label-danger use-tooltip" title="There was a problem uploading to SITS. Click to try and diagnose the problem.">
										${sitsStatus.status.description}
									</span><#--
								--></a>
							<#elseif sitsWarning>
								<span class="label label-danger use-tooltip" title="The mark or grade uploaded differs from the current mark or grade. You will need to upload the marks to SITS again.">
									${sitsStatus.status.description}
								</span>
							<#else>
								<span class="label ${sitsClass}">
									${sitsStatus.status.description}<#if sitsWarning>  (!)</#if>
								</span>
							</#if>
						</td>
						<td data-sortby="${(sitsStatus.dateOfUpload.millis?c)!""}"><#if sitsStatus.dateOfUpload??><@fmt.date sitsStatus.dateOfUpload /></#if></td>
						<td>${sitsStatus.actualMarkLastUploaded!""}%</td>
						<td>${sitsStatus.actualGradeLastUploaded!""}</td>
					<#else>
						<td><span class="label label-default">Upload not queued</span></td><td></td><td></td><td></td>
					</#if>
				<#else>
					<td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td>
				</#if>
			</tr>
		</#list>
	</tbody>
</table>

<script type="text/javascript">
	(function($) {
		$('.feedback-table').sortableTable({
			textExtraction: function(node) {
				var $el = $(node);
				if ($el.data('sortby')) {
					return $el.data('sortby');
				} else {
					return $el.text().trim();
				}
			}
		});

	})(jQuery);
</script>

</#escape>