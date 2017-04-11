<#escape x as x?html>

<h1>${relationshipType.description} meetings with no record </h1>

<#if tutorMap?keys?has_content>
	<table class="table table-striped table-condensed unrecorded">
		<thead>
			<tr>
				<th>${relationshipType.description}</th>
				<th>Number of unrecorded scheduled meetings</th>
			</tr>
		</thead>
		<tbody>
			<#list tutorMap?keys as tutorName>
				<tr>
					<td>${tutorName}</td>
					<td>
						<#assign count = mapGet(tutorMap, tutorName) />
						<#if (count >= 4)>
							<#assign class = 'danger' />
						<#elseif (count >= 2)>
							<#assign class = 'warning' />
						<#else>
							<#assign class = 'success' />
						</#if>
						<span class="badge progress-bar-${class}">${count}</span>
					</td>
				</tr>
			</#list>
		</tbody>
	</table>

	<script>
		jQuery(function($){
			$('table.unrecorded').tablesorter();
		});
	</script>
<#else>
	<p class="alert alert-info">No students are currently visible for ${department.name} in Tabula.</p>
</#if>
</#escape>