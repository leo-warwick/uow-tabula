<#escape x as x?html>
<h1>My ${relationshipType.studentRole}s</h1>

<#if studentCourseDetails?has_content>
	<#assign submitUrl>/profiles/${relationshipType.urlPart}/students</#assign>
	<#assign filterCommand = viewRelatedStudentsCommand />
	<#assign filterCommandName = "viewRelatedStudentsCommand" />
	<#assign filterResultsPath = "/WEB-INF/freemarker/profiles/relationships/student_view_results.ftl" />
	<#include "/WEB-INF/freemarker/filter_bar.ftl" />
<#else>
	<p class="alert alert-info">No ${relationshipType.studentRole}s are currently visible for you in Tabula.</p>
</#if>
</#escape>
