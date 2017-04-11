<#escape x as x?html>

<#assign hasRows = command.sheetFirstMarkers?has_content || command.sheetSecondMarkers?has_content />

<@f.form method="post" action=assignMarkersURL commandName="command">

	<h1>Spreadsheet upload of marker assignment</h1>
	<h4><span class="muted">for</span> ${assessment.name}</h4>

	<div class="submit-buttons">
		<#if hasRows>
			<button type="submit" name="finaliseSpreadsheet" class="btn btn-primary">Continue</button>
		</#if>
		<a href="${cancelUrl}" class="btn">Cancel</a>
	</div>


	<#if command.sheetErrors?size &gt; 0>
		<#assign firstRoleKey>${firstMarkerRole?lower_case?replace(" ", "_")}_name</#assign>
		<#assign secondRoleKey>${secondMarkerRole?lower_case?replace(" ", "_")}_name</#assign>
		<h2>Invalid rows</h2>
		<h6>The following rows had errors which prevent them being uploaded</h6>
		<table class="table table-bordered table-condensed table-striped">
			<thead>
				<tr>
					<th>Student ID</th>
					<th>Student Name</th>
					<th>Marker ID</th>
					<th>Marker Name</th>
				</tr>
			</thead>
			<tbody>
				<#list command.sheetErrors as row>
					<#list row.errors as error>
						<tr class="no-bottom-border">
							<#if error.rowData["student_id"]??><td>${error.rowData["student_id"]}</td><#else><td></td></#if>
							<#if error.rowData["student_name"]??><td>${error.rowData["student_name"]}</td><#else><td></td></#if>
							<#if error.rowData["agent_id"]??><td>${error.rowData["agent_id"]}</td><#else><td></td></#if>
							<#if error.rowData[firstRoleKey]??>
								<td>${error.rowData[firstRoleKey]}</td>
							<#elseif error.rowData[secondRoleKey]??>
								<td>${error.rowData[secondRoleKey]}</td>
							<#else>
								<td></td>
							</#if>
						</tr>
						<tr class="no-top-border">
							<#if error.field == "student_id">
								<td colspan="2">
									<@spring.message code=error.code arguments=error.codeArgument />
								</td>
								<td colspan="2"></td>
							<#else>
								<td colspan="2"></td>
								<td colspan="2">
									<@spring.message code=error.code arguments=error.codeArgument />
								</td>
							</#if>
						</tr>
					</#list>
				</#list>
			</tbody>
		</table>
	</#if>

	<#macro allocationTable allocations roleName bindName>
		<h2>${roleName} allocations</h2>
		<table class="table table-bordered table-condensed table-striped">
			<thead>
			<tr>
				<th>Student ID</th>
				<th>Student Name</th>
				<th>${roleName?cap_first} ID</th>
				<th>${roleName?cap_first} Name</th>
			</tr>
			</thead>
			<tbody>
				<#list allocations as allocation>
					<#list allocation.students as student>
					<tr>
						<td>
						${student.warwickId}
							<input type="hidden" name="${bindName}[${allocation.marker.userId}][${student_index}]" value="${student.userId}" />
						</td>
						<td>${student.fullName}</td>
						<td>${allocation.marker.warwickId}</td>
						<td>${allocation.marker.fullName}</td>
					</tr>
					</#list>
				</#list>
			</tbody>
		</table>
	</#macro>

	<#if command.sheetFirstMarkers?has_content>
		<@allocationTable command.sheetFirstMarkers firstMarkerRole "firstMarkerMapping"/>
	</#if>
	<#if command.sheetSecondMarkers?has_content>
		<@allocationTable command.sheetSecondMarkers secondMarkerRole "secondMarkerMapping"/>
	</#if>
	<#if command.unallocatedStudents?has_content>
		<h2>Unallocated students</h2>
		<p>The following students have not been assgined a marker in the spreadsheet.</p>
		<table class="table table-bordered table-condensed table-striped">
			<thead>
				<tr>
					<th>Student ID</th>
					<th>Student Name</th>
				</tr>
			</thead>
			<tbody><#list command.unallocatedStudents as student>
				<tr>
					<th>${student.warwickId}</th>
					<th>${student.fullName}</th>
				</tr>
			</#list></tbody>
		</table>

	</#if>
	<div class="submit-buttons">
		<#if hasRows>
			<button type="submit" name="finaliseSpreadsheet" class="btn btn-primary">Continue</button>
		</#if>
		<a href="${cancelUrl}" class="btn">Cancel</a>
	</div>

</@f.form>
</#escape>
