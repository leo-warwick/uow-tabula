${actor.fullName} has recorded that a scheduled ${role} meeting with you did not take place:

${meetingRecord.title!'A meeting you no longer have permission to view'} on ${dateTimeFormatter.print(meetingRecord.meetingDate)}

<#if meetingRecord.missedReason?has_content>
	"${meetingRecord.missedReason}"
</#if>