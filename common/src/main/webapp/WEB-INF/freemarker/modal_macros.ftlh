<#ftl strip_text=true />
<#--
	Macros for generating HTML for Bootstrap modals.

	Recommended usage is [#import "*/modal_macros.ftlh" as modal /]
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

<#--  outer most div  -->
<#macro modal cssClass="" id="" ariaLabel="" ariaLabelledby="" role="" style="" ariaHidden=true enabled=true backdrop=true>
  <#if enabled>
    <div
    aria-hidden="${ariaHidden?string('true', 'false')}"
    <#if ariaLabel?has_content>aria-label="${ariaLabel}"</#if>
    <#if ariaLabelledby?has_content>aria-labelledby="${ariaLabelledby}"</#if>
    <#if id?has_content>id="${id}"</#if>
    <#if role?has_content>role="${role}"</#if>
    tabindex="-1"
    class="modal fade ${cssClass}"
    <#if !backdrop>data-backdrop="static"</#if>
    >
  </#if>
  <#nested />
  <#if enabled></div></#if>
</#macro>
