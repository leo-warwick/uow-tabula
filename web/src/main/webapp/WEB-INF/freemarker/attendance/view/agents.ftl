<#escape x as x?html>
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />

<h1>${relationshipType.agentRole?capitalize}s</h1>

<div id="profile-modal" class="modal fade profile-subset"></div>

<div class="agent-search" style="display: none;">
	<div class="row">
		<div class="col-md-4">
			<div class="input-group">
				<input class="form-control" type="text" placeholder="Search for a student or ${relationshipType.agentRole}&hellip;"/>
				<span class="input-group-btn"><button class="btn btn-default"><i class="fa fa-search"></i></button></span>
			</div>
		</div>
		<div class="col-md-4">
			<span class="very-subtle" style="display: none;">Please enter at least 3 characters</span>
		</div>
		<div class="col-md-4">
			<div class="pull-right">
				<@fmt.bulk_email emails=agentsEmails title="Email these ${relationshipType.agentRole}s" subject=""/>
			</div>
		</div>
	</div>
</div>

<div class="fix-area">
	<table class="agents table table-striped">
		<thead class="fix-header pad-when-fixed">
			<tr>
				<th>${relationshipType.agentRole?capitalize}s</th>
				<th>${relationshipType.studentRole?capitalize}s</th>
				<th>Unrecorded</th>
				<th>Missed</th>
				<th></th>
			</tr>
		</thead>
		<tbody>
			<#list agents as agentData>
				<#assign studentPopoverContent>
					<ul>
						<#list agentData.students as student>
							<li>${student.fullName} <@pl.profile_link student.universityId /></li>
						</#list>
					</ul>
				</#assign>
				<tr>
					<td class="agent" data-sortby="<#if agentData.agentMember??>${agentData.agentMember.lastName}, ${agentData.agentMember.firstName}<#else>${agentData.agent}</#if>">
						<strong><#if agentData.agentMember??>${agentData.agentMember.fullName}<#else>${agentData.agent}</#if></strong>
						<div class="student-list" style="display: none;">
							${relationshipType.studentRole?capitalize}s:
							<#list agentData.students as student>
								<span class="name">${student.fullName}<span class="comma">, </span></span>
							</#list>
						</div>
					</td>
					<td class="students">
						<a href="#" class="use-popover" data-title="${relationshipType.studentRole?capitalize}s" data-content="${studentPopoverContent}" data-html="true">
							${agentData.students?size}
						</a>
					</td>
					<td class="unrecorded">
						<span class="<#if (agentData.unrecordedCount > 2)>badge progress-bar-danger<#elseif (agentData.unrecordedCount > 0)>badge progress-bar-warning</#if>">
							${agentData.unrecordedCount}
						</span>
					</td>
					<td class="missed">
						<span class="<#if (agentData.missedCount > 2)>badge progress-bar-danger<#elseif (agentData.missedCount > 0)>badge progress-bar-warning</#if>">
							${agentData.missedCount}
						</span>
					</td>
					<td class="button">
						<#if agentData.agentMember?? >
							<a href="<@routes.attendance.viewAgent department academicYear relationshipType agentData.agentMember />" class="btn btn-primary btn-xs">Attendance</a>
						<#else>
							&nbsp;
						</#if>
					</td>
				</tr>
			</#list>
		</tbody>
	</table>
</div>

<script type="text/javascript">
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();

		$('table.agents').tablesorter({
			sortList: [[0,0]],
			headers: { 4: { sorter: false }},
			textExtraction: function(node) {
				var $el = $(node);
				if ($el.data('sortby')) {
					return $el.data('sortby');
				} else {
					return $el.text().trim();
				}
			}
		});

		$(window).on('shown.bs.popover', function(){
			$('a.ajax-modal').ajaxModalLink();
		})
	});
</script>

</#escape>