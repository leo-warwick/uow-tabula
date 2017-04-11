${actor.fullName} has <#if verb == 'created'><#if ((actor.warwickId)!"") == ((meetingRecord.relationship.studentId)!"")>requested<#else>scheduled</#if> a<#else>${verb} a</#if> ${role} meeting with you:

<#if meetingRecord??>
	${meetingRecord.title!'A meeting you no longer have permission to view'} on ${dateTimeFormatter.print(meetingRecord.meetingDate)}
<#else>
	This meeting no longer exists.
</#if>