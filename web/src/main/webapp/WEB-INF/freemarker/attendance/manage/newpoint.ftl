<#escape x as x?html>

<#if command.schemes?size == 1>

	<h1>Add monitoring point to scheme: ${command.schemes?first.displayName}</h1>

<#else>

	<h1>Add monitoring point</h1>

	<#assign popoverContent><#noescape>
		<ul>
			<#list command.schemes?sort_by("displayName") as scheme>
				<li>${scheme.displayName}</li>
			</#list>
		</ul>
	</#noescape></#assign>
	<p>
		You are adding this point to
		<a href="#" class="use-popover" data-content="${popoverContent}" data-html="true" data-placement="top">
			<@fmt.p command.schemes?size "scheme" />
		</a>
	</p>

</#if>

<#function extractParam collection param>
	<#local result = [] />
	<#list collection as item>
		<#local result = result + [item[param]] />
	</#list>
	<#return result />
</#function>

<@f.form action="" method="POST" commandName="command">

	<#assign hasOverlap = false />

	<#list command.schemes as scheme>
		<input name="schemes" value="${scheme.id}" type="hidden" />
	</#list>
	<input name="returnTo" value="${returnTo}" type="hidden" />

	<#include "_managepoint.ftl" />

	<div>
		<button class="btn btn-primary spinnable spinner-auto" type="submit" name="<#if hasOverlap>submitConfirm<#else>submit</#if>" data-loading-text="Adding&hellip;">
			Add
		</button>
		<button class="btn btn-default" type="submit" name="cancel">Cancel</button>
	</div>
</@f.form>


</#escape>