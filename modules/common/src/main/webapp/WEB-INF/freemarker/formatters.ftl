<#compress>
<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>

<#macro module_name module>
<span class="mod-code">${module.code?upper_case}</span> <span class="mod-name">${module.name}</span>
</#macro>

<#macro date date at=false timezone=false seconds=false capitalise=true relative=true><#--
-->${dateBuilder(date, seconds, at, timezone, capitalise, relative)}<#--
--></#macro>

<#macro interval start end=""><#--
--><#if end?has_content>${intervalFormatter(start, end)}<#--
--><#else>${intervalFormatter(start)}</#if><#--
--></#macro>

<#macro p number singular plural="${singular}s" one="1" zero="0"><#--
--><#if number=1>${one}<#elseif number=0>${zero}<#else>${number}</#if><#--
--> <#if number=1>${singular}<#else>${plural}</#if></#macro>

<#macro tense date future past><#if date.afterNow>${future}<#else>${past}</#if></#macro>

<#macro usergroup_summary ug>
<div class="usergroup-summary">
<#if ug.baseWebgroup??>
	Webgroup "${ug.baseWebgroup}" (${ug.baseWebgroupSize} members)
	<#if ug.includeUsers?size gt 0>
	+${ug.includeUsers?size} extra users
	</#if>
	<#if ug.excludeUsers?size gt 0>
	-${ug.excludeUsers?size} excluded users
	</#if>
<#else>
	<#if ug.includeUsers?size gt 0>
	${ug.includeUsers?size} users
	</#if>
</#if>
</div>
</#macro>

<#-- comma separated list of users by name -->
<#macro user_list_csv ids>
<@userlookup ids=ids>
	<#list returned_users?keys?sort as id>
		<#assign returned_user=returned_users[id] />
		<#if returned_user.foundUser>
			${returned_user.fullName}<#if id_has_next>,</#if>
		<#else>
			${id}<#if id_has_next>,</#if>
		</#if>
	</#list>
	</@userlookup>
</#macro>

<#macro profile_name profile>${profile.fullName}</#macro>
<#macro profile_description profile><span class="profile-description">${profile.description!""}</span></#macro>

<#macro nationality nationality><#--
--><#if nationality = 'British (ex. Channel Islands & Isle of Man)' || nationality = 'British [NO LONGER IN USE: change to 2826]' || nationality = 'NAT code 000 should be used for British'><#--
	--><abbr title="${nationality}">British</abbr><#--
--><#elseif nationality?starts_with('(Obsolete) Formerly ')><#--
	--><abbr title="${nationality}">${nationality?substring(20)}</abbr><#--
--><#else><#--
	-->${nationality}<#--
--></#if></#macro>
</#compress>