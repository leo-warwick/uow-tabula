<#escape x as x?html>

<#macro paginator>
<#if page gt 0>
<a href="?page=${page-1}">Newer</a>
</#if>
<a href="?page=${page+1}">Older</a>
</#macro>

<#macro jobs jobs actions=false>
<table class="jobs-list" width="100%">
<thead>
<tr>
	<th>Created</th>
	<th>Type</th>
	<th>User</th>
	<th>Status</th>
	<th>Progress</th>
	<th>Data</th>
	<#if actions>
		<th>Actions</th>
	</#if>
</tr>
</thead>
<tbody>
<#list jobs as job>
<tr class="type-${job.jobType}<#if job_index % 2 = 0> even-row</#if>">
	<td><@fmt.date date=job.createdDate seconds=true /></td>
	<td>${job.jobType}</td>
	<td>
		<#if job.user?? && job.user.exists>
			${job.user.toString}
		</#if>
	</td>
	<td>${job.id} -${job.status!''}</td>
	<td>
		<#if job.finished && job.succeeded>
			<span class="label label-success">Succeeded</span>
		<#elseif job.finished>
			<span class="label label-important">Failed</span>
		<#elseif job.started>
			<span class="label label-info">Started</span>
		<#else>
			<span class="label label-warning">Waiting</span>
		</#if>
		<span class="percent">${job.progress!'?'}%</span>
	</td>
	<td class="data"><div class="extra-data">${job.data}</div></td>
	<#if actions>
		<td class="actions">
			<a class="btn btn-small btn-info" href="<@url page="/sysadmin/jobs/run" />?id=${job.id}">Run</a>
			<a class="btn btn-small btn-danger" href="<@url page="/sysadmin/jobs/kill" />?id=${job.id}">Kill</a>
		</td>
	</#if>
</tr>
</#list>
</tbody>
</table>
</#macro>

<h1>Background jobs</h1>

<#if page == 0>
<h2>Incomplete jobs</h2>

<#if unfinished?size gt 0>
<@jobs unfinished true />
<#else>
<p>There are no incomplete jobs.</p>
</#if>
</#if>

<h2>Completed jobs</h2>

<p>Results ${startIndex} - ${endIndex} <@paginator /></p>

<#if finished?size gt 0>
<@jobs finished />
<#else>
<p>There are no completed jobs.</p>
</#if>

</#escape>