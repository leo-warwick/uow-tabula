<#macro listSets setAllocations first>
	<#list setAllocations as setAllocation>
		<div class="set-${setAllocation.set.id} set-info hide">
			<#if first>
				<@groupSet setAllocation.firstMarkerGroups firstMarkerRole "firstMarkerMapping" />
			<#else>
				<@groupSet setAllocation.secondMarkerGroups secondMarkerRole "secondMarkerMapping" />
			</#if>
		</div>
	</#list>
</#macro>

<#macro groupSet groupsAllocations roleName markerBinding>
	<#list groupsAllocations as group>
		<div class="group">
			<h4>${group.name}</h4>
			<table class="table marker-small-group">
				<thead><tr><th>${roleName}</th><th>Students</th></tr></thead>
				<tbody>
					<#list group.students as student>
					<tr>
						<#if student_index == 0>
							<td rowspan="${group.students?size}">
								<#if group.tutors?has_content>
									<select class="marker-selector">
										<#list group.tutors as tutor>
											<option value="${tutor.userId}">${tutor.fullName}</option>
										</#list>
									</select>
								<#else>
									There are no valid markers for this group
								</#if>
							</td>
						</#if>
						<td>
						${student.fullName}
							<#if group.tutors?has_content>
								<input class="allocation" type="hidden" name="${markerBinding}[][${student_index}]" value="${student.userId}" />
							</#if>
						</td>
					</tr>
					</#list>
				</tbody>
			</table>
		</div>
	</#list>
</#macro>

<#escape x as x?html>
	<#if sets?has_content>
		<div class="form-horizontal">
			<@form.labelled_row "" "Small group set">
				<select class="set-selector">
					<option></option>
					<#list sets as set>
						<option value="set-${set.id}">${set.name}</option>
					</#list>
				</select>
			</@form.labelled_row>
		</div>
		<ul class="nav nav-tabs hide">
			<li class="active"><a href="#first-markers" data-toggle="tab">${firstMarkerRole}s</a></li>
			<#if hasSecondMarker><li><a href="#second-markers" data-toggle="tab">${secondMarkerRole}s</a></li></#if>
		</ul>
		<div class="tab-content hide">
			<div class="tab-pane role-container active" id="first-markers" data-rolebinding="firstMarkerMapping">
				<@listSets allocations true />
			</div>
			<#if hasSecondMarker>
				<div class="tab-pane role-container" id="second-markers" data-rolebinding="secondMarkerMapping">
					<@listSets allocations false />
				</div>
			</#if>
		</div>
		<div class="submit-buttons form-actions">
			<button type="submit" name="smallGroupImport" class="btn btn-primary">Save</button>
			<a class="btn" href="<@routes.groups.courseworkDeptHome assessment.module />">Cancel</a>
		</div>
	<#else>
		<div>
			Not enough tutors are set up as markers in the workflow for Small Group sets for
			<@fmt.module_name module=assessment.module withFormatting=false /> in ${assessment.academicYear.toString}.
			You need to make sure that every Small Group has tutors who are also markers.
		</div>
	</#if>
</#escape>