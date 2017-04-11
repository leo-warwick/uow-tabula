<#ftl strip_text=true />
<#compress><#-- Included into every Freemarker template. -->
<#if JspTaglibs??>
	<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
	<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
	<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>
	<#assign sso=JspTaglibs["/WEB-INF/tld/sso.tld"]>
	<#assign joda=JspTaglibs["/WEB-INF/tld/joda.tld"]>
	<#import "forms.ftl" as form />
	<#import "bs3-forms.ftl" as bs3form />
	<#import "routes.ftl" as routes />
	<#import "can_do.ftl" as can />
	<#import "component.ftl" as component />
</#if>
<#import "formatters.ftl" as fmt />

<#macro stylesheet path><link rel="stylesheet" href="<@url resource=path/>" type="text/css"></#macro>
<#macro script path><script src="<@url resource=path/>" type="text/javascript"></script></#macro>

<#function nonempty collection=[]>
<#return collection?? && collection?size gt 0/>
</#function>

</#compress>
