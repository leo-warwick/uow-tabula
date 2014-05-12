<#ftl strip_text=true />
<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.

TODO grab values from the Routes object in code, as that's pretty equivalent and
	we're repeating ourselves here. OR expose Routes directly.

-->
<#macro _u page context='/profiles'><@url context=context page=page /></#macro>

<#macro home><@_u page="/" /></#macro>

<#macro deptperms department><@_u page="/department/${department.code}/permissions" context="/admin" /></#macro>
<#macro displaysettings department><@_u page="/department/${department.code}/settings/display" context="/admin" /></#macro>

<#macro search><@_u page="/search" /></#macro>
<#macro profile profile><@_u page="/view/${profile.universityId}"/></#macro>
<#macro profile_by_id student><@_u page="/view/${student}"/></#macro>
<#macro photo profile><#if ((profile.universityId)!)?has_content><@_u page="/view/photo/${profile.universityId}.jpg"/><#else><@_u resource="/static/images/no-photo.jpg" /></#if></#macro>
<#macro relationshipPhoto profile relationship><@_u page="/view/photo/${relationship.agent}.jpg"/></#macro>

<#macro filter_students department><@_u page="/department/${department.code}/students" /></#macro>

<#macro relationship_students relationshipType><@_u page="/${relationshipType.urlPart}/students" /></#macro>
<#macro relationship_agents department relationshipType><@_u page="/department/${department.code}/${relationshipType.urlPart}" /></#macro>
<#macro relationship_missing department relationshipType><@_u page="/department/${department.code}/${relationshipType.urlPart}/missing" /></#macro>
<#macro relationship_allocate department relationshipType><@_u page="/department/${department.code}/${relationshipType.urlPart}/allocate" /></#macro>
<#macro relationship_template department relationshipType><@_u page="/department/${department.code}/${relationshipType.urlPart}/template" /></#macro>

<#macro relationship_edit relationshipType scjCode currentAgent>
	<@_u page="/${relationshipType.urlPart}/${scjCode}/edit?currentAgent=${currentAgent.universityId}" />
</#macro>

<#macro relationship_edit_set relationshipType scjCode newAgent>
	<@_u page="/${relationshipType.urlPart}/${scjCode}/edit?agent=${newAgent.universityId}" />
</#macro>

<#macro relationship_edit_replace relationshipType scjCode currentAgent newAgent>
	<@_u page="/${relationshipType.urlPart}/${scjCode}/edit?currentAgent=${currentAgent.universityId}&agent=${newAgent.universityId}" />
</#macro>

<#macro relationship_edit_no_agent relationshipType scjCode>
	<@_u page="/${relationshipType.urlPart}/${scjCode}/add" />
</#macro>

<#macro meeting_record scjCode relationshipType>
	<@_u page="/${relationshipType.urlPart}/meeting/${scjCode}/create" />
</#macro>
<#macro edit_meeting_record scjCode meeting_record>
	<@_u page="/${meeting_record.relationship.relationshipType.urlPart}/meeting/${scjCode}/edit/${meeting_record.id}" />
</#macro>

<#macro delete_meeting_record meeting_record><@_u page="/${meeting_record.relationship.relationshipType.urlPart}/meeting/${meeting_record.id}/delete" /></#macro>
<#macro restore_meeting_record meeting_record><@_u page="/${meeting_record.relationship.relationshipType.urlPart}/meeting/${meeting_record.id}/restore" /></#macro>
<#macro purge_meeting_record meeting_record><@_u page="/${meeting_record.relationship.relationshipType.urlPart}/meeting/${meeting_record.id}/purge" /></#macro>
<#macro save_meeting_approval meeting_record><@_u page="/${meeting_record.relationship.relationshipType.urlPart}/meeting/${meeting_record.id}/approval" /></#macro>

<#macro download_meeting_record_attachment relationshipType meeting><@_u page="/${relationshipType.urlPart}/meeting/${meeting.id}/"/></#macro>

<#macro create_scheduled_meeting_record scjCode relationshipType><@_u page="/${relationshipType.urlPart}/meeting/${scjCode}/schedule/create" /></#macro>
<#macro edit_scheduled_meeting_record meetingRecord scjCode relationshipType><@_u page="/${relationshipType.urlPart}/meeting/${scjCode}/schedule/${meetingRecord.id}/edit" /></#macro>
<#macro choose_action_scheduled_meeting_record meetingRecord scjCode relationshipType><@_u page="/${relationshipType.urlPart}/meeting/${scjCode}/schedule/${meetingRecord.id}/chooseaction" /></#macro>
<#macro confirm_scheduled_meeting_record meetingRecord scjCode relationshipType><@_u page="/${relationshipType.urlPart}/meeting/${scjCode}/schedule/${meetingRecord.id}/confirm" /></#macro>
<#macro missed_scheduled_meeting_record meetingRecord relationshipType><@_u page="/${relationshipType.urlPart}/meeting/${meetingRecord.id}/missed" /></#macro>

<#macro relationship_search><@_u page="/relationships/agents/search" /></#macro>
<#macro relationship_search_json><@_u page="/relationships/agents/search.json" /></#macro>

<#macro smallgroup group><@_u page="/groups/${group.id}/view" /></#macro>

<#macro create_member_note profile><@_u page="/${profile.universityId}/note/add" /></#macro>
<#macro edit_member_note memberNote><@_u page="/${memberNote.member.universityId}/note/${memberNote.id}/edit" /></#macro>
<#macro delete_member_note memberNote ><@_u page="/${memberNote.member.universityId}/note/${memberNote.id}/delete" /></#macro>
<#macro restore_member_note memberNote ><@_u page="/${memberNote.member.universityId}/note/${memberNote.id}/restore" /></#macro>
<#macro purge_member_note memberNote ><@_u page="/${memberNote.member.universityId}/note/${memberNote.id}/purge" /></#macro>
<#macro download_member_note_attachment memberNote><@_u page="/notes/${memberNote.id}/" /></#macro>

<#macro meeting_will_create_checkpoint student><@_u page="/profile/${student.universityId}/meetingcheckpoint" context="/attendance" /></#macro>

<#macro timetable_ical student webcal=true><#compress>
	<#local https_url><@_u page="/timetable/ical?timetableHash=${student.timetableHash}" /></#local>
	<#if webcal>
		${https_url?replace('https','webcals')}
	<#else>
		${https_url}
	</#if>
</#compress></#macro>
<#macro timetable_ical_regenerate><@_u page="/timetable/regeneratehash" /></#macro>

<#macro mrm_link studentCourseDetails studentCourseYearDetails>
	<a href="https://mrm.warwick.ac.uk/mrm/student/student.htm?sprCode=${((studentCourseDetails.sprCode)!)?url}&acYear=${((studentCourseYearDetails.academicYear.toString)!)?url}">
</#macro>

<#macro permissions scope><@_u page="/permissions/${scope.urlCategory}/${scope.urlSlug}" context="/admin" /></#macro>