<#ftl strip_text=true />
<#--
		Just a handy place to create macros for generating URLs to various places, to save time
		if we end up changing any of them.

		TODO grab values from the Routes object in code, as that's pretty equivalent and
		we're repeating ourselves here. OR expose Routes directly.

		-->

<#macro _u page context='/attendance'><@url context=context page=page /></#macro>

<#macro home academicYear="">
	<#if academicYear?has_content>
		<@_u page="/?academicYear=${academicYear}" />
	<#else>
		<@_u page="/" />
	</#if>
</#macro>

<#macro agentHome><@_u page="/agent"/></#macro>
<#macro agentHomeYears relationshipType><@_u page="/agent/${relationshipType.urlPart}"/></#macro>
<#macro agentHomeForYear relationshipType academicYear><@_u page="/agent/${relationshipType.urlPart}/${academicYear.startYear?c}"/></#macro>
<#macro agentStudent relationshipType academicYear student><@_u page="/agent/${relationshipType.urlPart}/${academicYear.startYear?c}/${student.universityId}"/></#macro>
<#macro agentRecord relationshipType academicYear student returnTo><@_u page="/agent/${relationshipType.urlPart}/${academicYear.startYear?c}/${student.universityId}/record?returnTo=${returnTo}"/></#macro>
<#macro agentRecordPoints relationshipType academicYear point returnTo><@_u page="/agent/${relationshipType.urlPart}/${academicYear.startYear?c}/point/${point.id}?returnTo=${returnTo}"/></#macro>

<#macro manageHome><@_u page="/manage"/></#macro>
<#macro manageHomeYears department><@_u page="/manage/${department.code}"/></#macro>
<#macro manageHomeForYear department academicYear><@_u page="/manage/${department.code}/${academicYear.startYear?c}"/></#macro>

<#macro manageNewScheme department academicYear><@_u page="/manage/${department.code}/${academicYear.startYear?c}/new"/></#macro>
<#macro manageAddStudents scheme>
	<@_u page="/manage/${scheme.department.code}/${scheme.academicYear.startYear?c}/new/${scheme.id}/students"/>
</#macro>
<#macro manageAddStudentsAllStudents scheme>
	<@_u page="/manage/${scheme.department.code}/${scheme.academicYear.startYear?c}/new/${scheme.id}/students/all"/>
</#macro>
<#macro manageNewSchemeAddPoints scheme>
	<@_u page="/manage/${scheme.department.code}/${scheme.academicYear.startYear?c}/new/${scheme.id}/points"/>
</#macro>

<#macro manageEditScheme department academicYear scheme><@_u page="/manage/${department.code}/${academicYear.startYear?c}/${scheme.id}/edit"/></#macro>
<#macro manageEditSchemeStudents department academicYear scheme><@_u page="/manage/${department.code}/${academicYear.startYear?c}/${scheme.id}/edit/students"/></#macro>
<#macro manageEditSchemePoints department academicYear scheme><@_u page="/manage/${department.code}/${academicYear.startYear?c}/${scheme.id}/edit/points"/></#macro>
<#macro manageDeleteScheme department academicYear scheme><@_u page="/manage/${department.code}/${academicYear.startYear?c}/${scheme.id}/delete"/></#macro>

<#macro manageSelectStudents scheme>
	<@_u page="/manage/${scheme.department.code}/${scheme.academicYear.startYear?c}/${scheme.id}/students/select"/>
</#macro>

<#macro manageAddPoints department academicYear><@_u page="/manage/${department.code}/${academicYear.startYear?c}/addpoints"/></#macro>
<#macro manageAddPointsBlank department academicYear><@_u page="/manage/${department.code}/${academicYear.startYear?c}/addpoints/new"/></#macro>
<#macro manageAddPointsCopy department academicYear><@_u page="/manage/${department.code}/${academicYear.startYear?c}/addpoints/copy"/></#macro>
<#macro manageAddPointsTemplate department academicYear><@_u page="/manage/${department.code}/${academicYear.startYear?c}/addpoints/template"/></#macro>

<#macro manageEditPoints department academicYear schemesParam="">
	<#if schemesParam?has_content><#local schemesParam = "?" + schemesParam /></#if>
	<@_u page="/manage/${department.code}/${academicYear.startYear?c}/editpoints${schemesParam}"/></#macro>
<#macro manageEditPoint point filterQuery returnTo><@_u page="/manage/${point.scheme.department.code}/${point.scheme.academicYear.startYear?c}/editpoints/${point.id}/edit?returnTo=${returnTo}&${filterQuery}"/></#macro>
<#macro manageDeletePoint point filterQuery returnTo><@_u page="/manage/${point.scheme.department.code}/${point.scheme.academicYear.startYear?c}/editpoints/${point.id}/delete?returnTo=${returnTo}&${filterQuery}"/></#macro>

<#macro noteView academicYear student point returnTo=""><@_u page="/note/${academicYear.startYear?c}/${student.universityId}/${point.id}/?returnTo=${returnTo}" /></#macro>
<#macro groupsNoteView student occurrence returnTo=""><@_u context="/groups" page="/note/${student.universityId}/${occurrence.id}/?returnTo=${returnTo}" /></#macro>
<#macro noteEdit academicYear student point><@_u page="/note/${academicYear.startYear?c}/${student.universityId}/${point.id}/edit"/></#macro>
<#macro bulkNoteEdit academicYear point students><@_u page="/note/${academicYear.startYear?c}/bulk/${point.id}/edit"/><#list students as student><#if student_index ==0>?<#else>&</#if>students=${student.universityId}</#list></#macro>

<#macro profile member><@_u page="/view/${member.universityId}" context="/profiles"/></#macro>
<#macro profileHome><@_u page="/profile"/></#macro>
<#macro profileRecord student academicYear returnTo="">
	<#local returnTo><#if returnTo?has_content>?returnTo=${returnTo}</#if></#local>
	<@_u page="/profile/${student.universityId}/${academicYear.startYear?c}/record${returnTo}"/>
</#macro>
<#macro profileMeetings student academicYear point><@_u page="/profile/${student.universityId}/${academicYear.startYear?c}/${point.id}/meetings"/></#macro>
<#macro profileGroups student academicYear point><@_u page="/profile/${student.universityId}/${academicYear.startYear?c}/${point.id}/groups"/></#macro>

<#macro viewHome><@_u page="/view"/></#macro>
<#macro viewHomeYears department><@_u page="/view/${department.code}"/></#macro>
<#macro viewHomeForYear department academicYear><@_u page="/view/${department.code}/${academicYear.startYear?c}"/></#macro>

<#macro viewReport department academicYear queryString><@_u page="/view/${department.code}/${academicYear.startYear?c}/report?${queryString}"/></#macro>
<#macro viewReportConfirm department academicYear><@_u page="/view/${department.code}/${academicYear.startYear?c}/report/confirm"/></#macro>

<#macro viewStudents department academicYear queryString="" page="" sortOrder="">
	<#local args = [] />
	<#if queryString?has_content>
		<#local args = args + [queryString] />
	</#if>
	<#if page?has_content>
		<#local args = args + ["page=" + page ] />
	</#if>
	<#if sortOrder?has_content>
		<#local args = args + ["sortOrder=" + sortOrder ] />
	</#if>
	<#local query><#if args?has_content>?</#if><#list args as arg>${arg}<#if arg_has_next>&</#if></#list></#local>
	<@_u page="/view/${department.code}/${academicYear.startYear?c}/students${query}"/>
</#macro>
<#macro viewSingleStudent department academicYear student><@_u page="/view/${department.code}/${academicYear.startYear?c}/students/${student.universityId}" /></#macro>
<#macro viewRecordStudent department academicYear student returnTo="">
	<#local returnTo><#if returnTo?has_content>?returnTo=${returnTo}</#if></#local>
	<@_u page="/view/${department.code}/${academicYear.startYear?c}/students/${student.universityId}/record${returnTo}"/>
</#macro>

<#macro viewPoints department academicYear filterQuery="">
	<#local filterQuery><#if filterQuery?has_content>?${filterQuery}&hasBeenFiltered=true</#if></#local>
	<@_u page="/view/${department.code}/${academicYear.startYear?c}/points${filterQuery}"/>
</#macro>
<#macro viewRecordPoints department academicYear point queryString returnTo><@_u page="/view/${department.code}/${academicYear.startYear?c}/points/${point.id}/record?returnTo=${returnTo?url}&hasBeenFiltered=true&${queryString}"/></#macro>

<#macro viewAgentsHome department academicYear><@_u page="/view/${department.code}/${academicYear.startYear?c}/agents"/></#macro>
<#macro viewAgents department academicYear relationshipType><@_u page="/view/${department.code}/${academicYear.startYear?c}/agents/${relationshipType.urlPart}"/></#macro>
<#macro viewAgent department academicYear relationshipType agent><@_u page="/view/${department.code}/${academicYear.startYear?c}/agents/${relationshipType.urlPart}/${agent.universityId}"/></#macro>
