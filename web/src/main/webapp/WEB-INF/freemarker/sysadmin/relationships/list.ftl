<#escape x as x?html>

<div class="pull-right">
	<a href="<@url page="/sysadmin/relationships/add" />" class="btn btn-primary btn-medium pull-right">
		Add a new relationship type
	</a>
</div>

<h1 class="with-settings">Student relationship types</h1>

<table class="table table-striped">
	<thead>
		<tr>
			<th>URL string</th>
			<th>Description</th>
			<th>Agent role</th>
			<th>Student role</th>
			<th>Enabled by default?</th>
			<th>For UGs?</th>
			<th>For PGTs?</th>
			<th>For PGRs?</th>
			<th>Actions</th>
		</tr>
	</thead>
	<tbody>
		<#list relationshipTypes as relationshipType>
			<tr>
				<td>${relationshipType.urlPart}</td>
				<td>${relationshipType.description}</td>
				<td>${relationshipType.agentRole}</td>
				<td>${relationshipType.studentRole}</td>
				<td><#if relationshipType.defaultDisplay><b>Yes</b><#else>No</#if></td>
				<td><#if relationshipType.expectedUG><b>Yes</b><#else>No</#if></td>
				<td><#if relationshipType.expectedPGT><b>Yes</b><#else>No</#if></td>
				<td><#if relationshipType.expectedPGR><b>Yes</b><#else>No</#if></td>
				<td>
					<a href="<@url page="/sysadmin/relationships/${relationshipType.urlPart}/edit" />" class="btn btn-primary btn-xs">Edit</a>
					<#if !relationshipType.empty>
					<div class="use-tooltip" title="Can't delete this type as there are relationships associated with it">
						<a href="<@url page="/sysadmin/relationships/${relationshipType.urlPart}/delete" />" class="btn btn-danger btn-xs disabled">Delete</a>
					</div>
					<#else>
						<a href="<@url page="/sysadmin/relationships/${relationshipType.urlPart}/delete" />" class="btn btn-danger btn-xs">Delete</a>
					</#if>
				</td>
			</tr>
		</#list>
	</tbody>
</table>

</#escape>