<#macro exams_sits_groups command >

	<#if command.availableUpstreamGroups?has_content>
	<div>
		<table id="sits-table" class="table table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers">
			<thead>
			<tr>
				<th class="for-check-all" style="width: 20px; padding-right: 0;"></th>
				<th class="sortable">Name</th>
				<th class="sortable">Members</th>
				<th class="sortable">Assessment group</th>
				<th class="sortable">CATS</th>
				<th class="sortable">Occurrence</th>
				<th class="sortable">Sequence</th>
				<th class="sortable">Type</th>
			</tr>
			</thead>
			<tbody>
				<#list command.availableUpstreamGroups as available>
					<#local isLinked = available.isLinked(command.assessmentGroups) />
					<tr>
						<td>
							<input
								type="checkbox"
								id="chk-${available.id}"
								name="upstreamGroups"
								value="${available.id}" class="upstreamGroups"
								${isLinked?string(" checked","")}
							>
						</td>
						<td><label for="chk-${available.id}">${available.name}<#if isLinked> <span class="label label-primary">Linked</span></#if></label></td>
						<td>${available.group.members?size}</td>
						<td>${available.group.assessmentGroup}</td>
						<td>${available.cats!'-'}</td>
						<td>${available.occurrence}</td>
						<td>${available.sequence}</td>
						<td>${available.assessmentType!'A'}</td>
					</tr>
				</#list>
			</tbody>
		</table>
	</div>

	<#else>
	<div class="modal-body">
		<p class="alert alert-danger">No SITS membership groups for ${command.module.code?upper_case} are available</p>
	</div>
	</#if>
</#macro>