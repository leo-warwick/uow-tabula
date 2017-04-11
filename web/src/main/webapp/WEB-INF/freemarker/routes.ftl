<#ftl strip_text=true />
<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.

TODO grab values from the Routes object in code, as that's pretty equivalent and
	we're repeating ourselves here. OR expose Routes directly.

-->
<#macro home><@url context="/" page="/" /></#macro>
<#macro zipProgress jobId><@url context="/" page="/zips/${jobId}" /></#macro>
<#macro zipComplete jobId><@url context="/" page="/zips/${jobId}/zip" /></#macro>

<#macro photo profile><#if ((profile.universityId)!)?has_content><@url context="/profiles" page="/view/photo/${profile.universityId}.jpg" /><#else><@url resource="/static/images/no-photo.jpg" /></#if></#macro>
<#macro relationshipPhoto profile relationship><@url context="/profiles" page="/view/photo/${profile.universityId}/${relationship.relationshipType.urlPart}/${relationship.agent}.jpg" /></#macro>

<#import "reports/routes.ftl" as reports />
<#import "admin/routes.ftl" as admin />
<#import "groups/routes.ftl" as groups />
<#import "exams/routes.ftl" as exams />
<#import "attendance/routes.ftl" as attendance />
<#import "coursework/routes.ftl" as coursework />
<#import "cm2/routes.ftl" as cm2 />
<#import "profiles/routes.ftl" as profiles />