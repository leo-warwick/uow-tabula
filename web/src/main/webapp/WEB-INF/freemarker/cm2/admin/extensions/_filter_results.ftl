<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/cm2_macros.ftl" as cm2 />
<#assign currentPage = command.page />
<#assign totalPages = results.total/command.extensionsPerPage?ceiling />

<div id="profile-modal" class="modal fade profile-subset"></div>

<div class="row extension-metadata">
	<div class="col-md-7">
		<p>Found <@fmt.p results.total "extension" />.</p>
	</div>
	<div class="col-md-5">
		<p class="alert alert-info">
			Students will automatically be notified by email when you approve, modify or revoke an extension.
		</p>
	</div>
	<div class="col-md-12">
		 <@cm2.pagination currentPage totalPages />
	</div>
</div>

<table class="students table table-striped sticky-table-headers expanding-table">
	<thead>
		<tr>
			<th>First name</th>
			<th>Last name</th>
			<th>Module</th>
			<th>Assignment</th>
			<th>Status</th>
			<th>Extension length</th>
			<th>Submission due</th>
		</tr>
	</thead>
	<tbody>
		<#list results.extensions as graph>
			<tr data-toggle="collapse" data-target="#extension${graph.extension.id}" class="clickable collapsed expandable-row">
				<td class="student-col toggle-cell toggle-icon">&nbsp;${graph.user.firstName}</td>
				<td class="student-col toggle-cell">&nbsp;${graph.user.lastName}&nbsp;<#if graph.user.warwickId??><@pl.profile_link graph.user.warwickId /><#else><@pl.profile_link graph.user.userId /></#if></td>
				<td><@fmt.module_name graph.extension.assignment.module false /></td>
				<td><a href="<@routes.cm2.assignmentextensions graph.extension.assignment />">${graph.extension.assignment.name}</a></td>
				<td>${graph.extension.state.description}</td>
				<td>
					<#if graph.hasApprovedExtension || graph.isAwaitingReview() || graph.extension.moreInfoRequired>
						<#if graph.duration != 0>
							<@fmt.p graph.duration "day"/>
						<#else>
							<@fmt.p graph.requestedExtraDuration "day"/>
						</#if>
					</#if>
				</td>
				<td <#if graph.deadline?has_content>data-datesort="${graph.deadline.millis?c!''}"</#if> class="deadline-col <#if graph.hasApprovedExtension>approved<#else>very-subtle</#if>">
					<#if graph.deadline?has_content><@fmt.date date=graph.deadline /></#if>
				</td>
			</tr>
			<#assign detailUrl><@routes.cm2.extensionDetail graph.extension /></#assign>
			<tr id="extension${graph.extension.id}" data-detailurl="${detailUrl}" class="collapse detail-row">
				<td colspan="7" class="detailrow-container">
					<i class="fa fa-spinner fa-spin"></i> Loading
				</td>
			</tr>
		</#list>
	</tbody>
</table>