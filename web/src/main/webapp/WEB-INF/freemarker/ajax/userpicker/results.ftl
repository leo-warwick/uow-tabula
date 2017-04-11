<#escape x as x?html>
<#compress>

<#macro results collection title>
<h3>${title}</h3>
<#if collection?size = 0>
<p>No results.</p>
<#else>
<table>
	<tr>
		<th>Name</th>
		<#if RequestParameters.isUniId = "true">
			<th>University ID</th>
		<#else>
			<th>Usercode</th>
		</#if>
	</tr>
<#list collection as u>
	<tr>
		<td>${u.fullName}</td>

		<td class="user-id">
			<#if RequestParameters.isUniId = "true">
				${u.warwickId!}
			<#else>
				${u.userId}
			</#if>

		</td>
	</tr>
</#list>
</table>
</#if>
</#macro>

<@results staff "Staff" />
<@results students "Students" />

</#compress>
</#escape>