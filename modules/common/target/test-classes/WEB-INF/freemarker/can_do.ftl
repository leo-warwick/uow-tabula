<#ftl strip_text=true />

<#function do permission item>
<#return permissions(permission, item) />
</#function>

<#function do_scopeless permission>
	<#return permissions(permission) />
</#function>

<#function do_with_selector permission item selector>
<#return permissions(permission, item, selector) />
</#function>