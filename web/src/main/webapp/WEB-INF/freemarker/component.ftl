<#ftl strip_text=true />

<#assign requestPath = (info.requestedUri.path!"") />

<#if requestPath == '/reports' || requestPath?starts_with('/reports/')>
	<#assign bodyClass="reports-page" />
	<#assign siteHeader="Reports" />
	<#assign subsite=true />
	<#assign title="Reports" />
	<#assign name="reports" />
	<#assign nonav=false />
	<#assign homeUrl><@routes.reports.home /></#assign>
<#elseif requestPath == '/admin' || requestPath?starts_with('/admin/')>
	<#assign bodyClass="admin-page" />
	<#assign siteHeader="Administration & Permissions" />
	<#assign subsite=true />
	<#assign title="Administration & Permissions" />
	<#assign name="admin" />
	<#assign nonav=false />
	<#assign homeUrl><@routes.admin.home /></#assign>
<#elseif requestPath == '/groups' || requestPath?starts_with('/groups/')>
	<#assign bodyClass="groups-page" />
	<#assign siteHeader="Small Group Teaching" />
	<#assign subsite=true />
	<#assign title="Small Group Teaching" />
	<#assign name="groups" />
	<#assign nonav=false />
	<#assign homeUrl><@routes.groups.home /></#assign>
<#elseif requestPath== '/exams' || requestPath?starts_with('/exams/')>
	<#assign bodyClass="exams-page" />
	<#assign siteHeader="Exams Management" />
	<#assign subsite=true />
	<#assign title="Exams Management" />
	<#assign name="exams" />
	<#assign context="/exams" />
	<#assign nonav=false />
	<#assign homeUrl><@routes.exams.home /></#assign>
<#elseif requestPath == '/attendance' || requestPath?starts_with('/attendance/')>
	<#assign bodyClass="attendance-page" />
	<#assign siteHeader="Monitoring Points" />
	<#assign subsite=true />
	<#assign title="Monitoring Points" />
	<#assign name="attendance" />
	<#assign nonav=false />
	<#assign homeUrl><@routes.attendance.home /></#assign>
<#elseif requestPath == '/coursework' || requestPath?starts_with('/coursework/')>
	<#assign bodyClass="coursework-page" />
	<#assign siteHeader="Coursework Management" />
	<#assign subsite=true />
	<#assign title="Coursework Management" />
	<#assign name="courses" />
	<#assign nonav=false />
	<#assign homeUrl><@routes.coursework.home /></#assign>
	<#assign context="/coursework" />
<#elseif requestPath == '/cm2' || requestPath?starts_with('/cm2/')>
	<#assign bodyClass="coursework-page" />
	<#assign siteHeader="CM2" />
	<#assign subsite=true />
	<#assign title="CM2" />
	<#assign name="cm2" />
	<#assign nonav=false />
	<#assign homeUrl><@routes.cm2.home /></#assign>
	<#assign context="/cm2" />
<#elseif requestPath == '/profiles' || requestPath?starts_with('/profiles/')>
	<#assign bodyClass="profiles-page" />
	<#assign siteHeader="Profiles" />
	<#assign subsite=true />
	<#assign title="Profiles" />
	<#assign name="profiles" />
	<#assign nonav=false />
	<#assign homeUrl><@routes.profiles.home /></#assign>
<#else>
	<#assign bodyClass="tabula-page" />
	<#assign siteHeader="Tabula" />
	<#assign subsite=false />
	<#assign title="Tabula" />
	<#assign name="home" />
	<#assign nonav=true />
</#if>