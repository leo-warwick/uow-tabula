<#escape x as x?html>
	<div class="deptheader">
		<h1>Create set of small groups - Allocate</h1>
		<h4><span class="muted">to</span> ${smallGroupSet.name}</h4>
	</div>
	<#assign submitUrl><@routes.groups.createcrossmodulegroupsallocate smallGroupSet /></#assign>
	<#include "_allocate.ftl" />
</#escape>