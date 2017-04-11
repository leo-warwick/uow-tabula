<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<div id="profile-modal" class="modal fade profile-subset"></div>

<p>Found ${results.total} extensions.</p>
<table class="table table-striped">
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
				<td><h6 class="toggle-icon-large">&nbsp;${graph.user.firstName}</h6></td>
				<td><h6>${graph.user.lastName}&nbsp;<#if graph.user.warwickId??><@pl.profile_link graph.user.warwickId /><#else><@pl.profile_link graph.user.userId /></#if></h6></td>
				<td>${graph.extension.assignment.module.code}</td>
				<td>${graph.extension.assignment.name}</td>
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
				<td>
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