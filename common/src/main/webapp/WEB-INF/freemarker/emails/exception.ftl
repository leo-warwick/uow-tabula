env=${environment}<#if standby> (standby)</#if>
time=${time.toDate()?string.long}
<#compress>
<#if requestInfo??>
<#if requestInfo.user?? && requestInfo.user.exists>
user=${requestInfo.user.realId}
<#if requestInfo.user.masquerading>
masqueradingAs=${requestInfo.user.apparentId}
</#if>
</#if>
<#if requestInfo.requestedUri??>
info.requestedUri=${requestInfo.requestedUri}
</#if>
</#if>
<#if request??>
request.remoteAddr=${request.remoteAddr}
request.requestURI=${request.requestURI}
request.method=${request.method}
<#assign parameters=request.parameterMap />
<#list parameters?keys?sort as key>
request.params[${key}]=[<#list parameters[key] as v>${v}<#if v_has_next>,</#if></#list>]
</#list>
</#if>
</#compress>

${exceptionStack}

<#if request??>
<#list request.headerNames as header>${header}: <#if header?lower_case == "authorization">***REMOVED***<#else>${request.getHeader(header)}</#if>
</#list>
</#if>

