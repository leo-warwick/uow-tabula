<#assign module = assignment.module />
<#assign department = module.adminDepartment />
<#assign feedbackGraphs = studentFeedbackGraphs />

<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<div id="profile-modal" class="modal fade profile-subset"></div>

<#function markingId user><#compress>
	<#if !user.warwickId?has_content || user.getExtraProperty("urn:websignon:usersource")! == 'WarwickExtUsers'>
		<#return user.userId />
	<#else>
		<#return user.warwickId />
	</#if>
</#compress></#function>

<#macro row graph>
	<#assign u = graph.student />
<tr class="item-container" data-contentid="${markingId(u)}" data-markingurl="${onlineMarkingUrls[u.userId]}">
	<#if showMarkingCompleted>
		<td class="check-col">
			<input type="checkbox" class="collection-checkbox" name="students" value="${u.userId}">
		</td>
	</#if>
	<#if department.showStudentName>
		<td class="student-col toggle-cell"><h6 class="toggle-icon">${u.firstName}</h6></td>
		<td class="student-col toggle-cell"><h6>${u.lastName}&nbsp;<#if u.warwickId??><@pl.profile_link u.warwickId /><#else><@pl.profile_link u.userId /></#if></h6></td>
	<#else>
		<td class="student-col toggle-cell"><h6 class="toggle-icon"><#if u.warwickId??>${u.warwickId}<#else>${u.userId}</#if></h6></td>
	</#if>
	<td class="status-col toggle-cell content-cell">
		<dl style="margin: 0; border-bottom: 0;">
		<dt>
			<#if graph.hasSubmission>
				<div class="label">Submitted</div>
			<#else>
				<div class="label label-warning">No submission</div>
			</#if>
			<#if graph.hasPublishedFeedback>
				<div class="label label-success">Published</div>
			<#elseif graph.hasCompletedFeedback>
				<div class="label label-success">Marking completed</div>
			<#elseif graph.hasUncompletedFeedback>
				<div class="label label-warning marked">Marked</div>
			<#elseif graph.hasRejectedFeedback>
				<div class="label label-important">Changes requested</div>
			</#if>
			</dt>
			<dd style="display: none;" class="table-content-container" data-contentid="${markingId(u)}">
				<div id="content-${markingId(u)}" class="content-container" data-contentid="${markingId(u)}">
					<p>No data is currently available. Please check that you are signed in.</p>
				</div>
			</dd>
		</dl>
	</td>
</tr>
</#macro>

<#escape x as x?html>
<h1>Online marking</h1>
<h5><span class="muted">for</span> ${assignment.name} (${assignment.module.code?upper_case})</h5>

	<#import "../turnitin/_report_macro.ftl" as tin />
	<#import "../submissionsandfeedback/_submission_details.ftl" as sd />

	<#if showMarkingCompleted>
	<div class="btn-toolbar">
		<div class="btn-group-group">
			<div class="btn-group">
				<a class="btn hover"><i class="icon-cog"></i> Actions:</a>
			</div>
			<div class="btn-group">
				<a id="marking-complete-button" data-container="body" class="must-have-selected btn use-tooltip form-post" href="<@routes.coursework.markingCompleted assignment marker />" title="" data-original-title="Finalise marks and feedback. Changes cannot be made to marks or feedback files after this point.">
					<i class="icon-ok"></i>
					Marking completed
				</a>
			</div>
		</div>
	</div>
	<#elseif showGenericFeedback>
	<div class="generic-feedback">
		<h6 class="toggle-icon edit-generic-feedback">
			<i class="row-icon icon-chevron-right icon-fixed-width" style="margin-top: 2px;"></i>
			Generic feedback
		</h6>
		<div class="edit-generic-feedback-container" style="display: none;"></div>
	</div>
	</#if>


<table id="online-marking" class="students table table-bordered table-striped tabula-greenLight sticky-table-headers expanding-table">
	<thead<#if feedbackGraphs?size == 0> style="display: none;"</#if>>
	<tr>
		<#if showMarkingCompleted>
			<th class="check-col" style="padding-right: 0px;">
				<input class="collection-check-all" type="checkbox">
				<input class="post-field" name="onlineMarking" type="hidden" value="true">
			</th>
		</#if>
		<#if department.showStudentName>
			<th class="student-col">First name</th>
			<th class="student-col">Last name</th>
		<#else>
			<th class="student-col">University ID</th>
		</#if>

		<th class="status-col">Status</th>
	</tr>
	</thead>

	<#if feedbackGraphs?size gt 0>
		<tbody>
			<#list feedbackGraphs as graph>
					<@row graph />
				</#list>
		</tbody>
	</#if>
</table>

	<#if feedbackGraphs?size gt 0>
	<script type="text/javascript">
		(function($) {
			var tsOptions = {
				<#if showMarkingCompleted>
					sortList: [<#if department.showStudentName>[3, 0], </#if>[2, 0], [1,0]],
					headers: { 0: { sorter: false} }
				<#else>
					sortList: [<#if department.showStudentName>[2, 0], </#if>[1, 0], [0,0]]
				</#if>
			};

			$('.expanding-table').expandingTable({
				contentUrlFunction: function($row){ return $row.data('markingurl'); },
				useIframe: true,
				tableSorterOptions: tsOptions
			});
		})(jQuery);
	</script>
	<#else>
	<p>There are no submissions to mark for this assignment.</p>
	</#if>
</#escape>