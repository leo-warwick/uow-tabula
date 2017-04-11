<#ftl strip_text=true />
<#--
	Macros for generating HTML for Bootstrap modals.

	Recommended usage is [#import "*/modal_macros.ftl" as modal /]
-->

<#--
	Wrap the nested content in a modal-header element with a close button.

	If enabled=false, the content is output without wrapping or button.
-->
<#macro header enabled=true>
	<#if enabled>
		<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal" aria-hidden="true" title="Close">&times;</button>
	</#if>
		<#nested />
	<#if enabled>
		</div>
	</#if>
</#macro>

<#macro body enabled=true>
	<#if enabled><div class="modal-body"></#if>
		<#nested />
	<#if enabled></div></#if>
</#macro>

<#macro footer enabled=true>
	<#if enabled><div class="modal-footer"></#if>
		<#nested />
	<#if enabled></div></#if>
</#macro>

<#macro wrapper enabled=true cssClass="">
	<#if enabled><div class="modal-dialog ${cssClass}"><div class="modal-content"></#if>
	<#nested />
	<#if enabled></div></div></#if>
</#macro>