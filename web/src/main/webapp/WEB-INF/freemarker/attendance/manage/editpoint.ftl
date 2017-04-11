<#escape x as x?html>

<h1>Edit monitoring point</h1>

<#assign popoverContent><#noescape>
<ul>
	<#list command.schemesToEdit?sort_by("displayName") as scheme>
		<li>${scheme.displayName}</li>
	</#list>
</ul>
</#noescape></#assign>

<p>
	You are editing this point on
	<a href="#" class="use-popover" data-content="${popoverContent}" data-html="true" data-placement="top">
		<@fmt.p command.schemesToEdit?size "scheme" />
	</a>
</p>

<#function extractParam collection param>
	<#local result = [] />
	<#list collection as item>
		<#local result = result + [item[param]] />
	</#list>
	<#return result />
</#function>

<@f.form action="" method="POST" commandName="command">

	<#assign hasOverlap = false />

	<input name="returnTo" value="${returnTo}" type="hidden" />

	<#include "_managepoint.ftl" />

	<div class="form-actions">
		<button class="btn btn-primary spinnable spinner-auto" type="submit" name="<#if hasOverlap>submitConfirm<#else>submit</#if>" data-loading-text="Saving&hellip;">
			Save
		</button>
		<a href="${returnTo}" class="btn btn-default">Cancel</a>
	</div>
</@f.form>


</#escape>