${actor.fullName} has <#if verb == 'created'>scheduled a<#else>${verb} a scheduled</#if> ${role} meeting with ${student.fullName} on your behalf:

<#if meetingRecord??>
	${meetingRecord.title!'A meeting you no longer have permission to view'} on ${dateTimeFormatter.print(meetingRecord.meetingDate)}
<#else>
	This meeting no longer exists.
</#if>
